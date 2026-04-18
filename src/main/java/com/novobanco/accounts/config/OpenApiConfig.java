package com.novobanco.accounts.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novoBancoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NovoBanco — Account Service API")
                        .description("Microservicio de Cuentas y Transacciones. " +
                                "Arquitectura Hexagonal con PostgreSQL 16.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("NovoBanco Engineering")
                                .email("engineering@novobanco.com"))
                        .license(new License().name("Private")));
    }
}
