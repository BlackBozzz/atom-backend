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
    @ConfigurationProperties(prefix = "app.legacy-db")
    public DataSource externalDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "legacyJdbc")
    public JdbcTemplate legacyJdbc(@Qualifier("externalDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}