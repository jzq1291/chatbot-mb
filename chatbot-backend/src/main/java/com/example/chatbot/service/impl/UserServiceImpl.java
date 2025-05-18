package com.example.chatbot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.User;
import com.example.chatbot.entity.UserRole;
import com.example.chatbot.mapper.UserMapper;
import com.example.chatbot.mapper.UserRoleMapper;
import com.example.chatbot.exception.BusinessException;
import com.example.chatbot.exception.ErrorCode;
import com.example.chatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResponse<User> getAllUsers(int page, int size) {
        Page<User> pageResult = userMapper.selectPage(new Page<>(page, size), null);
        pageResult.getRecords().forEach(this::loadUserRoles);
        return new PageResponse<>(
            pageResult.getRecords(),
            page,
            size,
            pageResult.getTotal()
        );
    }

    @Override
    public User getUserById(Long id) {
        User user = Optional.ofNullable(userMapper.selectById(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        loadUserRoles(user);
        return user;
    }

    @Override
    @Transactional
    public User createUser(User user) {
        if (userMapper.existsByUsername(user.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userMapper.existsByEmail(user.getEmail()) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userMapper.insert(user);
        
        // 保存用户角色
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> userRoleMapper.insertUserRole(user.getId(), role.name()));
        }
        
        return user;
    }

    @Override
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = getUserById(id);
        if (user.getUsername() != null && !user.getUsername().equals(existingUser.getUsername())) {
            if (userMapper.existsByUsername(user.getUsername()) > 0) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null && !user.getEmail().equals(existingUser.getEmail())) {
            if (userMapper.existsByEmail(user.getEmail()) > 0) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        userMapper.updateById(existingUser);
        
        // 更新用户角色
        if (user.getRoles() != null) {
            userRoleMapper.deleteUserRoles(id);
            user.getRoles().forEach(role -> userRoleMapper.insertUserRole(id, role.name()));
        }
        
        return getUserById(id);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRoleMapper.deleteUserRoles(id);
        if (userMapper.deleteById(id) == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        User user = userMapper.selectOne(queryWrapper);
        if (user != null) {
            loadUserRoles(user);
        }
        return Optional.ofNullable(user);
    }

    private void loadUserRoles(User user) {
        Set<String> roleNames = userRoleMapper.findRolesByUserId(user.getId());
        Set<UserRole> roles = roleNames.stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());
        user.setRoles(roles);
    }
} 