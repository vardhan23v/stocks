package com.stockrecommendation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

/**
 * JSP views: a controller returning "stocks" renders /WEB-INF/jsp/stocks.jsp
 * (prefix and suffix from spring.mvc.view.* in application.properties).
 * Files under WEB-INF cannot be opened directly from a browser, only through
 * a controller. The "fmt" bean (web.Formats) is made available to every page.
 */
@Configuration
public class WebConfig {

    @Bean
    public InternalResourceViewResolver jspViewResolver(@Value("${spring.mvc.view.prefix}") String prefix,
                                                        @Value("${spring.mvc.view.suffix}") String suffix) {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver(prefix, suffix);
        resolver.setViewClass(JstlView.class);
        resolver.setExposedContextBeanNames("fmt");
        return resolver;
    }
}
