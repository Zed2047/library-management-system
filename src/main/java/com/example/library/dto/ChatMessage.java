package com.example.library.dto;

import java.util.List;
import java.util.Map;

public class ChatMessage {
    private String message;
    private String reply;
    private List<Map<String, Object>> books;

    public ChatMessage() {}
    public ChatMessage(String message) {
        this.message = message;
    }

    public ChatMessage(String message, String reply, List<Map<String, Object>> books) {
        this.message = message;
        this.reply = reply;
        this.books = books;
    }

    //Message
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    //Reply
    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    //Books
    public List<Map<String, Object>> getBooks() {
        return books;
    }
    public void setBooks(List<Map<String, Object>> books){
        this.books=books;
    }
}
