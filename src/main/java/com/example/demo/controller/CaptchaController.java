package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author wuchen
 * @version 0.1
 * @date 2018/9/1 15:50
 * @use 访问滑动验证码相关页面
 */
@Controller
public class CaptchaController {

    @GetMapping("/captcha")
    public String login(){
        return "login";
    }
}
