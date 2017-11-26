package neo.model.util.threadpool;

import java.util.concurrent.BlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolThread extends Thread {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PoolThread.class);

	private BlockingDeque<Runnable> taskQueue = null;

	private boolean isStopped = false;

	public PoolThread(final BlockingDeque<Runnable> queue) {
		taskQueue = queue;
	}

	public synchronized void doStop() {
		isStopped = true;
		interrupt(); // break pool thread out of dequeue() call.
	}

	public synchronized boolean isStopped() {
		return isStopped;
	}

	@Override
	public void run() {
		while (!isStopped()) {
			try {
				final Runnable runnable = taskQueue.takeFirst();
				runnable.run();
			} catch (final Exception e) {
				LOG.error("error", e);
			}
		}
	}
}