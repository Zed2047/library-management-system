package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.entity.ChatHistory;
import com.example.library.mapper.ChatHistoryMapper;
import com.example.library.service.ChatHistoryService;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

}
