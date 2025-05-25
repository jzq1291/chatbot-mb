package com.example.chatbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.chatbot.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
    @Select("SELECT * FROM knowledge_base WHERE category = #{category}")
    Page<KnowledgeBase> findByCategory(Page<KnowledgeBase> page, String category);
    
    @Select("SELECT * FROM knowledge_base WHERE " +
            "LOWER(title) LIKE LOWER(#{pattern}) OR " +
            "LOWER(content) LIKE LOWER(#{pattern})")
    Page<KnowledgeBase> searchByKeyword(Page<KnowledgeBase> page, String pattern);

    @Select("<script>" +
            "SELECT * FROM knowledge_base " +
            "<where>" +
            "   <foreach item='keyword' collection='keywords' separator=' OR '>" +
            "       (LOWER(title) LIKE LOWER(CONCAT('%', #{keyword}, '%')) " +
            "        OR LOWER(content) LIKE LOWER(CONCAT('%', #{keyword}, '%')))" +
            "   </foreach>" +
            "</where>" +
            "</script>")
    List<KnowledgeBase> retrieveByKeywords(@Param("keywords") List<String> keywords);

    @Select("<script>" +
            "SELECT * FROM knowledge_base WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<KnowledgeBase> findByIds(@Param("ids") List<Long> ids);
} 