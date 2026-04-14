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
import org.springframework.http.HttpEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/files")
public class FileController {
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg", ".webp", ".gif"));

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${python.api-base-url:http://127.0.0.1:8000}")
    private String pythonApiBaseUrl;

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

    @GetMapping("/preview")
    public Result<FilePreviewResponse> preview(@RequestParam("name") String name, HttpServletRequest request) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error("文件名不能为空");
        }
        String cleanName = StringUtils.cleanPath(name);
        if (cleanName.contains("..")) {
            return Result.error("文件名非法");
        }

        DataProduct product = dataProductMapper.findByFileName(cleanName);
        if (product != null && !canPreviewProductFile(request, product)) {
            return Result.error("无权限预览该数据");
        }

        try {
            Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path target = dir.resolve(cleanName).normalize();
            if (!target.startsWith(dir)) {
                return Result.error("文件路径非法");
            }
            if (!Files.exists(target)) {
                return Result.error("文件不存在");
            }

            String lower = cleanName.toLowerCase();
            FilePreviewResponse payload = lower.endsWith(".csv")
                    ? previewCsv(target, 10)
                    : previewLineText(target, 10);
            payload.setFileName(cleanName);
            return Result.success(payload);
        } catch (IOException e) {
            return Result.error("读取预览失败: " + e.getMessage());
        }
    }

    @GetMapping("/graph-preview")
    public ResponseEntity<byte[]> graphPreview(@RequestParam("name") String name, HttpServletRequest request) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String cleanName = StringUtils.cleanPath(name);
        if (cleanName.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        if (!cleanName.toLowerCase().endsWith(".net")) {
            return ResponseEntity.badRequest().build();
        }
        if (!isAdmin(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DataProduct product = dataProductMapper.findByFileName(cleanName);
        if (product == null) {
            return ResponseEntity.notFound().build();
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

            byte[] image = renderGraphByPython(target);
            if (image == null || image.length == 0) {
                return ResponseEntity.internalServerError().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
            return new ResponseEntity<>(image, headers, HttpStatus.OK);
        } catch (Exception e) {
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
        if (userId.equals(product.getAuthorId())) {
            return true;
        }
        return orderMapper.countPurchasedByUserAndProduct(userId, product.getId()) > 0;
    }

    private boolean canPreviewProductFile(HttpServletRequest request, DataProduct product) {
        Long userId = parseUserIdFromToken(request);
        if (userId == null) {
            return false;
        }

        User user = userMapper.findById(userId);
        return user != null;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long userId = parseUserIdFromToken(request);
        if (userId == null) {
            return false;
        }
        User user = userMapper.findById(userId);
        return user != null && Integer.valueOf(1).equals(user.getRole());
    }

    private byte[] renderGraphByPython(Path filePath) {
        RestTemplate restTemplate = new RestTemplate();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath.toFile()));
        body.add("user_id", "admin");
        body.add("options", "{\"layout\":\"force\",\"theme\":\"light\",\"description\":\"力导向图预览\"}");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        String endpoint = pythonApiBaseUrl + "/visualization/render";
        ResponseEntity<byte[]> resp = restTemplate.postForEntity(endpoint, entity, byte[].class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("python render failed");
        }
        return resp.getBody();
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

    private FilePreviewResponse previewCsv(Path target, int maxRows) throws IOException {
        try {
            return previewCsvWithCharset(target, StandardCharsets.UTF_8, maxRows);
        } catch (MalformedInputException ex) {
            return previewCsvWithCharset(target, Charset.forName("GBK"), maxRows);
        }
    }

    private FilePreviewResponse previewCsvWithCharset(Path target, Charset charset, int maxRows) throws IOException {
        FilePreviewResponse resp = new FilePreviewResponse();
        resp.setColumns(new ArrayList<>());
        resp.setRows(new ArrayList<>());
        resp.setTabular(true);

        try (BufferedReader reader = Files.newBufferedReader(target, charset)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return resp;
            }

            List<String> headers = parseCsvLine(headerLine);
            for (int i = 0; i < headers.size(); i++) {
                String col = headers.get(i) == null ? "" : headers.get(i).trim();
                resp.getColumns().add(col.isEmpty() ? ("列" + (i + 1)) : col);
            }

            int count = 0;
            String line;
            while (count < maxRows && (line = reader.readLine()) != null) {
                List<String> row = parseCsvLine(line);
                while (row.size() < resp.getColumns().size()) {
                    row.add("");
                }
                if (row.size() > resp.getColumns().size()) {
                    for (int i = resp.getColumns().size(); i < row.size(); i++) {
                        resp.getColumns().add("列" + (i + 1));
                    }
                }
                resp.getRows().add(row);
                count++;
            }
        }
        return resp;
    }

    private FilePreviewResponse previewLineText(Path target, int maxRows) throws IOException {
        try {
            return previewLineTextWithCharset(target, StandardCharsets.UTF_8, maxRows);
        } catch (MalformedInputException ex) {
            return previewLineTextWithCharset(target, Charset.forName("GBK"), maxRows);
        }
    }

    private FilePreviewResponse previewLineTextWithCharset(Path target, Charset charset, int maxRows) throws IOException {
        FilePreviewResponse resp = new FilePreviewResponse();
        resp.setTabular(false);
        resp.setColumns(new ArrayList<>(Arrays.asList("内容")));
        resp.setRows(new ArrayList<>());

        try (BufferedReader reader = Files.newBufferedReader(target, charset)) {
            int count = 0;
            String line;
            while (count < maxRows && (line = reader.readLine()) != null) {
                resp.getRows().add(new ArrayList<>(Arrays.asList(line)));
                count++;
            }
        }
        return resp;
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                result.add(cell.toString());
                cell.setLength(0);
                continue;
            }
            cell.append(ch);
        }
        result.add(cell.toString());
        return result;
    }

    @Data
    public static class UploadResponse {
        private String originalName;
        private String savedName;
        private String savedPath;
    }

    @Data
    public static class FilePreviewResponse {
        private String fileName;
        private Boolean tabular;
        private List<String> columns;
        private List<List<String>> rows;
    }
}
