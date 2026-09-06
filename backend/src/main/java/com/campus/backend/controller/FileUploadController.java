package com.campus.backend.controller;

import com.campus.backend.common.Result;
import com.campus.backend.common.SecurityUtils;
import com.campus.backend.service.FileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "文件上传", description = "图片上传、分片上传、头像上传接口")
@RestController
@RequestMapping("/api/v2/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "上传单张图片")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        String url = fileUploadService.uploadImage(file, userId);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    @Operation(summary = "批量上传图片")
    @PostMapping("/images")
    public Result<Map<String, Object>> uploadImages(
            @RequestParam("files") List<MultipartFile> files) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<String> urls = fileUploadService.uploadImages(files, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("urls", urls);
        data.put("count", urls.size());
        return Result.success(data);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        String url = fileUploadService.uploadAvatar(file, userId);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }

    @Operation(summary = "上传分片")
    @PostMapping("/chunk")
    public Result<Map<String, Object>> uploadChunk(
            @RequestParam("chunk") MultipartFile chunk,
            @RequestParam("fileId") String fileId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileName") String fileName) {
        Long userId = SecurityUtils.getCurrentUserId();
        String message = fileUploadService.uploadChunk(chunk, fileId, chunkIndex, totalChunks, fileName, userId);
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("fileId", fileId);
        data.put("chunkIndex", chunkIndex);
        data.put("totalChunks", totalChunks);
        return Result.success(data);
    }

    @Operation(summary = "合并分片")
    @PostMapping("/merge")
    public Result<Map<String, String>> mergeChunks(
            @RequestParam("fileId") String fileId,
            @RequestParam("fileName") String fileName,
            @RequestParam("totalChunks") int totalChunks) {
        Long userId = SecurityUtils.getCurrentUserId();
        String url = fileUploadService.mergeChunks(fileId, fileName, totalChunks, userId);
        Map<String, String> data = new HashMap<>();
        data.put("url", url);
        return Result.success(data);
    }
}
