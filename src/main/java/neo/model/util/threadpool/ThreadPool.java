package neo.model.util.threadpool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * the thread pool class.
 *
 * @author coranos
 *
 */
public class ThreadPool {

	/**
	 * the task queue.
	 */
	private final BlockingDeque<StopRunnable> taskQueue;

	/**
	 * the list of threads.
	 */
	private final List<PoolThread> threads = new ArrayList<>();

	/**
	 * the stopped flag.
	 */
	private boolean isStopped = false;

	/**
	 * the constructor.
	 *
	 * @param threadCount
	 *            the number of threads to create.
	 */
	public ThreadPool(final int threadCount) {
		taskQueue = new LinkedBlockingDeque<>();

		for (int threadIx = 0; threadIx < threadCount; threadIx++) {
			threads.add(new PoolThread(taskQueue));
		}
		for (final PoolThread thread : threads) {
			thread.start();
		}
	}

	/**
	 * executes a runnable task.
	 *
	 * @param task
	 *            the task to execute.
	 */
	public synchronized void execute(final StopRunnable task) {
		if (isStopped) {
			throw new IllegalStateException("ThreadPool is stopped");
		}
		taskQueue.addLast(task);
	}

	/**
	 * return the size of the queue.
	 *
	 * @return the size of the queue.
	 */
	public synchronized int size() {
		return taskQueue.size();
	}

	/**
	 * stops all the threads in the queue.
	 */
	public synchronized void stop() {
		isStopped = true;
		for (final PoolThread thread : threads) {
			thread.doStop();
		}
		for (final PoolThread thread : threads) {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
