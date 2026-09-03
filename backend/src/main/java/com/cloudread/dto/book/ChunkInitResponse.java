package com.cloudread.dto.book;

public class ChunkInitResponse {

    private String uploadId;
    private long chunkSize;

    public ChunkInitResponse() {
    }

    public ChunkInitResponse(String uploadId, long chunkSize) {
        this.uploadId = uploadId;
        this.chunkSize = chunkSize;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(long chunkSize) {
        this.chunkSize = chunkSize;
    }
}
