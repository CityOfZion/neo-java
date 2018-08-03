package neo.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.kerby.util.Hex;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.TransactionType;
import neo.model.db.BlockDb;
import neo.model.util.MapUtil;
import neo.model.util.ModelUtil;
import neo.network.LocalControllerNode;
import neo.network.model.LocalNodeData;

/**
 * utilities for importing and exporting block.
 *
 * @author coranos
 *
 */
public final class BlockImportExportUtil {

	/**
	 * close bracket.
	 */
	private static final String CLOSE_BRACKET = "]";

	/**
	 * open bracket.
	 */
	private static final String OPEN_BRACKET = "[";

	/**
	 * a comma.
	 */
	private static final String COMMA = ",";

	/**
	 * JSON key "bytes".
	 */
	private static final String BYTES = "bytes";

	/**
	 * JSON key "blocks".
	 */
	private static final String BLOCKS = "blocks";

	/**
	 * JSON key "block_bins".
	 */
	private static final String BLOCK_BINS = "block_bins";

	/**
	 * JSON key "transactions".
	 */
	private static final String TRANSACTIONS = "transactions";

	/**
	 * JSON key "transaction_network_fees".
	 */
	private static final String TRANSACTION_NETWORK_FEES = "transaction_network_fees";

	/**
	 * JSON key "accounts".
	 */
	private static final String ACCOUNTS = "accounts";

	/**
	 * JSON key "total".
	 */
	private static final String TOTAL = "total";

	/**
	 * JSON key "active".
	 */
	private static final String ACTIVE = "active";

	/**
	 * JSON key "processing-time-ms".
	 */
	private static final String PROCESSING_TIME_MS = "processing-time-ms";

	/**
	 * JSON key "date".
	 */
	private static final String DATE = "date";

	/**
	 * the integer format.
	 */
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance();

	/**
	 * the date format.
	 */
	private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy.MM.dd");

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockImportExportUtil.class);

	/**
	 * exports the blocks to the file.
	 *
	 * @param controller
	 *            the controller.
	 */
	public static void exportBlocks(final LocalControllerNode controller) {
		final LocalNodeData localNodeData = controller.getLocalNodeData();
		final BlockDb blockDb = localNodeData.getBlockDb();
		try (OutputStream statsFileOut = new FileOutputStream(localNodeData.getChainExportStatsFileName());
				PrintWriter statsWriter = new PrintWriter(statsFileOut, true);) {
			statsWriter.println(OPEN_BRACKET);
			try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(localNodeData.getChainExportDataFileName()), 1024 * 1024 * 32))) {

				final long maxIndex = blockDb.getHeaderOfBlockWithMaxIndex().getIndexAsLong();
				final byte[] maxIndexBa = new UInt32(maxIndex + 1).toByteArray();
				out.write(maxIndexBa);

				if (LOG.isTraceEnabled()) {
					LOG.trace("export maxIndexBa aswritten {}", Hex.encode(maxIndexBa));
				}

				long startMs = -1;
				long interimBlocks = 0;
				long interimBytes = 0;
				final long[] interimTx = new long[TransactionType.values().length];
				final long[] interimTxNetworkFees = new long[TransactionType.values().length];
				long totalTx = 0;
				final Map<String, Long> numBlocksByTxCountMap = new TreeMap<>();

				@SuppressWarnings("unchecked")
				final Set<UInt160>[] activeAccountSet = new Set[TransactionType.values().length];
				for (int txOrdinal = 0; txOrdinal < activeAccountSet.length; txOrdinal++) {
					activeAccountSet[txOrdinal] = new TreeSet<>();
				}

				long procStartMs = System.currentTimeMillis();

				for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
					LOG.debug("STARTED export {} of {} ", blockIx, maxIndex);
					final Block block = localNodeData.getBlockDb().getFullBlockFromHeight(blockIx);
					final byte[] ba = block.toByteArray();
					final int length = Integer.reverseBytes(ba.length);
					out.writeInt(length);
					out.write(ba);
					LOG.debug("SUCCESS export {} of {} length {}", blockIx, maxIndex, ba.length);

					final Timestamp blockTs = block.getTimestamp();
					interimBlocks++;
					interimBytes += ba.length;
					for (final Transaction tx : block.getTransactionList()) {
						interimTx[tx.type.ordinal()]++;
						final Fixed8 systemFee = localNodeData.getTransactionSystemFeeMap().get(tx.type);
						interimTxNetworkFees[tx.type.ordinal()] += getNetworkFee(blockDb, tx, systemFee).value;
						totalTx++;
						for (final TransactionOutput txOut : tx.outputs) {
							activeAccountSet[tx.type.ordinal()].add(txOut.scriptHash);
						}
					}

					MapUtil.increment(numBlocksByTxCountMap, String.valueOf(block.getTransactionList().size()));

					if (startMs < 0) {
						startMs = blockTs.getTime();
					}

					final long ms = blockTs.getTime() - startMs;
					if (ms > (86400 * 1000)) {
						out.flush();
						final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();

						final JSONObject stats = getStats(blockDb, interimBlocks, interimBytes, interimTx,
								activeAccountSet, procStartMs, blockTs, interimTxNetworkFees, numBlocksByTxCountMap);
						if (blockIx > 0) {
							statsWriter.println(COMMA);
						}
						statsWriter.println(stats);

						LOG.info("INTERIM export {} of {}, bx {}, tx {} json {}", INTEGER_FORMAT.format(blockIx),
								INTEGER_FORMAT.format(maxIndex), INTEGER_FORMAT.format(maxBlockHeader.getIndexAsLong()),
								INTEGER_FORMAT.format(totalTx), stats);
						startMs = blockTs.getTime();

						for (int ix = 0; ix < interimTx.length; ix++) {
							interimTx[ix] = 0;
							interimTxNetworkFees[ix] = 0;
						}
						interimBlocks = 0;
						interimBytes = 0;
						for (int txOrdinal = 0; txOrdinal < activeAccountSet.length; txOrdinal++) {
							activeAccountSet[txOrdinal].clear();
						}

						numBlocksByTxCountMap.clear();

						procStartMs = System.currentTimeMillis();
					}

				}
				out.flush();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			} finally {
				statsWriter.println(CLOSE_BRACKET);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * return the network fee.
	 *
	 * @param blockDb
	 *            the block database.
	 * @param tx
	 *            the transaction.
	 * @param systemFee
	 *            the system fee.
	 * @return the network fee.
	 */
	private static Fixed8 getNetworkFee(final BlockDb blockDb, final Transaction tx, final Fixed8 systemFee) {
		switch (tx.type) {
		case MINER_TRANSACTION:
		case CLAIM_TRANSACTION:
		case ENROLLMENT_TRANSACTION:
		case ISSUE_TRANSACTION:
		case REGISTER_TRANSACTION:
			LOG.trace("txType:{}; No Network Fee", tx.type);
			return ModelUtil.FIXED8_ZERO;
		default:
		}
		Fixed8 totalInput = ModelUtil.FIXED8_ZERO;
		for (final CoinReference cr : tx.inputs) {
			final UInt256 prevHashReversed = cr.prevHash.reverse();
			final Transaction tiTx = blockDb.getTransactionWithHash(prevHashReversed);
			final int prevIndex = cr.prevIndex.asInt();
			final TransactionOutput txOut = tiTx.outputs.get(prevIndex);
			if (txOut.assetId.equals(ModelUtil.GAS_HASH)) {
				totalInput = ModelUtil.add(totalInput, txOut.value);
			}
		}

		Fixed8 totalOutput = ModelUtil.FIXED8_ZERO;
		for (final TransactionOutput txOut : tx.outputs) {
			if (txOut.assetId.equals(ModelUtil.GAS_HASH)) {
				totalOutput = ModelUtil.add(totalOutput, txOut.value);
			}
		}

		if (totalInput.equals(ModelUtil.FIXED8_ZERO) && totalOutput.equals(ModelUtil.FIXED8_ZERO)
				&& systemFee.equals(ModelUtil.FIXED8_ZERO)) {
			LOG.trace("txType:{}; Inout,Output, and System fees are all zero, No Network Fee", tx.type);
			return ModelUtil.FIXED8_ZERO;
		}

		final Fixed8 totalFee;
		try {
			totalFee = ModelUtil.subtract(totalOutput, totalInput);
		} catch (final RuntimeException e) {
			LOG.error("txType:{}; totalInput:{}; totalOutput:{}; systemFee:{}; hash:{};", tx.type, totalInput,
					totalOutput, systemFee, tx.getHash());
			throw new RuntimeException("error calculating totalFee", e);
		}
		final Fixed8 networkFee;
		;
		try {
			networkFee = ModelUtil.subtract(systemFee, totalFee);
		} catch (final RuntimeException e) {
			LOG.error("txType:{}; totalInput:{}; totalOutput:{}; systemFee:{}; totalFee:{}; hash:{};", tx.type,
					totalInput, totalOutput, systemFee, totalFee, tx.getHash());
			throw new RuntimeException("error calculating networkFee", e);
		}
		return networkFee;
	}

	/**
	 * gets stats.
	 *
	 * @param blockDb
	 *            the block db to use.
	 * @param interimBlocks
	 *            the interim block count to use.
	 * @param interimBytes
	 *            the interim byte count to use.
	 * @param interimTx
	 *            the interim transaction count to use.
	 * @param activeAccountSet
	 *            the active account set to use.
	 * @param procStartMs
	 *            start milliseconds.
	 * @param blockTs
	 *            block timestamp.
	 * @param interimTxNetworkFees
	 *            the interim transaction network fees to use.
	 * @param numBlocksByTxCountMap
	 *            the bins of how many transactions were in each block.
	 * @return the stats JSON.
	 */
	public static JSONObject getStats(final BlockDb blockDb, final long interimBlocks, final long interimBytes,
			final long[] interimTx, final Set<UInt160>[] activeAccountSet, final long procStartMs,
			final Timestamp blockTs, final long[] interimTxNetworkFees, final Map<String, Long> numBlocksByTxCountMap) {
		final String dateStr = DATE_FORMAT.format(blockTs);
		final JSONObject stats = new JSONObject();
		stats.put(DATE, dateStr);

		final long procMs = System.currentTimeMillis() - procStartMs;
		stats.put(PROCESSING_TIME_MS, procMs);

		final JSONObject active = new JSONObject();
		final Set<UInt160> totalActiveAccountSet = new TreeSet<>();
		for (final TransactionType tType : TransactionType.values()) {
			totalActiveAccountSet.addAll(activeAccountSet[tType.ordinal()]);
			active.put(tType.name().toLowerCase(), activeAccountSet[tType.ordinal()].size());
		}
		active.put(TOTAL, totalActiveAccountSet.size());
		final JSONObject accounts = new JSONObject();
		accounts.put(ACTIVE, active);
		accounts.put(TOTAL, blockDb.getAccountCount());
		stats.put(ACCOUNTS, accounts);

		final JSONObject transactions = new JSONObject();
		for (final TransactionType tType : TransactionType.values()) {
			transactions.put(tType.name().toLowerCase(), interimTx[tType.ordinal()]);
		}
		stats.put(TRANSACTIONS, transactions);
		final JSONObject transactionNetworkFees = new JSONObject();
		for (final TransactionType tType : TransactionType.values()) {
			transactionNetworkFees.put(tType.name().toLowerCase(), interimTxNetworkFees[tType.ordinal()]);
		}
		stats.put(TRANSACTION_NETWORK_FEES, transactionNetworkFees);

		final JSONObject blockBins = new JSONObject();
		for (final String binName : numBlocksByTxCountMap.keySet()) {
			final long numBlocks = numBlocksByTxCountMap.get(binName);
			blockBins.put(binName, numBlocks);
		}
		stats.put(BLOCK_BINS, blockBins);

		stats.put(BLOCKS, interimBlocks);
		stats.put(BYTES, interimBytes);
		return stats;
	}

	/**
	 * imports the blocks from the file.
	 *
	 * @param controller
	 *            the controller.
	 */
	public static void importBlocks(final LocalControllerNode controller) {
		final LocalNodeData localNodeData = controller.getLocalNodeData();
		final BlockDb blockDb = localNodeData.getBlockDb();

		try (OutputStream statsFileOut = new FileOutputStream(localNodeData.getChainExportStatsFileName());
				PrintWriter statsWriter = new PrintWriter(statsFileOut, true);) {
			statsWriter.println(OPEN_BRACKET);
			long maxIndex = 0;
			try (InputStream fileIn = new FileInputStream(localNodeData.getChainExportDataFileName());
					BufferedInputStream buffIn = new BufferedInputStream(fileIn, 1024 * 1024 * 32);
					DataInputStream in = new DataInputStream(buffIn);) {

				final byte[] maxIndexBa = new byte[UInt32.SIZE];
				in.read(maxIndexBa);

				if (LOG.isTraceEnabled()) {
					LOG.info("import maxIndexBa asread {}", Hex.encode(maxIndexBa));
				}

				ArrayUtils.reverse(maxIndexBa);
				maxIndex = new UInt32(maxIndexBa).asLong();

				LOG.info("started import {}", INTEGER_FORMAT.format(maxIndex));
				long startMs = -1;
				long interimBlocks = 0;
				long interimBytes = 0;
				final long[] interimTx = new long[TransactionType.values().length];
				final long[] interimTxNetworkFees = new long[TransactionType.values().length];
				long totalTx = 0;
				final Map<String, Long> numBlocksByTxCountMap = new TreeMap<>();

				@SuppressWarnings("unchecked")
				final Set<UInt160>[] activeAccountSet = new Set[TransactionType.values().length];
				for (int txOrdinal = 0; txOrdinal < activeAccountSet.length; txOrdinal++) {
					activeAccountSet[txOrdinal] = new TreeSet<>();
				}

				long procStartMs = System.currentTimeMillis();

				for (long blockIx = 0; blockIx < maxIndex; blockIx++) {
					final int length = Integer.reverseBytes(in.readInt());
					LOG.debug("STARTED import {} of {} length {}", INTEGER_FORMAT.format(blockIx),
							INTEGER_FORMAT.format(maxIndex), INTEGER_FORMAT.format(length));
					final byte[] ba = new byte[length];
					in.read(ba);
					final Block block = new Block(ByteBuffer.wrap(ba));
					final boolean forceSynch = (blockIx % BlockDb.BLOCK_FORCE_SYNCH_INTERVAL) == 0;
					blockDb.put(forceSynch, block);

					interimBlocks++;
					interimBytes += ba.length;

					for (final Transaction tx : block.getTransactionList()) {
						interimTx[tx.type.ordinal()]++;
						final Fixed8 systemFee = localNodeData.getTransactionSystemFeeMap().get(tx.type);
						interimTxNetworkFees[tx.type.ordinal()] += getNetworkFee(blockDb, tx, systemFee).value;
						totalTx++;
						for (final TransactionOutput txOut : tx.outputs) {
							activeAccountSet[tx.type.ordinal()].add(txOut.scriptHash);
						}
					}

					LOG.debug("SUCCESS import {} of {} hash {}", INTEGER_FORMAT.format(blockIx),
							INTEGER_FORMAT.format(maxIndex), block.hash);

					MapUtil.increment(numBlocksByTxCountMap, String.valueOf(block.getTransactionList().size()));

					final Timestamp blockTs = block.getTimestamp();

					if (startMs < 0) {
						startMs = blockTs.getTime();
					}

					final long ms = blockTs.getTime() - startMs;
					if (ms > (86400 * 1000)) {
						blockDb.put(true);
						final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();

						final JSONObject stats = getStats(blockDb, interimBlocks, interimBytes, interimTx,
								activeAccountSet, procStartMs, blockTs, interimTxNetworkFees, numBlocksByTxCountMap);
						if (blockIx > 0) {
							statsWriter.println(COMMA);
						}
						statsWriter.println(stats);

						final long maxBlockHeaderIndex = maxBlockHeader.getIndexAsLong();
						LOG.info("INTERIM import {} of {}, bx {}, tx {} json {}", INTEGER_FORMAT.format(blockIx),
								INTEGER_FORMAT.format(maxIndex), INTEGER_FORMAT.format(maxBlockHeaderIndex),
								INTEGER_FORMAT.format(totalTx), stats);
						startMs = blockTs.getTime();

						for (int ix = 0; ix < interimTx.length; ix++) {
							interimTx[ix] = 0;
							interimTxNetworkFees[ix] = 0;
						}
						interimBlocks = 0;
						interimBytes = 0;
						for (int txOrdinal = 0; txOrdinal < activeAccountSet.length; txOrdinal++) {
							activeAccountSet[txOrdinal].clear();
						}

						numBlocksByTxCountMap.clear();

						procStartMs = System.currentTimeMillis();
					}
				}
				blockDb.put(true);

				LOG.info("SUCCESS import {}, synched", INTEGER_FORMAT.format(maxIndex));
			} catch (final IOException e) {
				if (e instanceof EOFException) {
					blockDb.put(true);
					final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();
					LOG.error("FAILURE import {} of {}, synched, EOFException",
							INTEGER_FORMAT.format(maxBlockHeader.getIndexAsLong()), maxIndex);
					LOG.error("EOFException", e);
					return;
				} else {
					throw new RuntimeException(e);
				}
			} finally {
				statsWriter.println(CLOSE_BRACKET);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * the constructor.
	 */
	private BlockImportExportUtil() {
	}

}
