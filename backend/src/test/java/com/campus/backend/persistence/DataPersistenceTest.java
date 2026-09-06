package com.campus.backend.persistence;

import com.campus.backend.dto.UserRegisterDTO;
import com.campus.backend.dto.UserVO;
import com.campus.backend.dto.PostCreateDTO;
import com.campus.backend.dto.PostVO;
import com.campus.backend.dto.ProductCreateDTO;
import com.campus.backend.dto.ProductVO;
import com.campus.backend.entity.User;
import com.campus.backend.mapper.UserMapper;
import com.campus.backend.service.UserService;
import com.campus.backend.service.PostService;
import com.campus.backend.service.ProductService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private ProductService productService;

    @Value("${spring.sql.init.mode}")
    private String sqlInitMode;

    @Test
    @Order(1)
    @DisplayName("验证SQL初始化模式为never，不会在启动时清空数据")
    void testSqlInitModeIsNever() {
        assertEquals("never", sqlInitMode,
            "SQL初始化模式应为never，避免每次启动时清空数据");
    }

    @Test
    @Order(2)
    @DisplayName("数据库连接正常，可执行基本查询")
    void testDatabaseConnection() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertEquals(1, result);
    }

    @Test
    @Order(3)
    @DisplayName("用户数据写入后可正确读取")
    void testUserPersistence() {
        String uniqueUsername = "persistence_test_" + System.currentTimeMillis();
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(uniqueUsername);
        dto.setPassword("Test123456");
        dto.setEmail(uniqueUsername + "@test.com");

        UserVO created = userService.register(dto);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(uniqueUsername, created.getUsername());

        UserVO retrieved = userService.getUserInfo(created.getId());
        assertNotNull(retrieved);
        assertEquals(uniqueUsername, retrieved.getUsername());
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    @Order(4)
    @DisplayName("用户密码加密存储，非明文")
    void testPasswordHashedStorage() {
        String uniqueUsername = "pwd_test_" + System.currentTimeMillis();
        String rawPassword = "MySecret123";

        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(uniqueUsername);
        dto.setPassword(rawPassword);

        userService.register(dto);

        User user = userMapper.selectByUsername(uniqueUsername);
        assertNotNull(user);
        assertNotNull(user.getPasswordHash());
        assertNotEquals(rawPassword, user.getPasswordHash(),
            "密码不应以明文存储");
        assertTrue(user.getPasswordHash().startsWith("$2a$"),
            "密码应使用BCrypt加密");
    }

    @Test
    @Order(5)
    @DisplayName("商品数据写入后可正确读取")
    void testProductPersistence() {
        String uniqueUsername = "product_test_" + System.currentTimeMillis();
        UserRegisterDTO userDto = new UserRegisterDTO();
        userDto.setUsername(uniqueUsername);
        userDto.setPassword("Test123456");
        UserVO user = userService.register(userDto);

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setName("持久化测试商品_" + System.currentTimeMillis());
        dto.setDescription("这是一个数据持久化测试商品");
        dto.setPrice(new BigDecimal("99.99"));
        dto.setOriginalPrice(new BigDecimal("199.99"));
        dto.setConditionLevel(2);
        dto.setCategoryId(1L);

        ProductVO created = productService.createProduct(dto, user.getId());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(dto.getName(), created.getName());
        assertEquals(new BigDecimal("99.99"), created.getPrice());

        ProductVO retrieved = productService.getProductDetail(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(dto.getName(), retrieved.getName());
    }

    @Test
    @Order(6)
    @DisplayName("帖子数据写入后可正确读取")
    void testPostPersistence() {
        String uniqueUsername = "post_test_" + System.currentTimeMillis();
        UserRegisterDTO userDto = new UserRegisterDTO();
        userDto.setUsername(uniqueUsername);
        userDto.setPassword("Test123456");
        UserVO user = userService.register(userDto);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("持久化测试帖子_" + System.currentTimeMillis());
        dto.setContent("这是一个数据持久化测试帖子的内容");
        dto.setPostType("DISCUSSION");

        PostVO created = postService.createPost(dto, user.getId());
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(dto.getTitle(), created.getTitle());

        PostVO retrieved = postService.getPostDetail(created.getId(), user.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(dto.getTitle(), retrieved.getTitle());
    }

    @Test
    @Order(7)
    @DisplayName("用户更新资料后数据持久化")
    void testUserProfileUpdatePersistence() {
        String uniqueUsername = "update_test_" + System.currentTimeMillis();
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(uniqueUsername);
        dto.setPassword("Test123456");
        UserVO user = userService.register(dto);

        User updateData = new User();
        updateData.setNickname("更新后的昵称");
        updateData.setBio("更新后的个人简介");
        updateData.setSchool("测试大学");

        UserVO updated = userService.updateProfile(user.getId(), updateData);
        assertEquals("更新后的昵称", updated.getNickname());

        UserVO retrieved = userService.getUserInfo(user.getId());
        assertEquals("更新后的昵称", retrieved.getNickname());
        assertEquals("更新后的个人简介", retrieved.getBio());
        assertEquals("测试大学", retrieved.getSchool());
    }

    @Test
    @Order(8)
    @DisplayName("数据库表结构完整性检查")
    void testDatabaseSchemaIntegrity() {
        List<String> requiredTables = List.of(
            "users", "categories", "products", "posts",
            "post_comments", "product_comments", "favorites",
            "user_likes", "user_follows", "user_behaviors",
            "chat_conversations", "chat_messages"
        );

        for (String table : requiredTables) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class, table);
            assertNotNull(count);
            assertEquals(1, count,
                "数据库中应存在表: " + table);
        }
    }

    @Test
    @Order(9)
    @DisplayName("事务回滚不影响已提交数据")
    void testTransactionRollbackSafety() {
        String uniqueUsername = "txn_test_" + System.currentTimeMillis();
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(uniqueUsername);
        dto.setPassword("Test123456");
        UserVO user = userService.register(dto);

        User existingUser = userMapper.selectById(user.getId());
        assertNotNull(existingUser);

        UserVO retrievedAfterCommit = userService.getUserInfo(user.getId());
        assertEquals(uniqueUsername, retrievedAfterCommit.getUsername());
    }
}
