package com.campus.backend.service;

import com.campus.backend.config.FileStorageProperties;
import com.campus.backend.service.impl.FileUploadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileUploadServiceImpl 单元测试
 * 覆盖文件上传验证、分片上传、安全校验等核心逻辑
 */
class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    private FileUploadServiceImpl fileUploadService;
    private FileStorageProperties props;

    // 有效的 JPEG 文件头 (FF D8 FF E0 - JFIF)
    private static final byte[] JPEG_HEADER = new byte[]{
        (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
        0x00, 0x10, 0x4A, 0x46  // JFIF marker + padding
    };

    // 有效的 PNG 文件头 (89 50 4E 47)
    private static final byte[] PNG_HEADER = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47,
        0x0D, 0x0A, 0x1A, 0x0A  // PNG header remainder
    };

    // 有效的 WebP 文件头 (52 49 46 46)
    private static final byte[] WEBP_HEADER = new byte[]{
        0x52, 0x49, 0x46, 0x46,
        0x00, 0x00, 0x00, 0x00  // RIFF + padding
    };

    // 非图片内容 (纯文本)
    private static final byte[] TEXT_CONTENT = "This is not an image file content".getBytes();

    @BeforeEach
    void setUp() throws IOException {
        props = Mockito.mock(FileStorageProperties.class);

        Path uploadDir = tempDir.resolve("uploads");
        Path tempPathDir = tempDir.resolve("uploads").resolve("temp");
        Files.createDirectories(uploadDir);
        Files.createDirectories(tempPathDir);

        Mockito.when(props.getBasePath()).thenReturn(uploadDir.toString());
        Mockito.when(props.getTempPath()).thenReturn(tempPathDir.toString());
        Mockito.when(props.getImageMaxSize()).thenReturn(10485760L);  // 10MB
        Mockito.when(props.getAvatarMaxSize()).thenReturn(5242880L);  // 5MB
        Mockito.when(props.getAllowedTypes()).thenReturn("image/jpeg,image/png,image/webp");
        Mockito.when(props.getAllowedExtensions()).thenReturn("jpg,jpeg,png,webp");
        Mockito.when(props.getCompressionQuality()).thenReturn(0.8);
        Mockito.when(props.getMaxWidth()).thenReturn(1920);
        Mockito.when(props.getMaxHeight()).thenReturn(1080);

        fileUploadService = new FileUploadServiceImpl(props);
    }

    // ========== uploadImage 测试 ==========

    @Test
    @DisplayName("正常上传图片成功")
    void testUploadImage_Success() {
        // 创建一个足够大的 JPEG 文件 (Thumbnailator 需要有效图片数据)
        // 使用 PNG 格式因为更容易创建有效的最小 PNG
        byte[] minimalPng = createMinimalPng();
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png", minimalPng
        );

        String url = fileUploadService.uploadImage(file, 1L);

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    @DisplayName("空文件抛出异常")
    void testUploadImage_EmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.jpg", "image/jpeg", new byte[0]
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadImage(file, 1L);
        });
        assertTrue(exception.getMessage().contains("上传文件不能为空"));
    }

    @Test
    @DisplayName("超过大小限制抛出异常")
    void testUploadImage_OversizedFile() {
        // 创建一个超过10MB限制的文件
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        // 在文件头写入有效的JPEG魔数
        System.arraycopy(JPEG_HEADER, 0, largeContent, 0, JPEG_HEADER.length);

        MockMultipartFile file = new MockMultipartFile(
            "file", "large.jpg", "image/jpeg", largeContent
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadImage(file, 1L);
        });
        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }

    @Test
    @DisplayName("不支持的Content-Type抛出异常")
    void testUploadImage_InvalidContentType() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", JPEG_HEADER
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadImage(file, 1L);
        });
        assertTrue(exception.getMessage().contains("不支持的图片格式"));
    }

    @Test
    @DisplayName("不支持的文件扩展名抛出异常")
    void testUploadImage_InvalidExtension() {
        // Content-Type 是 image/jpeg 但扩展名是 .gif (不在允许列表中)
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.gif", "image/jpeg", JPEG_HEADER
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadImage(file, 1L);
        });
        assertTrue(exception.getMessage().contains("不支持的文件扩展名"));
    }

    @Test
    @DisplayName("文件扩展名是jpg但内容不是图片 - 魔数验证失败")
    void testUploadImage_FakeImageFile() {
        // 扩展名和Content-Type都正确，但文件内容是纯文本
        MockMultipartFile file = new MockMultipartFile(
            "file", "fake.jpg", "image/jpeg", TEXT_CONTENT
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadImage(file, 1L);
        });
        assertTrue(exception.getMessage().contains("文件内容不是有效的图片格式"));
    }

    // ========== uploadAvatar 测试 ==========

    @Test
    @DisplayName("头像上传成功 - 验证裁剪压缩")
    void testUploadAvatar_Success() {
        byte[] minimalPng = createMinimalPng();
        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.png", "image/png", minimalPng
        );

        String url = fileUploadService.uploadAvatar(file, 1L);

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".png"));
    }

    @Test
    @DisplayName("头像超过大小限制抛出异常")
    void testUploadAvatar_OversizedFile() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB, 超过5MB限制
        System.arraycopy(PNG_HEADER, 0, largeContent, 0, PNG_HEADER.length);

        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.png", "image/png", largeContent
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadAvatar(file, 1L);
        });
        assertTrue(exception.getMessage().contains("文件大小超过限制"));
    }

    // ========== uploadChunk 测试 ==========

    @Test
    @DisplayName("分片上传成功")
    void testUploadChunk_Success() {
        byte[] chunkData = JPEG_HEADER;
        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", chunkData
        );

        String result = fileUploadService.uploadChunk(chunk, "file-123", 0, 3, "photo.jpg", 1L);

        assertNotNull(result);
        assertTrue(result.contains("分片"));
        assertTrue(result.contains("1/3"));

        // 验证分片文件已写入临时目录
        Path chunkPath = tempDir.resolve("uploads").resolve("temp").resolve("file-123").resolve("0");
        assertTrue(Files.exists(chunkPath));
    }

    @Test
    @DisplayName("分片上传 - 非法文件名被拦截")
    void testUploadChunk_InvalidFileName() {
        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", JPEG_HEADER
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "file-123", 0, 3, "../etc/passwd", 1L);
        });
        assertTrue(exception.getMessage().contains("非法字符"));
    }

    // ========== mergeChunks 测试 ==========

    @Test
    @DisplayName("分片合并成功")
    void testMergeChunks_Success() throws IOException {
        String fileId = "merge-test-file";
        int totalChunks = 3;

        // 手动创建分片文件
        Path chunkDir = Path.of(props.getTempPath(), fileId);
        Files.createDirectories(chunkDir);

        // 每个分片使用有效的 JPEG 头
        for (int i = 0; i < totalChunks; i++) {
            Path chunkPath = chunkDir.resolve(String.valueOf(i));
            Files.write(chunkPath, JPEG_HEADER);
        }

        // 手动注册分片进度
        var chunkProgress = (java.util.concurrent.ConcurrentHashMap<String, java.util.Set<Integer>>)
            ReflectionTestUtils.getField(fileUploadService, "chunkProgress");
        java.util.Set<Integer> uploaded = java.util.concurrent.ConcurrentHashMap.newKeySet();
        for (int i = 0; i < totalChunks; i++) {
            uploaded.add(i);
        }
        chunkProgress.put(fileId, uploaded);

        // 合并后的文件需要是有效图片格式
        // 由于分片内容只是 JPEG 头部，合并后 detectFileType 会检测为 image/jpeg
        String url = fileUploadService.mergeChunks(fileId, "result.jpg", totalChunks, 1L);

        assertNotNull(url);
        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".jpg"));

        // 验证临时目录已被清理
        assertFalse(Files.exists(chunkDir));
    }

    @Test
    @DisplayName("分片不完整时合并失败")
    void testMergeChunks_IncompleteChunks() throws IOException {
        String fileId = "incomplete-test-file";
        int totalChunks = 3;

        // 只创建2个分片文件
        Path chunkDir = Path.of(props.getTempPath(), fileId);
        Files.createDirectories(chunkDir);
        for (int i = 0; i < 2; i++) {
            Files.write(chunkDir.resolve(String.valueOf(i)), JPEG_HEADER);
        }

        // 只注册2个分片进度
        var chunkProgress = (java.util.concurrent.ConcurrentHashMap<String, java.util.Set<Integer>>)
            ReflectionTestUtils.getField(fileUploadService, "chunkProgress");
        java.util.Set<Integer> uploaded = java.util.concurrent.ConcurrentHashMap.newKeySet();
        uploaded.add(0);
        uploaded.add(1);
        chunkProgress.put(fileId, uploaded);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.mergeChunks(fileId, "result.jpg", totalChunks, 1L);
        });
        assertTrue(exception.getMessage().contains("分片不完整"));
    }

    @Test
    @DisplayName("分片目录不存在时合并失败")
    void testMergeChunks_NoTempDir() {
        String fileId = "nonexistent-file";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileUploadService.mergeChunks(fileId, "result.jpg", 3, 1L);
        });
        assertTrue(exception.getMessage().contains("分片文件不存在"));
    }

    // ========== validateFileName 测试 (通过公开方法间接测试) ==========

    @Test
    @DisplayName("路径遍历攻击被拦截")
    void testValidateFileName_PathTraversal() {
        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", JPEG_HEADER
        );

        // 测试 ../ 路径遍历
        RuntimeException exception1 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "../../../etc/passwd.jpg", 1L);
        });
        assertTrue(exception1.getMessage().contains("非法字符"));

        // 测试 .. 路径遍历
        RuntimeException exception2 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "..hidden.jpg", 1L);
        });
        assertTrue(exception2.getMessage().contains("非法字符"));

        // 测试 / 路径遍历
        RuntimeException exception3 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "path/to/file.jpg", 1L);
        });
        assertTrue(exception3.getMessage().contains("非法字符"));

        // 测试 \ 路径遍历
        RuntimeException exception4 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "path\\to\\file.jpg", 1L);
        });
        assertTrue(exception4.getMessage().contains("非法字符"));
    }

    @Test
    @DisplayName("特殊字符被拦截")
    void testValidateFileName_SpecialCharacters() {
        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", JPEG_HEADER
        );

        // 测试含空格的文件名
        RuntimeException exception1 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "my file.jpg", 1L);
        });
        assertTrue(exception1.getMessage().contains("非法字符"));

        // 测试含分号的文件名
        RuntimeException exception2 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "file;name.jpg", 1L);
        });
        assertTrue(exception2.getMessage().contains("非法字符"));

        // 测试含管道符的文件名
        RuntimeException exception3 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "file|name.jpg", 1L);
        });
        assertTrue(exception3.getMessage().contains("非法字符"));

        // 测试空文件名
        RuntimeException exception4 = assertThrows(RuntimeException.class, () -> {
            fileUploadService.uploadChunk(chunk, "fid", 0, 1, "   ", 1L);
        });
        assertTrue(exception4.getMessage().contains("文件名不能为空"));
    }

    @Test
    @DisplayName("合法文件名通过验证")
    void testValidateFileName_ValidNames() {
        byte[] chunkData = JPEG_HEADER;
        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", chunkData
        );

        // 中文文件名
        assertDoesNotThrow(() -> {
            fileUploadService.uploadChunk(chunk, "fid-cn", 0, 1, "照片.jpg", 1L);
        });

        // 字母数字和下划线
        assertDoesNotThrow(() -> {
            fileUploadService.uploadChunk(chunk, "fid-en", 0, 1, "my_photo_2024.jpg", 1L);
        });

        // 带连字符
        assertDoesNotThrow(() -> {
            fileUploadService.uploadChunk(chunk, "fid-dash", 0, 1, "photo-2024.jpg", 1L);
        });
    }

    // ========== uploadImages 批量上传测试 ==========

    @Test
    @DisplayName("批量上传图片成功")
    void testUploadImages_Success() {
        byte[] minimalPng = createMinimalPng();
        MockMultipartFile file1 = new MockMultipartFile(
            "files", "test1.png", "image/png", minimalPng
        );
        MockMultipartFile file2 = new MockMultipartFile(
            "files", "test2.png", "image/png", minimalPng
        );

        List<String> urls = fileUploadService.uploadImages(List.of(file1, file2), 1L);

        assertNotNull(urls);
        assertEquals(2, urls.size());
        for (String url : urls) {
            assertTrue(url.startsWith("/uploads/"));
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建一个最小的有效 PNG 文件字节数组
     * PNG 文件结构: 签名(8字节) + IHDR chunk + IDAT chunk + IEND chunk
     */
    private byte[] createMinimalPng() {
        try {
            // 使用 Java 内置方式创建一个 1x1 的 PNG 图片
            java.awt.image.BufferedImage image =
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB);
            image.setRGB(0, 0, 0xFF0000); // 红色像素

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            boolean written = javax.imageio.ImageIO.write(image, "png", baos);
            if (!written) {
                throw new RuntimeException("无法创建测试 PNG 图片");
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("创建测试图片失败", e);
        }
    }
}
