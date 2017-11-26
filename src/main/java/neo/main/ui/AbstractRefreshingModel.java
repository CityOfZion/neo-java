package neo.main.ui;

import javax.swing.table.AbstractTableModel;

import neo.network.NodeDataChangeListener;

/**
 * an abstract class for a UI model that refreshes periodically.
 *
 * @author coranos
 *
 */
public abstract class AbstractRefreshingModel extends AbstractTableModel implements NodeDataChangeListener {

	private static final long serialVersionUID = 1L;

	/**
	 * min number of milliseconds to sleep between refreshes.
	 */
	private static final int REFRESH_THREAD_SLEEP_MS = 1000;

	/**
	 * if true, refresh the page.
	 */
	private boolean refresh = false;

	/**
	 * if true, stop the refresh thread.
	 */
	private boolean stop = false;

	/**
	 * the refresh thread. Checks refresh flag. If the refresh flag is true, calls
	 * fireTableDataChanged() and sets refresh to false. It then waits
	 * REFRESH_THREAD_SLEEP_MS and checks again.
	 */
	private final Thread refreshThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (!stop) {
				synchronized (AbstractRefreshingModel.this) {
					if (refresh) {
						fireTableDataChanged();
						refresh = false;
					}
				}
				try {
					Thread.sleep(REFRESH_THREAD_SLEEP_MS);
				} catch (final InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}, getThreadName());

	/**
	 * constructor, starts the refresh thread.
	 */
	public AbstractRefreshingModel() {
		refreshThread.start();
	}

	/**
	 * @return the thread name.
	 */
	public abstract String getThreadName();

	/**
	 * setter for the refresh field.
	 *
	 * @param refresh
	 *            the value to use.
	 */
	public final void setRefresh(final boolean refresh) {
		this.refresh = refresh;
	}

	/**
	 * stops the refresh thread, and waits for it to stop.
	 *
	 * @throws InterruptedException
	 *             if an error occurs.
	 */
	public final void stop() throws InterruptedException {
		stop = true;
		refreshThread.join();
	}

}
