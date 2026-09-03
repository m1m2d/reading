package com.cloudread.dto.book;

public class FileRefResponse {

    private String hash;
    private String relativePath;
    private long size;
    private String format;
    private String originalName;
    private boolean duplicate;
    private Long existingBookId;
    private String existingTitle;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }

    public Long getExistingBookId() {
        return existingBookId;
    }

    public void setExistingBookId(Long existingBookId) {
        this.existingBookId = existingBookId;
    }

    public String getExistingTitle() {
        return existingTitle;
    }

    public void setExistingTitle(String existingTitle) {
        this.existingTitle = existingTitle;
    }
}
