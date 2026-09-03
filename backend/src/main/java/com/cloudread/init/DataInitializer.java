package com.cloudread.init;

import com.cloudread.dto.book.BookMeta;
import com.cloudread.dto.book.FileRefResponse;
import com.cloudread.entity.Book;
import com.cloudread.entity.Category;
import com.cloudread.entity.Comment;
import com.cloudread.entity.SysUser;
import com.cloudread.entity.SystemConfig;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.CategoryMapper;
import com.cloudread.mapper.CommentMapper;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.mapper.SystemConfigMapper;
import com.cloudread.security.JwtUser;
import com.cloudread.service.BookService;
import com.cloudread.storage.CoverGenerator;
import com.cloudread.storage.HashService;
import com.cloudread.storage.StorageService;
import com.cloudread.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 首次启动时初始化管理员、示例用户、默认分类、系统配置与示例书籍。
 */
@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final SystemConfigMapper configMapper;
    private final BookMapper bookMapper;
    private final CommentMapper commentMapper;
    private final PasswordEncoder passwordEncoder;
    private final BookService bookService;
    private final StorageService storageService;
    private final HashService hashService;
    private final CoverGenerator coverGenerator;
    private final String uploadDir;

    public DataInitializer(SysUserMapper userMapper,
                           CategoryMapper categoryMapper,
                           SystemConfigMapper configMapper,
                           BookMapper bookMapper,
                           CommentMapper commentMapper,
                           PasswordEncoder passwordEncoder,
                           BookService bookService,
                           StorageService storageService,
                           HashService hashService,
                           CoverGenerator coverGenerator,
                           @Value("${app.upload-dir}") String uploadDir) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.configMapper = configMapper;
        this.bookMapper = bookMapper;
        this.commentMapper = commentMapper;
        this.passwordEncoder = passwordEncoder;
        this.bookService = bookService;
        this.storageService = storageService;
        this.hashService = hashService;
        this.coverGenerator = coverGenerator;
        this.uploadDir = uploadDir;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedCategories();
        seedConfigs();
        seedSampleBook();
    }

    private void seedUsers() {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setNickname("系统管理员");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole(SysUser.ROLE_ADMIN);
        admin.setStatus(SysUser.STATUS_NORMAL);
        userMapper.insert(admin);

        SysUser demo = new SysUser();
        demo.setUsername("demo");
        demo.setNickname("演示用户");
        demo.setPassword(passwordEncoder.encode("demo123"));
        demo.setRole(SysUser.ROLE_USER);
        demo.setStatus(SysUser.STATUS_NORMAL);
        userMapper.insert(demo);
        log.info("已初始化账号: admin/admin123（管理员）, demo/demo123（普通用户）");
    }

    private void seedCategories() {
        if (categoryMapper.selectCount(null) > 0) {
            return;
        }
        Category literature = category("文学小说", null, 1);
        Category computer = category("计算机技术", null, 2);
        category("历史传记", null, 3);
        category("经管励志", null, 4);
        category("科学科普", null, 5);
        category("其他", null, 6);
        category("现代文学", literature.getId(), 1);
        category("古典名著", literature.getId(), 2);
        category("网络文学", literature.getId(), 3);
        category("编程开发", computer.getId(), 1);
        category("人工智能", computer.getId(), 2);
        category("数据库", computer.getId(), 3);
        log.info("已初始化默认分类树");
    }

    private Category category(String name, Long parentId, int sort) {
        Category category = new Category();
        category.setName(name);
        category.setParentId(parentId);
        category.setSort(sort);
        categoryMapper.insert(category);
        return category;
    }

    private void seedConfigs() {
        if (configMapper.selectCount(null) > 0) {
            return;
        }
        insertConfig("upload.maxSizeMb", "200", "电子书文件大小上限（MB）");
        insertConfig("allowedFormats", "pdf,epub,txt,mobi", "允许上传的电子书格式白名单");
        insertConfig("reviewEnabled", "true", "新上传书籍是否需要人工审核");
        insertConfig("registerEnabled", "true", "是否开放新用户自动注册");
        insertConfig("cover.maxSizeMb", "10", "封面图片大小上限（MB）");
        insertConfig("allowedCoverFormats", "jpg,png", "允许的封面格式");
        insertConfig("chunkThresholdMb", "100", "超过该大小启用分片上传（MB）");
        log.info("已初始化系统配置");
    }

    private void insertConfig(String key, String value, String desc) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(desc);
        configMapper.insert(config);
    }

    private void seedSampleBook() throws Exception {
        if (bookMapper.selectCount(null) > 0) {
            return;
        }
        SysUser admin = userMapper.selectList(null).stream()
                .filter(u -> "admin".equals(u.getUsername())).findFirst().orElse(null);
        SysUser demo = userMapper.selectList(null).stream()
                .filter(u -> "demo".equals(u.getUsername())).findFirst().orElse(null);
        if (admin == null || demo == null) {
            return;
        }

        String content = """
                《云阅 CloudRead 平台使用指南》

                一、什么是云阅？
                云阅是一个轻量级、可溯源、高互动的 B/S 架构电子书共享与管理平台，
                支持 PDF、EPUB、TXT、MOBI 格式的电子书上传、在线阅读与下载。

                二、核心功能
                1. 登录即注册：输入用户名和密码，系统自动完成注册或登录。
                2. 书籍上传：支持手动上传封面；未上传时自动解析书籍内置封面，
                   解析失败则根据书名和作者自动生成默认封面。
                3. 书籍溯源：每本书拥有 SHA-256 数字指纹，可随时校验文件是否被篡改，
                   并查看上传者（脱敏）、上传时间、版本变更历史。
                4. 互动社区：支持评论、两级嵌套回复、点赞与收藏。
                5. 管理端：实时监控 JVM 指标、前端性能埋点、ERROR/WARN 实时日志流、
                   SQLite 数据库状态、内容审核与用户管理。

                三、演示账号
                管理员：admin / admin123
                普通用户：demo / demo123

                四、分片上传
                超过 100MB 的大文件会自动启用分片上传，前端按 5MB 分片计算哈希并上传，
                后端合并后重新计算 SHA-256 作为唯一数字指纹。

                五、阅读进度
                在线阅读时系统会自动记录您的阅读进度，下次打开可继续阅读。

                祝您阅读愉快！
                """;
        Path sampleDir = Paths.get(uploadDir).resolve("2026/08/15");
        Files.createDirectories(sampleDir);
        Path sampleFile = sampleDir.resolve("cloudread-guide.txt");
        Files.write(sampleFile, content.getBytes(StandardCharsets.UTF_8));

        String hash = hashService.sha256(sampleFile);
        FileRefResponse fileRef = new FileRefResponse();
        fileRef.setHash(hash);
        fileRef.setRelativePath("2026/08/15/cloudread-guide.txt");
        fileRef.setSize(Files.size(sampleFile));
        fileRef.setFormat("txt");
        fileRef.setOriginalName("云阅平台使用指南.txt");

        BookMeta meta = new BookMeta();
        meta.setTitle("云阅平台使用指南");
        meta.setAuthor("云阅团队");
        meta.setDescription("云阅 CloudRead 电子书共享与管理平台的使用指南，包含核心功能与演示账号说明。");
        meta.setCategoryId(categoryIdByName("其他"));

        byte[] coverBytes = coverGenerator.generate(meta.getTitle(), meta.getAuthor());
        StoredFile cover = storageService.storeCoverBytes(coverBytes, hash + "c", "png");
        fileRef.setRelativePath(fileRef.getRelativePath());

        JwtUser adminUser = new JwtUser(admin.getId(), admin.getUsername(), admin.getRole(), admin.getStatus());
        bookService.commit(meta, fileRef, null, adminUser, "127.0.0.1");

        Book sample = bookMapper.selectOne(null);
        bookService.review(sample.getId(), Book.STATUS_APPROVED, "系统初始化示例数据", adminUser);
        if (commentMapper.selectCount(null) == 0) {
            Comment c1 = new Comment();
            c1.setBookId(sample.getId());
            c1.setUserId(demo.getId());
            c1.setContent("欢迎使用云阅电子书平台！书籍溯源功能真的很实用。");
            c1.setLikeCount(2);
            c1.setStatus(1);
            commentMapper.insert(c1);

            Comment c2 = new Comment();
            c2.setBookId(sample.getId());
            c2.setUserId(admin.getId());
            c2.setContent("感谢支持！你也可以尝试上传自己的电子书，系统会自动生成封面。");
            c2.setParentId(c1.getId());
            c2.setLikeCount(1);
            c2.setStatus(1);
            commentMapper.insert(c2);
        }
        log.info("已初始化示例书籍《云阅平台使用指南》");
    }

    private Long categoryIdByName(String name) {
        List<Category> categories = categoryMapper.selectList(null);
        return categories.stream().filter(c -> name.equals(c.getName())).map(Category::getId).findFirst().orElse(null);
    }
}
