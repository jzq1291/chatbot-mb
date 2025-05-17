package com.example.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chatbot.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    @Select("SELECT * FROM knowledge_base WHERE category = #{category}")
    List<KnowledgeBase> findByCategory(String category);
    
    @Select("SELECT * FROM knowledge_base WHERE " +
            "LOWER(title) LIKE LOWER(#{pattern}) OR " +
            "LOWER(content) LIKE LOWER(#{pattern})")
    List<KnowledgeBase> searchByKeyword(String pattern);
} 