package com.atomskills.atombackend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class MainDbConfig {

    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public HikariDataSource dataSource() {
        return new HikariDataSource();
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate mainJdbcTemplate(
            @Qualifier("dataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }
}