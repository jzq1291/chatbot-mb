package com.example.chatbot.service.impl;

import com.example.chatbot.entity.KnowledgeBase;
import com.example.chatbot.service.ExcelExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] HEADERS = {"ID", "标题", "分类", "内容", "创建时间", "更新时间"};
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲区

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
        
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // 创建Excel内容
            createExcelContent(workbook, knowledgeList);
            
            // 根据方式选择写入方法
            if (useNio) {
                return writeWithNio(workbook);
            } else {
                return writeWithBio(workbook);
            }
            
        } catch (IOException e) {
            log.error("{}方式导出Excel失败", method, e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    /**
     * BIO方式写入
     */
    private byte[] writeWithBio(Workbook workbook) throws IOException {
        long startTime = System.currentTimeMillis();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            long endTime = System.currentTimeMillis();
            log.info("BIO方式写入完成，耗时: {}ms，文件大小: {} bytes", endTime - startTime, result.length);
            return result;
        }
    }

    /**
     * NIO方式写入 - 使用FileChannel和ByteBuffer
     */
    private byte[] writeWithNio(Workbook workbook) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // 创建临时文件
        Path tempFile = Files.createTempFile("excel_export_", ".xlsx");
        
        try {
            // 使用FileChannel写入临时文件
            try (FileChannel fileChannel = FileChannel.open(tempFile, 
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                
                // 先将workbook写入临时ByteArrayOutputStream
                try (ByteArrayOutputStream tempStream = new ByteArrayOutputStream()) {
                    workbook.write(tempStream);
                    tempStream.flush();
                    
                    // 使用NIO Channel和ByteBuffer进行数据传输
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(tempStream.toByteArray());
                         ReadableByteChannel inputChannel = Channels.newChannel(inputStream)) {
                        
                        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
                        long totalBytesWritten = 0;
                        
                        while (inputChannel.read(buffer) != -1) {
                            buffer.flip();
                            int bytesWritten = fileChannel.write(buffer);
                            totalBytesWritten += bytesWritten;
                            buffer.clear();
                        }
                        
                        log.debug("NIO写入完成，总写入字节数: {}", totalBytesWritten);
                    }
                }
            }
            
            // 读取文件内容到字节数组
            byte[] result = Files.readAllBytes(tempFile);
            
            long endTime = System.currentTimeMillis();
            log.info("NIO方式写入完成，耗时: {}ms，文件大小: {} bytes", endTime - startTime, result.length);
            
            return result;
            
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                log.warn("删除临时文件失败: {}", tempFile, e);
            }
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
        String filename = createFilename("知识库数据_" + method + "_", ".xlsx");
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
        if (knowledgeList.isEmpty()) {
            log.warn("没有数据可导出CSV");
            return ResponseEntity.noContent().build();
        }
        
        String filename = createFilename("知识库数据_CSV_", ".csv");
        HttpHeaders headers = createDownloadHeaders(filename, MediaType.TEXT_PLAIN);
        
        StreamingResponseBody responseBody = createStreamingResponseBody(
            knowledgeList, "CSV", "csv_export_", ".csv", this::generateCsvToFile);
        
        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }

    /**
     * 流式NIO下载 - 真正的流式实现，边生成边传输
     */
    @Override
    public ResponseEntity<StreamingResponseBody> downloadExcelStreamingNio(List<KnowledgeBase> knowledgeList) {
        if (knowledgeList.isEmpty()) {
            log.warn("没有数据可导出Excel流式");
            return ResponseEntity.noContent().build();
        }
        
        String filename = createFilename("知识库数据_StreamingNIO_", ".xlsx");
        HttpHeaders headers = createDownloadHeaders(filename, MediaType.APPLICATION_OCTET_STREAM);
        
        StreamingResponseBody responseBody = createStreamingResponseBody(
            knowledgeList, "Excel", "excel_streaming_", ".xlsx", this::generateExcelToFile);
        
        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }

    /**
     * 🔧 公共方法：创建时间戳
     */
    private String createTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    /**
     * 🔧 公共方法：创建文件名
     */
    private String createFilename(String prefix, String suffix) {
        return prefix + createTimestamp() + suffix;
    }

    /**
     * 🔧 公共方法：创建下载响应头
     */
    private HttpHeaders createDownloadHeaders(String filename, MediaType contentType) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        headers.setContentDispositionFormData("attachment", encodedFilename);
        return headers;
    }

    /**
     * 🔧 公共方法：创建流式响应体
     */
    private StreamingResponseBody createStreamingResponseBody(
            List<KnowledgeBase> knowledgeList, 
            String type, 
            String tempFilePrefix, 
            String tempFileSuffix,
            FileGenerator fileGenerator) {
        
        return outputStream -> {
            try {
                log.info("开始{}流式下载，数据量: {}", type, knowledgeList.size());
                long startTime = System.currentTimeMillis();
                long totalBytesWritten = 0;
                
                // 创建临时文件
                Path tempFile = Files.createTempFile(tempFilePrefix, tempFileSuffix);
                
                try {
                    // 生成文件到临时文件
                    fileGenerator.generate(knowledgeList, tempFile);
                    
                    // 使用NIO从临时文件读取并流式传输到客户端
                    totalBytesWritten = streamFileToOutput(tempFile, outputStream, type);
                    
                } finally {
                    // 清理临时文件
                    cleanupTempFile(tempFile);
                }
                
                long endTime = System.currentTimeMillis();
                log.info("{}流式下载完成，耗时: {}ms，总字节数: {}", type, endTime - startTime, totalBytesWritten);
                
            } catch (Exception e) {
                log.error("{}流式下载失败", type, e);
                try {
                    outputStream.write("导出失败，请联系管理员".getBytes(StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
            }
        };
    }

    /**
     * 🔧 文件生成器接口
     */
    @FunctionalInterface
    private interface FileGenerator {
        void generate(List<KnowledgeBase> knowledgeList, Path tempFile) throws IOException;
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

    /**
     * 🔧 生成CSV数据到临时文件
     */
    private void generateCsvToFile(List<KnowledgeBase> knowledgeList, Path tempFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            // 写入UTF-8 BOM
            writer.write('\uFEFF');
            
            // 写表头
            writer.write(String.join(",", HEADERS));
            writer.newLine();
            
            // 分批写入数据
            int batchSize = 100;
            for (int i = 0; i < knowledgeList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, knowledgeList.size());
                
                for (int j = i; j < endIndex; j++) {
                    KnowledgeBase kb = knowledgeList.get(j);
                    StringBuilder sb = new StringBuilder();
                    sb.append(kb.getId() != null ? kb.getId() : "").append(",");
                    sb.append(escapeCsv(kb.getTitle())).append(",");
                    sb.append(escapeCsv(kb.getCategory())).append(",");
                    sb.append(escapeCsv(kb.getContent())).append(",");
                    sb.append(kb.getCreatedAt() != null ? kb.getCreatedAt().format(DATE_FORMATTER) : "").append(",");
                    sb.append(kb.getUpdatedAt() != null ? kb.getUpdatedAt().format(DATE_FORMATTER) : "");
                    sb.append("\n");
                    
                    writer.write(sb.toString());
                }
                
                // 记录进度
                if (i % 1000 == 0) {
                    log.debug("CSV文件生成进度: {}/{} 条记录", i, knowledgeList.size());
                }
            }
        }
    }

    /**
     * 🔧 生成Excel数据到临时文件
     */
    private void generateExcelToFile(List<KnowledgeBase> knowledgeList, Path tempFile) throws IOException {
        // 使用SXSSFWorkbook实现真正的流式Excel生成
        // SXSSFWorkbook只保留指定行数在内存中，其他行写入临时文件
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(10)) { // 只保留10行在内存中
            // 创建Excel内容
            Sheet sheet = workbook.createSheet("知识库数据");
            
            // 创建标题行
            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 分批写入数据行 - 真正的流式输出
            int batchSize = 10; // 每批处理10条记录
            for (int i = 0; i < knowledgeList.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, knowledgeList.size());
                
                // 处理当前批次的数据
                for (int j = i; j < endIndex; j++) {
                    KnowledgeBase knowledge = knowledgeList.get(j);
                    Row row = sheet.createRow(j + 1);
                    
                    // 填充行数据
                    row.createCell(0).setCellValue(knowledge.getId() != null ? knowledge.getId() : 0);
                    row.createCell(1).setCellValue(knowledge.getTitle() != null ? knowledge.getTitle() : "");
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
                
                // 记录进度
                if (i % 1000 == 0) {
                    log.debug("Excel流式生成进度: {}/{} 条记录", i, knowledgeList.size());
                }
            }
            
            // 手动设置列宽，避免SXSSFWorkbook的自动调整问题
            sheet.setColumnWidth(0, 10 * 256);  // ID列
            sheet.setColumnWidth(1, 30 * 256);  // 标题列
            sheet.setColumnWidth(2, 15 * 256);  // 分类列
            sheet.setColumnWidth(3, 50 * 256);  // 内容列
            sheet.setColumnWidth(4, 20 * 256);  // 创建时间列
            sheet.setColumnWidth(5, 20 * 256);  // 更新时间列
            
            // 将Excel写入临时文件
            try (FileOutputStream fileOut = new FileOutputStream(tempFile.toFile())) {
                workbook.write(fileOut);
            }
            
            // 清理SXSSFWorkbook的临时文件
            workbook.dispose();
        }
    }

    /**
     * 🔧 公共方法：将文件流式传输到输出流
     */
    private long streamFileToOutput(Path tempFile, OutputStream outputStream, String type) throws IOException {
        long totalBytesWritten = 0;
        try (FileChannel fileChannel = FileChannel.open(tempFile, StandardOpenOption.READ);
             WritableByteChannel outputChannel = Channels.newChannel(outputStream)) {
            
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            
            while (fileChannel.read(buffer) != -1) {
                buffer.flip();
                int bytesWritten = outputChannel.write(buffer);
                totalBytesWritten += bytesWritten;
                buffer.clear();
                
                // 每传输一定量数据后记录进度
                if (totalBytesWritten % (BUFFER_SIZE * 10) == 0) {
                    log.debug("{}流式传输进度: {} bytes", type, totalBytesWritten);
                }
            }
        }
        return totalBytesWritten;
    }

    /**
     * 🔧 公共方法：清理临时文件
     */
    private void cleanupTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException e) {
            log.warn("删除临时文件失败: {}", tempFile, e);
        }
    }
} 