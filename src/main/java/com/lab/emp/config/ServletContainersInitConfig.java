package com.lab.emp.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;

/**
 * 替代 web.xml 的 Servlet 3.0 编程式初始化：
 * 注册 DispatcherServlet、根容器与 Web 子容器、字符编码过滤器。
 * <p>
 * 由 Servlet 容器通过 SPI 自动发现（SpringServletContainerInitializer），无需 web.xml。
 * </p>
 */
public class ServletContainersInitConfig extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** 根容器：数据源、事务、Service、Mapper */
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{SpringConfig.class};
    }

    /** Web 子容器：Controller、拦截器、切面、消息转换器 */
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{SpringMvcConfig.class};
    }

    /** DispatcherServlet 映射根路径，接管全部请求 */
    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    /** 统一 UTF-8 编码，避免请求体与响应中的中文乱码 */
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceRequestEncoding(true);
        encodingFilter.setForceResponseEncoding(true);
        return new Filter[]{encodingFilter};
    }
}