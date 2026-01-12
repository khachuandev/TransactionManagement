package com.example.Transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI(@Value("${openapi.service.title}") String title,
                           @Value("${openapi.service.version}") String version,
                           @Value("${openapi.service.description}") String description,
                           @Value("${openapi.service.serverUrl}") String serverUrl,
                           @Value("${openapi.service.serverName}") String serverName) {
        return new OpenAPI().info(new Info().title(title)
                        .version(version).description(description))
                .servers(List.of(new Server().url(serverUrl).description(serverName)));
    }
}
