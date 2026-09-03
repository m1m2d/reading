package com.cloudread.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cloudread.common.BusinessException;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.book.BookMeta;
import com.cloudread.dto.book.BookQuery;
import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.book.FileRefResponse;
import com.cloudread.dto.book.TraceResponse;
import com.cloudread.dto.book.VerifyResponse;
import com.cloudread.entity.Book;
import com.cloudread.entity.Category;
import com.cloudread.entity.Favorite;
import com.cloudread.entity.ReadingProgress;
import com.cloudread.entity.SysUser;
import com.cloudread.entity.SystemLog;
import com.cloudread.entity.TraceLog;
import com.cloudread.entity.VersionHistory;
import com.cloudread.mapper.BookMapper;
import com.cloudread.mapper.CategoryMapper;
import com.cloudread.mapper.FavoriteMapper;
import com.cloudread.mapper.ReadingProgressMapper;
import com.cloudread.mapper.SysUserMapper;
import com.cloudread.mapper.SystemLogMapper;
import com.cloudread.mapper.TraceLogMapper;
import com.cloudread.mapper.VersionHistoryMapper;
import com.cloudread.security.JwtUser;
import com.cloudread.storage.CoverExtractor;
import com.cloudread.storage.CoverGenerator;
import com.cloudread.storage.HashService;
import com.cloudread.storage.StorageService;
import com.cloudread.storage.StoredFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long TXT_CONTENT_LIMIT = 10L * 1024 * 1024;

    private final BookMapper bookMapper;
    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;
    private final SysUserMapper userMapper;
    private final FavoriteMapper favoriteMapper;
    private final TraceLogMapper traceLogMapper;
    private final VersionHistoryMapper versionHistoryMapper;
    private final ReadingProgressMapper readingProgressMapper;
    private final SystemLogMapper systemLogMapper;
    private final StorageService storageService;
    private final HashService hashService;
    private final CoverExtractor coverExtractor;
    private final CoverGenerator coverGenerator;
    private final ConfigService configService;
    private final UserActionService userActionService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionTemplate transactionTemplate;

    public BookService(BookMapper bookMapper,
                       CategoryMapper categoryMapper,
                       CategoryService categoryService,
                       SysUserMapper userMapper,
                       FavoriteMapper favoriteMapper,
                       TraceLogMapper traceLogMapper,
                       VersionHistoryMapper versionHistoryMapper,
                       ReadingProgressMapper readingProgressMapper,
                       SystemLogMapper systemLogMapper,
                       StorageService storageService,
                       HashService hashService,
                       CoverExtractor coverExtractor,
                       CoverGenerator coverGenerator,
                       ConfigService configService,
                       UserActionService userActionService,
                       PlatformTransactionManager transactionManager) {
        this.bookMapper = bookMapper;
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
        this.userMapper = userMapper;
        this.favoriteMapper = favoriteMapper;
        this.traceLogMapper = traceLogMapper;
        this.versionHistoryMapper = versionHistoryMapper;
        this.readingProgressMapper = readingProgressMapper;
        this.systemLogMapper = systemLogMapper;
        this.storageService = storageService;
        this.hashService = hashService;
        this.coverExtractor = coverExtractor;
        this.coverGenerator = coverGenerator;
        this.configService = configService;
        this.userActionService = userActionService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    // ---------- 文件上传 ----------

    public FileRefResponse storeFile(MultipartFile file, Long userId, String ip) {
        validateBookFile(file);
        try {
            String hash = hashService.sha256(file.getInputStream());
            StoredFile stored = storageService.storeBookFile(file, hash);
            return toFileRef(stored);
        } catch (IOException e) {
            throw new BusinessException("读取文件失败: " + e.getMessage());
        }
    }

    public FileRefResponse completeChunk(String uploadId, String originalName, Long userId, String ip) {
        StoredFile stored = storageService.completeChunk(uploadId, originalName);
        return toFileRef(stored);
    }

    private FileRefResponse toFileRef(StoredFile stored) {
        FileRefResponse response = new FileRefResponse();
        response.setHash(stored.getHash());
        response.setRelativePath(stored.getRelativePath());
        response.setSize(stored.getSize());
        response.setFormat(stored.getFormat());
        response.setOriginalName(stored.getOriginalName());
        Book existing = findByHash(stored.getHash());
        if (existing != null) {
            response.setDuplicate(true);
            response.setExistingBookId(existing.getId());
            response.setExistingTitle(existing.getTitle());
        }
        return response;
    }

    /**
     * 提交元数据完成入库：处理封面、重复/新版本、溯源与版本历史记录。
     */
    public BookVO commit(BookMeta meta, FileRefResponse fileRef, MultipartFile cover,
                         JwtUser user, String ip) {
        if (fileRef == null || fileRef.getHash() == null || fileRef.getRelativePath() == null) {
            throw new BusinessException("缺少文件引用信息，请先上传文件");
        }
        Path path = storageService.resolveBook(fileRef.getRelativePath());
        try {
            String actualHash = hashService.sha256(path);
            if (!actualHash.equals(fileRef.getHash())) {
                throw new BusinessException("文件哈希校验失败，请重新上传");
            }
        } catch (IOException e) {
            throw new BusinessException("文件读取失败: " + e.getMessage());
        }

        // 耗时文件操作与查重放在事务外，事务内只做纯写，避免 SQLite 读->写快照冲突
        String coverUrl = resolveCover(meta, fileRef, cover, path);
        Book book = transactionTemplate.execute(status -> persistBook(meta, fileRef, coverUrl, user, ip));
        userActionService.record(user.getId(), "UPLOAD_BOOK", "上传书籍: " + book.getTitle(), ip);
        return toVO(book, user.getId());
    }

    private Book persistBook(BookMeta meta, FileRefResponse fileRef, String coverUrl, JwtUser user, String ip) {
        Book book;
        if (meta.getVersionOf() != null) {
            Book target = bookMapper.selectById(meta.getVersionOf());
            if (target == null) {
                throw new BusinessException(Result.CODE_NOT_FOUND, "要更新版本的书籍不存在");
            }
            if (!target.getUploaderId().equals(user.getId()) && !user.isAdmin()) {
                throw new BusinessException(Result.CODE_FORBIDDEN, "只能由上传者或管理员更新版本");
            }
            book = upgradeVersion(target, meta, fileRef, coverUrl, user);
        } else {
            Book existing = findByHash(fileRef.getHash());
            if (existing != null) {
                throw new BusinessException(Result.CODE_CONFLICT,
                        "该文件已存在（《" + existing.getTitle() + "》），请选择“作为新版本上传”或放弃上传");
            }
            book = createBook(meta, fileRef, coverUrl, user, ip);
        }
        return book;
    }

    private Book createBook(BookMeta meta, FileRefResponse fileRef, String coverUrl,
                            JwtUser user, String ip) {
        Book book = new Book();
        applyMeta(book, meta);
        book.setFilePath(fileRef.getRelativePath());
        book.setFileFormat(fileRef.getFormat());
        book.setFileSize(fileRef.getSize());
        book.setFileHash(fileRef.getHash());
        book.setVersionNo(1);
        book.setUploaderId(user.getId());
        book.setUploadIp(ip);
        book.setDownloadCount(0L);
        book.setStatus(configService.bool(ConfigService.KEY_REVIEW_ENABLED, true)
                ? Book.STATUS_PENDING : Book.STATUS_APPROVED);
        book.setCoverUrl(coverUrl);
        bookMapper.insert(book);

        insertTrace(book.getId(), "UPLOAD", user.getId(),
                json("hash", book.getFileHash(), "size", book.getFileSize(), "format", book.getFileFormat(),
                        "status", book.getStatus()));
        insertVersion(book.getId(), 1, book.getFileHash(), "初始版本");
        return book;
    }

    private Book upgradeVersion(Book target, BookMeta meta, FileRefResponse fileRef, String coverUrl, JwtUser user) {
        VersionHistory history = new VersionHistory();
        history.setBookId(target.getId());
        history.setVersionNo(target.getVersionNo());
        history.setFileHash(target.getFileHash());
        history.setChangeLog(meta.getChangeLog() == null || meta.getChangeLog().isBlank()
                ? "旧版本归档" : meta.getChangeLog());
        versionHistoryMapper.insert(history);

        String oldHash = target.getFileHash();
        target.setTitle(meta.getTitle());
        target.setAuthor(meta.getAuthor());
        target.setIsbn(meta.getIsbn());
        target.setDescription(meta.getDescription());
        if (meta.getCategoryId() != null) {
            target.setCategoryId(meta.getCategoryId());
        }
        target.setFilePath(fileRef.getRelativePath());
        target.setFileFormat(fileRef.getFormat());
        target.setFileSize(fileRef.getSize());
        target.setFileHash(fileRef.getHash());
        target.setVersionNo(target.getVersionNo() + 1);
        target.setCoverUrl(coverUrl);
        target.setUpdatedAt(now());
        bookMapper.updateById(target);

        insertTrace(target.getId(), "UPDATE_VERSION", user.getId(),
                json("fromHash", oldHash, "toHash", fileRef.getHash(), "newVersion", target.getVersionNo(),
                        "changeLog", meta.getChangeLog()));
        return target;
    }

    private String resolveCover(BookMeta meta, FileRefResponse fileRef, MultipartFile cover, Path bookPath) {
        String coverName = fileRef.getHash() + "c";
        if (cover != null && !cover.isEmpty()) {
            StoredFile stored = storageService.storeCover(cover, coverName);
            return "/api/v1/files/covers/" + stored.getRelativePath();
        }
        // 尝试解析书籍内部封面
        try (InputStream in = Files.newInputStream(bookPath)) {
            byte[] extracted = coverExtractor.extract(in, fileRef.getFormat());
            if (extracted != null && extracted.length > 0) {
                String ext = detectImageType(extracted);
                StoredFile stored = storageService.storeCoverBytes(extracted, coverName, ext);
                return "/api/v1/files/covers/" + stored.getRelativePath();
            }
        } catch (Exception e) {
            log.debug("解析书籍封面失败: {}", e.getMessage());
        }
        // 自动生成默认文字封面
        byte[] generated = coverGenerator.generate(meta.getTitle(), meta.getAuthor());
        try {
            StoredFile stored = storageService.storeCoverBytes(generated, coverName, "png");
            return "/api/v1/files/covers/" + stored.getRelativePath();
        } catch (IOException e) {
            throw new BusinessException("生成封面失败: " + e.getMessage());
        }
    }

    private String detectImageType(byte[] data) {
        if (data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        if (data.length > 7 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F') {
            return "gif";
        }
        return "png";
    }

    private void applyMeta(Book book, BookMeta meta) {
        book.setTitle(meta.getTitle());
        book.setAuthor(meta.getAuthor());
        book.setIsbn(meta.getIsbn());
        book.setDescription(meta.getDescription());
        book.setCategoryId(meta.getCategoryId());
    }

    private void validateBookFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("电子书文件不能为空");
        }
        String name = file.getOriginalFilename();
        String ext = name == null || !name.contains(".") ? "" : name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        String allowed = configService.get(ConfigService.KEY_ALLOWED_FORMATS);
        if (!java.util.Arrays.asList(allowed.split(",")).contains(ext)) {
            throw new BusinessException("不支持的文件格式: " + ext + "（允许: " + allowed + "）");
        }
        long maxBytes = configService.intValue(ConfigService.KEY_MAX_SIZE_MB, 200) * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件超过大小限制（"
                    + configService.intValue(ConfigService.KEY_MAX_SIZE_MB, 200) + "MB）");
        }
    }

    private Book findByHash(String hash) {
        return bookMapper.selectOne(new LambdaQueryWrapper<Book>().eq(Book::getFileHash, hash).last("LIMIT 1"));
    }

    private void insertTrace(Long bookId, String action, Long operatorId, String detail) {
        TraceLog trace = new TraceLog();
        trace.setBookId(bookId);
        trace.setAction(action);
        trace.setOperatorId(operatorId);
        trace.setDetail(detail);
        traceLogMapper.insert(trace);
    }

    private void insertVersion(Long bookId, int versionNo, String hash, String changeLog) {
        VersionHistory version = new VersionHistory();
        version.setBookId(bookId);
        version.setVersionNo(versionNo);
        version.setFileHash(hash);
        version.setChangeLog(changeLog);
        versionHistoryMapper.insert(version);
    }

    private String json(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ---------- 查询 ----------

    public PageResult<BookVO> list(BookQuery query, Long currentUserId) {
        Page<Book> page = new Page<>(Math.max(1, query.getPage()), Math.min(200, Math.max(1, query.getSize())));
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Book::getStatus, Book.STATUS_APPROVED);
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Book::getTitle, kw)
                    .or().like(Book::getAuthor, kw)
                    .or().like(Book::getIsbn, kw));
        }
        if (query.getCategoryId() != null) {
            List<Long> categoryIds = categoryService.categoryAndDescendantIds(query.getCategoryId());
            if (categoryIds.isEmpty()) {
                wrapper.eq(Book::getCategoryId, query.getCategoryId());
            } else {
                wrapper.in(Book::getCategoryId, categoryIds);
            }
        }
        if ("downloads".equals(query.getSort())) {
            wrapper.orderByDesc(Book::getDownloadCount);
        } else if ("title".equals(query.getSort())) {
            wrapper.orderByAsc(Book::getTitle);
        } else {
            wrapper.orderByDesc(Book::getCreatedAt);
        }
        Page<Book> result = bookMapper.selectPage(page, wrapper);
        List<BookVO> vos = toVOList(result.getRecords(), currentUserId);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(), vos);
    }

    public BookVO detail(Long id, Long currentUserId) {
        Book book = requireBook(id);
        if (book.getStatus() != Book.STATUS_APPROVED
                && (currentUserId == null || !book.getUploaderId().equals(currentUserId))) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "书籍不存在或未通过审核");
        }
        return toVO(book, currentUserId);
    }

    public List<BookVO> myUploads(Long userId) {
        List<Book> books = bookMapper.selectList(new LambdaQueryWrapper<Book>()
                .eq(Book::getUploaderId, userId)
                .orderByDesc(Book::getCreatedAt));
        return toVOList(books, userId);
    }

    public TraceResponse trace(Long id) {
        Book book = requireBook(id);
        TraceResponse response = new TraceResponse();
        response.setTitle(book.getTitle());
        response.setFileHash(book.getFileHash());
        response.setFileFormat(book.getFileFormat());
        response.setFileSize(book.getFileSize());
        response.setVersionNo(book.getVersionNo());
        SysUser uploader = book.getUploaderId() == null ? null : userMapper.selectById(book.getUploaderId());
        response.setUploaderName(uploader == null ? "未知" : displayName(uploader));
        response.setUploadIp(book.getUploadIp());
        response.setCreatedAt(book.getCreatedAt());
        response.setTraceLogs(traceLogMapper.selectList(new LambdaQueryWrapper<TraceLog>()
                .eq(TraceLog::getBookId, id).orderByDesc(TraceLog::getId)));
        response.setVersions(versionHistoryMapper.selectList(new LambdaQueryWrapper<VersionHistory>()
                .eq(VersionHistory::getBookId, id).orderByDesc(VersionHistory::getVersionNo)));
        return response;
    }

    public VerifyResponse verify(Long id, JwtUser operator) {
        Book book = requireBook(id);
        VerifyResponse response = new VerifyResponse();
        response.setRecordedHash(book.getFileHash());
        try {
            Path path = storageService.resolveBook(book.getFilePath());
            String current = hashService.sha256(path);
            response.setCurrentHash(current);
            boolean consistent = current.equalsIgnoreCase(book.getFileHash());
            response.setConsistent(consistent);
            response.setMessage(consistent ? "文件完整，未被篡改" : "文件异常，可能已被篡改！");
            if (!consistent) {
                SystemLog warn = new SystemLog();
                warn.setLevel("WARN");
                warn.setModule("SECURITY");
                warn.setMessage("书籍《" + book.getTitle() + "》(id=" + id + ") 哈希校验不一致，疑似被篡改");
                systemLogMapper.insert(warn);
            }
            insertTrace(id, "VERIFY", operator == null ? null : operator.getId(),
                    json("consistent", consistent, "currentHash", current));
            return response;
        } catch (IOException e) {
            response.setConsistent(false);
            response.setMessage("校验失败：无法读取物理文件");
            return response;
        }
    }

    public Path download(Long id, JwtUser operator) {
        Book book = requireBook(id);
        bookMapper.update(null, new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, id).setSql("download_count = download_count + 1"));
        insertTrace(id, "DOWNLOAD", operator == null ? null : operator.getId(), null);
        if (operator != null) {
            userActionService.record(operator.getId(), "DOWNLOAD", "下载书籍: " + book.getTitle());
        }
        return storageService.resolveBook(book.getFilePath());
    }

    /**
     * 在线阅读：仅已通过审核的书籍可公开访问物理文件。
     */
    public Path fileForRead(Long id, Long currentUserId) {
        Book book = requireBook(id);
        if (book.getStatus() != Book.STATUS_APPROVED
                && (currentUserId == null || !book.getUploaderId().equals(currentUserId))) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "书籍不存在或未通过审核");
        }
        return storageService.resolveBook(book.getFilePath());
    }

    public String txtContent(Long id) throws IOException {
        Book book = requireBook(id);
        if (!"txt".equalsIgnoreCase(book.getFileFormat())) {
            throw new BusinessException("仅 TXT 格式支持在线阅读翻页");
        }
        Path path = storageService.resolveBook(book.getFilePath());
        if (Files.size(path) > TXT_CONTENT_LIMIT) {
            throw new BusinessException("TXT 文件过大（>10MB），请下载后阅读");
        }
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public ReadingProgress getProgress(Long bookId, Long userId) {
        return readingProgressMapper.selectOne(new LambdaQueryWrapper<ReadingProgress>()
                .eq(ReadingProgress::getBookId, bookId).eq(ReadingProgress::getUserId, userId));
    }

    public void saveProgress(Long bookId, Long userId, String position) {
        ReadingProgress existing = getProgress(bookId, userId);
        if (existing == null) {
            existing = new ReadingProgress();
            existing.setBookId(bookId);
            existing.setUserId(userId);
            existing.setPosition(position);
            existing.setUpdatedAt(now());
            readingProgressMapper.insert(existing);
        } else {
            existing.setPosition(position);
            existing.setUpdatedAt(now());
            readingProgressMapper.updateById(existing);
        }
    }

    // ---------- 管理端 ----------

    public PageResult<BookVO> adminList(BookQuery query) {
        Page<Book> page = new Page<>(Math.max(1, query.getPage()), Math.min(200, Math.max(1, query.getSize())));
        LambdaQueryWrapper<Book> wrapper = new LambdaQueryWrapper<>();
        if (query.getStatus() != null) {
            wrapper.eq(Book::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like(Book::getTitle, kw).or().like(Book::getAuthor, kw).or().like(Book::getIsbn, kw));
        }
        wrapper.orderByDesc(Book::getCreatedAt);
        Page<Book> result = bookMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getCurrent(), result.getSize(),
                toVOList(result.getRecords(), null));
    }

    @Transactional
    public void review(Long id, int status, String reason, JwtUser admin) {
        Book book = requireBook(id);
        book.setStatus(status);
        book.setUpdatedAt(now());
        bookMapper.updateById(book);
        insertTrace(id, status == Book.STATUS_APPROVED ? "APPROVE" : "REJECT", admin.getId(),
                json("status", status, "reason", reason));
        userActionService.record(admin.getId(), "REVIEW_BOOK",
                (status == Book.STATUS_APPROVED ? "通过审核: " : "驳回审核: ") + book.getTitle());
    }

    @Transactional
    public void deleteBook(Long id, JwtUser admin) {
        Book book = requireBook(id);
        insertTrace(id, "DELETE", admin.getId(), json("title", book.getTitle()));
        bookMapper.deleteById(id);
        traceLogMapper.delete(new LambdaQueryWrapper<TraceLog>().eq(TraceLog::getBookId, id));
        versionHistoryMapper.delete(new LambdaQueryWrapper<VersionHistory>().eq(VersionHistory::getBookId, id));
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>().eq(Favorite::getBookId, id));
        readingProgressMapper.delete(new LambdaQueryWrapper<ReadingProgress>().eq(ReadingProgress::getBookId, id));
        storageService.deleteQuietly(storageService.resolveBook(book.getFilePath()));
        if (book.getCoverUrl() != null && book.getCoverUrl().startsWith("/api/v1/files/covers/")) {
            String rel = book.getCoverUrl().substring("/api/v1/files/covers/".length());
            try {
                storageService.deleteQuietly(storageService.resolveCover(rel));
            } catch (Exception ignored) {
            }
        }
        userActionService.record(admin.getId(), "DELETE_BOOK", "删除书籍: " + book.getTitle());
    }

    // ---------- VO 组装 ----------

    public List<BookVO> toVOList(List<Book> books, Long currentUserId) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> categoryNames = categoryNames(books);
        Map<Long, String> userNames = userNames(books);
        Map<Long, String> userAvatars = userAvatars(books);
        Set<Long> favoriteIds = favoriteIds(books, currentUserId);
        List<BookVO> vos = new ArrayList<>(books.size());
        for (Book book : books) {
            BookVO vo = toVO(book, null);
            vo.setCategoryName(categoryNames.get(book.getCategoryId()));
            vo.setUploaderName(userNames.get(book.getUploaderId()));
            vo.setUploaderAvatar(userAvatars.get(book.getUploaderId()));
            vo.setFavorite(currentUserId != null && favoriteIds.contains(book.getId()));
            vos.add(vo);
        }
        return vos;
    }

    public BookVO toVO(Book book, Long currentUserId) {
        BookVO vo = new BookVO();
        vo.setId(book.getId());
        vo.setTitle(book.getTitle());
        vo.setAuthor(book.getAuthor());
        vo.setIsbn(book.getIsbn());
        vo.setDescription(book.getDescription());
        vo.setCoverUrl(book.getCoverUrl());
        vo.setCategoryId(book.getCategoryId());
        vo.setFileFormat(book.getFileFormat());
        vo.setFileSize(book.getFileSize());
        vo.setFileHash(book.getFileHash());
        vo.setVersionNo(book.getVersionNo());
        vo.setUploaderId(book.getUploaderId());
        vo.setUploadIp(book.getUploadIp());
        vo.setStatus(book.getStatus());
        vo.setDownloadCount(book.getDownloadCount());
        vo.setCreatedAt(book.getCreatedAt());
        vo.setUpdatedAt(book.getUpdatedAt());
        if (book.getCategoryId() != null) {
            Category category = categoryMapper.selectById(book.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
        if (book.getUploaderId() != null) {
            SysUser uploader = userMapper.selectById(book.getUploaderId());
            vo.setUploaderName(uploader == null ? "未知" : displayName(uploader));
            vo.setUploaderAvatar(uploader == null ? null : uploader.getAvatarUrl());
        }
        if (currentUserId != null) {
            Long count = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getBookId, book.getId()).eq(Favorite::getUserId, currentUserId));
            vo.setFavorite(count != null && count > 0);
        }
        return vo;
    }

    private Book requireBook(Long id) {
        Book book = bookMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "书籍不存在");
        }
        return book;
    }

    private Map<Long, String> categoryNames(List<Book> books) {
        Set<Long> ids = books.stream().map(Book::getCategoryId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private Map<Long, String> userNames(List<Book> books) {
        Set<Long> ids = books.stream().map(Book::getUploaderId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, this::displayName));
    }

    private Map<Long, String> userAvatars(List<Book> books) {
        Set<Long> ids = books.stream().map(Book::getUploaderId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>();
        userMapper.selectBatchIds(ids).forEach(u -> result.put(u.getId(), u.getAvatarUrl()));
        return result;
    }

    private Set<Long> favoriteIds(List<Book> books, Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        Set<Long> bookIds = books.stream().map(Book::getId).collect(Collectors.toSet());
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId).in(Favorite::getBookId, bookIds))
                .stream().map(Favorite::getBookId).collect(Collectors.toSet());
    }

    private String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname();
    }

    private String now() {
        return LocalDateTime.now().format(TIME);
    }
}
