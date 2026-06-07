package com.example.library.service;

import com.example.library.dto.ChatMessage;

public interface AIService {
    ChatMessage sendMessage(String message,Long userId);
    void clearHistory(Long userId);
}
