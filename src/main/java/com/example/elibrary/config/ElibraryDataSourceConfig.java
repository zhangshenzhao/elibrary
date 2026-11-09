package com.example.elibrary.config;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源配置：elibrary 单数据源（Druid 连接池）。
 * 配置前缀 spring.datasource.druid.elibrary，与 iaas-batch 的 MysqlDataSourceConfig 同模式。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Configuration
public class ElibraryDataSourceConfig {

    @Primary
    @Bean(name = "elibraryDataSource")
    @Qualifier("elibraryDataSource")
    @ConfigurationProperties("spring.datasource.druid.elibrary")
    public DataSource dataSourceElibrary() {
        return DruidDataSourceBuilder.create().build();
    }
}
