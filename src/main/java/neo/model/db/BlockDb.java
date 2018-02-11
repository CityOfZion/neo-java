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
	 * deletes the highest block in the database.
	 */
	void deleteHighestBlock();

	/**
	 * return a map of account, assetid, and value for all accounts.
	 *
	 * @return a map of account, assetid, and value for all accounts.
	 */
	Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap();

	/**
	 * returns the number of accounts in the database.
	 *
	 * @return the number of accounts in the database.
	 */
	long getAccountCount();

	/**
	 * return the block count.
	 *
	 * @return the block count.
	 */
	long getBlockCount();

	/**
	 * return the filze size of the database.
	 *
	 * @return the filze size of the database.
	 */
	long getFileSize();

	/**
	 * return the block with the given hash, with transactions attached.
	 *
	 * @param hash
	 *            the hash to use.
	 * @return the block with the given hash.
	 */
	Block getFullBlockFromHash(UInt256 hash);

	/**
	 * return the block at the given height, with transactions attached.
	 *
	 * @param blockHeight
	 *            the block height to use.
	 * @return the block at the given height.
	 */
	Block getFullBlockFromHeight(long blockHeight);

	/**
	 * return the block with the given hash, withount transactions attached.
	 *
	 * @param hash
	 *            the hash to use.
	 * @return the block with the given hash.
	 */
	Block getHeaderOfBlockFromHash(UInt256 hash);

	/**
	 * return the block at the given height, withount transactions attached.
	 *
	 * @param blockHeight
	 *            the block height to use.
	 * @return the block at the given height.
	 */
	Block getHeaderOfBlockFromHeight(long blockHeight);

	/**
	 * return the block with the highest index.
	 *
	 * @return the block with the highest index.
	 */
	Block getHeaderOfBlockWithMaxIndex();

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
	 * @param blocks
	 *            the blocks to use.
	 */
	void put(Block... blocks);

	/**
	 * validates the database.
	 */
	void validate();
}
