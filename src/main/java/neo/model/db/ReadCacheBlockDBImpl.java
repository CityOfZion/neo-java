package neo.model.db;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONObject;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.db.h2.BlockDbH2Impl;

/**
 * a blockdb implementation that caches read-only requests between writes.
 *
 * @author coranos
 *
 */
public final class ReadCacheBlockDBImpl implements BlockDb {

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
	 * the constructor.
	 *
	 * @param config
	 *            the configuration to use.
	 */
	public ReadCacheBlockDBImpl(final JSONObject config) {
		delegate = new BlockDbH2Impl(config);
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
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		return delegate.getAccountAssetValueMap();
	}

	@Override
	public long getBlockCount() {
		final Long cachedBlockCount = getCachedBlockCount();
		if (cachedBlockCount != null) {
			return cachedBlockCount;
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
	public void put(final Block block) {
		delegate.put(block);
		clearCache();
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

}
