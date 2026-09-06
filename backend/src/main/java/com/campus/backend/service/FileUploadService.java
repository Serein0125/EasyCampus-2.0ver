package com.campus.backend.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface FileUploadService {
    /** 上传单张图片 */
    String uploadImage(MultipartFile file, Long userId);

    /** 批量上传图片 */
    List<String> uploadImages(List<MultipartFile> files, Long userId);

    /** 上传头像（额外裁剪压缩） */
    String uploadAvatar(MultipartFile file, Long userId);

    /** 上传分片 */
    String uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks, String fileName, Long userId);

    /** 合并分片 */
    String mergeChunks(String fileId, String fileName, int totalChunks, Long userId);
}
