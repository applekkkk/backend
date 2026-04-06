package com.lk.datamarket.controller;

import com.lk.datamarket.common.Result;
import com.lk.datamarket.domain.DataProduct;
import com.lk.datamarket.domain.User;
import com.lk.datamarket.mapper.DataProductMapper;
import com.lk.datamarket.mapper.OrderMapper;
import com.lk.datamarket.mapper.UserMapper;
import com.lk.datamarket.utils.JwtUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg", ".webp", ".gif"));

    @Value("${file.upload-path}")
    private String uploadPath;

    @Autowired
    private DataProductMapper dataProductMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/upload")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".net")) {
            return Result.error("仅支持上传 .csv 或 .net 文件");
        }

        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            int dotIndex = originalFilename.lastIndexOf('.');
            String extension = dotIndex >= 0 ? originalFilename.substring(dotIndex).toLowerCase() : "";
            String savedName = UUID.randomUUID().toString().replace("-", "") + extension;
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

    @PostMapping("/upload-image")
    public Result<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要上传的图片");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = fileExtension(originalFilename);
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            return Result.error("仅支持上传 png/jpg/jpeg/webp/gif 图片");
        }

        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String savedName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = dir.resolve(savedName).normalize();

            file.transferTo(target.toFile());

            UploadResponse resp = new UploadResponse();
            resp.setOriginalName(originalFilename);
            resp.setSavedName(savedName);
            resp.setSavedPath(target.toString());
            return Result.success(resp);
        } catch (IOException e) {
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("name") String name, HttpServletRequest request) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String cleanName = StringUtils.cleanPath(name);
        if (cleanName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        DataProduct product = dataProductMapper.findByFileName(cleanName);
        if (product != null && !canDownloadProductFile(request, product)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    private boolean canDownloadProductFile(HttpServletRequest request, DataProduct product) {
        Long userId = parseUserIdFromToken(request);
        if (userId == null) {
            return false;
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            return false;
        }

        if (Integer.valueOf(1).equals(user.getRole())) {
            return true;
        }
        if (userId.equals(product.getAuthorId())) {
            return true;
        }
        return orderMapper.countPurchasedByUserAndProduct(userId, product.getId()) > 0;
    }

    private Long parseUserIdFromToken(HttpServletRequest request) {
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = auth.substring(7).trim();
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Object id = claims.get("id");
            if (id == null) return null;
            return Long.parseLong(String.valueOf(id));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String fileExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) return "";
        return name.substring(dotIndex).toLowerCase();
    }

    @Data
    public static class UploadResponse {
        private String originalName;
        private String savedName;
        private String savedPath;
    }
}
