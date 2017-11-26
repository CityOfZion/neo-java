package neo.model.util.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class ThreadPool {

	private BlockingDeque<Runnable> taskQueue = null;
	private final List<PoolThread> threads = new ArrayList<>();
	private boolean isStopped = false;

	public ThreadPool(final int noOfThreads) {
		taskQueue = new LinkedBlockingDeque<>();

		for (int i = 0; i < noOfThreads; i++) {
			threads.add(new PoolThread(taskQueue));
		}
		for (final PoolThread thread : threads) {
			thread.start();
		}
	}

	public synchronized void execute(final Runnable task) throws Exception {
		if (isStopped) {
			throw new IllegalStateException("ThreadPool is stopped");
		}
		taskQueue.addLast(task);
	}

	public synchronized int size() {
		return taskQueue.size();
	}

	public synchronized void stop() {
		isStopped = true;
		for (final PoolThread thread : threads) {
			thread.doStop();
		}
	}

}