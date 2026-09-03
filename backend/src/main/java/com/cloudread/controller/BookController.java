package com.cloudread.controller;

import com.cloudread.common.BusinessException;
import com.cloudread.common.IpUtils;
import com.cloudread.common.PageResult;
import com.cloudread.common.Result;
import com.cloudread.dto.book.BookMeta;
import com.cloudread.dto.book.BookQuery;
import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.book.ChunkInitRequest;
import com.cloudread.dto.book.ChunkInitResponse;
import com.cloudread.dto.book.FileRefResponse;
import com.cloudread.dto.book.ProgressRequest;
import com.cloudread.dto.book.TraceResponse;
import com.cloudread.dto.book.VerifyResponse;
import com.cloudread.entity.ReadingProgress;
import com.cloudread.security.JwtUser;
import com.cloudread.security.SecurityUtils;
import com.cloudread.service.BookService;
import com.cloudread.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Tag(name = "书籍模块", description = "上传、分片、检索、阅读、下载、溯源")
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final BookService bookService;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;

    public BookController(BookService bookService, StorageService storageService, ObjectMapper objectMapper) {
        this.bookService = bookService;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "上传电子书文件（返回哈希与文件引用）")
    @PostMapping("/file")
    public Result<FileRefResponse> uploadFile(@RequestParam("file") MultipartFile file,
                                              HttpServletRequest request) {
        JwtUser user = SecurityUtils.requireUser();
        return Result.ok(bookService.storeFile(file, user.getId(), IpUtils.clientIp(request)));
    }

    @Operation(summary = "初始化分片上传")
    @PostMapping("/upload/init")
    public Result<ChunkInitResponse> initChunk(@Valid @RequestBody ChunkInitRequest request) {
        SecurityUtils.requireUser();
        return Result.ok(storageService.initChunk(request.getFileName(), request.getFileSize(),
                request.getTotalChunks()));
    }

    @Operation(summary = "上传单个分片")
    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(@RequestParam("uploadId") String uploadId,
                                    @RequestParam("index") int index,
                                    @RequestParam("chunk") MultipartFile chunk) {
        SecurityUtils.requireUser();
        storageService.saveChunk(uploadId, index, chunk);
        return Result.ok();
    }

    @Operation(summary = "合并分片并计算哈希")
    @PostMapping("/upload/complete")
    public Result<FileRefResponse> completeChunk(@RequestParam("uploadId") String uploadId,
                                                 @RequestParam("originalName") String originalName,
                                                 HttpServletRequest request) {
        JwtUser user = SecurityUtils.requireUser();
        return Result.ok(bookService.completeChunk(uploadId, originalName, user.getId(), IpUtils.clientIp(request)));
    }

    @Operation(summary = "提交书籍元数据完成入库")
    @PostMapping("/commit")
    public Result<BookVO> commit(@RequestParam("meta") String metaJson,
                                 @RequestParam("hash") String hash,
                                 @RequestParam("relativePath") String relativePath,
                                 @RequestParam("format") String format,
                                 @RequestParam("size") long size,
                                 @RequestParam("originalName") String originalName,
                                 @RequestPart(value = "cover", required = false) MultipartFile cover,
                                 HttpServletRequest request) {
        JwtUser user = SecurityUtils.requireUser();
        BookMeta meta;
        try {
            meta = objectMapper.readValue(metaJson, BookMeta.class);
        } catch (IOException e) {
            throw new BusinessException("书籍元数据格式错误");
        }
        FileRefResponse fileRef = new FileRefResponse();
        fileRef.setHash(hash);
        fileRef.setRelativePath(relativePath);
        fileRef.setFormat(format);
        fileRef.setSize(size);
        fileRef.setOriginalName(originalName);
        return Result.ok(bookService.commit(meta, fileRef, cover, user, IpUtils.clientIp(request)));
    }

    @Operation(summary = "分页查询已上架书籍")
    @GetMapping
    public Result<PageResult<BookVO>> list(BookQuery query) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(bookService.list(query, userId));
    }

    @Operation(summary = "我的上传")
    @GetMapping("/my")
    public Result<List<BookVO>> myUploads() {
        return Result.ok(bookService.myUploads(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "书籍详情")
    @GetMapping("/{id}")
    public Result<BookVO> detail(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        return Result.ok(bookService.detail(id, userId));
    }

    @Operation(summary = "获取溯源信息")
    @GetMapping("/{id}/trace")
    public Result<TraceResponse> trace(@PathVariable Long id) {
        return Result.ok(bookService.trace(id));
    }

    @Operation(summary = "实时校验文件是否被篡改")
    @PostMapping("/{id}/verify")
    public Result<VerifyResponse> verify(@PathVariable Long id) {
        JwtUser user = SecurityUtils.currentUser().orElse(null);
        return Result.ok(bookService.verify(id, user));
    }

    @Operation(summary = "在线阅读文件流（inline）")
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUser().map(JwtUser::getId).orElse(null);
        Path path = bookService.fileForRead(id, userId);
        BookVO book = bookService.detail(id, userId);
        String filename = book.getTitle() + "." + book.getFileFormat();
        return resourceResponse(path, filename, true);
    }

    @Operation(summary = "下载原文件（记录下载次数）")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        JwtUser user = SecurityUtils.currentUser().orElse(null);
        Path path = bookService.download(id, user);
        BookVO book = bookService.detail(id, user == null ? null : user.getId());
        String filename = book.getTitle() + "." + book.getFileFormat();
        return resourceResponse(path, filename, false);
    }

    @Operation(summary = "TXT 全文内容（用于翻页阅读）")
    @GetMapping("/{id}/content")
    public Result<String> content(@PathVariable Long id) {
        try {
            return Result.ok(bookService.txtContent(id));
        } catch (IOException e) {
            throw new BusinessException("读取文本内容失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取我的阅读进度")
    @GetMapping("/{id}/progress")
    public Result<ReadingProgress> progress(@PathVariable Long id) {
        return Result.ok(bookService.getProgress(id, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "保存阅读进度")
    @PutMapping("/{id}/progress")
    public Result<Void> saveProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest request) {
        bookService.saveProgress(id, SecurityUtils.currentUserId(), request.getPosition());
        return Result.ok();
    }

    private ResponseEntity<Resource> resourceResponse(Path path, String filename, boolean inline) {
        Resource resource = new FileSystemResource(path);
        ContentDisposition disposition = ContentDisposition.builder(inline ? "inline" : "attachment")
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        MediaType mediaType = mediaType(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType)
                .contentLength(sizeOf(path))
                .body(resource);
    }

    private long sizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }

    private MediaType mediaType(String filename) {
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (name.endsWith(".txt")) {
            return MediaType.parseMediaType("text/plain;charset=UTF-8");
        }
        if (name.endsWith(".epub")) {
            return MediaType.parseMediaType("application/epub+zip");
        }
        if (name.endsWith(".mobi")) {
            return MediaType.parseMediaType("application/x-mobipocket-ebook");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
