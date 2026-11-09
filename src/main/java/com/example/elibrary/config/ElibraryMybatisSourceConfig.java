package com.example.elibrary.config;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * MyBatis 装配：扫描 com.example.elibrary.mapper，加载 classpath*:mapper/*.xml，
 * 提供 SqlSessionFactory / 事务管理器 / SqlSessionTemplate。
 * 与 iaas-batch 的 IaasBatchMybatisSourceConfig 同模式，单数据源加 @Primary。
 *
 * @author makejava
 * @since 2026-11-07
 */
@Configuration
@MapperScan(basePackages = {"com.example.elibrary.mapper"}, sqlSessionTemplateRef = "elibrarySqlSessionTemplate")
public class ElibraryMybatisSourceConfig {

    @Resource
    @Qualifier("elibraryDataSource")
    private DataSource elibraryDataSource;

    // 数据库并发闸门：Semaphore(10) 限制并发访问 MySQL 的数量
    @Resource
    private DbConcurrencyInterceptor dbConcurrencyInterceptor;

    @Primary
    @Bean(name = "elibrarySqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(elibraryDataSource);
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        // 注册插件：所有 SQL 执行都先过信号量闸门
        bean.setPlugins(new Interceptor[]{dbConcurrencyInterceptor});
        return bean.getObject();
    }

    @Primary
    @Bean("elibraryTransactionManager")
    public DataSourceTransactionManager transactionManager() {
        return new DataSourceTransactionManager(elibraryDataSource);
    }

    @Primary
    @Bean("elibrarySqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(
            @Qualifier("elibrarySqlSessionFactory") SqlSessionFactory sessionFactory) {
        return new SqlSessionTemplate(sessionFactory);
    }
}
