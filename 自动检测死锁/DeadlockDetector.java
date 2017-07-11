package sample.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ptmind on 2017/7/11.
 */
public class DeadlockDetector {
	private static final Logger log = LoggerFactory.getLogger(DeadlockDetector.class);

	private final ScheduledExecutorService scheduler;
	private final Task task;

	public DeadlockDetector() {
		this.scheduler = Executors.newScheduledThreadPool(1, new DeadlockDetectorThreadFactory());

		ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
		this.task = new Task(mxBean);
	}

	public void detected() {
		Thread thread = new Thread(task);
		thread.setName("DeadlockDetectorThread");
		thread.start();
	}

	public ScheduledFuture<?> detectedAtFixedRate(long initialDelay, long period, TimeUnit unit) {
		ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(task, 0, 1, unit);
		return  future;
	}

	public void stop() {
		scheduler.shutdown();
	}

	private static class Task implements Runnable {

		private final ThreadMXBean mxBean;

		public Task(ThreadMXBean mxBean) {
			this.mxBean = mxBean;
		}

		public void run() {
			long[] deadLockedThreadIDs = mxBean.findDeadlockedThreads();
			if (deadLockedThreadIDs != null) {
				ThreadInfo[] infos = mxBean.getThreadInfo(deadLockedThreadIDs, 8);
				handleDeadlock(infos);
			}
		}

		private void handleDeadlock(ThreadInfo[] infos) {
			for (ThreadInfo info : infos) {
				StringBuilder builder = new StringBuilder("DeadLock detected!\n");
				builder.append(info);
				log.warn(builder.toString());
			}
		}
	}

	private static class DeadlockDetectorThreadFactory implements ThreadFactory {

		private final ThreadGroup group;

		DeadlockDetectorThreadFactory() {
			SecurityManager manager = System.getSecurityManager();
			this.group = (manager != null) ? manager.getThreadGroup() : Thread.currentThread().getThreadGroup();
		}

		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(group, runnable, "DeadlockDetectorThread", 0);

			if (thread.isDaemon())
				thread.setDaemon(false);
			if (thread.getPriority() != Thread.NORM_PRIORITY)
				thread.setPriority(Thread.NORM_PRIORITY);

			return thread;
		}
	}
}
