package com.cloudread.dto.book;

import com.cloudread.entity.TraceLog;
import com.cloudread.entity.VersionHistory;

import java.util.List;

public class TraceResponse {

    private String title;
    private String fileHash;
    private String fileFormat;
    private long fileSize;
    private Integer versionNo;
    private String uploaderName;
    private String uploadIp;
    private String createdAt;
    private List<TraceLog> traceLogs;
    private List<VersionHistory> versions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getUploadIp() {
        return uploadIp;
    }

    public void setUploadIp(String uploadIp) {
        this.uploadIp = uploadIp;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<TraceLog> getTraceLogs() {
        return traceLogs;
    }

    public void setTraceLogs(List<TraceLog> traceLogs) {
        this.traceLogs = traceLogs;
    }

    public List<VersionHistory> getVersions() {
        return versions;
    }

    public void setVersions(List<VersionHistory> versions) {
        this.versions = versions;
    }
}
