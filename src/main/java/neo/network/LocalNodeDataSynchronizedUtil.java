package neo.network;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.network.InvPayload;
import neo.model.network.InventoryType;
import neo.model.util.GenesisBlockUtil;
import neo.network.model.LocalNodeData;
import neo.network.model.RemoteNodeData;

/**
 * the local node data utility methods that require synchronization.
 *
 * @author coranos
 *
 */
public final class LocalNodeDataSynchronizedUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalNodeDataSynchronizedUtil.class);

	/**
	 * add the header if it is new.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param header
	 *            the header to add.
	 * @return true if added, gfalse if not.
	 */
	public static boolean addHeaderIfNew(final LocalNodeData localNodeData, final Header header) {
		final long headerIndex = header.getIndexAsLong();
		LOG.trace("STARTED addHeaderIfNew adding header : index:{}; hash:{};", headerIndex, header.hash);
		boolean headerChanged = false;
		synchronized (localNodeData) {
			headerChanged = addHeaderIfNewUnsynchronized(localNodeData, header);
		}
		LOG.trace("SUCCESS addHeaderIfNew adding header changed:{}; index:{}; hash:{};", headerChanged, headerIndex,
				header.hash);
		return headerChanged;
	}

	/**
	 * add the header if it is new, this is the unsynchronized helper method.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param header
	 *            the header to add.
	 * @return true if added, gfalse if not.
	 */
	private static boolean addHeaderIfNewUnsynchronized(final LocalNodeData localNodeData, final Header header) {
		final long headerIndex = header.getIndexAsLong();
		LOG.trace("STARTED addHeaderIfNewUnsynchronized adding header : index:{}; hash:{};", headerIndex, header.hash);
		final Block highestBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
		if (highestBlock != null) {
			final long maxBlockIndex = highestBlock.getIndexAsLong();
			// if
			// ("1b9a78b3c1358af990039a4ba7cbc5c581855aed06955c3cd9aa718acc3a8516".equals(header.hash.toString()))
			// {
			// LOG.error("bad 2000190 header,blockIndex:{};maxBlockIndex:{};hash:{};",
			// headerIndex, maxBlockIndex,
			// header.hash);
			// return false;
			// }
			if (headerIndex <= maxBlockIndex) {
				final String message = "FAILURE addHeaderIfNewUnsynchronized[1]"
						+ " (headerIndex[{}] <= maxBlockIndex[{}]) adding header : index:{}; hash:{};";
				LOG.trace(message, headerIndex, maxBlockIndex, headerIndex, header.hash);
				return false;
			}
		}
		if (!localNodeData.getUnverifiedBlockPoolSet().isEmpty()) {
			final long maxUnverifiedBlockIndex = localNodeData.getUnverifiedBlockPoolSet().last().getIndexAsLong();
			if (headerIndex <= maxUnverifiedBlockIndex) {
				final String message = "FAILURE addHeaderIfNewUnsynchronized[2]"
						+ " (headerIndex[{}] <= maxUnverifiedBlockIndex[{}]) adding header : index:{}; hash:{};";
				LOG.trace(message, headerIndex, maxUnverifiedBlockIndex, headerIndex, header.hash);
				return false;
			}
		}
		if (localNodeData.getVerifiedHeaderPoolMap().containsKey(headerIndex)) {
			final String message = "FAILURE addHeaderIfNewUnsynchronized[3]"
					+ " getVerifiedHeaderPoolMap().containsKey():true; adding header : index:{}; hash:{};";
			LOG.trace(message, headerIndex, header.hash);
			return false;
		}
		if (localNodeData.getVerifiedHeaderPoolMap().containsKey(headerIndex)) {
			final String message = "FAILURE addHeaderIfNewUnsynchronized[4]"
					+ " getVerifiedHeaderPoolMap().containsKey(headerIndex):true; adding header : index:{}; hash:{};";
			LOG.trace(message, headerIndex, header.hash);
			return false;
		}

		if (localNodeData.getUnverifiedHeaderPoolSet().contains(header)) {
			final String message = "FAILURE addHeaderIfNewUnsynchronized[5]"
					+ " getUnverifiedHeaderPoolSet().contains(header):true; adding header : index:{}; hash:{};";
			LOG.trace(message, headerIndex, header.hash);
			return false;
		}

		localNodeData.getUnverifiedHeaderPoolSet().add(header);
		localNodeData.updateHighestHeaderTime();
		LOG.trace("SUCCESS addHeaderIfNewUnsynchronized adding header : index:{}; hash:{};", headerIndex, header.hash);
		return true;
	}

	/**
	 * add an unverified block ( blocks may come out of order, verification ensures
	 * the hash is in the correct order).
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param block
	 *            the block to add.
	 */
	public static void addUnverifiedBlock(final LocalNodeData localNodeData, final Block block) {
		synchronized (localNodeData) {
			localNodeData.getUnverifiedBlockPoolSet().add(block);
		}
	}

	/**
	 * return the magic number.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @return the magic number.
	 */
	public static long getMagic(final LocalNodeData localNodeData) {
		final long magic;
		synchronized (localNodeData) {
			magic = localNodeData.getMagic();
		}
		return magic;
	}

	/**
	 * refreshes the block file size in the local node data..
	 *
	 * @param localNodeData
	 *            the local node data to update.
	 */
	public static void refreshBlockFileSize(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			final long blockFileSize = localNodeData.getBlockDb().getFileSize();
			localNodeData.setBlockFileSize(blockFileSize);
		}
	}

	/**
	 * refreshes the block has height in the local node data from the CityOfZion
	 * servers.
	 *
	 * @param localNodeData
	 *            the local node data to update.
	 */
	public static void refreshCityOfZionBlockHeight(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			CityOfZionBlockUtil.refreshCityOfZionBlockHeight(localNodeData);
		}
	}

	/**
	 * remove headers for blocks where the blocks have already been added to the
	 * verified block data.
	 *
	 * @param localNodeData
	 *            the local node data to update.
	 * @param blockIndex
	 *            the max blockindex, all headers not over this index should be
	 *            removed.
	 */
	private static void removeHeadersNotOverBlockIndexUnsynchronized(final LocalNodeData localNodeData,
			final long blockIndex) {
		final Iterator<Header> unverifiedHeaderIt = localNodeData.getUnverifiedHeaderPoolSet().iterator();
		while (unverifiedHeaderIt.hasNext()) {
			final Header unverifiedHeader = unverifiedHeaderIt.next();
			if (unverifiedHeader.getIndexAsLong() <= blockIndex) {
				unverifiedHeaderIt.remove();
			}
		}
	}

	/**
	 * request addresses from the remote node.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param remoteNodeData
	 *            the remote node data to use.
	 */
	public static void requestAddresses(final LocalNodeData localNodeData, final RemoteNodeData remoteNodeData) {
		synchronized (localNodeData) {
			MessageUtil.sendGetAddresses(remoteNodeData, localNodeData);
		}
	}

	/**
	 * request blocks from the remote node.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param remoteNodeData
	 *            the remote node data to use.
	 */
	public static void requestBlocks(final LocalNodeData localNodeData, final RemoteNodeData remoteNodeData) {
		synchronized (localNodeData) {
			requestBlocksUnsynchronized(localNodeData, remoteNodeData);
		}
	}

	/**
	 * request blocks from the remote node, this is the unsynchronized helper
	 * method..
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param remoteNodeData
	 *            the remote node data to use.
	 */
	private static void requestBlocksUnsynchronized(final LocalNodeData localNodeData,
			final RemoteNodeData remoteNodeData) {
		if (!localNodeData.getVerifiedHeaderPoolMap().isEmpty()) {
			final List<UInt256> hashs = new ArrayList<>();

			final Iterator<Entry<Long, Header>> headerIt = localNodeData.getVerifiedHeaderPoolMap().entrySet()
					.iterator();
			while ((hashs.size() < InvPayload.MAX_HASHES) && headerIt.hasNext()) {
				final Entry<Long, Header> headerElt = headerIt.next();
				final Header header = headerElt.getValue();
				final UInt256 hashRaw = header.hash;
				if (localNodeData.getBlockDb().containsBlockWithHash(hashRaw)) {
					headerIt.remove();
				} else {
					final byte[] ba = hashRaw.getBytesCopy();
					final UInt256 hash = new UInt256(ba);
					hashs.add(hash);
					if (LOG.isDebugEnabled()) {
						LOG.debug("requestBlocks send {} getblocks {} {}", remoteNodeData.getHostAddress(),
								header.getIndexAsLong(), hash.toReverseHexString());
					}
				}
			}
			MessageUtil.sendGetData(remoteNodeData, localNodeData, InventoryType.BLOCK, hashs.toArray(new UInt256[0]));
		} else {
			if (localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex() == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("requestBlocks send {} hash is genesis.", remoteNodeData.getHostAddress());
				}
				MessageUtil.sendGetData(remoteNodeData, localNodeData, InventoryType.BLOCK,
						GenesisBlockUtil.GENESIS_HASH);
			} else {
				LOG.info("SKIPPING requestBlocks, no hashes.");
			}
		}
	}

	/**
	 * request headers from the remote node.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param remoteNodeData
	 *            the remote node data to use.
	 */
	public static void requestHeaders(final LocalNodeData localNodeData, final RemoteNodeData remoteNodeData) {
		synchronized (localNodeData) {
			final UInt256 hashRaw;
			if (localNodeData.getUnverifiedBlockPoolSet().isEmpty()
					&& (!localNodeData.getVerifiedHeaderPoolMap().isEmpty())) {
				final long highestHeaderIndex = localNodeData.getVerifiedHeaderPoolMap().lastKey();
				final Header highestHeader = localNodeData.getVerifiedHeaderPoolMap().get(highestHeaderIndex);
				LOG.debug("requestHeaders getVerifiedHeaderPoolMap height:{};hash:{};", highestHeaderIndex,
						highestHeader.hash);
				hashRaw = highestHeader.hash;
			} else {
				final Block highestBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
				if (highestBlock != null) {
					LOG.debug("requestHeaders getHighestBlock height:{};hash:{};", highestBlock.getIndexAsLong(),
							highestBlock.hash);
					hashRaw = highestBlock.hash;
				} else {
					LOG.debug("requestHeaders hash is genesis.");
					hashRaw = GenesisBlockUtil.GENESIS_HASH;
				}
			}
			final byte[] ba = hashRaw.getBytesCopy();
			final UInt256 hash = new UInt256(ba);
			LOG.debug("requestHeaders hash:{};", hash);

			// fixed bug at height 2000190
			// final String goodHashStr =
			// "8cb9fee28a48a45468e3c0a229fd4473288cdd9794c10cac7b8f4681ca404342";
			// final UInt256 goodHash = new
			// UInt256(ByteBuffer.wrap(Hex.decode(goodHashStr)));
			// MessageUtil.sendGetHeaders(remoteNodeData, localNodeData, goodHash);

			MessageUtil.sendGetHeaders(remoteNodeData, localNodeData, hash);
		}

	}

	/**
	 * verify any unverified blocks, by checking for their prevHash in the
	 * blockchain.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @return true if any block was added to the chain.
	 */
	public static boolean verifyUnverifiedBlocks(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			boolean anyBlockChanged = false;

			final List<Block> putBlockList = new ArrayList<>();
			final Set<UInt256> putBlockHashs = new TreeSet<>();
			for (final Block block : localNodeData.getUnverifiedBlockPoolSet()) {
				if (localNodeData.getBlockDb().containsBlockWithHash(block.prevHash)
						|| block.hash.equals(GenesisBlockUtil.GENESIS_HASH) || putBlockHashs.contains(block.prevHash)) {
					putBlockList.add(block);
					putBlockHashs.add(block.hash);
					anyBlockChanged = true;
					localNodeData.updateHighestBlockTime();
					final long blockIndex = block.getIndexAsLong();
					localNodeData.getVerifiedHeaderPoolMap().remove(blockIndex);
				}
			}
			if (!putBlockList.isEmpty()) {
				final boolean forceSynch;
				final long localBlockCount = localNodeData.getBlockDb().getBlockCount();
				final long blockchainBlockCount = localNodeData.getBlockchainBlockCount();
				final long localBlockCountLowerBy = blockchainBlockCount - localBlockCount;
				if (localBlockCountLowerBy < 10) {
					forceSynch = true;
				} else {
					forceSynch = false;
				}
				if (LOG.isDebugEnabled()) {
					final String msg = "INTERIM put forceSynch={};localBlockCount:{};blockchainBlockCount:{};localBlockCountLowerBy:{};";
					LOG.debug(msg, forceSynch, localBlockCount, blockchainBlockCount, localBlockCountLowerBy);
				}

				localNodeData.getBlockDb().put(forceSynch, putBlockList.toArray(new Block[0]));
			}

			final Block highestBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
			if (highestBlock != null) {
				final long highestIndex = highestBlock.getIndexAsLong();
				final Iterator<Block> blockIt = localNodeData.getUnverifiedBlockPoolSet().iterator();
				while (blockIt.hasNext()) {
					final Block block = blockIt.next();
					if (block.getIndexAsLong() <= highestIndex) {
						blockIt.remove();
						anyBlockChanged = true;
					}
				}
			}

			return anyBlockChanged;
		}
	}

	/**
	 * verify any unverified headers, by checking if their block height is less than
	 * the max block height in the blockchain.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @return true if any block was added to the chain.
	 */
	public static boolean verifyUnverifiedHeaders(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			LOG.debug("STARTED verifyUnverifiedHeaders");
			boolean anyHeaderChanged = false;
			final Block highestBlock = localNodeData.getBlockDb().getHeaderOfBlockWithMaxIndex();
			final UInt256 highestBlockHash;
			final long highestBlockIndex;
			if (highestBlock == null) {
				highestBlockHash = GenesisBlockUtil.GENESIS_HASH;
				highestBlockIndex = 0;
			} else {
				highestBlockHash = highestBlock.hash;
				highestBlockIndex = highestBlock.getIndexAsLong();
			}

			removeHeadersNotOverBlockIndexUnsynchronized(localNodeData, highestBlockIndex);

			final Iterator<Header> unverifiedHeaderIt = localNodeData.getUnverifiedHeaderPoolSet().iterator();
			while (unverifiedHeaderIt.hasNext()) {
				final Header unverifiedHeader = unverifiedHeaderIt.next();
				if (LOG.isTraceEnabled()) {
					LOG.trace(
							"INTERIM verifyUnverifiedHeaders, unverifiedHeader index:{};hash:{}; VerifiedHeaderPoolMap.isEmpty:{};",
							unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
							localNodeData.getVerifiedHeaderPoolMap().isEmpty());
				}
				if (localNodeData.getVerifiedHeaderPoolMap().isEmpty()) {
					if (LOG.isTraceEnabled()) {
						final String message = "INTERIM verifyUnverifiedHeaders[1],"
								+ " unverifiedHeader index:{};hash:{};prevHash:{}; highestBlock index:{};hash:{};";
						LOG.trace(message, unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
								unverifiedHeader.prevHash, highestBlockIndex, highestBlockHash);
					}
					if (unverifiedHeader.prevHash.equals(highestBlockHash)) {
						localNodeData.getVerifiedHeaderPoolMap().put(unverifiedHeader.getIndexAsLong(),
								unverifiedHeader);
						unverifiedHeaderIt.remove();
						anyHeaderChanged = true;
						if (LOG.isTraceEnabled()) {
							final String message = "SUCCESS verifyUnverifiedHeaders[1],"
									+ " unverifiedHeader index:{};hash:{};prevHash:{};";
							LOG.trace(message, unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
									unverifiedHeader.prevHash);
						}
					} else {
						if (LOG.isTraceEnabled()) {
							final String message = "FAILURE verifyUnverifiedHeaders[2],"
									+ " unverifiedHeader index:{};hash:{};prevHash:{};highestBlockHash:{};";
							LOG.trace(message, unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
									unverifiedHeader.prevHash, highestBlockHash);
						}
					}
				} else {
					final long verifiedHeaderHeight = localNodeData.getVerifiedHeaderPoolMap().lastKey();
					final Header verifiedHeader = localNodeData.getVerifiedHeaderPoolMap().get(verifiedHeaderHeight);

					if (LOG.isTraceEnabled()) {
						final String message = "INTERIM verifyUnverifiedHeaders[2],"
								+ " unverifiedHeader index:{};hash:{};prevHash:{}; verifiedHeader index:{};hash:{};";
						LOG.trace(message, unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
								unverifiedHeader.prevHash, verifiedHeaderHeight, verifiedHeader.hash);
					}

					if (unverifiedHeader.prevHash.equals(verifiedHeader.hash)) {
						localNodeData.getVerifiedHeaderPoolMap().put(unverifiedHeader.getIndexAsLong(),
								unverifiedHeader);
						unverifiedHeaderIt.remove();
						anyHeaderChanged = true;
						if (LOG.isTraceEnabled()) {
							LOG.trace("SUCCESS verifyUnverifiedHeaders, unverifiedHeader index:{};hash:{};prevHash:{};",
									unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
									unverifiedHeader.prevHash);
						}
					} else {
						if (LOG.isTraceEnabled()) {
							LOG.trace("FAILURE verifyUnverifiedHeaders, unverifiedHeader index:{};hash:{};prevHash:{};",
									unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash,
									unverifiedHeader.prevHash);
						}
					}
				}
			}
			LOG.debug("SUCCESS verifyUnverifiedHeaders, anyHeaderChanged:{};", anyHeaderChanged);
			return anyHeaderChanged;
		}
	}

	/**
	 * the constructor.
	 */
	private LocalNodeDataSynchronizedUtil() {

	}
}
