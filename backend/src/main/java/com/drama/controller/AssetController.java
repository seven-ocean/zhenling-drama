package com.drama.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.drama.common.R;
import com.drama.entity.Asset;
import com.drama.service.AssetService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 素材接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping("/upload")
    public R<Asset> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dramaId", required = false) String dramaId,
            @RequestParam(value = "type", required = false) String type) {
        return R.ok(assetService.upload(file, dramaId, type));
    }

    @GetMapping("/drama/{dramaId}")
    public R<List<Asset>> listByDramaId(
            @PathVariable String dramaId,
            @RequestParam(required = false) String type) {
        return R.ok(assetService.listByDramaId(dramaId, type));
    }

    @GetMapping
    public R<IPage<Asset>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String dramaId,
            @RequestParam(required = false) String type) {
        return R.ok(assetService.page(pageNum, pageSize, dramaId, type));
    }

    @GetMapping("/{id}")
    public R<Asset> getById(@PathVariable String id) {
        return R.ok(assetService.getById(id));
    }

    @PostMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        assetService.delete(id);
        return R.ok();
    }

    /**
     * 本地文件下载/访问（支持图片预览、视频播放等）
     * 路径格式: /api/v1/assets/download/{basePath}/{datePath}/{filename}
     */
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // 获取请求路径中 /download/ 之后的部分
            String requestUri = request.getRequestURI();
            String relativePath = requestUri.substring(requestUri.indexOf("/download/") + "/download/".length());

            // 安全检查：防止路径穿越攻击
            if (relativePath.contains("..") || relativePath.startsWith("/")) {
                return ResponseEntity.badRequest().build();
            }

            Path filePath = Paths.get("./data/storage", relativePath).normalize();
            // 二次安全检查：确保最终路径仍在 storage 目录内
            Path baseDir = Paths.get("./data/storage").normalize().toAbsolutePath();

            if (!filePath.toAbsolutePath().startsWith(baseDir)) {
                log.warn("Blocked path traversal attempt: {}", relativePath);
                return ResponseEntity.status(403).build();
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("File not found: {}", filePath);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String filename = filePath.getFileName().toString();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + encodedFilename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Download file error", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}