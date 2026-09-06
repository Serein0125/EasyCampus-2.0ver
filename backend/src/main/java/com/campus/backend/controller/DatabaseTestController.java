package com.campus.backend.controller;

import com.campus.backend.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "数据库连接测试等调试接口")
@org.springframework.context.annotation.Profile("dev")
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db/status")
    @Operation(summary = "检查数据库连接状态")
    public Result<Map<String, Object>> checkDbStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("connected", true);
            result.put("message", "数据库连接正常");

            DatabaseMetaData metaData = jdbcTemplate.getDataSource().getConnection().getMetaData();
            result.put("databaseProductName", metaData.getDatabaseProductName());
            result.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            result.put("driverName", metaData.getDriverName());
            result.put("driverVersion", metaData.getDriverVersion());
            result.put("url", metaData.getURL());
            result.put("username", metaData.getUserName());

            Map<String, String> tableCounts = new HashMap<>();
            String[] tables = {"users", "products", "categories", "chat_conversations", "chat_messages", "favorites"};
            for (String table : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
                    tableCounts.put(table, count != null ? String.valueOf(count) : "0");
                } catch (Exception e) {
                    tableCounts.put(table, "查询失败: " + e.getMessage());
                }
            }
            result.put("tableCounts", tableCounts);

            return Result.success(result);
        } catch (Exception e) {
            result.put("connected", false);
            result.put("message", "数据库连接失败");
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return Result.error("数据库连接测试失败: " + e.getMessage());
        }
    }

    @GetMapping("/db/query")
    @Operation(summary = "执行简单数据库查询测试")
    public Result<Map<String, Object>> testQuery(
            @RequestParam(defaultValue = "users") String table,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (limit > 20) {
                limit = 20;
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT * FROM " + table + " LIMIT ?", limit);
            result.put("table", table);
            result.put("rowCount", rows.size());
            result.put("data", rows);
            return Result.success(result);
        } catch (Exception e) {
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/db/ping")
    @Operation(summary = "数据库ping测试")
    public Result<String> ping() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", String.class);
            return new Result<>(200, "PONG - 数据库连接正常", "PONG - 数据库连接正常");
        } catch (Exception e) {
            return Result.error("PONG失败: " + e.getMessage());
        }
    }
}
