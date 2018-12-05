package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author wuchen
 * @version 0.1
 * @date 2018/9/1 15:50
 * @use
 */
@Controller
public class CaptchaController {

    @GetMapping("/captcha")
    public String login(){
        return "captcha";
    }
}
