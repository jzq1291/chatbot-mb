package com.example.chatbot.service.impl;

import com.example.chatbot.dto.AuthRequest;
import com.example.chatbot.dto.AuthResponse;
import com.example.chatbot.entity.User;
import com.example.chatbot.entity.UserRole;
import com.example.chatbot.exception.BusinessException;
import com.example.chatbot.exception.ErrorCode;
import com.example.chatbot.mapper.UserMapper;
import com.example.chatbot.security.JwtTokenProvider;
import com.example.chatbot.service.AuthService;
import com.example.chatbot.service.TokenBlacklistService;
import com.example.chatbot.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final UserService userService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userService.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            String token = jwtTokenProvider.generateToken(authentication);
            List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
            return new AuthResponse(token, user.getUsername(), roles);
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public AuthResponse register(AuthRequest request) {
        if (userMapper.existsByUsername(request.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (userMapper.existsByEmail(request.getEmail()) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setRoles(Set.of(UserRole.ROLE_USER));

        userService.createUser(user);
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = jwtTokenProvider.generateToken(authentication);
        List<String> roles = user.getRoles().stream()
            .map(Enum::name)
            .collect(Collectors.toList());
        return new AuthResponse(token, user.getUsername(), roles);
    }

    @Override
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // 获取 token 的过期时间
            long expirationTime = jwtTokenProvider.getExpirationTime(token);
            // 将 token 加入黑名单
            tokenBlacklistService.addToBlacklist(token, expirationTime);
        }
    }
} 