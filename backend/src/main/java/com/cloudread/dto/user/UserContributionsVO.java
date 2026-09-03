package com.cloudread.dto.user;

import com.cloudread.dto.book.BookVO;
import com.cloudread.dto.post.PostVO;

import java.util.List;

public class UserContributionsVO {

    private List<BookVO> books;
    private List<PostVO> posts;

    public UserContributionsVO() {
    }

    public UserContributionsVO(List<BookVO> books, List<PostVO> posts) {
        this.books = books;
        this.posts = posts;
    }

    public List<BookVO> getBooks() {
        return books;
    }

    public void setBooks(List<BookVO> books) {
        this.books = books;
    }

    public List<PostVO> getPosts() {
        return posts;
    }

    public void setPosts(List<PostVO> posts) {
        this.posts = posts;
    }
}
