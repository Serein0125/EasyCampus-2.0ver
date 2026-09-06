package com.campus.backend.controller;

import com.campus.backend.common.SecurityUtils;
import com.campus.backend.service.FileUploadService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FileUploadController 集成测试
 * 使用独立 MockMvc (不加载 Spring 上下文) 测试控制器层
 * 避免 @WebMvcTest 因 WebConfig/SecurityConfig 依赖链导致的 ApplicationContext 加载失败
 */
class FileUploadControllerTest {

    private MockMvc mockMvc;
    private FileUploadService fileUploadService;
    private MockedStatic<SecurityUtils> securityUtilsMock;

    @BeforeEach
    void setUp() {
        fileUploadService = Mockito.mock(FileUploadService.class);
        FileUploadController controller = new FileUploadController(fileUploadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Mock SecurityUtils.getCurrentUserId() 静态方法
        securityUtilsMock = mockStatic(SecurityUtils.class);
        securityUtilsMock.when(SecurityUtils::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    // ========== 上传图片测试 ==========

    @Test
    @DisplayName("POST /v2/upload/image - 上传图片成功返回200")
    void testUploadImage_Success() throws Exception {
        Mockito.when(fileUploadService.uploadImage(any(), eq(1L)))
            .thenReturn("/uploads/2024/01/01/abc.png");

        MockMultipartFile file = new MockMultipartFile(
            "file", "test.png", "image/png",
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        mockMvc.perform(multipart("/v2/upload/image").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").value("/uploads/2024/01/01/abc.png"));
    }

    @Test
    @DisplayName("POST /v2/upload/image - 不传文件返回400")
    void testUploadImage_NoFile() throws Exception {
        // 不传 file 参数时，Spring MVC 会返回 400 Bad Request
        mockMvc.perform(post("/v2/upload/image")
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isBadRequest());
    }

    // ========== 上传头像测试 ==========

    @Test
    @DisplayName("POST /v2/upload/avatar - 上传头像成功返回200")
    void testUploadAvatar_Success() throws Exception {
        Mockito.when(fileUploadService.uploadAvatar(any(), eq(1L)))
            .thenReturn("/uploads/2024/01/01/avatar.png");

        MockMultipartFile file = new MockMultipartFile(
            "file", "avatar.png", "image/png",
            new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        mockMvc.perform(multipart("/v2/upload/avatar").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").value("/uploads/2024/01/01/avatar.png"));
    }

    // ========== 分片上传测试 ==========

    @Test
    @DisplayName("POST /v2/upload/chunk - 上传分片成功返回200")
    void testUploadChunk_Success() throws Exception {
        Mockito.when(fileUploadService.uploadChunk(any(), eq("fid"), eq(0), eq(3), eq("photo.jpg"), eq(1L)))
            .thenReturn("分片 1/3 上传成功");

        MockMultipartFile chunk = new MockMultipartFile(
            "chunk", "chunk0", "application/octet-stream", new byte[1024]
        );

        mockMvc.perform(multipart("/v2/upload/chunk")
                .file(chunk)
                .param("fileId", "fid")
                .param("chunkIndex", "0")
                .param("totalChunks", "3")
                .param("fileName", "photo.jpg"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.message").value("分片 1/3 上传成功"));
    }

    // ========== 合并分片测试 ==========

    @Test
    @DisplayName("POST /v2/upload/merge - 合并分片成功返回200")
    void testMergeChunks_Success() throws Exception {
        Mockito.when(fileUploadService.mergeChunks(eq("fid"), eq("photo.jpg"), eq(3), eq(1L)))
            .thenReturn("/uploads/2024/01/01/merged.jpg");

        mockMvc.perform(post("/v2/upload/merge")
                .param("fileId", "fid")
                .param("fileName", "photo.jpg")
                .param("totalChunks", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.url").value("/uploads/2024/01/01/merged.jpg"));
    }
}
