package com.novobanco.accounts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;


@Component
public class StartupLogger {

    private static final Logger LOG = LoggerFactory.getLogger(StartupLogger.class);

    private final Environment environment;

    @Value("${server.port:8080}")
    private String port;

    public StartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0
                ? String.join(", ", activeProfiles).toUpperCase()
                : "DEFAULT (desarrollo local)";

        LOG.info("╔══════════════════════════════════════════════════════╗");
        LOG.info("║     NovoBanco — Account Service iniciado             ║");
        LOG.info("╠══════════════════════════════════════════════════════╣");
        LOG.info("║  Ambiente   : {}",      padRight(profile, 36) + "   ║");
        LOG.info("║  Puerto     : {}",      padRight(port, 36) + "   ║");
        LOG.info("║  Swagger UI : http://localhost:{}/swagger-ui.html", port);
        LOG.info("║  API Base   : http://localhost:{}/api/v1", port);
        LOG.info("╚══════════════════════════════════════════════════════╝");
    }

    private String padRight(String text, int length) {
        if (text.length() >= length) {
            return text.substring(0, length);
        }
        return text + " ".repeat(length - text.length());
    }
}
