package com.cloudread.common;

import com.cloudread.entity.SystemLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路追踪：为每个请求生成 TraceID，贯穿后端日志、system_log 与响应头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private final AsyncLogWriter asyncLogWriter;

    public TraceIdFilter(AsyncLogWriter asyncLogWriter) {
        this.asyncLogWriter = asyncLogWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        }
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            String uri = request.getRequestURI();
            boolean noisy = "OPTIONS".equals(request.getMethod())
                    || uri.startsWith("/ws/")
                    || uri.startsWith("/api/v1/files/")
                    || uri.startsWith("/actuator/")
                    || uri.startsWith("/v3/api-docs")
                    || uri.startsWith("/webjars/")
                    || uri.startsWith("/doc.html");
            if (!noisy) {
                SystemLog entry = new SystemLog();
                entry.setTraceId(traceId);
                entry.setLevel("INFO");
                entry.setModule("HTTP");
                entry.setMessage(request.getMethod() + " " + uri + " " + response.getStatus());
                entry.setRequestUri(uri);
                entry.setMethod(request.getMethod());
                entry.setIp(IpUtils.clientIp(request));
                entry.setCostMs((int) cost);
                asyncLogWriter.write(entry);
            }
            MDC.remove("traceId");
        }
    }
}
