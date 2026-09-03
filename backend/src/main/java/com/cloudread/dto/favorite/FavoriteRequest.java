package com.cloudread.dto.favorite;

import jakarta.validation.constraints.NotNull;

public class FavoriteRequest {

    @NotNull(message = "书籍ID不能为空")
    private Long bookId;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}
