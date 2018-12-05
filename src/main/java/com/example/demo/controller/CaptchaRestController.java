package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.example.demo.model.ResponseMessage;
import com.example.demo.service.AutzQueryService;
import com.example.demo.util.CaptchaUtils;
import com.example.demo.util.UtilString;
import com.example.demo.util.UtilWeb;
import org.apache.commons.lang.StringUtils;
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

/**
 * @author wuchen
 * @version 0.1
 * @date 2018/9/1 15:54
 * @use
 */
@RestController
@RequestMapping("captcha")
public class CaptchaRestController {
    /**
     * 日志提供器
     */
    private final static Logger log = LoggerFactory.getLogger(CaptchaRestController.class);

    @Autowired
    private AutzQueryService autzQueryService;

    /**
     * 判断是否验证成功
     *
     * @param request 请求来源
     * @return 成功则返回验证码，失败则返回失败信息
     */
    @PostMapping("/checkCaptcha")
    public ResponseMessage checkCaptcha(HttpServletRequest request) {
        String point = request.getParameter("point");
        String host = UtilWeb.getIpAddr(request);
        Integer veriCode = autzQueryService.getCurrentIdCaptcha(host);
        if ((Integer.valueOf(point) < veriCode + 4) && (Integer.valueOf(point) > veriCode - 4)) {
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
     * @return
     * @throws IOException
     */
    @PostMapping("/captchaImage")
    @ResponseBody
    public String captchaImage(HttpServletRequest request) throws IOException {
        CaptchaUtils.init(request.getServletContext());
        String imgname = request.getParameter("imgname");
        // 分解出文件名
        if (!StringUtils.isEmpty(imgname)) {
            imgname = imgname.substring(imgname.lastIndexOf("/") + 1, imgname.lastIndexOf("png") + 3);
        }
        CaptchaUtils resourImg = new CaptchaUtils();
        // 读取文件
        Map<String, String> result = resourImg.create(request, imgname);
        if (result.size() > 0) {
            return JSON.toJSONString(result);
        } else {
            return null;
        }

    }

    /**
     * 从缓存中去加载某些图片
     * @param imageName 图片名
     * @param response 从缓存中获取的图片
     */
    @GetMapping("/image/{imageName:.+}")
    public void getImage(@PathVariable String imageName, HttpServletResponse response) {
        if (UtilString.isNotEmpty(imageName)) {
            try {
                // 先从缓存中获取是否有对应图片名的base64字符串
                String base64Str = autzQueryService.getCaptchaImageBase64Str(imageName);
                if (UtilString.isNotEmpty(base64Str)) {
                    // 有的话则转为图片的输入流，并写出去
                    InputStream inputStreamFromBase64Str = CaptchaUtils.getInputStreamFromBase64Str(base64Str);
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
