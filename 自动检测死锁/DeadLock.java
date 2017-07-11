package sample.test;

import sample.utils.DeadlockDetector;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by ptmind on 2017/7/11.
 */
public class DeadLock {

	public static void main(String[] args) throws InterruptedException {
		DeadlockDetector detector = new DeadlockDetector();

		Lock lock1 = new ReentrantLock();
		Lock lock2 = new ReentrantLock();

		//ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		//Lock lock1 = readWriteLock.readLock();
		//Lock lock2 = readWriteLock.writeLock();

		Write write = new Write(lock1, lock2);
		Thread wThread = new Thread(write);
		wThread.setName("write");

		Read read = new Read(lock1, lock2);
		Thread rThread = new Thread(read);
		rThread.setName("read");

		wThread.start();
		rThread.start();

		ScheduledFuture<?> future = detector.detectedAtFixedRate(0, 1, TimeUnit.SECONDS);
		Thread.sleep(10000);

		/*String wStack = Arrays.toString(wThread.getStackTrace());
		System.out.println(wStack);

		System.out.println("\n");

		String rStack = Arrays.toString(rThread.getStackTrace());
		System.out.println(rStack);*/
		future.cancel(false);
		Thread.sleep(10000);
		detector.stop();
		//detector.detected();
	}

	private static class Write implements Runnable {

		private Lock lock1;
		private Lock lock2;

		public Write(Lock lock1, Lock lock2) {
			this.lock1 = lock1;
			this.lock2 = lock2;
		}

		public void run() {
			Thread curThread = Thread.currentThread();
			System.out.println(curThread.getId() + " in working.");
			deadlock();
			System.out.println(curThread.getId() + " done.");
		}

		private void deadlock() {
			lock1.lock();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lock2.lock();
		}
	}

	private static class Read implements Runnable {

		private Lock lock1;
		private Lock lock2;

		public Read(Lock lock1, Lock lock2) {
			this.lock1 = lock1;
			this.lock2 = lock2;
		}

		public void run() {
			Thread curThread = Thread.currentThread();
			System.out.println(curThread.getId() + " in working.");
			deadlock();
			System.out.println(curThread.getId() + " done.");
		}

		private void deadlock() {
			lock2.lock();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lock1.lock();
		}
	}
}
