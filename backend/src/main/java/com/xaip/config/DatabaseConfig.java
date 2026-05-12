package com.xaip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Bean
    public DataSource dataSource() {
        String url = datasourceUrl;
        String username = dbUsername;
        String password = dbPassword;

        // If Render/Heroku style postgres:// URL is provided, convert to JDBC
        if (url != null && url.startsWith("postgres://")) {
            try {
                URI dbUri = new URI(url);
                String host = dbUri.getHost();
                int port = dbUri.getPort();
                String dbName = dbUri.getPath().substring(1);
                
                if (dbUri.getUserInfo() != null) {
                    username = dbUri.getUserInfo().split(":")[0];
                    password = dbUri.getUserInfo().split(":")[1];
                }
                
                url = "jdbc:postgresql://" + host + ":" + (port == -1 ? 5432 : port) + "/" + dbName;
            } catch (URISyntaxException e) {
                // Fallback to original if parsing fails
            }
        }

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
