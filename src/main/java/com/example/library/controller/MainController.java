package com.example.library.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.example.library.entity.Book;
import com.example.library.entity.Borrow;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import com.example.library.service.BorrowService;

@Controller
public class MainController {
    @Autowired
    private UserService userService;
    @Autowired
    private BookService bookService;
    @Autowired
    private BorrowService borrowService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }
    @GetMapping("/captcha")
    public void captcha(HttpSession session, HttpServletResponse response) throws IOException {
        // 生成干扰线验证码：宽200，高100，验证码长度4
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 20);
        session.setAttribute("captcha", captcha.getCode());
        response.setContentType("image/png");
        captcha.write(response.getOutputStream());
    }

    @PostMapping("/doRegister")
    public String doRegister(@RequestParam String username, @RequestParam String password,@RequestParam String code,HttpSession session,Model model) {

        String captcha = (String) session.getAttribute("captcha");
        if (captcha == null || !captcha.equalsIgnoreCase(code)) {
            model.addAttribute("error", "验证码错误");
            return "register";
        }
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "用户名已存在");
            return "register";
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("reader");
        user.setStatus(1);

        userService.save(user);

        model.addAttribute("msg", "注册成功，请登录");
        return "login";
    }
    @PostMapping("/doLogin")
    public String dologin(@RequestParam String username, @RequestParam String password, HttpSession session, Model model){
        User user=userService.login(username,password);
        if(user==null){
            model.addAttribute("error","用户名或密码错误");
            return "login";
        }
        session.setAttribute("user",user);
        return "redirect:/home";
    }
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        model.addAttribute("user", user);
        LocalDateTime now = LocalDateTime.now();

        // 超期数量：未归还 + 到期时间 < 现在
        long overdueCount = borrowService.lambdaQuery()
                .eq(Borrow::getUserId, user.getId())
                .eq(Borrow::getStatus, 0)
                .lt(Borrow::getDueTime, now)
                .count();
        // 即将到期数量：未归还 + 到期时间在[现在, 3天后]之间
        long dueSoonCount = borrowService.lambdaQuery()
                .eq(Borrow::getUserId, user.getId())
                .eq(Borrow::getStatus, 0)
                .ge(Borrow::getDueTime, now)
                .le(Borrow::getDueTime, now.plusDays(3))
                .count();
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("dueSoonCount", dueSoonCount);

        // 统计数据（基于册数）
        long total = bookService.list().stream()
                .mapToInt(b -> b.getQuantity() != null ? b.getQuantity() : 0).sum();
        long borrowed = borrowService.lambdaQuery().eq(Borrow::getStatus, 0).count();
        long available = total - borrowed;

        model.addAttribute("totalBooks", total);
        model.addAttribute("availableBooks", available);
        model.addAttribute("borrowedBooks", borrowed);

        // 最新 5 本书
        List<Book> recentBooks = bookService.lambdaQuery()
                .orderByDesc(Book::getId)
                .last("LIMIT 5")
                .list();
        model.addAttribute("recentBooks", recentBooks);

        return "home";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }
}
