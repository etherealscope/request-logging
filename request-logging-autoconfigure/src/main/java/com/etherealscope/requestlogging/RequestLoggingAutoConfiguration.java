package com.etherealscope.requestlogging;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Servlet;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnProperty(name = "ethereal.request-logging.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@EnableConfigurationProperties(RequestLoggingProperties.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class RequestLoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RequestLoggingFilter.class)
    public RequestLoggingFilter requestLoggingFilter(RequestLoggingProperties requestLoggingProperties) {
        return new RequestLoggingFilter(requestLoggingProperties);
    }

}
