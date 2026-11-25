package com.familyhome.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Family Home API",
                        version = "1.0",
                        description =
                                "REST API for household tasks, weekly statistics, shopping lists, events, member management, and vehicle bookings."))
public class OpenApiConfig {}
