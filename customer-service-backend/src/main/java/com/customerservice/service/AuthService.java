package com.customerservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.customerservice.common.BusinessException;
import com.customerservice.dto.LoginRequest;
import com.customerservice.entity.SysUser;
import com.customerservice.mapper.SysUserMapper;
import com.customerservice.util.JwtUtil;
import com.customerservice.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (user == null || !encoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    public SysUser me() {
        Long id = UserContext.userId();
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user;
    }

    public BCryptPasswordEncoder encoder() {
        return encoder;
    }
}
