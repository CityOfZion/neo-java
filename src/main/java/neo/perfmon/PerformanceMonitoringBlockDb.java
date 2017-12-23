package neo.perfmon;

import java.util.Map;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.db.BlockDb;
import neo.model.db.BlockDbImpl;

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
	private final BlockDb delegate = new BlockDbImpl();

	/**
	 * the constructor.
	 */
	public PerformanceMonitoringBlockDb() {
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public boolean containsBlockWithHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("containsBlockWithHash")) {
			return delegate.containsBlockWithHash(hash);
		}
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		try (PerformanceMonitor m = new PerformanceMonitor("getAccountAssetValueMap")) {
			return delegate.getAccountAssetValueMap();
		}
	}

	@Override
	public Block getBlock(final long blockHeight, final boolean withTransactions) {
		try (PerformanceMonitor m = new PerformanceMonitor("getBlockWithHeight")) {
			return delegate.getBlock(blockHeight, withTransactions);
		}
	}

	@Override
	public Block getBlock(final UInt256 hash, final boolean withTransactions) {
		try (PerformanceMonitor m = new PerformanceMonitor("getBlockWithHash")) {
			return delegate.getBlock(hash, withTransactions);
		}
	}

	@Override
	public long getBlockCount() {
		try (PerformanceMonitor m = new PerformanceMonitor("getBlockCount")) {
			return delegate.getBlockCount();
		}
	}

	@Override
	public Block getBlockWithMaxIndex(final boolean withTransactions) {
		try (PerformanceMonitor m = new PerformanceMonitor("getBlockWithMaxIndex")) {
			return delegate.getBlockWithMaxIndex(withTransactions);
		}
	}

	@Override
	public long getFileSize() {
		try (PerformanceMonitor m = new PerformanceMonitor("getFileSize")) {
			return delegate.getFileSize();
		}
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		try (PerformanceMonitor m = new PerformanceMonitor("getTransactionWithHash")) {
			return delegate.getTransactionWithHash(hash);
		}
	}

	@Override
	public void put(final Block block) {
		try (PerformanceMonitor m = new PerformanceMonitor("put")) {
			delegate.put(block);
		}
	}

}
