package neo.network;

import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.codec.DecoderException;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Header;
import neo.model.util.GenesisBlockUtil;
import neo.model.util.MapUtil;
import neo.model.util.ModelUtil;
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
	 * the block height exceeds the header height.
	 */
	private static final String TOO_HIGH_IN_BLOCK = "too-high-in-block";

	/**
	 * the block is a duplicate.
	 */
	private static final String DUPLICATE_IN_BLOCK = "duplicate-in-block";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalNodeDataSynchronizedUtil.class);

	/**
	 * add the block if it is new, and the parent hash exists.
	 *
	 * @param localNodeData
	 *            the local node data to use.
	 * @param block
	 *            the block to add.
	 * @return true if added, gfalse if not.
	 */
	private static boolean addBlockIfNewAndParentExistsUnsynchronized(final LocalNodeData localNodeData,
			final Block block) {
		boolean blockChanged = false;
		final long blockIndex = block.getIndexAsLong();
		LOG.trace("STARTED addBlockIfNewAndParentExistsUnsynchronized adding block to db : index:{}; hash:{}; prev:{};",
				blockIndex, block.hash, block.prevHash);
		if (localNodeData.getBlockDb().containsBlockWithHash(block.prevHash)
				|| block.hash.equals(GenesisBlockUtil.GENESIS_HASH)) {
			if (!localNodeData.getBlockDb().containsBlockWithHash(block.hash)) {
				localNodeData.getBlockDb().put(block);
				final Block checkBlock = localNodeData.getBlockDb().getBlock(block.hash);

				final String blockBaHash = ModelUtil.toHexString(block.toByteArray());
				final String checkBlockBaHash = ModelUtil.toHexString(checkBlock.toByteArray());

				if (!blockBaHash.equals(checkBlockBaHash)) {
					LOG.error("checkBlockBaHash does not match blockBaHash.");
					LOG.error("blockBaHash     :{}", blockBaHash);
					LOG.error("checkBlockBaHash:{}", checkBlockBaHash);
				}

				localNodeData.updateHighestBlockTime();

				localNodeData.getVerifiedHeaderPoolMap().remove(blockIndex);

				blockChanged = true;
				LOG.trace(
						"SUCCESS addBlockIfNewAndParentExistsUnsynchronized adding block to db : index:{}; hash:{}; prev:{};",
						blockIndex, block.hash, block.prevHash);
			} else {
				MapUtil.increment(LocalNodeData.API_CALL_MAP, DUPLICATE_IN_BLOCK);
				LOG.trace(
						"FAILURE addBlockIfNewAndParentExistsUnsynchronized hash exists in db : index:{}; hash:{}; prev:{};",
						blockIndex, block.hash, block.prevHash);
			}
		} else {
			LOG.trace(
					"FAILURE addBlockIfNewAndParentExistsUnsynchronized prevHash does not exist in db : index:{}; hash:{}; prev:{};",
					blockIndex, block.hash, block.prevHash);
			MapUtil.increment(LocalNodeData.API_CALL_MAP, TOO_HIGH_IN_BLOCK);
		}
		return blockChanged;
	}

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
		final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex();
		if (highestBlock != null) {
			final long maxBlockIndex = highestBlock.getIndexAsLong();
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

	public static void initUnknownBlockHashHeightSet(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			BlockControlUtil.initUnknownBlockHashHeightSet(localNodeData);
		}
	}

	public static void refreshBlockFileSize(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			final long blockFileSize = localNodeData.getBlockDb().getFileSize();
			localNodeData.setBlockFileSize(blockFileSize);
		}
	}

	public static final void refreshCityOfZionBlockHeight(final LocalNodeData localNodeData)
			throws ClientProtocolException, IOException, DecoderException {
		synchronized (localNodeData) {
			BlockControlUtil.refreshCityOfZionBlockHeight(localNodeData);
		}
	}

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

	public static void requestAddresses(final LocalNodeData localNodeData, final RemoteNodeData r) {
		synchronized (localNodeData) {
			MessageUtil.sendGetAddresses(r, localNodeData);
		}
	}

	public static void requestBlocks(final LocalNodeData localNodeData, final RemoteNodeData peer) {
		synchronized (localNodeData) {
			try {
				BlockControlUtil.requestBlocksUnsynchronized(localNodeData, peer);
			} catch (final IOException | DecoderException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void requestHeaders(final LocalNodeData localNodeData, final RemoteNodeData peer) {
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
				final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex();
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

			MessageUtil.sendGetHeaders(peer, localNodeData, hash);
		}

	}

	public static boolean verifyUnverifiedBlocks(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			boolean anyBlockChanged = false;
			for (final Block block : localNodeData.getUnverifiedBlockPoolSet()) {
				final boolean blockChanged = addBlockIfNewAndParentExistsUnsynchronized(localNodeData, block);
				if (blockChanged) {
					anyBlockChanged = true;
				}
			}

			final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex();
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

	public static boolean verifyUnverifiedHeaders(final LocalNodeData localNodeData) {
		synchronized (localNodeData) {
			LOG.debug("STARTED verifyUnverifiedHeaders");
			boolean anyHeaderChanged = false;
			final Block highestBlock = localNodeData.getBlockDb().getBlockWithMaxIndex();
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
						LOG.trace(
								"INTERIM verifyUnverifiedHeaders, unverifiedHeader index:{};hash:{};prevHash:{}; highestBlock index:{};hash:{};",
								unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash, unverifiedHeader.prevHash,
								highestBlockIndex, highestBlockHash);
					}
					if (unverifiedHeader.prevHash.equals(highestBlockHash)) {
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
				} else {
					final long verifiedHeaderHeight = localNodeData.getVerifiedHeaderPoolMap().lastKey();
					final Header verifiedHeader = localNodeData.getVerifiedHeaderPoolMap().get(verifiedHeaderHeight);

					if (LOG.isTraceEnabled()) {
						LOG.trace(
								"INTERIM verifyUnverifiedHeaders, unverifiedHeader index:{};hash:{};prevHash:{}; verifiedHeader index:{};hash:{};",
								unverifiedHeader.getIndexAsLong(), unverifiedHeader.hash, unverifiedHeader.prevHash,
								verifiedHeaderHeight, verifiedHeader.hash);
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
