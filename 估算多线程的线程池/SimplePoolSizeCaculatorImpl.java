package com.huan.controller;

import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:huanhuan.zhan@qunar.com">詹欢欢</a>
 * @since 2018/11/27 - 15:13
 */
public class SimplePoolSizeCaculatorImpl extends PoolSizeCalculator {
    @Override
    protected Runnable creatTask() {
        return new AsyncIOTask();
    }

    @Override
    protected BlockingQueue<Runnable> createWorkQueue() {
        return new LinkedBlockingQueue<Runnable>(1000);
    }

    @Override
    protected long getCurrentThreadCPUTime() {
        return ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();
    }

    public static void main(String[] args) {
        PoolSizeCalculator poolSizeCalculator = new SimplePoolSizeCaculatorImpl();
        poolSizeCalculator.calculateBoundaries(new BigDecimal(1.0),
                new BigDecimal(100000));

        /*//估算后的结果
        ThreadPoolExecutor pool = new ThreadPoolExecutor(68, 68, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(4167));*/

    }

}
