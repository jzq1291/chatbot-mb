package com.example.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.chatbot.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    @Select("SELECT * FROM knowledge_base WHERE category = #{category}")
    Page<KnowledgeBase> findByCategory(Page<KnowledgeBase> page, String category);
    
    @Select("SELECT * FROM knowledge_base WHERE " +
            "LOWER(title) LIKE LOWER(#{pattern}) OR " +
            "LOWER(content) LIKE LOWER(#{pattern})")
    Page<KnowledgeBase> searchByKeyword(Page<KnowledgeBase> page, String pattern);

    @Select("SELECT * FROM knowledge_base WHERE " +
            "LOWER(title) LIKE LOWER(#{pattern}) OR " +
            "LOWER(content) LIKE LOWER(#{pattern})")
    List<KnowledgeBase> retrieveByKeyword(String pattern);
} 