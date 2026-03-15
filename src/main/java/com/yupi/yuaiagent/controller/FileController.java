package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 提供智能体生成文件的下载（如 PDF），供前端打开/下载。
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final String PDF_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";

    /**
     * 下载智能体生成的 PDF 文件。
     * 前端可用：/api/files/pdf?name=太原小店区约会计划.pdf 或 name=xxx.pdf
     *
     * @param name 文件名（与生成时一致，支持中文，会做 URL 解码）
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPdf(@RequestParam("name") String name) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(name.trim(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = name.trim();
        }
        // 防止路径穿越，只允许文件名（不含 / \）
        if (decoded.contains("..") || decoded.contains("/") || decoded.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        Path base = Paths.get(PDF_DIR);
        Path resolved = base.resolve(decoded).normalize();
        if (!resolved.startsWith(base)) {
            return ResponseEntity.badRequest().build();
        }
        File file = resolved.toFile();
        if (!file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodeRfc5987(decoded))
                .body(resource);
    }

    private static String encodeRfc5987(String s) {
        try {
            return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
