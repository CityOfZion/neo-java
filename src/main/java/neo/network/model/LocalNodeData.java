package neo.network.model;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONObject;

import neo.model.core.AbstractBlockBase;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.db.BlockDb;
import neo.network.model.socket.SocketFactory;

/**
 * the lass containing all the data usd for the local node.
 *
 * @author coranos
 *
 */
public class LocalNodeData {

	/**
	 * the API Call map, used to track call stats.
	 */
	public static final Map<String, Long> API_CALL_MAP = Collections.synchronizedMap(new TreeMap<>());

	/**
	 * the timers map which governs when to send automatic messages.
	 */
	private final Map<String, TimerData> timersMap;

	/**
	 * the block database.
	 */
	private final BlockDb blockDb;

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
	 * the start block count.
	 */
	private final long startBlockCount;

	/**
	 * the blockchain block count.
	 */
	private long blockchainBlockCount;

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
	 * the RPC server timeout, in millisconds.
	 */
	private final long rpcServerTimeoutMillis;

	/**
	 * the nonce.
	 */
	private final int nonce;

	/**
	 * the local server port.
	 */
	private final int port;

	/**
	 * the JSON disabled calls.
	 */
	private final Set<String> rpcDisabledCalls;

	/**
	 * the local seed node file.
	 */
	private final File seedNodeFile;

	/**
	 * the local good node file.
	 */
	private final File goodNodeFile;

	/**
	 * the socket factory.
	 */
	private final SocketFactory socketFactory;

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
	 * @param rpcServerTimeoutMillis
	 *            the RPC server timeout to use, in milliseconds.
	 * @param timersMap
	 *            the timers map to use, which governs when to send automatic
	 *            messages.
	 * @param goodNodeFile
	 *            the file of known "good" remote nodes.
	 * @param seedNodeFile
	 *            the file of seed nodes. All seed nodes should be good, but not all
	 *            good nodes are seed nodes.
	 * @param port
	 *            the port for the local server.
	 * @param nonce
	 *            the nonce.
	 * @param blockDbClass
	 *            the block DB class.
	 * @param socketFactoryClass
	 *            the socket factory class.
	 * @param blockDbConfig
	 *            the blockdb configuration.
	 * @param rpcDisabledCalls
	 *            the RPC calls taht are disabled.
	 */
	public LocalNodeData(final long magic, final int activeThreadCount, final long rpcClientTimeoutMillis,
			final long rpcServerTimeoutMillis, final Class<BlockDb> blockDbClass,
			final Map<String, TimerData> timersMap, final int nonce, final int port, final File seedNodeFile,
			final File goodNodeFile, final Class<SocketFactory> socketFactoryClass, final JSONObject blockDbConfig,
			final Set<String> rpcDisabledCalls) {
		startTime = System.currentTimeMillis();
		this.magic = magic;
		this.activeThreadCount = activeThreadCount;
		this.rpcClientTimeoutMillis = rpcClientTimeoutMillis;
		this.rpcServerTimeoutMillis = rpcServerTimeoutMillis;
		this.timersMap = timersMap;
		this.nonce = nonce;
		this.port = port;
		this.seedNodeFile = seedNodeFile;
		this.goodNodeFile = goodNodeFile;
		this.rpcDisabledCalls = Collections.unmodifiableSet(rpcDisabledCalls);
		try {
			blockDb = blockDbClass.getConstructor(JSONObject.class).newInstance(blockDbConfig);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException(e);
		}
		try {
			socketFactory = socketFactoryClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		startBlockCount = blockDb.getBlockCount();
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
	public long getBlockchainBlockCount() {
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

	/**
	 * return the file of good nodes.
	 *
	 * @return the file of good nodes.
	 */
	public File getGoodNodeFile() {
		return goodNodeFile;
	}

	/**
	 * return the last time the highest block changed.
	 *
	 * @return the last time the highest block changed.
	 */
	public Date getHighestBlockTime() {
		return highestBlockTime;
	}

	/**
	 * return the last time the highest header changed.
	 *
	 * @return the last time the highest header changed.
	 */
	public Date getHighestHeaderTime() {
		return highestHeaderTime;
	}

	/**
	 * return the magic long.
	 *
	 * @return the magic long.
	 */
	public long getMagic() {
		return magic;
	}

	/**
	 * return the nonce.
	 *
	 * @return the nonce.
	 */
	public int getNonce() {
		return nonce;
	}

	/**
	 * return the port.
	 *
	 * @return the port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * return the RPC client timeout.
	 *
	 * @return the RPC client timeout.
	 */
	public long getRpcClientTimeoutMillis() {
		return rpcClientTimeoutMillis;
	}

	/**
	 * returns the RPC calls that are disabled.
	 *
	 * @return the RPC calls that are disabled.
	 */
	public Set<String> getRpcDisabledCalls() {
		return rpcDisabledCalls;
	}

	/**
	 * return the RPC server timeout.
	 *
	 * @return the RPC server timeout.
	 */
	public long getRpcServerTimeoutMillis() {
		return rpcServerTimeoutMillis;
	}

	/**
	 * return the file of seed nodes.
	 *
	 * @return the file of seed nodes.
	 */
	public File getSeedNodeFile() {
		return seedNodeFile;
	}

	/**
	 * return the socket factory.
	 *
	 * @return the socket factory.
	 */
	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	/**
	 * return the start block count.
	 *
	 * @return the start block count.
	 */
	public long getStartBlockCount() {
		return startBlockCount;
	}

	/**
	 * return the start time.
	 *
	 * @return the start time.
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * return the timers map, used for timing the sending of messages to remote
	 * nodes.
	 *
	 * @return the timers map, used for timing the sending of messages to remote
	 *         nodes.
	 */
	public Map<String, TimerData> getTimersMap() {
		return timersMap;
	}

	/**
	 * return the pool of unverified blocks.
	 *
	 * @return the pool of unverified blocks.
	 */
	public SortedSet<Block> getUnverifiedBlockPoolSet() {
		return unverifiedBlockPoolSet;
	}

	/**
	 * return the pool of unverified headers.
	 *
	 * @return the pool of unverified headers.
	 */
	public SortedSet<Header> getUnverifiedHeaderPoolSet() {
		return unverifiedHeaderPoolSet;
	}

	/**
	 * return the pool of verified headers.
	 *
	 * @return the pool of verified headers.
	 */
	public SortedMap<Long, Header> getVerifiedHeaderPoolMap() {
		return verifiedHeaderPoolMap;
	}

	/**
	 * sets the block count as read from the blockchain CityOfZion web service.
	 *
	 * @param blockchainBlockCount
	 *            the block count to use.
	 */
	public void setBlockchainBlockCount(final long blockchainBlockCount) {
		this.blockchainBlockCount = blockchainBlockCount;
	}

	/**
	 * sets the size of the block file (all files in the directory, combined).
	 *
	 * @param blockFileSize
	 *            the block file size to use.
	 */
	public void setBlockFileSize(final long blockFileSize) {
		this.blockFileSize = blockFileSize;
	}

	/**
	 * update the highest block time to be the current time.
	 */
	public void updateHighestBlockTime() {
		highestBlockTime = new Date();
	}

	/**
	 * update the highest header time to be the current time.
	 */
	public void updateHighestHeaderTime() {
		highestHeaderTime = new Date();
	}

}
