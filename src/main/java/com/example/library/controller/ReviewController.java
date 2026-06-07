package com.example.library.controller;

import com.example.library.entity.Review;
import com.example.library.entity.User;
import com.example.library.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/review")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    @PostMapping("/add")
    public String add(@RequestParam Long bookId, @RequestParam(required=false) String content, @RequestParam(defaultValue = "3") int rating, HttpSession session, RedirectAttributes redirectAttributes){
        if (rating < 1 || rating > 5) {
            redirectAttributes.addFlashAttribute("error", "请选择1-5星评分");
            return "redirect:/book/detail?id=" + bookId;
        }
        if (content != null && content.length() > 500) {
            redirectAttributes.addFlashAttribute("error", "评价内容不能超过500字");
            return "redirect:/book/detail?id=" + bookId;
        }
        User user=(User)session.getAttribute("user");
        Review review=new Review();
        review.setUserId(user.getId());
        review.setBookId(bookId);
        review.setContent(content);
        review.setRating(rating);
        reviewService.save(review);

        return "redirect:/book/detail?id="+bookId;
    }
    @GetMapping("/delete")
    public String delete(@RequestParam Long id,HttpSession session){
        Review review=reviewService.getById(id);
        if (review == null) {
            return "redirect:/book/list";
        }
        User user=(User)session.getAttribute("user");
        if(user.getId().equals(review.getUserId()) || "admin".equals(user.getRole())){ // 当前用户 或 管理员
            reviewService.removeById(id);
        }
        return "redirect:/book/detail?id="+review.getBookId();
    }
}
