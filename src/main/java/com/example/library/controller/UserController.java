package com.example.library.controller;

import com.example.library.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.library.service.UserService;
@Controller
public class UserController {
    @GetMapping("/user/change-password")
    public String toChangePassword() {
        return "change-password";
    }
    @Autowired
    private UserService userService;

    @PostMapping("/user/change-password")
    public String doChangePassword(@RequestParam String oldPassword, @RequestParam String newPassword, @RequestParam String confirmPassword, HttpSession session, RedirectAttributes redirectAttributes) {

        User user = (User) session.getAttribute("user");
        if (!user.getPassword().equals(oldPassword)) {
            redirectAttributes.addFlashAttribute("error", "旧密码错误");
            return "redirect:/user/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "新密码和确认密码不匹配");
            return "redirect:/user/change-password";
        }
        boolean success = userService.changePassword(user.getId(), oldPassword, newPassword);
        if (!success) {
            redirectAttributes.addFlashAttribute("error", "修改失败，请重试");
            return "redirect:/user/change-password";
        }
        user.setPassword(newPassword);
        session.setAttribute("user", user);
        redirectAttributes.addFlashAttribute("msg", "密码修改成功");
        return "redirect:/user/change-password";
    }
}
