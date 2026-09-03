package com.cloudread.storage;

import com.cloudread.common.BusinessException;
import com.cloudread.common.Result;
import com.cloudread.dto.book.ChunkInitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);
    public static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024;

    private final Path uploadRoot;
    private final Path coverRoot;
    private final Path imageRoot;
    private final Path avatarRoot;
    private final Path tmpRoot;
    private final HashService hashService;

    public StorageService(@Value("${app.upload-dir}") String uploadDir,
                          @Value("${app.cover-dir}") String coverDir,
                          @Value("${app.image-dir}") String imageDir,
                          @Value("${app.avatar-dir}") String avatarDir,
                          @Value("${app.tmp-dir}") String tmpDir,
                          HashService hashService) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.coverRoot = Paths.get(coverDir).toAbsolutePath().normalize();
        this.imageRoot = Paths.get(imageDir).toAbsolutePath().normalize();
        this.avatarRoot = Paths.get(avatarDir).toAbsolutePath().normalize();
        this.tmpRoot = Paths.get(tmpDir).toAbsolutePath().normalize();
        this.hashService = hashService;
    }

    public StoredFile storeBookFile(MultipartFile file, String hash) {
        String ext = extension(file.getOriginalFilename());
        Path dir = datedDir(uploadRoot);
        Path target = dir.resolve(hash + ext);
        copyTo(file, target);
        return new StoredFile(rel(uploadRoot, target), target, file.getSize(), hash,
                ext.replace(".", "").toLowerCase(), file.getOriginalFilename());
    }

    public StoredFile storeCover(MultipartFile file, String hash) {
        String ext = extension(file.getOriginalFilename());
        if (!".jpg".equalsIgnoreCase(ext) && !".jpeg".equalsIgnoreCase(ext) && !".png".equalsIgnoreCase(ext)) {
            throw new BusinessException("封面仅支持 JPG/PNG 格式");
        }
        Path dir = datedDir(coverRoot);
        String finalExt = ".jpeg".equalsIgnoreCase(ext) ? ".jpg" : ext;
        Path target = dir.resolve(hash + finalExt);
        copyTo(file, target);
        String format = finalExt.replace(".", "").toLowerCase();
        return new StoredFile(rel(coverRoot, target), target, file.getSize(), hash, format, file.getOriginalFilename());
    }

    public StoredFile storeCoverBytes(byte[] bytes, String hash, String ext) throws IOException {
        Path dir = datedDir(coverRoot);
        String name = hash + (ext.startsWith(".") ? ext : "." + ext);
        Path target = dir.resolve(name);
        Files.write(target, bytes);
        return new StoredFile(rel(coverRoot, target), target, bytes.length, hash, ext, "generated-cover");
    }

    /**
     * 存储讨论帖图片，校验扩展名、大小与文件头，返回可访问的 URL 列表。
     */
    public List<String> storePostImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        if (images.size() > 9) {
            throw new BusinessException("帖子最多支持 9 张图片");
        }
        List<String> urls = new java.util.ArrayList<>(images.size());
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            String ext = extension(image.getOriginalFilename());
            if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext)
                    && !".gif".equals(ext) && !".webp".equals(ext)) {
                throw new BusinessException("帖子图片仅支持 JPG/PNG/GIF/WEBP 格式");
            }
            if (image.getSize() > 10L * 1024 * 1024) {
                throw new BusinessException("单张图片不能超过 10MB");
            }
            validateImageMagic(image);
            String name = UUID.randomUUID().toString().replace("-", "");
            Path dir = datedDir(imageRoot);
            Path target = dir.resolve(name + ext);
            copyTo(image, target);
            urls.add("/api/v1/files/images/" + rel(imageRoot, target));
        }
        return urls;
    }

    /**
     * 存储用户头像：JPG/PNG/WEBP，不超过 5MB，文件头校验。
     */
    public String storeAvatar(MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        String ext = extension(avatar.getOriginalFilename());
        if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext) && !".webp".equals(ext)) {
            throw new BusinessException("头像仅支持 JPG/PNG/WEBP 格式");
        }
        if (avatar.getSize() > 5L * 1024 * 1024) {
            throw new BusinessException("头像不能超过 5MB");
        }
        validateImageMagic(avatar);
        String name = UUID.randomUUID().toString().replace("-", "");
        Path dir = datedDir(avatarRoot);
        Path target = dir.resolve(name + ext);
        copyTo(avatar, target);
        return "/api/v1/files/avatars/" + rel(avatarRoot, target);
    }

    private void validateImageMagic(MultipartFile image) {
        try (InputStream in = image.getInputStream()) {
            byte[] head = in.readNBytes(12);
            boolean ok = (head.length > 2 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF)
                    || (head.length > 3 && (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G')
                    || (head.length > 3 && head[0] == 'G' && head[1] == 'I' && head[2] == 'F' && head[3] == '8')
                    || (head.length > 11 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                    && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P');
            if (!ok) {
                throw new BusinessException("图片文件头校验失败，文件可能不是有效图片");
            }
        } catch (IOException e) {
            throw new BusinessException("读取图片失败: " + e.getMessage());
        }
    }

    /**
     * 根据数据库相对路径解析物理文件，并防止路径穿越。
     */
    public Path resolveBook(String relativePath) {
        Path p = uploadRoot.resolve(relativePath).normalize();
        if (!p.startsWith(uploadRoot)) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "非法的文件路径");
        }
        if (!Files.exists(p)) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "物理文件不存在或已被移除");
        }
        return p;
    }

    public Path resolveCover(String relativePath) {
        Path p = coverRoot.resolve(relativePath).normalize();
        if (!p.startsWith(coverRoot)) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "非法的文件路径");
        }
        if (!Files.exists(p)) {
            throw new BusinessException(Result.CODE_NOT_FOUND, "封面文件不存在");
        }
        return p;
    }

    public ChunkInitResponse initChunk(String fileName, long fileSize, int totalChunks) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Path dir = tmpRoot.resolve(uploadId);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建分片目录失败");
        }
        return new ChunkInitResponse(uploadId, DEFAULT_CHUNK_SIZE);
    }

    public void saveChunk(String uploadId, int index, MultipartFile chunk) {
        Path dir = tmpRoot.resolve(uploadId).normalize();
        if (!dir.startsWith(tmpRoot) || !Files.isDirectory(dir)) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "无效的分片上传会话");
        }
        Path target = dir.resolve(index + ".part");
        copyTo(chunk, target);
    }

    public StoredFile completeChunk(String uploadId, String originalName) {
        Path dir = tmpRoot.resolve(uploadId).normalize();
        if (!dir.startsWith(tmpRoot) || !Files.isDirectory(dir)) {
            throw new BusinessException(Result.CODE_BAD_REQUEST, "无效的分片上传会话");
        }
        try {
            Path merged = Files.createTempFile(tmpRoot, "merge-", ".tmp");
            try (OutputStream out = Files.newOutputStream(merged)) {
                List<Path> parts;
                try (Stream<Path> files = Files.list(dir)
                        .sorted(Comparator.comparingInt(p -> Integer.parseInt(p.getFileName().toString().replace(".part", ""))))) {
                    parts = files.toList();
                }
                if (parts.isEmpty()) {
                    throw new BusinessException("未接收到任何分片");
                }
                for (Path part : parts) {
                    Files.copy(part, out);
                }
            }
            String hash = hashService.sha256(merged);
            String ext = extension(originalName);
            Path targetDir = datedDir(uploadRoot);
            Path target = targetDir.resolve(hash + ext);
            if (Files.exists(target)) {
                Files.deleteIfExists(merged);
            } else {
                Files.move(merged, target, StandardCopyOption.ATOMIC_MOVE);
            }
            cleanup(uploadId);
            return new StoredFile(rel(uploadRoot, target), target, Files.size(target), hash,
                    ext.replace(".", "").toLowerCase(), originalName);
        } catch (IOException e) {
            throw new BusinessException("合并分片失败: " + e.getMessage());
        }
    }

    public void cleanup(String uploadId) {
        Path dir = tmpRoot.resolve(uploadId).normalize();
        if (!dir.startsWith(tmpRoot)) {
            return;
        }
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> walk = Files.walk(dir)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            }
        } catch (IOException e) {
            log.warn("清理分片目录失败: {}", e.getMessage());
        }
    }

    public void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", e.getMessage());
        }
    }

    private Path datedDir(Path root) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path dir = root.resolve(date);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException("创建存储目录失败");
        }
        return dir;
    }

    private void copyTo(MultipartFile file, Path target) {
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target);
        } catch (IOException e) {
            throw new BusinessException("文件写入失败: " + e.getMessage());
        }
    }

    private String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        String ext = name.substring(name.lastIndexOf('.'));
        return ext.length() > 10 ? "" : ext.toLowerCase();
    }

    private String rel(Path root, Path target) {
        return root.relativize(target).toString().replace('\\', '/');
    }
}
