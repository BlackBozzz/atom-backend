package com.atomskills.atombackend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class ExternalDbConfig {
    @Bean(name = "externalDataSource")
    @ConfigurationProperties(prefix = "app.external-db")
    public HikariDataSource externalDataSource() {
        return new HikariDataSource();
    }

    @Bean(name = "externalJdbcTemplate")
    public JdbcTemplate externalJdbc(
            @Qualifier("externalDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}