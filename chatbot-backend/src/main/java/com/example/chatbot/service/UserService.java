package com.example.chatbot.service;
import com.example.chatbot.dto.PageResponse;
import com.example.chatbot.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    PageResponse<User> getAllUsers(int page, int size);
    User getUserById(Long id);
    User createUser(User user);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    Optional<User> findByUsername(String username);
}