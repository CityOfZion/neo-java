package neo.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.NumberFormat;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.UInt32;
import neo.model.core.Block;
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
			out.write(new UInt32(maxIndex + 1).toByteArray());
			for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
				LOG.info("STARTED export {} of {} ", blockIx, maxIndex);
				final byte[] ba = controller.getLocalNodeData().getBlockDb().getFullBlockFromHeight(blockIx)
						.toByteArray();
				out.writeInt(ba.length);
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
		try (InputStream fileIn = new FileInputStream(CHAIN_ACC_FILE_NM);
				BufferedInputStream buffIn = new BufferedInputStream(fileIn, 1024 * 1024 * 32);
				DataInputStream in = new DataInputStream(buffIn);) {
			final byte[] maxIndexBa = new byte[UInt32.SIZE];
			in.read(maxIndexBa);
			ArrayUtils.reverse(maxIndexBa);
			final long maxIndex = new UInt32(maxIndexBa).asLong();

			final NumberFormat integerFormat = NumberFormat.getIntegerInstance();
			final NumberFormat doubleFormat = NumberFormat.getNumberInstance();
			LOG.info("started import {}", integerFormat.format(maxIndex));
			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();

			final long startMs = System.currentTimeMillis();
			long numBlocks = 0;
			long numTx = 0;

			for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
				final int length = Integer.reverseBytes(in.readInt());
				LOG.debug("STARTED import {} of {} length {}", integerFormat.format(blockIx),
						integerFormat.format(maxIndex), integerFormat.format(length));
				final byte[] ba = new byte[length];
				in.read(ba);
				final Block block = new Block(ByteBuffer.wrap(ba));
				final boolean forceSynch = (blockIx % 500) == 0;
				blockDb.put(forceSynch, block);

				numBlocks++;
				numTx += block.getTransactionList().size();

				LOG.debug("SUCCESS import {} of {} hash {}", integerFormat.format(blockIx),
						integerFormat.format(maxIndex), block.hash);
				if (forceSynch) {
					final long ms = System.currentTimeMillis() - startMs;
					final double tps = numTx / (ms / 1000.0);
					final double bps = numBlocks / (ms / 1000.0);
					final Block maxBlockHeader = blockDb.getHeaderOfBlockWithMaxIndex();
					LOG.info("INTERIM import {} of {}, synched, height {}, accounts {}, tx {}, bps {} tps {}",
							integerFormat.format(blockIx), integerFormat.format(maxIndex),
							integerFormat.format(maxBlockHeader.getIndexAsLong()),
							integerFormat.format(blockDb.getAccountCount()), integerFormat.format(numTx),
							doubleFormat.format(bps), doubleFormat.format(tps), maxBlockHeader.getTimestamp());
				}
			}
			blockDb.put(true);
			LOG.info("SUCCESS import {}, synched", integerFormat.format(maxIndex));
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
