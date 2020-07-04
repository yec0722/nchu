package com.atguigu.gmall1213.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PassportController {
    @GetMapping("login.html")
    public String login(HttpServletRequest request) {
        /*  http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
        originUrl: 从哪个位置点击的登录
         */
        String originUrl = request.getParameter("originUrl");
        //这段代码是从哪个位置登录后仍返回哪个位置
        request.setAttribute("originUrl", originUrl);
        return "login";
    }
}
