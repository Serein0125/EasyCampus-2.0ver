package com.campus.backend.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final JdbcTemplate jdbcTemplate;
    
    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public Health health() {
        try {
            // 执行简单的数据库查询来检查连接
            Map<String, Object> result = jdbcTemplate.queryForMap("SELECT 1 as status");
            
            if (result != null && result.containsKey("status")) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "connected")
                    .withDetail("query_result", result.get("status"))
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", "Unexpected query result")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("error", e.getMessage())
                .withDetail("error_type", e.getClass().getSimpleName())
                .build();
        }
    }
}