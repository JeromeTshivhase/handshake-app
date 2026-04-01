package com.trustmarket.handshake.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.http-cache.public-max-age-seconds:60}")
    private long publicApiCacheMaxAgeSeconds;

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

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!HttpMethod.GET.matches(request.getMethod())) {
                    return true;
                }

                String path = request.getRequestURI();
                boolean hasAuthHeader = request.getHeader(HttpHeaders.AUTHORIZATION) != null;
                boolean isPublicListingPath = path.startsWith("/api/listings") && !path.startsWith("/api/listings/me");
                boolean isPublicCategoryPath = path.startsWith("/api/categories");
                if (!hasAuthHeader && (isPublicListingPath || isPublicCategoryPath)) {
                    String cacheControl = CacheControl.maxAge(publicApiCacheMaxAgeSeconds, TimeUnit.SECONDS)
                            .cachePublic()
                            .mustRevalidate()
                            .getHeaderValue();
                    response.setHeader(HttpHeaders.CACHE_CONTROL, cacheControl);
                }
                return true;
            }
        });
    }
}

