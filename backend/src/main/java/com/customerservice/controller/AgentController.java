package com.customerservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.customerservice.common.BusinessException;
import com.customerservice.common.Result;
import com.customerservice.entity.SysUser;
import com.customerservice.service.AgentService;
import com.customerservice.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    private void checkAdmin() {
        if (!"ADMIN".equals(UserContext.role())) {
            throw new BusinessException(403, "无权限操作");
        }
    }

    @GetMapping
    public Result<IPage<SysUser>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(agentService.page(page, size, keyword));
    }

    @PostMapping
    public Result<Void> create(@RequestBody SysUser agent) {
        checkAdmin();
        agentService.create(agent);
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SysUser agent) {
        checkAdmin();
        agentService.update(id, agent);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        checkAdmin();
        agentService.toggleStatus(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkAdmin();
        agentService.delete(id);
        return Result.ok();
    }
}
