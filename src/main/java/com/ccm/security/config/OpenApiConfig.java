package com.ccm.security.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Bean
    GroupedOpenApi openapi() {
        return GroupedOpenApi.builder()
                .group("CCM-Security")
                .displayName("CCM Security")
                .pathsToMatch("/**")
                // Ensure this package exists in your project
                .packagesToScan("com.ccm.security.controller")
                .build();
    }

    @Bean
    OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("CCM Security API")
                        .description("Microservice for Security Management")
                        .contact(new Contact())
                        .version("1.0"))
                // Setting the server URL with contextPath ensures Try It Out works correctly
                .servers(Collections.singletonList(new Server().url(contextPath)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}