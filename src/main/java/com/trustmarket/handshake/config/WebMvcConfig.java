package com.trustmarket.handshake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * Serve files from the local upload directory under /uploads/**
     * so that stored image URLs resolve to actual files.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + uploadDir.replaceAll("/$", "") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}

