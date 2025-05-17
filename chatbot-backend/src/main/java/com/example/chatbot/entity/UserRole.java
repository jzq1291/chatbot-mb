package com.example.chatbot.entity;

import lombok.Getter;

@Getter
public enum UserRole {
    ROLE_ADMIN("管理员"),
    ROLE_USER("普通用户"),
    ROLE_KNOWLEDGEMANAGER("知识库管理员");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

}