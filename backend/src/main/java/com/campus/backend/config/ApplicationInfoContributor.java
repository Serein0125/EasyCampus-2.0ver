package com.campus.backend.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationInfoContributor implements InfoContributor {
    
    private final Environment environment;
    
    public ApplicationInfoContributor(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", environment.getProperty("app.name", "校园二手交易平台"));
        appInfo.put("version", environment.getProperty("app.version", "1.0.0"));
        appInfo.put("description", environment.getProperty("app.description", "大学生课程期末作业项目"));
        appInfo.put("startup_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        appInfo.put("active_profile", environment.getActiveProfiles().length > 0 ? 
            String.join(",", environment.getActiveProfiles()) : "default");
        
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("java_version", System.getProperty("java.version"));
        systemInfo.put("java_vendor", System.getProperty("java.vendor"));
        systemInfo.put("os_name", System.getProperty("os.name"));
        systemInfo.put("os_version", System.getProperty("os.version"));
        systemInfo.put("user_name", System.getProperty("user.name"));
        
        Map<String, Object> dbInfo = new HashMap<>();
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null) {
            dbInfo.put("url", maskSensitiveInfo(dbUrl));
            dbInfo.put("driver", environment.getProperty("spring.datasource.driver-class-name"));
        }
        
        builder.withDetail("application", appInfo)
               .withDetail("system", systemInfo)
               .withDetail("database", dbInfo);
    }
    
    private String maskSensitiveInfo(String url) {
        // 隐藏密码信息
        if (url.contains("password=")) {
            return url.replaceAll("password=[^&]*", "password=*****");
        }
        return url;
    }
}