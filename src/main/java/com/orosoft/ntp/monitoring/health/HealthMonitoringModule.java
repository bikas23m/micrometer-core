package com.orosoft.ntp.monitoring.health;


import com.orosoft.ntp.monitoring.config.HealthConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Health Monitoring Module - Framework Independent Micrometer Integration
 *
 * @author bikas
 */
public class HealthMonitoringModule {

    private final Logger logger = LoggerFactory.getLogger(HealthMonitoringModule.class);

    private final HealthConfig config;
    private final PrometheusMeterRegistry registry;
    private HttpServer httpServer;
    private volatile boolean running = false;

    /**
     * Initialize with default configuration
     */
    public HealthMonitoringModule() {
        this(new HealthConfig());
    }

    /**
     * Initialize with custom configuration
     */
    public HealthMonitoringModule(HealthConfig config) {
        this.config = config;
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        configureCommonTags();
        registerMetrics();
    }

    /**
     * Add common tags to all metrics
     */
    private void configureCommonTags() {
        registry.config().commonTags(
                "application", config.getApplicationName(),
                "instance", getHostname()
        );
    }

    /**
     * Register JVM and system metrics based on configuration
     */
    private void registerMetrics() {
        if (config.isEnableMemoryMetrics()) {
            new JvmMemoryMetrics().bindTo(registry);
            logger.debug("JVM Memory metrics enabled");
        }

        if (config.isEnableGcMetrics()) {
            new JvmGcMetrics().bindTo(registry);
            logger.debug("GC metrics enabled");
        }

        if (config.isEnableThreadMetrics()) {
            new JvmThreadMetrics().bindTo(registry);
            new JvmThreadDeadlockMetrics().bindTo(registry);
            logger.debug("Thread metrics enabled");
        }

        if (config.isEnableClassLoaderMetrics()) {
            new ClassLoaderMetrics().bindTo(registry);
            logger.debug("ClassLoader metrics enabled");
        }

        if (config.isEnableProcessorMetrics()) {
            new ProcessorMetrics().bindTo(registry);
            logger.debug("Processor metrics enabled");
        }

        if (config.isEnableJvmMetrics()) {
            new JvmInfoMetrics().bindTo(registry);
            logger.debug("JVM Info metrics enabled");
        }
    }

    /**
     * Start the monitoring module
     * - Registers metrics
     * - Starts HTTP server (if enabled)
     */
    public void start() throws IOException {
        if (running) {
            logger.debug("Health monitoring module already running");
            return;
        }

        if (config.isEnableServer()) {
            startHttpServer();
        }

        running = true;
        logger.debug("Health Monitoring Module started successfully");
        logger.debug("Application: " + config.getApplicationName());
        if (config.isEnableServer()) {
            logger.debug("Metrics URL: http://localhost:" + config.getServerPort()
                    + config.getMetricsEndpoint());
            logger.debug("Health URL: http://localhost:" + config.getServerPort()
                    + config.getHealthEndpoint());
        }
    }

    /**
     * Start embedded HTTP server for metrics exposition
     */
    private void startHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(config.getServerPort()), 0);

        // Metrics endpoint - Prometheus format
        httpServer.createContext(config.getMetricsEndpoint(), exchange -> {
            try {
                String response = registry.scrape();
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type",
                        "text/plain; version=0.0.4; charset=utf-8");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                handleError(exchange, e);
            }
        });

        // Health check endpoint
        httpServer.createContext(config.getHealthEndpoint(), exchange -> {
            try {
                String health = "{"
                        + "\"status\":\"UP\","
                        + "\"application\":\"" + config.getApplicationName() + "\","
                        + "\"timestamp\":" + System.currentTimeMillis()
                        + "}";
                byte[] responseBytes = health.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                handleError(exchange, e);
            }
        });

        // Root endpoint - info page
        httpServer.createContext("/", exchange -> {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                String info = "<html><body>"
                        + "<h1>Health Monitoring Module</h1>"
                        + "<p>Application: " + config.getApplicationName() + "</p>"
                        + "<p><a href='" + config.getMetricsEndpoint() + "'>Metrics</a></p>"
                        + "<p><a href='" + config.getHealthEndpoint() + "'>Health</a></p>"
                        + "</body></html>";
                byte[] responseBytes = info.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });

        httpServer.setExecutor(Executors.newFixedThreadPool(2));
        httpServer.start();

        logger.debug("HTTP server started on port {}", config.getServerPort());
    }

    /**
     * Handle HTTP errors gracefully
     */
    private void handleError(HttpExchange exchange, Exception e) {
        try {
            String error = "{\"error\":\"" + e.getMessage() + "\"}";
            byte[] errorBytes = error.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(500, errorBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorBytes);
            }
        } catch (IOException ioException) {
            logger.error("Error handling error: ", ioException);
        }
    }

    /**
     * Stop the monitoring module gracefully
     */
    public void stop() {
        if (!running) {
            return;
        }

        if (httpServer != null) {
            httpServer.stop(0);
            logger.debug("HTTP server stopped");
        }

        registry.close();
        running = false;
        logger.debug("Health Monitoring Module stopped");
    }

    /**
     * Get the Prometheus registry for custom metric registration
     */
    public PrometheusMeterRegistry getRegistry() {
        return registry;
    }

    /**
     * Get the meter registry for metric registration
     */
    public MeterRegistry getMeterRegistry() {
        return registry;
    }

    /**
     * Scrape current metrics (useful for custom integrations)
     */
    public String scrapeMetrics() {
        return registry.scrape();
    }

    /**
     * Get hostname for tagging
     */
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Add shutdown hook for graceful termination
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }
}

