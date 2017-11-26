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

	private final Map<String, TimerData> timersMap;

	private final BlockDb blockDb = BlockDb.getInstance();

	private Date highestBlockTime;

	private Date highestHeaderTime;

	private int blockchainBlockCount;

	private long blockFileSize;

	private final long magic;

	private final int activeThreadCount;

	private final SortedMap<Long, Header> verifiedHeaderPoolMap = new TreeMap<>();

	private final SortedSet<Header> unverifiedHeaderPoolSet = new TreeSet<>(
			AbstractBlockBase.getAbstractBlockBaseComparator());

	private final SortedSet<Block> unverifiedBlockPoolSet = new TreeSet<>(
			AbstractBlockBase.getAbstractBlockBaseComparator());

	public LocalNodeData(final long magic, final int activeThreadCount, final Map<String, TimerData> timersMap) {
		this.magic = magic;
		this.activeThreadCount = activeThreadCount;
		this.timersMap = timersMap;
	}

	public int getActiveThreadCount() {
		return activeThreadCount;
	}

	public int getBlockchainBlockCount() {
		return blockchainBlockCount;
	}

	public BlockDb getBlockDb() {
		return blockDb;
	}

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
