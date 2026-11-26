package com.groupt1.hms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static assets (css/js/images/fonts/uploads) from the frontend template folder.
        // Add both relative paths to support running from Maven CLI (subfolder) or IntelliJ (root folder).
        // Note: the dynamic pages (/, /index.html, /rooms.html, /login.html, ...) are handled by
        // Spring MVC controllers, which take precedence over this catch-all resource handler.
        registry.addResourceHandler("/**")
                .addResourceLocations("file:../free-hotel-website-template/")
                .addResourceLocations("file:free-hotel-website-template/")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.setCacheSeconds(0);
        interceptor.setUseExpiresHeader(true);
        interceptor.setUseCacheControlHeader(true);
        interceptor.setUseCacheControlNoStore(true);
        
        // Apply no-cache to all dynamic endpoints, but exclude static assets
        registry.addInterceptor(interceptor)
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/fonts/**", "/style/**");
    }
}

