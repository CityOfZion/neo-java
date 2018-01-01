package neo.perfmon;

import java.util.Map;

import org.json.JSONObject;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.db.BlockDb;
import neo.model.db.ReadCacheBlockDBImpl;

/**
 * the performance monitoring class.
 *
 * @author coranos
 *
 */
public final class PerformanceMonitoringBlockDb implements BlockDb {

	/**
	 * the delegate.
	 */
	private final BlockDb delegate;

	/**
	 * the constructor.
	 *
	 * @param config
	 *            the configuration to use.
	 */
	public PerformanceMonitoringBlockDb(final JSONObject config) {
		delegate = new ReadCacheBlockDBImpl(config);
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean containsBlockWithHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.containsBlockWithHash")) {
			return delegate.containsBlockWithHash(hash);
		}
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getAccountAssetValueMap")) {
			return delegate.getAccountAssetValueMap();
		}
	}

	@Override
	public long getBlockCount() {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getBlockCount")) {
			return delegate.getBlockCount();
		}
	}

	@Override
	public long getFileSize() {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getFileSize")) {
			return delegate.getFileSize();
		}
	}

	@Override
	public Block getFullBlockFromHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getFullBlockFromHash")) {
			return delegate.getFullBlockFromHash(hash);
		}
	}

	@Override
	public Block getFullBlockFromHeight(final long blockHeight) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getFullBlockFromHeight")) {
			return delegate.getFullBlockFromHeight(blockHeight);
		}
	}

	@Override
	public Block getHeaderOfBlockFromHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getHeaderOfBlockFromHash")) {
			return delegate.getHeaderOfBlockFromHash(hash);
		}
	}

	@Override
	public Block getHeaderOfBlockFromHeight(final long blockHeight) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getHeaderOfBlockFromHeight")) {
			return delegate.getHeaderOfBlockFromHeight(blockHeight);
		}
	}

	@Override
	public Block getHeaderOfBlockWithMaxIndex() {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getHeaderOfBlockWithMaxIndex")) {
			return delegate.getHeaderOfBlockWithMaxIndex();
		}
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.getTransactionWithHash")) {
			return delegate.getTransactionWithHash(hash);
		}
	}

	@Override
	public void put(final Block block) {
		try (PerformanceMonitor m = new PerformanceMonitor("BlockDb.put")) {
			delegate.put(block);
		}
	}

}
