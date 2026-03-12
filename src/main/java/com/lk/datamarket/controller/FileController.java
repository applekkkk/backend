package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${file.upload-path}")
    private String uploadPath;

    @PostMapping("/upload")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        if (!originalFilename.toLowerCase().endsWith(".csv")) {
            return Result.error("仅支持上传 .csv 文件");
        }

        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String savedName = UUID.randomUUID().toString().replace("-", "") + ".csv";
            Path target = dir.resolve(savedName).normalize();

            file.transferTo(target.toFile());

            UploadResponse resp = new UploadResponse();
            resp.setOriginalName(originalFilename);
            resp.setSavedName(savedName);
            resp.setSavedPath(target.toString());
            return Result.success(resp);
        } catch (IOException e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String cleanName = StringUtils.cleanPath(name);
        if (cleanName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path target = dir.resolve(cleanName).normalize();
            if (!target.startsWith(dir)) {
                return ResponseEntity.badRequest().build();
            }
            if (!Files.exists(target)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(target.toUri());
            String contentType = Files.probeContentType(target);
            if (contentType == null || contentType.isEmpty()) {
                contentType = "application/octet-stream";
            }
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanName + "\"")
                .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @Data
    public static class UploadResponse {
        private String originalName;
        private String savedName;
        private String savedPath;
    }
}
