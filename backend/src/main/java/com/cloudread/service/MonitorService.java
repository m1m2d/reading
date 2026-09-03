package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.PageResult;
import com.cloudread.dto.monitor.FrontendReportRequest;
import com.cloudread.entity.FrontendMonitor;
import com.cloudread.entity.SystemLog;
import com.cloudread.mapper.FrontendMonitorMapper;
import com.cloudread.mapper.SystemLogMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

@Service
public class MonitorService {

    private final MeterRegistry meterRegistry;
    private final HikariDataSource dataSource;
    private final SystemLogMapper systemLogMapper;
    private final FrontendMonitorMapper frontendMonitorMapper;
    private final Path dataDir;

    public MonitorService(MeterRegistry meterRegistry,
                          HikariDataSource dataSource,
                          SystemLogMapper systemLogMapper,
                          FrontendMonitorMapper frontendMonitorMapper,
                          @Value("${app.data-dir}") String dataDir) {
        this.meterRegistry = meterRegistry;
        this.dataSource = dataSource;
        this.systemLogMapper = systemLogMapper;
        this.frontendMonitorMapper = frontendMonitorMapper;
        this.dataDir = Paths.get(dataDir).toAbsolutePath().normalize();
    }

    public Map<String, Object> backendMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("memoryUsedMb", round(mb(sumGauges("jvm.memory.used"))));
        jvm.put("memoryMaxMb", round(mb(sumGauges("jvm.memory.max"))));
        jvm.put("memoryCommittedMb", round(mb(sumGauges("jvm.memory.committed"))));
        jvm.put("processCpu", round(gauge("process.cpu.usage") * 100));
        jvm.put("systemCpu", round(gauge("system.cpu.usage") * 100));
        jvm.put("threads", (long) gauge("jvm.threads.live"));
        jvm.put("gcPauseCount", (long) sumCounters("jvm.gc.pause"));
        jvm.put("gcPauseTimeMs", round(sumCountersSeconds("jvm.gc.pause") * 1000));
        jvm.put("uptimeSeconds", (long) gauge("process.uptime"));
        result.put("jvm", jvm);

        Map<String, Object> http = new LinkedHashMap<>();
        Collection<Timer> timers = meterRegistry.find("http.server.requests").timers();
        long total = 0;
        long errors = 0;
        double maxMs = 0;
        double totalSeconds = 0;
        for (Timer timer : timers) {
            long count = timer.count();
            total += count;
            String status = timer.getId().getTag("status");
            if (status != null && status.startsWith("5")) {
                errors += count;
            }
            maxMs = Math.max(maxMs, timer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            totalSeconds += timer.totalTime(java.util.concurrent.TimeUnit.SECONDS);
        }
        http.put("totalRequests", total);
        http.put("errorRequests", errors);
        http.put("successRate", total == 0 ? 100.0 : round((total - errors) * 100.0 / total));
        http.put("maxResponseMs", round(maxMs));
        http.put("avgResponseMs", total == 0 ? 0 : round(totalSeconds * 1000 / total));
        result.put("http", http);

        Map<String, Object> db = new LinkedHashMap<>();
        db.put("dbFileMb", round(mb(fileSize(dataDir.resolve("cloudread.db")))));
        db.put("walFileMb", round(mb(fileSize(dataDir.resolve("cloudread.db-wal")))));
        db.put("activeConnections", dataSource.getHikariPoolMXBean().getActiveConnections());
        db.put("idleConnections", dataSource.getHikariPoolMXBean().getIdleConnections());
        db.put("totalConnections", dataSource.getHikariPoolMXBean().getTotalConnections());
        db.put("pendingConnections", dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
        Long slow = systemLogMapper.selectCount(new LambdaQueryWrapper<SystemLog>()
                .eq(SystemLog::getModule, "DB").eq(SystemLog::getLevel, "WARN"));
        db.put("slowQueries", slow == null ? 0 : slow);
        result.put("db", db);

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    public void reportFrontend(FrontendReportRequest request, Long userId) {
        FrontendMonitor monitor = new FrontendMonitor();
        monitor.setUserId(userId);
        monitor.setPageUrl(request.getPageUrl());
        monitor.setFcpMs(request.getFcpMs());
        monitor.setLcpMs(request.getLcpMs());
        monitor.setJsErrors(request.getJsErrors() == null ? 0 : request.getJsErrors());
        monitor.setApiTotal(request.getApiTotal() == null ? 0 : request.getApiTotal());
        monitor.setApiFail(request.getApiFail() == null ? 0 : request.getApiFail());
        monitor.setUserAgent(request.getUserAgent());
        frontendMonitorMapper.insert(monitor);
    }

    public PageResult<FrontendMonitor> frontendList(long page, long size) {
        Page<FrontendMonitor> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        Page<FrontendMonitor> result = frontendMonitorMapper.selectPage(p,
                new LambdaQueryWrapper<FrontendMonitor>().orderByDesc(FrontendMonitor::getId));
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    public PageResult<SystemLog> systemLogs(String level, String keyword, long page, long size) {
        Page<SystemLog> p = new Page<>(Math.max(1, page), Math.min(200, Math.max(1, size)));
        LambdaQueryWrapper<SystemLog> wrapper = new LambdaQueryWrapper<>();
        if (level != null && !level.isBlank()) {
            wrapper.eq(SystemLog::getLevel, level.toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(SystemLog::getMessage, keyword.trim());
        }
        wrapper.orderByDesc(SystemLog::getId);
        Page<SystemLog> result = systemLogMapper.selectPage(p, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    private double sumGauges(String name) {
        return meterRegistry.find(name).gauges().stream().mapToDouble(Gauge::value).sum();
    }

    private double gauge(String name) {
        return meterRegistry.find(name).gauges().stream().mapToDouble(Gauge::value).findFirst().orElse(0);
    }

    private double sumCounters(String name) {
        return meterRegistry.find(name).counters().stream().mapToDouble(c -> c.count()).sum();
    }

    private double sumCountersSeconds(String name) {
        return meterRegistry.find(name).timers().stream()
                .mapToDouble(t -> t.totalTime(java.util.concurrent.TimeUnit.SECONDS)).sum();
    }

    private long fileSize(Path path) {
        try {
            return Files.exists(path) ? Files.size(path) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double mb(double bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
