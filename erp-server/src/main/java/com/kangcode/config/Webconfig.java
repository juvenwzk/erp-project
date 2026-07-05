package com.kangcode.config;


import com.kangcode.Service.UploadStorageService;
import com.kangcode.interceptor.TokenInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Webconfig implements WebMvcConfigurer {

    @Autowired
    private TokenInterceptor tokenInterceptor;

    @Autowired
    private UploadStorageService uploadStorageService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/uploads/**",
                        "/assets/**",
                        "/actuator/**",
                        "/*.html",
                        "/favicon.ico",
                        "/index.html",
                        "/login.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadStorageService.getLocalRoot() + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
