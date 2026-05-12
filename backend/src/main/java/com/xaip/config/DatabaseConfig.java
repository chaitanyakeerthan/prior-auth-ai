package com.xaip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${SPRING_DATASOURCE_URL:}")
    private String rawUrl;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = rawUrl;
        String username = dbUsername;
        String password = dbPassword;

        // Handle the various URL formats Render/Heroku provide
        if (url != null && !url.isEmpty()) {
            // Convert postgresql:// or postgres:// to jdbc:postgresql://
            if (url.startsWith("postgresql://") || url.startsWith("postgres://")) {
                try {
                    // Normalize the prefix for URI parsing if needed
                    String uriString = url.startsWith("postgresql://") ? url.replace("postgresql://", "postgres://") : url;
                    URI dbUri = new URI(uriString);
                    
                    String host = dbUri.getHost();
                    int port = dbUri.getPort();
                    String dbName = dbUri.getPath().substring(1);
                    
                    if (dbUri.getUserInfo() != null) {
                        username = dbUri.getUserInfo().split(":")[0];
                        password = dbUri.getUserInfo().split(":")[1];
                    }
                    
                    url = "jdbc:postgresql://" + host + ":" + (port == -1 ? 5432 : port) + "/" + dbName;
                } catch (Exception e) {
                    // If parsing fails, at least try to prepend jdbc:
                    if (!url.startsWith("jdbc:")) {
                        url = "jdbc:" + url;
                    }
                }
            } else if (!url.startsWith("jdbc:")) {
                url = "jdbc:postgresql://" + url;
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
