package com.example.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chatbot.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    @Select("SELECT * FROM chat_messages WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY created_at ASC")
    List<ChatMessage> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, Long userId);
    
    @Select("SELECT * FROM chat_messages WHERE session_id = #{sessionId} AND user_id = #{userId} ORDER BY created_at DESC LIMIT 20")
    List<ChatMessage> findLast10BySessionIdAndUserIdOrderByCreatedAtDesc(String sessionId, Long userId);

    @Select("SELECT * FROM (SELECT DISTINCT ON (session_id) session_id, created_at FROM chat_messages WHERE user_id = #{userId} ORDER BY session_id, created_at DESC) t ORDER BY created_at DESC")
    List<String> findDistinctSessionIdByUserId(Long userId);

    @Delete("DELETE FROM chat_messages WHERE session_id = #{sessionId} AND user_id = #{userId}")
    void deleteBySessionIdAndUserId(String sessionId, Long userId);
} 