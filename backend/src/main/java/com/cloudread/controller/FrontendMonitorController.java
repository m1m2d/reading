package com.cloudread.controller;

import com.cloudread.common.Result;
import com.cloudread.dto.monitor.FrontendReportRequest;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "前端监控埋点")
@RestController
@RequestMapping("/api/v1/monitor/frontend")
public class FrontendMonitorController {

    private final MonitorService monitorService;

    public FrontendMonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @Operation(summary = "上报前端性能与错误指标")
    @PostMapping("/report")
    public Result<Void> report(@RequestBody FrontendReportRequest request) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        monitorService.reportFrontend(request, userId);
        return Result.ok();
    }
}
