package com.cloudread.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String coverDir;
    private final String imageDir;
    private final String avatarDir;

    public WebMvcConfig(@Value("${app.upload-dir}") String uploadDir,
                        @Value("${app.cover-dir}") String coverDir,
                        @Value("${app.image-dir}") String imageDir,
                        @Value("${app.avatar-dir}") String avatarDir) {
        this.uploadDir = uploadDir;
        this.coverDir = coverDir;
        this.imageDir = imageDir;
        this.avatarDir = avatarDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/files/books/**")
                .addResourceLocations(dirLocation(uploadDir));
        registry.addResourceHandler("/api/v1/files/covers/**")
                .addResourceLocations(dirLocation(coverDir));
        registry.addResourceHandler("/api/v1/files/images/**")
                .addResourceLocations(dirLocation(imageDir));
        registry.addResourceHandler("/api/v1/files/avatars/**")
                .addResourceLocations(dirLocation(avatarDir));
    }

    private String dirLocation(String dir) {
        String location = Paths.get(dir).toAbsolutePath().normalize().toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        return location;
    }
}
