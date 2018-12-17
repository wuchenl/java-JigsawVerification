package com.example.demo.support;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuchenl
 * @date 2018/12/8.
 * 读取滑动验证码原始图片所在路径和数量
 */
@ConfigurationProperties(prefix = "com.letters7.wuchen.captcha.source")
public class CaptchaConfig {
    private String path;
    private Integer size;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
