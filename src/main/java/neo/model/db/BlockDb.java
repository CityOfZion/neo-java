package neo.model.db;

import java.util.Map;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.Transaction;

/**
 * the block database interface.
 *
 * @author coranos
 *
 */
public interface BlockDb {

	/**
	 * close the database.
	 */
	void close();

	/**
	 * return true if the hash is in the database.
	 *
	 * @param hash
	 *            the hash to use.
	 * @return true if the hash is in the database.
	 */
	boolean containsBlockWithHash(UInt256 hash);

	/**
	 * return a map of account, assetid, and value for all accounts.
	 *
	 * @return a map of account, assetid, and value for all accounts.
	 */
	Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap();

	/**
	 * return the block at the given height.
	 *
	 * @param blockHeight
	 *            the block height to use.
	 * @return the block at the given height.
	 */
	Block getBlock(long blockHeight);

	/**
	 * return the block with the given hash.
	 *
	 * @param hash
	 *            the hash to use.
	 * @return the block with the given hash.
	 */
	Block getBlock(UInt256 hash);

	/**
	 * return the block count.
	 *
	 * @return the block count.
	 */
	long getBlockCount();

	/**
	 * return the block with the highest index.
	 *
	 * @return the block with the highest index.
	 */
	Block getBlockWithMaxIndex();

	/**
	 * return the filze size of the database.
	 *
	 * @return the filze size of the database.
	 */
	long getFileSize();

	/**
	 * return the transaction with the given hash.
	 *
	 * @param hash
	 *            the transaction hash to use.
	 * @return the transaction with the given hash.
	 */
	Transaction getTransactionWithHash(UInt256 hash);

	/**
	 * puts the given block into the database.
	 *
	 * @param block
	 *            the block to use.
	 */
	void put(Block block);

}
