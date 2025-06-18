package com.example.chatbot.service.impl;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {"ID", "标题", "分类", "内容", "创建时间", "更新时间"};
    // SXSSF默认内存行数

    @Override
    public ResponseEntity<byte[]> downloadExcel(List<KnowledgeBase> knowledgeList, boolean useNio) {
        try {
            String method = useNio ? "NIO" : "BIO";
            log.info("开始{}方式下载Excel文件，数据量: {}", method, knowledgeList.size());
            
            if (knowledgeList.isEmpty()) {
                log.warn("没有数据可导出");
                return ResponseEntity.noContent().build();
            }
            
            // 创建Excel内容
            byte[] excelData = exportToExcel(knowledgeList, useNio);
            return createDownloadResponse(excelData, method);
            
        } catch (Exception e) {
            String method = useNio ? "NIO" : "BIO";
            log.error("{}方式下载Excel文件失败", method, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Excel导出（BIO或NIO方式）
     */
    private byte[] exportToExcel(List<KnowledgeBase> knowledgeList, boolean useNio) {
        String method = useNio ? "NIO" : "BIO";
        log.info("开始使用{}方式导出Excel，数据量: {}", method, knowledgeList.size());
        long startTime = System.currentTimeMillis();
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // 创建Excel内容
            createExcelContent(workbook, knowledgeList);
            
            // 根据方式选择写入方法
            if (useNio) {
                writeWithNio(workbook, outputStream);
            } else {
                writeWithBio(workbook, outputStream);
            }
            
            byte[] result = outputStream.toByteArray();
            
            long endTime = System.currentTimeMillis();
            log.info("{}方式导出Excel完成，耗时: {}ms，文件大小: {} bytes", 
                method, endTime - startTime, result.length);
            
            return result;
            
        } catch (IOException e) {
            log.error("{}方式导出Excel失败", method, e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * BIO方式写入
     */
    private void writeWithBio(Workbook workbook, ByteArrayOutputStream outputStream) {
        try {
            workbook.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException("BIO写入失败", e);
        }
    }

    /**
     * NIO方式写入
     */
    private void writeWithNio(Workbook workbook, ByteArrayOutputStream outputStream) {
        try {
            // 先将workbook写入临时ByteArrayOutputStream
            ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
            workbook.write(tempStream);
            tempStream.flush();
            
            // 使用NIO Channel进行数据传输
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(tempStream.toByteArray());
                 ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
                 WritableByteChannel outputChannel = Channels.newChannel(outputStream)) {
                
                ByteBuffer buffer = ByteBuffer.allocate(8192); // 8KB缓冲区
                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();
                    outputChannel.write(buffer);
                    buffer.clear();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("NIO写入失败", e);
        }
    }

    /**
     * 创建Excel内容
     */
    private void createExcelContent(Workbook workbook, List<KnowledgeBase> knowledgeList) {
        Sheet sheet = workbook.createSheet("知识库数据");
        
        // 创建标题行
        createHeaderRow(sheet, workbook);
        
        // 填充数据行
        fillDataRows(sheet, knowledgeList, workbook);
        
        // 自动调整列宽
        autoSizeColumns(sheet);
    }

    /**
     * 创建标题行
     */
    private void createHeaderRow(Sheet sheet, Workbook workbook) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        Row headerRow = sheet.createRow(0);
        
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * 填充数据行
     */
    private void fillDataRows(Sheet sheet, List<KnowledgeBase> knowledgeList, Workbook workbook) {
        for (int i = 0; i < knowledgeList.size(); i++) {
            KnowledgeBase knowledge = knowledgeList.get(i);
            Row row = sheet.createRow(i + 1);
            
            fillRowData(row, knowledge, workbook);
        }
    }

    /**
     * 填充单行数据
     */
    private void fillRowData(Row row, KnowledgeBase knowledge, Workbook workbook) {
        // ID
        row.createCell(0).setCellValue(knowledge.getId() != null ? knowledge.getId() : 0);
        
        // 标题
        row.createCell(1).setCellValue(knowledge.getTitle() != null ? knowledge.getTitle() : "");
        
        // 分类
        row.createCell(2).setCellValue(knowledge.getCategory() != null ? knowledge.getCategory() : "");
        
        // 内容（设置自动换行）
        Cell contentCell = row.createCell(3);
        contentCell.setCellValue(knowledge.getContent() != null ? knowledge.getContent() : "");
        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);
        contentCell.setCellStyle(wrapStyle);
        
        // 创建时间
        row.createCell(4).setCellValue(knowledge.getCreatedAt() != null ? 
            knowledge.getCreatedAt().format(DATE_FORMATTER) : "");
        
        // 更新时间
        row.createCell(5).setCellValue(knowledge.getUpdatedAt() != null ? 
            knowledge.getUpdatedAt().format(DATE_FORMATTER) : "");
    }

    /**
     * 自动调整列宽
     */
    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    /**
     * 创建下载响应
     */
    private ResponseEntity<byte[]> createDownloadResponse(byte[] excelData, String method) {
        // 生成文件名
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "知识库数据_" + method + "_" + timestamp + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedFilename);
        headers.setContentLength(excelData.length);
        
        log.info("{}方式导出Excel文件成功，数据量: {}，文件大小: {} bytes", 
            method, excelData.length, excelData.length);
        
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }
    
    /**
     * 创建标题行样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    @Override
    public ResponseEntity<StreamingResponseBody> downloadCsv(List<KnowledgeBase> knowledgeList) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "知识库数据_CSV_" + timestamp + ".csv";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", encodedFilename);
        StreamingResponseBody responseBody = outputStream -> {
            try {
                // 写入UTF-8 BOM，确保Excel正确识别编码
                outputStream.write(new byte[] {(byte)0xEF, (byte)0xBB, (byte)0xBF});
                // 写表头
                String headerLine = String.join(",", HEADERS) + "\n";
                outputStream.write(headerLine.getBytes(StandardCharsets.UTF_8));
                // 写数据
                for (KnowledgeBase kb : knowledgeList) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(kb.getId() != null ? kb.getId() : "").append(",");
                    sb.append(escapeCsv(kb.getTitle())).append(",");
                    sb.append(escapeCsv(kb.getCategory())).append(",");
                    sb.append(escapeCsv(kb.getContent())).append(",");
                    sb.append(kb.getCreatedAt() != null ? kb.getCreatedAt().format(DATE_FORMATTER) : "").append(",");
                    sb.append(kb.getUpdatedAt() != null ? kb.getUpdatedAt().format(DATE_FORMATTER) : "");
                    sb.append("\n");
                    outputStream.write(sb.toString().getBytes(StandardCharsets.UTF_8));
                }
                outputStream.flush();
            } catch (Exception e) {
                log.error("CSV流式导出失败", e);
            }
        };
        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }

    /**
     * CSV字段转义
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r") || v.contains("\"")) {
            return '"' + v + '"';
        }
        return v;
    }
} 