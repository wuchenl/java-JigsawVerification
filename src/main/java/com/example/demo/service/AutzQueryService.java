package com.example.demo.service;

/**
 * @author itw_wangjb03
 * @date 2018/5/4
 * sprint13 by itw_wangjb03：用于处理验证码
 */
public interface AutzQueryService {
    /**
     * 获取当前IP验证码
     * @param host 当前IP
     * @return 验证码
     */
    String getCurrentIPCode(String host);

    /**
     * 获取对应IP地址的滑动验证码的距离
     * @param host IP
     * @return 距离
     */
    Integer getCurrentIdCaptcha(String host);

    /**
     * 给当前IP放置一个验证码
     * @param host IP
     */
    String putCurrentIpCode(String host);

    /**
     * 获取存放在redis中的某个图片的base64编码
     * @param imageName
     * @return
     */
    String getCaptchaImageBase64Str(String imageName);

    /**
     * 移除当前IP的验证码
     * @param host 当前IP
     */
    void removeCurrentIPCode(String host);
}
