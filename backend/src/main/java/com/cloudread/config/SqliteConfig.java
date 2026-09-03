package com.cloudread.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class SqliteConfig implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqliteConfig.class);

    private final JdbcTemplate jdbcTemplate;

    public SqliteConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("PRAGMA journal_mode=WAL");
        jdbcTemplate.execute("PRAGMA busy_timeout=5000");
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");
        jdbcTemplate.execute("PRAGMA synchronous=NORMAL");
        ensureColumn("sys_user", "avatar_url", "TEXT");
        String wal = jdbcTemplate.queryForObject("PRAGMA journal_mode", String.class);
        log.info("SQLite journal_mode = {}", wal);
    }

    /**
     * SQLite 不支持 ADD COLUMN IF NOT EXISTS，启动时检测并补齐新增列。
     */
    private void ensureColumn(String table, String column, String type) {
        try {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(" + table + ")");
            boolean exists = columns.stream().anyMatch(c -> column.equals(String.valueOf(c.get("name"))));
            if (!exists) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
                log.info("已为表 {} 新增列 {}", table, column);
            }
        } catch (Exception e) {
            log.warn("检查/新增列 {}.{} 失败: {}", table, column, e.getMessage());
        }
    }
}
