package com.lab.emp.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Spring 根容器配置（业务层 / 持久层）：
 * 数据源、SqlSessionFactory、Mapper 扫描、事务管理器。
 * <p>
 * 扫描范围排除 Controller 与 ControllerAdvice（由 SpringMVC 子容器负责），
 * 使父子容器职责清晰、避免同一 Bean 被创建两次。
 * </p>
 */
@Configuration
@PropertySource("classpath:jdbc.properties")
@EnableTransactionManagement
@ComponentScan(basePackages = "com.lab.emp",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Controller.class),
                @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = ControllerAdvice.class),
                // 切面由 SpringMVC 子容器加载并织入（子容器中显式开启 @EnableAspectJAutoProxy）
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.lab\\.emp\\.aspect\\..*"),
                // 关键：SpringMvcConfig 本身也是 @Configuration，必须排除，否则根容器会
                // 重复加载 @EnableWebMvc，在没有 ServletContext 的测试环境中直接启动失败
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SpringMvcConfig.class)
        })
public class SpringConfig {

    @Value("${jdbc.driver}")
    private String driver;

    @Value("${jdbc.url}")
    private String url;

    @Value("${jdbc.username}")
    private String username;

    @Value("${jdbc.password}")
    private String password;

    @Value("${jdbc.initialSize:5}")
    private int initialSize;

    @Value("${jdbc.maxActive:20}")
    private int maxActive;

    @Value("${jdbc.minIdle:5}")
    private int minIdle;

    @Value("${jdbc.maxWait:3000}")
    private long maxWait;

    @Bean(initMethod = "init", destroyMethod = "close")
    public DataSource dataSource() throws SQLException {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(driver);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(initialSize);
        dataSource.setMaxActive(maxActive);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxWait(maxWait);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        return dataSource;
    }

    /**
     * 关键点：使用 MyBatis-Plus 的 MybatisSqlSessionFactoryBean，
     * 而不是原生 SqlSessionFactoryBean —— 后者不会注入 BaseMapper 的通用 CRUD，
     * 运行期会抛 "Invalid bound statement (not found)"。
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                               com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor mybatisPlusInterceptor,
                                               com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.lab.emp.entity");
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml"));
        factoryBean.setPlugins(mybatisPlusInterceptor);

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(false);
        factoryBean.setConfiguration(configuration);

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        factoryBean.setGlobalConfig(globalConfig);

        return factoryBean.getObject();
    }

    /**
     * Mapper 接口扫描：为 com.lab.emp.mapper 下的接口生成代理对象并注册为 Bean。
     */
    @Bean
    public static MapperScannerConfigurer mapperScannerConfigurer() {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        scanner.setBasePackage("com.lab.emp.mapper");
        scanner.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return scanner;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}