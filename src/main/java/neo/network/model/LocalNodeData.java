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

import neo.model.bytes.Fixed8;
import neo.model.core.AbstractBlockBase;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.core.Transaction;
import neo.model.core.TransactionType;
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
	 * the start block height.
	 */
	private final long startBlockHeight;

	/**
	 * the network name.
	 */
	private final String networkName;

	/**
	 * the blockchain block height.
	 */
	private long blockchainBlockHeight;

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
	 * the local tcp server port.
	 */
	private final int tcpPort;

	/**
	 * the local rpc server port.
	 */
	private final int rpcPort;

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
	 * the map of system fees by transaction type.
	 */
	private final Map<TransactionType, Fixed8> transactionSystemFeeMap;

	/**
	 * the name for the exported chain file.
	 */
	private final String chainExportDataFileName;

	/**
	 * the name for the exported chain file's stats file.
	 */
	private final String chainExportStatsFileName;

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
	 * the set of unverified transactions, sorted by hash.
	 */
	private final SortedSet<Transaction> unverifiedTransactionSet = new TreeSet<>(Transaction.getComparator());

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
	 * @param tcpPort
	 *            the tcp port for the local server.
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
	 * @param rpcPort
	 *            the rpc port for the local server.
	 * @param networkName
	 *            the network name.
	 * @param transactionSystemFeeMap
	 *            the map of system fees by transaction.
	 * @param chainExportDataFileName
	 *            the file name for the exported chain data.
	 * @param chainExportStatsFileName
	 *            the file name for the exported chain statistics.
	 */
	public LocalNodeData(final long magic, final int activeThreadCount, final long rpcClientTimeoutMillis,
			final long rpcServerTimeoutMillis, final Class<BlockDb> blockDbClass,
			final Map<String, TimerData> timersMap, final int nonce, final int tcpPort, final File seedNodeFile,
			final File goodNodeFile, final Class<SocketFactory> socketFactoryClass, final JSONObject blockDbConfig,
			final Set<String> rpcDisabledCalls, final int rpcPort, final String networkName,
			final Map<TransactionType, Fixed8> transactionSystemFeeMap, final String chainExportDataFileName,
			final String chainExportStatsFileName) {
		startTime = System.currentTimeMillis();
		this.magic = magic;
		this.activeThreadCount = activeThreadCount;
		this.rpcClientTimeoutMillis = rpcClientTimeoutMillis;
		this.rpcServerTimeoutMillis = rpcServerTimeoutMillis;
		this.timersMap = timersMap;
		this.nonce = nonce;
		this.tcpPort = tcpPort;
		this.rpcPort = rpcPort;
		this.networkName = networkName;
		this.seedNodeFile = seedNodeFile;
		this.goodNodeFile = goodNodeFile;
		this.transactionSystemFeeMap = transactionSystemFeeMap;
		this.chainExportDataFileName = chainExportDataFileName;
		this.chainExportStatsFileName = chainExportStatsFileName;
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
		final Block maxHeader = blockDb.getHeaderOfBlockWithMaxIndex();
		if (maxHeader == null) {
			startBlockHeight = 0;
		} else {
			startBlockHeight = maxHeader.getIndexAsLong();
		}
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
	 * return the blockchain block height.
	 *
	 * @return the blockchain block height.
	 */
	public long getBlockchainBlockHeight() {
		return blockchainBlockHeight;
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
	 * return the name for the exported chain file.
	 *
	 * @return the name for the exported chain file.
	 */
	public String getChainExportDataFileName() {
		return chainExportDataFileName;
	}

	/**
	 * return the name for the exported chain file's stats file.
	 *
	 * @return the name for the exported chain file's stats file.
	 */
	public String getChainExportStatsFileName() {
		return chainExportStatsFileName;
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
	 * return the network name.
	 *
	 * @return the network name.
	 */
	public String getNetworkName() {
		return networkName;
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
	 * return the rpc port.
	 *
	 * @return the rpc port.
	 */
	public int getRpcPort() {
		return rpcPort;
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
	 * return the start block height.
	 *
	 * @return the start block height.
	 */
	public long getStartBlockHeight() {
		return startBlockHeight;
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
	 * return the tcp port.
	 *
	 * @return the tcp port.
	 */
	public int getTcpPort() {
		return tcpPort;
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
	 * returns the map of transaction types to system fees.
	 *
	 * @return the map of transaction types to system fees.
	 */
	public Map<TransactionType, Fixed8> getTransactionSystemFeeMap() {
		return transactionSystemFeeMap;
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
	 * return the set of unverified transactions.
	 *
	 * @return the set of unverified transactions.
	 */
	public SortedSet<Transaction> getUnverifiedTransactionSet() {
		return unverifiedTransactionSet;
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
	 * sets the block height as read from the blockchain CityOfZion web service.
	 *
	 * @param blockchainBlockHeight
	 *            the block height to use.
	 */
	public void setBlockchainBlockHeight(final long blockchainBlockHeight) {
		this.blockchainBlockHeight = blockchainBlockHeight;
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
