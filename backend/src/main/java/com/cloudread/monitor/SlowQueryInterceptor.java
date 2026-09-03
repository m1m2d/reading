package com.cloudread.monitor;

import com.cloudread.common.AsyncLogWriter;
import com.cloudread.entity.SystemLog;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.sql.Statement;
import java.util.Properties;

/**
 * 慢查询拦截：执行耗时超过阈值的 SQL 记入 system_log。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, org.apache.ibatis.session.ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
@Component
public class SlowQueryInterceptor implements Interceptor {

    private final AsyncLogWriter asyncLogWriter;
    private final long slowThresholdMs;

    public SlowQueryInterceptor(@Lazy AsyncLogWriter asyncLogWriter,
                                @Value("${app.slow-query-ms:500}") long slowThresholdMs) {
        this.asyncLogWriter = asyncLogWriter;
        this.slowThresholdMs = slowThresholdMs;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long cost = System.currentTimeMillis() - start;
        if (cost >= slowThresholdMs) {
            try {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = boundSql.getSql();
                if (sql.length() > 500) {
                    sql = sql.substring(0, 500) + "...";
                }
                SystemLog entry = new SystemLog();
                entry.setLevel("WARN");
                entry.setModule("DB");
                entry.setMessage("慢查询(" + cost + "ms): " + sql);
                entry.setCostMs((int) cost);
                asyncLogWriter.write(entry);
            } catch (Exception ignored) {
                // 慢查询记录失败不影响主流程
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
