package com.ptmind.ptfence.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;

/**
 * Created by ptmind on 2017/5/12.
 */
public class MethodExecutor<T> {
    //当函数的参数有且只有一个,且传入的参数值为null时使用
    private static final Object[] defaultArgs = new Object[1];
    private static final Logger log = LoggerFactory.getLogger(MethodExecutor.class);

    private final Object target;
    private final Class<?>[] parameterTypes;
    private volatile Method method;

    public MethodExecutor(Object target, String methodName, Class<?>... parameterTypes) {
        if (target == null) {
            throw new NullPointerException("target must be not null.");
        }

        this.target = target;
        initMethod(methodName, parameterTypes);
        this.parameterTypes = parameterTypes;
    }

    public void execRun(CountDownLatch latch, Object... args) {
        ThreadPoolExecutor executor = ThreadPoolHolder.getThreadPool();
        WorkerRun worker = new WorkerRun(latch, this.target, this.method, args, parameterTypes);
        executor.execute(worker);
    }

    public Future<T> execCall(CountDownLatch latch, Object... args) {
        ThreadPoolExecutor executor = ThreadPoolHolder.getThreadPool();
        WorkerCall<T> worker = new WorkerCall<T>(latch, this.target, this.method, args, parameterTypes);

        Future<T> result = executor.submit(worker);
        return result;
    }

    public void initMethod(String methodName, Class<?>... parameterTypes) {
        Class<?> cls = target.getClass();

        try {
            this.method = cls.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException noSuchMethodException) {
            log.debug("initMethod(String methodName, Class<?>... parameterTypes):", methodName, noSuchMethodException);
            throw new RuntimeException(noSuchMethodException);
        }
    }

    private static class WorkerRun implements Runnable {

        private static final Logger log = LoggerFactory.getLogger(WorkerRun.class);

        private final CountDownLatch latch;

        private final Object target;
        private final Object[] args;
        private final Class<?>[] parameterTypes;
        private volatile Method method;

        public WorkerRun(CountDownLatch latch, Object target, Method method, Object[] args, Class<?>[] parameterTypes) {
            this.latch = latch;
            this.target = target;
            this.method = method;
            this.args = args;
            this.parameterTypes = parameterTypes;
        }

        public void run() {
            try {
                if (this.args != null) {
                    this.method.invoke(this.target, this.args);
                } else {
                    if (parameterTypes.length == 1) {
                        this.method.invoke(this.target, defaultArgs);
                    } else {
                        this.method.invoke(this.target);
                    }
                }
            } catch (InvocationTargetException invocationTargetException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), invocationTargetException.getMessage());
                throw new RuntimeException(invocationTargetException);
            } catch (IllegalAccessException illegalAccessException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), illegalAccessException.getMessage());
                throw new RuntimeException(illegalAccessException);
            } catch (IllegalArgumentException illegalArgumentException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), illegalArgumentException.getMessage());
                throw new RuntimeException(illegalArgumentException);
            } finally {
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    private static class WorkerCall<T> implements Callable<T> {

        private static final Logger log = LoggerFactory.getLogger(WorkerCall.class);

        private final CountDownLatch latch;

        private final Object target;
        private final Object[] args;
        private final Class<?>[] parameterTypes;
        private volatile Method method;

        public WorkerCall(CountDownLatch latch, Object target, Method method, Object[] args, Class<?>[] parameterTypes) {
            this.latch = latch;
            this.target = target;
            this.method = method;
            this.args = args;
            this.parameterTypes = parameterTypes;
        }

        public T call() {
            try {
                if (this.args != null) {
                    return (T) this.method.invoke(this.target, this.args);
                } else {
                    if (parameterTypes.length == 1) {
                        return (T) this.method.invoke(this.target, defaultArgs);
                    } else {
                        return (T) this.method.invoke(this.target);
                    }
                }
            } catch (InvocationTargetException invocationTargetException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), invocationTargetException.getMessage());
                throw new RuntimeException(invocationTargetException);
            } catch (IllegalAccessException illegalAccessException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), illegalAccessException.getMessage());
                throw new RuntimeException(illegalAccessException);
            } catch (IllegalArgumentException illegalArgumentException) {
                Thread curThread = Thread.currentThread();
                log.debug("threadId:{}, threadName:{} ==> {}", curThread.getId(), curThread.getName(), illegalArgumentException.getMessage());
                throw new RuntimeException(illegalArgumentException);
            } finally {
                if (latch != null) {
                    latch.countDown();
                }
            }
        }
    }

    private static class ThreadPoolHolder {
        private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(30, 80, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());

        public static ThreadPoolExecutor getThreadPool() {
            return executor;
        }
    }
}