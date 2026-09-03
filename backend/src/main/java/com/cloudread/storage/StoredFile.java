package com.cloudread.storage;

public class StoredFile {

    private final String relativePath;
    private final java.nio.file.Path absolutePath;
    private final long size;
    private final String hash;
    private final String format;
    private final String originalName;

    public StoredFile(String relativePath, java.nio.file.Path absolutePath, long size,
                      String hash, String format, String originalName) {
        this.relativePath = relativePath;
        this.absolutePath = absolutePath;
        this.size = size;
        this.hash = hash;
        this.format = format;
        this.originalName = originalName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public java.nio.file.Path getAbsolutePath() {
        return absolutePath;
    }

    public long getSize() {
        return size;
    }

    public String getHash() {
        return hash;
    }

    public String getFormat() {
        return format;
    }

    public String getOriginalName() {
        return originalName;
    }
}
