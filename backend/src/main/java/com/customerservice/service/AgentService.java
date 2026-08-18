package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.customerservice.common.BusinessException;
import com.customerservice.entity.SysUser;
import com.customerservice.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AgentService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public IPage<SysUser> page(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getRole, "AGENT");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getId);
        return sysUserMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void create(SysUser agent) {
        if (!StringUtils.hasText(agent.getUsername())) {
            throw new BusinessException("登录名不能为空");
        }
        if (!StringUtils.hasText(agent.getPassword())) {
            throw new BusinessException("密码不能为空");
        }
        Long exists = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, agent.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("登录名已存在");
        }
        agent.setId(null);
        agent.setRole("AGENT");
        agent.setPassword(encoder.encode(agent.getPassword()));
        if (agent.getStatus() == null) {
            agent.setStatus(1);
        }
        if (!StringUtils.hasText(agent.getNickname())) {
            agent.setNickname(agent.getUsername());
        }
        sysUserMapper.insert(agent);
    }

    public void update(Long id, SysUser agent) {
        SysUser existing = sysUserMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("客服不存在");
        }
        agent.setId(id);
        // 更新时不修改登录名与角色
        agent.setUsername(null);
        agent.setRole(null);
        if (StringUtils.hasText(agent.getPassword())) {
            agent.setPassword(encoder.encode(agent.getPassword()));
        } else {
            agent.setPassword(null);
        }
        sysUserMapper.updateById(agent);
    }

    public void toggleStatus(Long id) {
        SysUser agent = sysUserMapper.selectById(id);
        if (agent == null) {
            throw new BusinessException("客服不存在");
        }
        agent.setStatus(agent.getStatus() != null && agent.getStatus() == 1 ? 0 : 1);
        sysUserMapper.updateById(agent);
    }

    public void delete(Long id) {
        sysUserMapper.deleteById(id);
    }
}
