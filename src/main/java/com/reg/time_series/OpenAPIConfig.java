package com.reg.time_series;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TimeSeries API")
                        .version("1.0")
                        .description("""
                            API documentation for the TimeSeries application.
                            <br><br>
                            <a href="/" style="font-size: 16px; padding: 10px; 
                            background-color: #4CAF50; color: white; 
                            text-decoration: none; border-radius: 4px;">
                            ← Back to Main Page</a>
                            """)
                                .contact(new Contact()
                                .name("Ács Géza")
                                .email("acsgeza@gmail.com")));
    }
}
