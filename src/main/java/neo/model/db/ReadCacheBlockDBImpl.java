package neo.model.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.db.mapdb.BlockDbMapDbImpl;
import neo.perfmon.PerformanceMonitor;

/**
 * a blockdb implementation that caches read-only requests between writes.
 *
 * @author coranos
 *
 */
public final class ReadCacheBlockDBImpl implements BlockDb {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ReadCacheBlockDBImpl.class);

	/**
	 * the delegate.
	 */
	private final BlockDb delegate;

	/**
	 * the set of known hashes.
	 */
	private final Set<UInt256> cachedHashSet = Collections.synchronizedSet(new TreeSet<>());

	/**
	 * the block count.
	 */
	private Long cachedBlockCount;

	/**
	 * the thread for putting blocks.
	 */
	private final Thread putThread;

	/**
	 * the runnable for putting blocks.
	 */
	private final PutRunnable putRunnable;

	/**
	 * the count of blocks in the queue to be put into the database.
	 */
	private int putCount = 0;

	/**
	 * the constructor.
	 *
	 * @param config
	 *            the configuration to use.
	 */
	public ReadCacheBlockDBImpl(final JSONObject config) {
		delegate = new BlockDbMapDbImpl(config);
		putRunnable = new PutRunnable();
		putThread = new Thread(putRunnable);
		putThread.start();
	}

	/**
	 * clears all cached objects.
	 */
	private void clearCache() {
		cachedHashSet.clear();
		setCachedBlockCount(null);
	}

	@Override
	public void close() {
		clearCache();
		putRunnable.stop();
		try {
			putThread.join();
		} catch (final InterruptedException e) {
			throw new RuntimeException(e);
		}
		delegate.close();
	}

	@Override
	public boolean containsBlockWithHash(final UInt256 hash) {
		if (cachedHashSet.contains(hash)) {
			return true;
		}
		if (delegate.containsBlockWithHash(hash)) {
			cachedHashSet.add(hash);
			return true;
		}
		return false;
	}

	@Override
	public void deleteHighestBlock() {
		delegate.deleteHighestBlock();
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		return delegate.getAccountAssetValueMap();
	}

	@Override
	public long getBlockCount() {
		final Long cachedBlockCount = getCachedBlockCount();
		if (cachedBlockCount != null) {
			return cachedBlockCount + putCount;
		}

		final long blockCount = delegate.getBlockCount();
		setCachedBlockCount(blockCount);
		return blockCount;
	}

	/**
	 * return the cached block count.
	 *
	 * @return the cached block count.
	 */
	public synchronized Long getCachedBlockCount() {
		return cachedBlockCount;
	}

	@Override
	public long getFileSize() {
		return delegate.getFileSize();
	}

	@Override
	public Block getFullBlockFromHash(final UInt256 hash) {
		return delegate.getFullBlockFromHash(hash);
	}

	@Override
	public Block getFullBlockFromHeight(final long blockHeight) {
		return delegate.getFullBlockFromHeight(blockHeight);
	}

	@Override
	public Block getHeaderOfBlockFromHash(final UInt256 hash) {
		return delegate.getHeaderOfBlockFromHash(hash);
	}

	@Override
	public Block getHeaderOfBlockFromHeight(final long blockHeight) {
		return delegate.getHeaderOfBlockFromHeight(blockHeight);
	}

	@Override
	public Block getHeaderOfBlockWithMaxIndex() {
		return delegate.getHeaderOfBlockWithMaxIndex();
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		return delegate.getTransactionWithHash(hash);
	}

	@Override
	public void put(final Block... blocks) {
		putRunnable.put(blocks);
	}

	/**
	 * sets the cached block count.
	 *
	 * @param cachedBlockCount
	 *            the block count to cache.
	 */
	public synchronized void setCachedBlockCount(final Long cachedBlockCount) {
		this.cachedBlockCount = cachedBlockCount;
	}

	@Override
	public void validate() {
		delegate.validate();
	}

	/**
	 * the runnable object for putting blocks asynchronously.
	 *
	 * @author coranos
	 *
	 */
	private final class PutRunnable implements Runnable {

		/**
		 * the block list.
		 */
		private final List<Block> blockList = Collections.synchronizedList(new ArrayList<>());

		/**
		 * the stopped flag.
		 */
		private boolean stopped = false;

		/**
		 * puts the blocks into the putList.
		 *
		 * @param blocks
		 *            the blocks to add.
		 */
		public void put(final Block... blocks) {
			synchronized (blockList) {
				for (final Block block : blocks) {
					blockList.add(block);
				}
			}
		}

		@Override
		public void run() {
			while (!stopped) {
				final List<Block> putList = new ArrayList<>();
				// pull out all the blocks we are going to put into the database.
				synchronized (blockList) {
					putList.addAll(blockList);
					blockList.clear();
				}
				if (!putList.isEmpty()) {
					// pull out all the blocks into the database.
					try (PerformanceMonitor m1 = new PerformanceMonitor("ReadCacheBlockDBImpl.put")) {
						try (PerformanceMonitor m2 = new PerformanceMonitor("ReadCacheBlockDBImpl.put[PerBlock]",
								putList.size())) {
							delegate.put(putList.toArray(new Block[0]));
							putCount += putList.size();
						}
					}
					synchronized (blockList) {
						// if we put more than 500 blocks into the database, or no blocks came while we
						// were comitting, clear cache (which refrehes the stats).
						if (blockList.isEmpty() || (putCount > 500)) {
							try (PerformanceMonitor m1 = new PerformanceMonitor("ReadCacheBlockDBImpl.clearCache")) {
								clearCache();
								putCount = 0;
							}
						}
					}
				}

				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
					LOG.debug("thread interrupted, stopping", e);
					stopped = true;
				}
			}
		}

		/**
		 * stops the thread.
		 */
		public void stop() {
			stopped = true;

		}

	}
}
