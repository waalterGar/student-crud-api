package com.students.crud_students.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Student Management & Notifications API")
                        .version("1.0.0")
                        .description("REST API for student management and academic notification integration.")
                        .contact(new Contact()
                                .name("API Support Team")
                                .email("support@students.com")));
    }
}