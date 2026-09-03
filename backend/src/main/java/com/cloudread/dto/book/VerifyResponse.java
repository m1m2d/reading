package com.cloudread.dto.book;

public class VerifyResponse {

    private boolean consistent;
    private String recordedHash;
    private String currentHash;
    private String message;

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }

    public String getRecordedHash() {
        return recordedHash;
    }

    public void setRecordedHash(String recordedHash) {
        this.recordedHash = recordedHash;
    }

    public String getCurrentHash() {
        return currentHash;
    }

    public void setCurrentHash(String currentHash) {
        this.currentHash = currentHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
