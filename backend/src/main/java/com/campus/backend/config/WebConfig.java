package com.campus.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public WebConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .resourceChain(true);

        registry.addResourceHandler("/vite.svg")
                .addResourceLocations("classpath:/static/vite.svg");

        // 上传文件静态资源映射（使用绝对路径）
        String uploadPath = Paths.get(fileStorageProperties.getBasePath()).toAbsolutePath().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .resourceChain(true);

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaPathResourceResolver());
    }

    private static class SpaPathResourceResolver extends PathResourceResolver {
        @Override
        public Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource resource = location.createRelative(resourcePath);
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }

            if (!resourcePath.startsWith("api/") &&
                !resourcePath.startsWith("v2/") &&
                !resourcePath.startsWith("v3/") &&
                !resourcePath.startsWith("uploads/") &&
                !resourcePath.contains(".") &&
                !resourcePath.startsWith("ws") &&
                !resourcePath.startsWith("error")) {
                return new ClassPathResource("/static/index.html");
            }

            return null;
        }
    }
}
