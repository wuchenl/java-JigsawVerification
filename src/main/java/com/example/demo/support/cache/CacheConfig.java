package com.example.demo.support.cache;

import com.google.common.collect.Maps;
import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * @author wuchen
 * @version 0.1
 * @date 2018/12/13 14:36
 * @use
 */
@ConfigurationProperties(prefix = "spring.cache")
public class CacheConfig {

    /**
     * 缓存类型
     */
    private CacheType type;
    /**
     * 扩展了springboot默认的List<String> cacheNames方式，支持过期时间的设置
     */
    private Map<String,Long> cacheNames= Maps.newConcurrentMap();

    public CacheType getType() {
        return type;
    }

    public CacheConfig setType(CacheType type) {
        this.type = type;
        return this;
    }

    public Map<String, Long> getCacheNames() {
        return cacheNames;
    }

    public CacheConfig setCacheNames(Map<String, Long> cacheNames) {
        this.cacheNames = cacheNames;
        return this;
    }
}
