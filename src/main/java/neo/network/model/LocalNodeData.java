package neo.network.model;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import neo.model.core.AbstractBlockBase;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.db.BlockDb;

public class LocalNodeData {

	public static final Map<String, Long> API_CALL_MAP = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * the timers map which governs when to send automatic messages.
	 */
	private final Map<String, TimerData> timersMap;

	/**
	 * the block database.
	 */
	private final BlockDb blockDb = BlockDb.getInstance();

	/**
	 * the last time we got the highest block.
	 */
	private Date highestBlockTime;

	/**
	 * the last time we got the highest header.
	 */
	private Date highestHeaderTime;

	/**
	 * the start time.
	 */
	private final long startTime;

	/**
	 * the blockchain block count.
	 */
	private int blockchainBlockCount;

	/**
	 * the block file size.
	 */
	private long blockFileSize;

	/**
	 * the magic.
	 */
	private final long magic;

	/**
	 * the active thread count.
	 */
	private final int activeThreadCount;

	/**
	 * the RPC client timeout, in millisconds.
	 */
	private final long rpcClientTimeoutMillis;

	/**
	 * the map of verified headers, by blockchain height.
	 */
	private final SortedMap<Long, Header> verifiedHeaderPoolMap = new TreeMap<>();

	/**
	 * the set of unverified headers, sorted by blockchain height.
	 */
	private final SortedSet<Header> unverifiedHeaderPoolSet = new TreeSet<>(
			AbstractBlockBase.getAbstractBlockBaseComparator());

	/**
	 * the set of unverified blocks, sorted by blockchain height.
	 */
	private final SortedSet<Block> unverifiedBlockPoolSet = new TreeSet<>(
			AbstractBlockBase.getAbstractBlockBaseComparator());

	/**
	 * the constructor.
	 *
	 * @param magic
	 *            the magic long to use.
	 * @param activeThreadCount
	 *            the active thread count to use.
	 * @param rpcClientTimeoutMillis
	 *            the RPC client timeout to use, in milliseconds.
	 * @param timersMap
	 *            the timers map to use, which governs when to send automatic
	 *            messages.
	 */
	public LocalNodeData(final long magic, final int activeThreadCount, final long rpcClientTimeoutMillis,
			final Map<String, TimerData> timersMap) {
		startTime = System.currentTimeMillis();
		this.magic = magic;
		this.activeThreadCount = activeThreadCount;
		this.rpcClientTimeoutMillis = rpcClientTimeoutMillis;
		this.timersMap = timersMap;
	}

	/**
	 * return the active thread count.
	 *
	 * @return the active thread count.
	 */
	public int getActiveThreadCount() {
		return activeThreadCount;
	}

	/**
	 * return the blockchain block count.
	 *
	 * @return the blockchain block count.
	 */
	public int getBlockchainBlockCount() {
		return blockchainBlockCount;
	}

	/**
	 * return the block database.
	 *
	 * @return the block database.
	 */
	public BlockDb getBlockDb() {
		return blockDb;
	}

	/**
	 * the block file size.
	 *
	 * @return the block file size.
	 */
	public long getBlockFileSize() {
		return blockFileSize;
	}

	public Date getHighestBlockTime() {
		return highestBlockTime;
	}

	public Date getHighestHeaderTime() {
		return highestHeaderTime;
	}

	public long getMagic() {
		return magic;
	}

	/**
	 * return the RPC client timeout.
	 *
	 * @return the RPC client timeout.
	 */
	public long getRpcClientTimeoutMillis() {
		return rpcClientTimeoutMillis;
	}

	public long getStartTime() {
		return startTime;
	}

	public Map<String, TimerData> getTimersMap() {
		return timersMap;
	}

	public SortedSet<Block> getUnverifiedBlockPoolSet() {
		return unverifiedBlockPoolSet;
	}

	public SortedSet<Header> getUnverifiedHeaderPoolSet() {
		return unverifiedHeaderPoolSet;
	}

	public SortedMap<Long, Header> getVerifiedHeaderPoolMap() {
		return verifiedHeaderPoolMap;
	}

	public void setBlockchainBlockCount(final int blockchainBlockCount) {
		this.blockchainBlockCount = blockchainBlockCount;
	}

	public void setBlockFileSize(final long blockFileSize) {
		this.blockFileSize = blockFileSize;
	}

	public void updateHighestBlockTime() {
		highestBlockTime = new Date();
	}

	public void updateHighestHeaderTime() {
		highestHeaderTime = new Date();
	}

}
