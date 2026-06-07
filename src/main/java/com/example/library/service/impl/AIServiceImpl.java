package com.example.library.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.library.dto.ChatMessage;
import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.entity.ChatHistory;
import com.example.library.entity.User;
import com.example.library.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AIServiceImpl implements AIService {
    //注入依赖
    @Autowired
    ChatHistoryService chatHistoryService;
    @Autowired
    BorrowService borrowService;
    @Autowired
    BookService bookService;
    @Autowired
    UserService userService;
    @Autowired
    private ObjectMapper objectMapper;

    //注入deepseek配置
    @Value("${deepseek.api-url}")
    private String deepseekApiUrl;
    @Value("${deepseek.api-key}")
    private String deepseekApiKey;
    @Value("${deepseek.model}")
    private String deepseekModel;

    @Override
    public ChatMessage sendMessage(String message, Long userId) {

        //查用户借阅记录
        List<Borrow> borrows = borrowService.lambdaQuery()
                .eq(Borrow::getUserId, userId).list();
        String borrowHistory = borrows.isEmpty() ? "无借阅记录" : borrows.stream()
                .map(b -> bookService.getById(b.getBookId()))     //流水线操作
                .filter(Objects::nonNull)   //过滤
                .map(Book::getTitle)  //映射
                .collect(Collectors.joining("、"));  //连接

        //查相似书友（（Jaccard 系数））
        Set<Long> myBookIds = borrows.stream()
                .map(Borrow::getBookId)
                .collect(Collectors.toSet());

        List<User> allUsers = userService.list();
        allUsers.removeIf(u -> u.getId().equals(userId)); // 排除自己

        List<Map<String, Object>> similarUsers = new ArrayList<>();
        for (User other : allUsers) {
            List<Borrow> otherBorrows = borrowService.lambdaQuery()
                    .eq(Borrow::getUserId, other.getId())
                    .list();
            Set<Long> otherBookIds = otherBorrows.stream()
                    .map(Borrow::getBookId)
                    .collect(Collectors.toSet());

            if (otherBookIds.isEmpty()) continue;

            // Jaccard = 交集大小 / 并集大小
            Set<Long> intersection = new HashSet<>(myBookIds);
            intersection.retainAll(otherBookIds);               // 交集：两者都有的书

            Set<Long> union = new HashSet<>(myBookIds);
            union.addAll(otherBookIds);                         // 并集：两者所有不重复的书

            double similarity = (double) intersection.size() / union.size();

            if (similarity > 0) {
                String commonBooks = intersection.stream()
                        .map(id -> bookService.getById(id))
                        .filter(Objects::nonNull)
                        .map(Book::getTitle)
                        .collect(Collectors.joining("、"));

                Map<String, Object> map = new HashMap<>();
                map.put("username", other.getUsername());
                map.put("similarity", String.format("%.0f%%", similarity * 100));
                map.put("similarityValue", similarity);   // 排序用
                map.put("commonBooks", commonBooks);
                similarUsers.add(map);
            }
        }

        // 按相似度从高到低排序，取前3
        similarUsers.sort((a, b) -> Double.compare(
                (double) b.get("similarityValue"), (double) a.get("similarityValue")
        ));
        if (similarUsers.size() > 3) {
            similarUsers = similarUsers.subList(0, 3);
        }

        // 拼成文本
        String bookFriendInfo;
        if (similarUsers.isEmpty()) {
            bookFriendInfo = "暂无相似书友";
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> u : similarUsers) {
                sb.append(u.get("username"))
                        .append("（相似度").append(u.get("similarity"))
                        .append("，共同读过：").append(u.get("commonBooks")).append("）\n");
            }
            bookFriendInfo = sb.toString();
        }

        //查全馆图书
        List<Book> allBooks = bookService.list();
        String bookList = allBooks.stream()
                .map(b -> b.getTitle() + "（" + b.getAuthor() + "）")
                .collect(Collectors.joining("\n"));
        //查最近对话
        List<ChatHistory> histories = chatHistoryService.lambdaQuery()
                .eq(ChatHistory::getUserId, userId)
                .orderByDesc(ChatHistory::getCreateTime)
                .last("limit 6")  // 上下文6条，控制token
                .list();
        Collections.reverse((histories));
        String recentChats = histories.isEmpty() ? "" : histories.stream()
                .map(h -> h.getRole() + ":" + h.getContent())
                .collect(Collectors.joining("\n"));
        //查用户
        User user = userService.getById(userId);
        String username = user != null ? user.getUsername() : "读者";

        //拼接prompt
        String systemPrompt = String.join("\n",
                "你是大学图书馆的AI助手「小书」，友好热情，回复简短（不超过200字）。\n" +
                        "核心原则：根据用户的实际问题自然回应，不要强行推荐或分析。\n" +
                        "只有当用户明确请你推荐书时，才根据借阅历史推荐馆内已有的书。\n" +
                        "只有当用户问到书友或阅读偏好时，才分析偏好、匹配书友。\n" +
                        "提到馆内藏书时，用《书名》格式标注。"
        );
        String userPrompt = String.join("\n",
                "[当前用户]" + username,
                "[可用背景信息]\n" +
                        "- 借阅历史：" + borrowHistory + "\n" +
                        "- 图书馆馆藏：" + allBooks.size() + "本书\n" +
                        "- 书友匹配：" + bookFriendInfo,
                "[最近对话]\n" + (recentChats.isEmpty() ? "无" : recentChats),
                "[用户问题]" + message
        );

        //调用deepseek
        //设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepseekApiKey);//身份凭证

        List<Map<String, String>> messages = new ArrayList<>(); //创建一个消息列表，存对话

        Map<String, String> sysMsg = new HashMap<>();  //一条发给ai的对话
        sysMsg.put("role", "system");    //系统角色设定
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg); //加到列表

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");     //用户提问
        userMsg.put("content", userPrompt);
        messages.add(userMsg); //加到列表

        //构建请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", deepseekModel);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 连接超时 5 秒
        factory.setReadTimeout(30000);     // 读取超时 30 秒
        RestTemplate restTemplate = new RestTemplate(factory);

        String reply;
        boolean success = false;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    deepseekApiUrl+"/chat/completions",
                    requestEntity,
                    String.class

            );
            JsonNode root = objectMapper.readTree(response.getBody());
            reply = root.path("choices").get(0).path("message").path("content").asText();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
            reply = "抱歉，AI服务暂时不可用，请稍后再试。";
        }
        //找书
        List<Map<String, Object>> matchedBooks = new ArrayList<>();
        Pattern pattern = Pattern.compile("《(.+?)》");
        Matcher matcher = pattern.matcher(reply);
        while (matcher.find()) {
            String bookTitle = matcher.group(1); // 提取书名号中间的标题
            for (Book b : allBooks) {
                if (b.getTitle().equals(bookTitle)) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", b.getId());
                    map.put("title", b.getTitle());
                    matchedBooks.add(map);
                    break; // 找到就跳出内层循环
                }
            }
        }

        //保存用户对话记录
        ChatHistory userHistory = new ChatHistory();
        userHistory.setUserId(userId);
        userHistory.setRole("user");
        userHistory.setContent(message);
        chatHistoryService.save(userHistory);

        //保存ai回答记录（仅成功时保存）
        if (success) {
            ChatHistory aiHistory = new ChatHistory();
            aiHistory.setUserId(userId);
            aiHistory.setRole("assistant");
            aiHistory.setContent(reply);
            chatHistoryService.save(aiHistory);
        }

        return new ChatMessage(message, reply, matchedBooks);
    }
    @Override
    public void clearHistory(Long userId){
        chatHistoryService.lambdaUpdate()
                .eq(ChatHistory::getUserId,userId)
                .remove();
    }
}