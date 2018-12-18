package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.example.demo.model.ResponseMessage;
import com.example.demo.service.AutzQueryService;
import com.example.demo.support.CaptchaConfig;
import com.example.demo.support.CaptchaConst;
import com.example.demo.util.*;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * @author wuchen
 * @version 0.1
 * @date 2018/9/1 15:54
 * @use 滑动验证码rest接口
 */
@RestController
@RequestMapping("captcha")
public class CaptchaRestController {

    /**
     * 偏移量区间
     */
    private static final int OFFSET = 4;
    /**
     * 日志提供器
     * Modifiers should be declared in the correct order
     */
    private static final Logger log = LoggerFactory.getLogger(CaptchaRestController.class);

    @Autowired
    private AutzQueryService autzQueryService;

    @Autowired(required = false)
    private CaptchaConfig captchaConfig;

    /**
     * 判断是否验证成功
     *
     * @param request 请求来源
     * @return 成功则返回验证码，失败则返回失败信息
     */
    @PostMapping("/checkCaptcha")
    public ResponseMessage checkCaptcha(HttpServletRequest request, @NonNull String point) {
        if (!UtilString.isNumber(point)) {
            log.warn("传入的偏移量为:{}", point);
            return ResponseMessage.error("非法参数！");
        }
        String host = UtilWeb.getIpAddr(request);
        Integer veriCode = autzQueryService.getCurrentIdCaptcha(host);
        if ((Integer.valueOf(point) < veriCode + OFFSET) && (Integer.valueOf(point) > veriCode - OFFSET)) {
            // 验证通过后，生成一个验证码放入缓存并返回给前台
            String code = autzQueryService.putCurrentIpCode(host);
            //说明验证通过
            return ResponseMessage.ok(code);
        } else {
            return ResponseMessage.error("error");

        }

    }

    /**
     * 生成图片
     *
     * @param request 请求
     * @return 返回图片及其对应请求地址
     * @throws IOException 丢出异常
     */
    @PostMapping("/captchaImage")
    @ResponseBody
    public String captchaImage(HttpServletRequest request) throws IOException {
        CaptchaUtil resUtil = new CaptchaUtil();
        String hostIp = UtilWeb.getIpAddr(request);
        byte[] imageData ;

        // 获取验证码原图
        String sourceImageName = getSourceImageName();
        String pngName = sourceImageName.substring(sourceImageName.lastIndexOf("/") + 1);
        String pngBaseStr = autzQueryService.getCaptchaImageBase64Str(pngName);
        InputStream sourceImageInputStream;
        if (UtilString.isNotEmpty(pngBaseStr)) {
            log.info("从缓存加载了原文件:{}", pngName);
            sourceImageInputStream = CaptchaUtil.getInputStreamFromBase64Str(pngBaseStr);
        } else {
            // 获取对应的流
            sourceImageInputStream = getSourceImageInputStream(sourceImageName);
        }
        if (Objects.nonNull(sourceImageInputStream)) {
            imageData = UtilFile.input2byte(sourceImageInputStream);
        }else {
            log.error("读取原文件异常！");
            return null;
        }
        // 读取文件
        Map<String, String> result = resUtil.createCaptchaImage(hostIp, sourceImageName, imageData);
        if (result.size() > 0) {
            return JSON.toJSONString(result);
        } else {
            return null;
        }

    }

    private String getSourceImageName() {
        Random random = new Random();
        // 获取原始图片的完整路径，随机采用一张
        int sourceSize = random.nextInt(captchaConfig.getSize());
        return UtilString.join(captchaConfig.getPath(), sourceSize, CaptchaConst.PIC_SUFFIX);
    }

    /**
     * 根据原图文件路径去获取对应的文件流
     *
     * @param sourceImageName 原图名
     * @return 文件流
     * @throws IOException 异常
     */
    private InputStream getSourceImageInputStream(String sourceImageName) throws IOException {
        return Resources.getResourceAsStream(sourceImageName);
    }

    /**
     * 从缓存中去加载某些图片
     *
     * @param imageName 图片名
     * @param response  从缓存中获取的图片
     */
    @GetMapping("/image/{imageName:.+}")
    public void getImage(@PathVariable String imageName, HttpServletResponse response) {
        if (UtilString.isNotEmpty(imageName)) {
            try {
                // 先从缓存中获取是否有对应图片名的base64字符串
                String base64Str = autzQueryService.getCaptchaImageBase64Str(imageName);
                if (UtilString.isNotEmpty(base64Str)) {
                    // 有的话则转为图片的输入流，并写出去
                    InputStream inputStreamFromBase64Str = CaptchaUtil.getInputStreamFromBase64Str(base64Str);
                    if (Objects.nonNull(inputStreamFromBase64Str)) {
                        BufferedImage bufferedImage = ImageIO.read(inputStreamFromBase64Str);
                        ImageIO.write(bufferedImage, "png", response.getOutputStream());
                    }
                }
            } catch (IOException e) {
                log.error("获取图片失败！");
            }
        }
    }
}
