package com.example.chatbot.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageResponse<T> {
    private List<T> content;
    private int currentPage;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public PageResponse(List<T> content, int currentPage, int pageSize, long totalElements) {
        this.content = content;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = (int) Math.ceil((double) totalElements / pageSize);
    }
} 