package com.example.chatbot.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface UserRoleMapper {
    
    @Insert("INSERT INTO user_roles (user_id, role) VALUES (#{userId}, #{role})")
    void insertUserRole(Long userId, String role);

    @Delete("DELETE FROM user_roles WHERE user_id = #{userId}")
    void deleteUserRoles(Long userId);

    @Select("SELECT role FROM user_roles WHERE user_id = #{userId}")
    Set<String> findRolesByUserId(Long userId);
} 