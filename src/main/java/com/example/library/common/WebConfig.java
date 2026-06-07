package com.example.library.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry){
        registry.addInterceptor(new LoginInterceptor()).addPathPatterns("/**").
                excludePathPatterns(                     // 除了下面这些...
                "/login", "/register", "/doLogin", "/captcha", "/","/doRegister", "/css/**", "/js/**", "/admin.css"
        );
    }
}
