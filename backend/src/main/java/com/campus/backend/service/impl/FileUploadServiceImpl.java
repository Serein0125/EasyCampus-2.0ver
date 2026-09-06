package com.campus.backend.service.impl;

import com.campus.backend.config.FileStorageProperties;
import com.campus.backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadServiceImpl implements FileUploadService {

    private final FileStorageProperties props;

    // 分片上传进度跟踪：fileId -> 已上传的分片索引集合
    private final ConcurrentHashMap<String, Set<Integer>> chunkProgress = new ConcurrentHashMap<>();

    private static final Map<String, byte[]> FILE_SIGNATURES = Map.of(
        "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
        "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
        "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    @Override
    public String uploadImage(MultipartFile file, Long userId) {
        validateImage(file, props.getImageMaxSize());
        String url = storeFile(file, userId);
        log.info("用户{}上传图片成功: {}", userId, url);
        return url;
    }

    @Override
    public List<String> uploadImages(List<MultipartFile> files, Long userId) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadImage(file, userId));
        }
        return urls;
    }

    @Override
    public String uploadAvatar(MultipartFile file, Long userId) {
        validateImage(file, props.getAvatarMaxSize());
        String url = storeAndCompressAvatar(file, userId);
        log.info("用户{}上传头像成功: {}", userId, url);
        return url;
    }

    @Override
    public String uploadChunk(MultipartFile chunk, String fileId, int chunkIndex, int totalChunks, String fileName, Long userId) {
        // 验证文件名安全性
        validateFileName(fileName);

        // 创建临时目录
        Path tempDir = resolveTempPath().resolve(fileId);
        try {
            Files.createDirectories(tempDir);
        } catch (AccessDeniedException e) {
            log.error("分片上传临时目录无写权限, tempDir={}, userId={}", tempDir, userId, e);
            throw new RuntimeException("分片上传失败: 服务器临时目录无写入权限，请联系管理员", e);
        } catch (IOException e) {
            log.error("创建分片临时目录失败, tempDir={}, userId={}", tempDir, userId, e);
            throw new RuntimeException("分片上传失败: 无法创建临时目录(" + e.getMessage() + ")", e);
        }

        // 保存分片
        Path chunkPath = tempDir.resolve(String.valueOf(chunkIndex));
        try {
            Files.copy(chunk.getInputStream(), chunkPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("保存分片失败", e);
        }

        // 记录进度
        chunkProgress.computeIfAbsent(fileId, k -> ConcurrentHashMap.newKeySet()).add(chunkIndex);

        Set<Integer> uploaded = chunkProgress.get(fileId);
        log.info("用户{}上传分片 {}/{}, 文件ID: {}", userId, uploaded.size(), totalChunks, fileId);

        return String.format("分片 %d/%d 上传成功", uploaded.size(), totalChunks);
    }

    @Override
    public String mergeChunks(String fileId, String fileName, int totalChunks, Long userId) {
        validateFileName(fileName);

        Path tempDir = resolveTempPath().resolve(fileId);
        if (!Files.exists(tempDir)) {
            throw new RuntimeException("分片文件不存在，请先上传分片");
        }

        Set<Integer> uploaded = chunkProgress.getOrDefault(fileId, Collections.emptySet());
        if (uploaded.size() != totalChunks) {
            throw new RuntimeException(String.format("分片不完整，已上传%d/%d", uploaded.size(), totalChunks));
        }

        // 生成目标文件路径
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(fileName);
        String newFileName = UUID.randomUUID().toString() + "." + ext;
        Path targetDir = resolveBasePath().resolve(datePath);

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(newFileName);

            // 合并分片
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkPath = tempDir.resolve(String.valueOf(i));
                    Files.copy(chunkPath, out);
                }
            }

            // 验证合并后的文件
            byte[] header = readFileHeader(targetFile);
            String detectedType = detectFileType(header);
            if (detectedType == null) {
                Files.deleteIfExists(targetFile);
                throw new RuntimeException("合并后的文件不是有效的图片格式");
            }

            // 压缩图片
            compressImage(targetFile, ext);

            // 清理临时文件
            cleanupTempDir(tempDir, fileId);

            String url = "/uploads/" + datePath + "/" + newFileName;
            log.info("用户{}合并分片上传成功: {}", userId, url);
            return url;
        } catch (AccessDeniedException e) {
            log.error("合并分片时目标目录无写权限, targetDir={}, userId={}", targetDir, userId, e);
            throw new RuntimeException("合并分片失败: 服务器上传目录无写入权限，请联系管理员", e);
        } catch (IOException e) {
            log.error("合并分片失败, targetDir={}, userId={}", targetDir, userId, e);
            throw new RuntimeException("合并分片失败: " + e.getMessage(), e);
        }
    }

    // ========== 私有方法 ==========

    private void validateImage(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > maxSize) {
            throw new RuntimeException(String.format("文件大小超过限制（最大%dMB）", maxSize / 1024 / 1024));
        }

        // 验证 Content-Type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(props.getAllowedTypes().split(","));
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new RuntimeException("不支持的图片格式，仅支持 JPG/PNG/WEBP");
        }

        // 验证文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }
        validateFileName(originalFilename);
        String ext = getExtension(originalFilename).toLowerCase();
        List<String> allowedExts = Arrays.asList(props.getAllowedExtensions().split(","));
        if (!allowedExts.contains(ext)) {
            throw new RuntimeException("不支持的文件扩展名，仅支持 " + props.getAllowedExtensions());
        }

        // 验证文件头魔数
        try {
            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                int read = is.read(header);
                if (read < 4) {
                    throw new RuntimeException("无法读取文件头");
                }
            }
            String detectedType = detectFileType(header);
            if (detectedType == null) {
                throw new RuntimeException("文件内容不是有效的图片格式");
            }
        } catch (IOException e) {
            throw new RuntimeException("文件验证失败", e);
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("文件名不能为空");
        }
        // 防止路径遍历攻击
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new RuntimeException("文件名包含非法字符");
        }
        // 防止特殊字符
        if (!fileName.matches("^[a-zA-Z0-9._\\-\\u4e00-\\u9fa5]+$")) {
            throw new RuntimeException("文件名包含非法字符");
        }
    }

    private String detectFileType(byte[] header) {
        // JPEG variant with JFIF/EXIF
        if (header.length >= 2 &&
            (header[0] & 0xFF) == 0xFF &&
            (header[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (header.length >= 4 &&
            (header[0] & 0xFF) == 0x89 &&
            (header[1] & 0xFF) == 0x50 &&
            (header[2] & 0xFF) == 0x4E &&
            (header[3] & 0xFF) == 0x47) {
            return "image/png";
        }
        if (header.length >= 4 &&
            (header[0] & 0xFF) == 0x52 &&
            (header[1] & 0xFF) == 0x49 &&
            (header[2] & 0xFF) == 0x46 &&
            (header[3] & 0xFF) == 0x46) {
            return "image/webp";
        }
        return null;
    }

    private byte[] readFileHeader(Path file) throws IOException {
        byte[] header = new byte[8];
        try (InputStream is = Files.newInputStream(file)) {
            is.read(header);
        }
        return header;
    }

    private Path resolveBasePath() {
        Path base = Paths.get(props.getBasePath());
        if (!base.isAbsolute()) {
            base = base.toAbsolutePath();
        }
        return base;
    }

    private Path resolveTempPath() {
        Path temp = Paths.get(props.getTempPath());
        if (!temp.isAbsolute()) {
            temp = temp.toAbsolutePath();
        }
        return temp;
    }

    private String storeFile(MultipartFile file, Long userId) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + ext;

        Path targetDir;
        try {
            targetDir = resolveBasePath().resolve(datePath);
            Files.createDirectories(targetDir);
        } catch (AccessDeniedException e) {
            log.error("上传目录无写权限, basePath={}, userId={}", props.getBasePath(), userId, e);
            throw new RuntimeException("文件存储失败: 服务器上传目录无写入权限，请联系管理员", e);
        } catch (IOException e) {
            log.error("创建上传目录失败, basePath={}, userId={}", props.getBasePath(), userId, e);
            throw new RuntimeException("文件存储失败: 无法创建上传目录(" + e.getMessage() + ")", e);
        }

        Path targetFile = targetDir.resolve(newFileName);
        try {
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("写入文件失败, targetFile={}, userId={}", targetFile, userId, e);
            // 清理可能已创建的空文件
            try { Files.deleteIfExists(targetFile); } catch (IOException ignored) {}
            throw new RuntimeException("文件存储失败: 写入文件时发生错误(" + e.getMessage() + ")", e);
        }

        // 压缩图片（压缩失败不影响上传结果，仅记录日志）
        compressImage(targetFile, ext);

        log.info("用户{}上传图片成功: {} (size={}B)", userId, targetFile, file.getSize());
        return "/uploads/" + datePath + "/" + newFileName;
    }

    private String storeAndCompressAvatar(MultipartFile file, Long userId) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + ext;

        Path targetDir;
        try {
            targetDir = resolveBasePath().resolve(datePath);
            Files.createDirectories(targetDir);
        } catch (AccessDeniedException e) {
            log.error("头像上传目录无写权限, basePath={}, userId={}", props.getBasePath(), userId, e);
            throw new RuntimeException("头像上传失败: 服务器上传目录无写入权限，请联系管理员", e);
        } catch (IOException e) {
            log.error("创建头像上传目录失败, basePath={}, userId={}", props.getBasePath(), userId, e);
            throw new RuntimeException("头像上传失败: 无法创建上传目录(" + e.getMessage() + ")", e);
        }

        Path targetFile = targetDir.resolve(newFileName);

        try {
            // 头像特殊处理：裁剪为正方形并压缩
            Thumbnails.of(file.getInputStream())
                .size(300, 300)
                .keepAspectRatio(true)
                .outputQuality(props.getCompressionQuality())
                .outputFormat(ext)
                .toFile(targetFile.toFile());
        } catch (IOException e) {
            log.error("头像处理写入失败, targetFile={}, userId={}", targetFile, userId, e);
            try { Files.deleteIfExists(targetFile); } catch (IOException ignored) {}
            throw new RuntimeException("头像上传失败: 处理图片时发生错误(" + e.getMessage() + ")", e);
        }

        log.info("用户{}上传头像成功: {} (size={}B)", userId, targetFile, file.getSize());
        return "/uploads/" + datePath + "/" + newFileName;
    }

    private void compressImage(Path filePath, String ext) {
        try {
            BufferedImage original = ImageIO.read(filePath.toFile());
            if (original == null) {
                log.warn("无法读取图片，跳过压缩: {}", filePath);
                return;
            }

            int width = original.getWidth();
            int height = original.getHeight();

            // 临时文件名使用 .tmp.{ext} 格式，避免 Thumbnailator 因后缀不匹配而自动追加扩展名
            // 例如: uuid.jpg → uuid.jpg.tmp.jpg（.jpg 是已知格式，不会被重复追加）
            Path compressed = filePath.resolveSibling(filePath.getFileName() + ".tmp." + ext);

            // 仅在图片超过最大尺寸时压缩
            if (width > props.getMaxWidth() || height > props.getMaxHeight()) {
                Thumbnails.of(filePath.toFile())
                    .size(props.getMaxWidth(), props.getMaxHeight())
                    .keepAspectRatio(true)
                    .outputQuality(props.getCompressionQuality())
                    .outputFormat(ext)
                    .toFile(compressed.toFile());

                Files.deleteIfExists(filePath);
                Files.move(compressed, filePath);
                log.info("图片已压缩: {} ({}x{} -> 按比例缩放)", filePath, width, height);
            } else {
                // 即使尺寸合适，也进行质量压缩
                Thumbnails.of(filePath.toFile())
                    .scale(1.0)
                    .outputQuality(props.getCompressionQuality())
                    .outputFormat(ext)
                    .toFile(compressed.toFile());

                // 仅在压缩后文件更小时替换
                long originalSize = Files.size(filePath);
                long compressedSize = Files.size(compressed);
                if (compressedSize < originalSize) {
                    Files.deleteIfExists(filePath);
                    Files.move(compressed, filePath);
                    log.info("图片质量压缩: {} ({}B -> {}B, 节省{}%)",
                        filePath, originalSize, compressedSize,
                        (1 - (double)compressedSize/originalSize) * 100);
                } else {
                    Files.deleteIfExists(compressed);
                }
            }
        } catch (Exception e) {
            log.warn("图片压缩失败，保留原文件: {}", filePath, e);
        }
    }

    private void cleanupTempDir(Path tempDir, String fileId) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", tempDir, e);
        }
        chunkProgress.remove(fileId);
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
