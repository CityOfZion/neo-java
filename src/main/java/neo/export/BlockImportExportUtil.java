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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt160;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.TransactionType;
import neo.model.db.BlockDb;
import neo.network.LocalControllerNode;

/**
 * utilities for importing and exporting block.
 *
 * @author coranos
 *
 */
public final class BlockImportExportUtil {

	/**
	 * the name for the exported chain file.
	 */
	private static final String CHAIN_ACC_FILE_NM = "chain.acc";

	/**
	 * the name for the exported chain file's stats file.
	 */
	private static final String CHAIN_STATS_FILE_NM = "chain-stats.json";

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
		try (DataOutputStream out = new DataOutputStream(
				new BufferedOutputStream(new FileOutputStream(CHAIN_ACC_FILE_NM), 1024 * 1024 * 32))) {
			final long maxIndex = controller.getLocalNodeData().getBlockDb().getHeaderOfBlockWithMaxIndex()
					.getIndexAsLong();
			final byte[] maxIndexBa = new UInt32(maxIndex + 1).toByteArray();
			ArrayUtils.reverse(maxIndexBa);
			out.write(maxIndexBa);
			for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
				LOG.info("STARTED export {} of {} ", blockIx, maxIndex);
				final byte[] ba = controller.getLocalNodeData().getBlockDb().getFullBlockFromHeight(blockIx)
						.toByteArray();
				out.writeInt(Integer.reverseBytes(ba.length));
				out.write(ba);
				LOG.info("SUCCESS export {} of {} ", blockIx, maxIndex);
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * imports the blocks from the file.
	 *
	 * @param controller
	 *            the controller.
	 */
	public static void importBlocks(final LocalControllerNode controller) {
		final NumberFormat integerFormat = NumberFormat.getIntegerInstance();
		final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();

		try (OutputStream statsFileOut = new FileOutputStream(CHAIN_STATS_FILE_NM);
				PrintWriter statsWriter = new PrintWriter(statsFileOut, true);) {
			long maxIndex = 0;
			try (InputStream fileIn = new FileInputStream(CHAIN_ACC_FILE_NM);
					BufferedInputStream buffIn = new BufferedInputStream(fileIn, 1024 * 1024 * 32);
					DataInputStream in = new DataInputStream(buffIn);) {

				statsWriter.println("[");

				final byte[] maxIndexBa = new byte[UInt32.SIZE];
				in.read(maxIndexBa);
				ArrayUtils.reverse(maxIndexBa);
				maxIndex = new UInt32(maxIndexBa).asLong();

				LOG.info("started import {}", integerFormat.format(maxIndex));

				final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy.MM.dd");
				long startMs = -1;
				long interimBlocks = 0;
				final long[] interimTx = new long[TransactionType.values().length];
				final long[] totalTx = new long[TransactionType.values().length];
				final Set<UInt160> activeAccountSet = new TreeSet<>();

				long procStartMs = System.currentTimeMillis();

				for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
					final int length = Integer.reverseBytes(in.readInt());
					LOG.debug("STARTED import {} of {} length {}", integerFormat.format(blockIx),
							integerFormat.format(maxIndex), integerFormat.format(length));
					final byte[] ba = new byte[length];
					in.read(ba);
					final Block block = new Block(ByteBuffer.wrap(ba));
					final boolean forceSynch = (blockIx % 500) == 0;
					blockDb.put(forceSynch, block);

					interimBlocks++;

					for (final Transaction tx : block.getTransactionList()) {
						interimTx[tx.type.ordinal()]++;
						totalTx[tx.type.ordinal()]++;
						for (final TransactionOutput txOut : tx.outputs) {
							activeAccountSet.add(txOut.scriptHash);
						}
					}

					LOG.debug("SUCCESS import {} of {} hash {}", integerFormat.format(blockIx),
							integerFormat.format(maxIndex), block.hash);
					final Timestamp blockTs = block.getTimestamp();

					if (startMs < 0) {
						startMs = blockTs.getTime();
					}

					final long ms = blockTs.getTime() - startMs;
					if (ms > (86400 * 1000)) {
						final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();

						final String dateStr = dateFormat.format(blockTs);
						final JSONObject stats = new JSONObject();
						stats.put("date", dateStr);

						final long procMs = System.currentTimeMillis() - procStartMs;
						stats.put("processing-time-ms", procMs);

						final JSONObject accounts = new JSONObject();
						accounts.put("active", activeAccountSet.size());
						accounts.put("total", blockDb.getAccountCount());
						stats.put("accounts", accounts);

						final JSONObject transactions = new JSONObject();
						for (final TransactionType tType : TransactionType.values()) {
							transactions.put(tType.name().toLowerCase(), interimTx[tType.ordinal()]);
						}
						stats.put("transactions", transactions);

						stats.put("blocks", interimBlocks);
						if (blockIx > 0) {
							statsWriter.println(",");
						}
						statsWriter.println(stats);

						LOG.info("INTERIM import {} of {}, bx {}, tx {} json {}", integerFormat.format(blockIx),
								integerFormat.format(maxIndex), integerFormat.format(maxBlockHeader.getIndexAsLong()),
								integerFormat.format(totalTx), stats);
						startMs = blockTs.getTime();

						for (int ix = 0; ix < interimTx.length; ix++) {
							interimTx[ix] = 0;
						}
						interimBlocks = 0;
						activeAccountSet.clear();
						procStartMs = System.currentTimeMillis();
					}
				}
				blockDb.put(true);

				LOG.info("SUCCESS import {}, synched", integerFormat.format(maxIndex));
			} catch (final IOException e) {
				if (e instanceof EOFException) {
					blockDb.put(true);
					final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();
					LOG.error("FAILURE import {} of {}, synched", integerFormat.format(maxBlockHeader.getIndexAsLong()),
							maxIndex);
					return;
				} else {
					throw new RuntimeException(e);
				}
			} finally {
				statsWriter.println("]");
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
