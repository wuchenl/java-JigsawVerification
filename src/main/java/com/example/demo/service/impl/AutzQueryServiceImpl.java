package com.example.demo.service.impl;

import com.example.demo.service.AutzQueryService;
import com.example.demo.support.CaptchaConst;
import com.example.demo.support.cache.CacheManagerHolder;
import com.example.demo.util.UtilString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * @author itw_wangjb03
 * @date 2018/5/4
 * sprint13 by itw_wangjb03：用于处理当前IP的验证码
 */
@Service
public class AutzQueryServiceImpl implements AutzQueryService {

    private static final Logger log = LoggerFactory.getLogger(AutzQueryServiceImpl.class);


    /**
     * 获取当前IP验证码
     * sprint by itw_wangjb03：获取当前IP的验证码
     *
     * @param host 当前IP
     * @return 验证码
     */
    @Override
    public String getCurrentIPCode(String host) {
        String verificationCode = null;
        // 先去取cache块
        Cache cache = CacheManagerHolder.getManager().getCache(CaptchaConst.VERIFICATION_CODE);
        if (cache != null) {
            // 再取当前IP
            Cache.ValueWrapper wrapper = cache.get(host);
            if (wrapper != null) {
                // 有值就返回出去
                verificationCode = wrapper.get().toString();
            }
        }
        return verificationCode;
    }

    @Override
    public Integer getCurrentIdCaptcha(String host) {
        String verificationCode = null;
        // 先去取cache块
        Cache cache = CacheManagerHolder.getManager().getCache(CaptchaConst.VERIFICATION_CODE);
        if (cache != null) {
            // 再取当前IP
            Cache.ValueWrapper wrapper = cache.get(host);
            if (wrapper != null) {
                // 有值就返回出去
                verificationCode = wrapper.get().toString();
            }
        }
        if (UtilString.isNotEmpty(verificationCode)) {
            return Integer.parseInt(verificationCode);
        }
        return 0;
    }

    /**
     * 给当前IP放置一个验证码
     *
     * @param host IP
     */
    @Override
    public String putCurrentIpCode(String host) {
        String veriCode = "";
        if (UtilString.isNotEmpty(host)) {
            String code = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 6);
            CacheManager manager = CacheManagerHolder.getManager();
            if (Objects.nonNull(manager)) {
                Cache cache = manager.getCache(CaptchaConst.VERIFICATION_CODE);
                if (Objects.nonNull(cache)) {
                    cache.put(host, code);
                }
            }
            veriCode = code;
        }
        return veriCode;
    }

    /**
     * 获取存放在redis中的某个图片的base64编码
     *
     * @param imageName
     * @return
     */
    @Override
    public String getCaptchaImageBase64Str(String imageName) {
        String base64Str = null;
        // 先去取cache块
        Cache cache = CacheManagerHolder.getManager().getCache(CaptchaConst.CACHE_CAPTCHA_IMG);
        if (cache != null) {
            // 再取当前IP
            Cache.ValueWrapper wrapper = cache.get(imageName);
            if (wrapper != null) {
                log.info("验证码图片从缓存加载成功:{}", imageName);
                // 有值就返回出去
                base64Str = wrapper.get().toString();
            }
        }
        return base64Str;
    }

    /**
     * 移除当前IP的验证码
     * sprint13 by itw_wangjb03：用于清除当前IP的验证码
     *
     * @param host 当前IP
     */
    @Override
    public void removeCurrentIPCode(String host) {
        // 先去取cache块
        Cache cache = CacheManagerHolder.getManager().getCache(CaptchaConst.VERIFICATION_CODE);
        if (cache != null) {
            // 再取当前IP
            Cache.ValueWrapper wrapper = cache.get(host);
            if (wrapper != null) {
                // 判断是否有验证码
                String verificationCode = wrapper.get().toString();
                // 有的话就移除该验证码
                if (UtilString.isNotEmpty(verificationCode)) {
                    cache.evict(host);
                }
            }
        }
    }

}
