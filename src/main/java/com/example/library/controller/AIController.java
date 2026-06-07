package com.example.library.controller;

import com.example.library.dto.ChatMessage;
import com.example.library.entity.ChatHistory;
import com.example.library.entity.User;
import com.example.library.service.AIService;
import com.example.library.service.ChatHistoryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/ai")
public class AIController {
    @Autowired
    private AIService aiService;
    @Autowired
    private ChatHistoryService chatHistoryService;

    @GetMapping("/chat")
    public String chat(Model model, HttpSession session){
        User user = (User) session.getAttribute("user");
        List<ChatHistory> histories = chatHistoryService.lambdaQuery()
                .eq(ChatHistory::getUserId, user.getId())
                .orderByAsc(ChatHistory::getCreateTime)  // 时间正序，页面从上到下显示
                .list();
        model.addAttribute("histories", histories);
        return "ai-chat";
    }

    @PostMapping("/send")
    @ResponseBody
    public ChatMessage send(@RequestParam String message, HttpSession session) {
        User user = (User) session.getAttribute("user");
        try {
            return aiService.sendMessage(message, user.getId());
        } catch (Exception e) {
            e.printStackTrace();
            ChatMessage error = new ChatMessage();
            error.setReply("抱歉，AI 服务暂时不可用，请稍后再试。");
            return error;
        }
    }

    @PostMapping("/clear")
    public String clear(HttpSession session) {
        User user = (User) session.getAttribute("user");
        aiService.clearHistory(user.getId());
        return "redirect:/ai/chat";
    }
}
