package com.example.demo.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 带有名称的 线程 工厂类
 * @author zoubin02 on 5/5/14.
 */
public class NamedThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger threadNum = new AtomicInteger(1);

    private final String prefix;

    private final boolean daemon;

    private final ThreadGroup group;

    public NamedThreadFactory() {
        this("pool-" + POOL_SEQ.getAndIncrement(), false);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean daemon) {
        this.prefix = prefix + "-thread-";
        this.daemon = daemon;
        SecurityManager s = System.getSecurityManager();
        group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = prefix + threadNum.getAndIncrement();
        Thread ret = new Thread(group, runnable, name, 0);
        ret.setDaemon(daemon);
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }
}
