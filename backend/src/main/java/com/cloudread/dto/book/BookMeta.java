package com.cloudread.dto.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BookMeta {

    @NotBlank(message = "书名不能为空")
    @Size(max = 128, message = "书名长度不能超过128")
    private String title;

    @Size(max = 64, message = "作者长度不能超过64")
    private String author;

    @Size(max = 32, message = "ISBN长度不能超过32")
    private String isbn;

    @Size(max = 2000, message = "简介长度不能超过2000")
    private String description;

    private Long categoryId;

    /** 重复文件选择“作为新版本上传”时，携带已存在书籍 ID */
    private Long versionOf;

    @Size(max = 500, message = "版本说明长度不能超过500")
    private String changeLog;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getVersionOf() {
        return versionOf;
    }

    public void setVersionOf(Long versionOf) {
        this.versionOf = versionOf;
    }

    public String getChangeLog() {
        return changeLog;
    }

    public void setChangeLog(String changeLog) {
        this.changeLog = changeLog;
    }
}
