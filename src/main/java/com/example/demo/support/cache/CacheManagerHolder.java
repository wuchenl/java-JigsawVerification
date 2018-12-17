package com.example.demo.support.cache;


import com.example.demo.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author zoubin02
 */
@Component
public class CacheManagerHolder {

    private static final Logger logger = LoggerFactory.getLogger(CacheManagerHolder.class);

    // 定时任务执行器
    private final ScheduledExecutorService scheduledExecutorService
            = Executors.newScheduledThreadPool(1, new NamedThreadFactory("PRINT_CACHE", true));

    private ScheduledFuture<?> sendFuture = null;

    private int monitorInterval = 60*60;


    //单例模式
    private CacheManagerHolder(){

    }
    private static CacheManagerHolder instance;
    public static CacheManagerHolder getInstance(){
        if(instance==null){
            instance = new CacheManagerHolder();
        }
        return instance;
    }


    @Autowired(required = false)
    private CacheManager cacheManager;

    public static CacheManager target;

    public static final CacheManager getManager() {
        return target;
    }

    @PostConstruct
    public void init() {
        //如果系统没有配置cacheManager,则使用ConcurrentMapCacheManager
        if (cacheManager == null) {
            cacheManager = new ConcurrentMapCacheManager();
        }

        if (target == null){
            target = cacheManager;
            logger.info("系统选择了缓存-{}",cacheManager.getClass());
        }

        //
        sendFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            // 收集统计信息
            try {
                send();
            } catch (Throwable t) { // 防御性容错
                logger.error("Unexpected error occur at send statistic, cause: " + t.getMessage(), t);
            }
        }, monitorInterval, monitorInterval, TimeUnit.MINUTES);
    }


    private void send() {
        logger.info("");
        cacheManager.getCacheNames().forEach(
                cacheName -> {
                    logger.info("["+cacheName + "]-------->{}",cacheManager.getCache(cacheName).getClass());
                }
        );
        logger.info("");
    }



    public void destroy(){
        if(sendFuture!=null){
            sendFuture.cancel(true);
        }
    }




}
