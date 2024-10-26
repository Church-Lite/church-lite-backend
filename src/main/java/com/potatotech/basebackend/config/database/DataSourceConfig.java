package com.potatotech.basebackend.config.database;

import com.potatotech.basebackend.config.context.ConfigContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Autowired
    ConfigContextImpl configContext;

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(configContext.getUrl());
        dataSource.setUsername(configContext.getUsername());
        dataSource.setPassword(configContext.getPasswod());
        return dataSource;
    }
}
