package neo.rpc.client.test.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.db.BlockDb;
import neo.model.util.ModelUtil;

/**
 * the mock block database.
 *
 * @author coranos
 *
 */
public abstract class AbstractJsonMockBlockDb implements BlockDb {

	/**
	 * the hash JSON key.
	 */
	private static final String HASH = "hash";

	/**
	 * the block JSON key.
	 */
	private static final String BLOCK = "block";

	/**
	 * the index JSON key.
	 */
	private static final String INDEX = "index";

	/**
	 * return the Block in the given mock block.
	 *
	 * @param mockBlock
	 *            the mock block to use.
	 * @param withTransactions
	 *            if true, add transactions. If false, only return the block header.
	 * @return the Block in the given mock block.
	 */
	private static Block getBlock(final JSONObject mockBlock, final boolean withTransactions) {
		final String blockHex = mockBlock.getString(BLOCK);
		final Block block = new Block(ByteBuffer.wrap(ModelUtil.decodeHex(blockHex)));
		if (!withTransactions) {
			block.getTransactionList().clear();
		}
		return block;
	}

	/**
	 * the constructor.
	 */
	public AbstractJsonMockBlockDb() {
	}

	@Override
	public final void close() {
	}

	@Override
	public final boolean containsBlockWithHash(final UInt256 hash) {
		final String hashHex = hash.toHexString();
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			if (mockBlock.getString(HASH).equals(hashHex)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void deleteHighestBlock() {
	}

	@Override
	public final Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		final Map<UInt160, Map<UInt256, Fixed8>> accountAssetValueMap = new TreeMap<>();
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			final Block block = getBlock(mockBlock, true);
			for (final Transaction transaction : block.getTransactionList()) {
				for (final TransactionOutput output : transaction.outputs) {
					if (!accountAssetValueMap.containsKey(output.scriptHash)) {
						accountAssetValueMap.put(output.scriptHash, new TreeMap<>());
					}
					final Map<UInt256, Fixed8> assetValueMap = accountAssetValueMap.get(output.scriptHash);
					final Fixed8 value = output.value;
					if (assetValueMap.containsKey(output.assetId)) {
						final Fixed8 oldValue = assetValueMap.get(output.assetId);
						final Fixed8 newValue = ModelUtil.add(value, oldValue);
						assetValueMap.put(output.assetId, newValue);
					} else {
						assetValueMap.put(output.assetId, value);
					}
				}
			}
		}
		return accountAssetValueMap;
	}

	@Override
	public long getAccountCount() {
		return getAccountAssetValueMap().size();
	}

	@Override
	public Map<UInt256, Fixed8> getAssetValueMap(final UInt160 account) {
		return getAccountAssetValueMap().get(account);
	}

	/**
	 * return the block at the given height, with transactions attached.
	 *
	 * @param blockHeight
	 *            the block height to use.
	 * @param withTransactions
	 *            if true, add transactions. If false, only return the block header.
	 * @return the block at the given height.
	 */
	private Block getBlock(final long blockHeight, final boolean withTransactions) {
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			if (mockBlock.getLong(INDEX) == blockHeight) {
				final Block block = getBlock(mockBlock, withTransactions);
				return block;
			}
		}
		throw new RuntimeException("no block at height:" + blockHeight);
	}

	/**
	 * returns the block with the given hash.
	 *
	 * @param hash
	 *            the hash to use.
	 * @param withTransactions
	 *            if true, add transactions. If false, only return the block header.
	 * @return the block with the given hash.
	 */
	private Block getBlock(final UInt256 hash, final boolean withTransactions) {
		final String hashHex = hash.toHexString();
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			if (mockBlock.getString(HASH).equals(hashHex)) {
				final Block block = getBlock(mockBlock, withTransactions);
				return block;
			}
		}
		throw new RuntimeException("no block at hash:" + hash);
	}

	@Override
	public final long getBlockCount() {
		return getMockBlockDb().length();
	}

	@Override
	public final Long getBlockIndexFromTransactionHash(final UInt256 hash) {
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			final Block block = getBlock(mockBlock, true);
			for (final Transaction transaction : block.getTransactionList()) {
				if (transaction.getHash().equals(hash)) {
					return block.getIndexAsLong();
				}
			}
		}
		throw new RuntimeException("no transaction with hash:" + hash);
	}

	/**
	 * return the block with the maximum value in the index column.
	 *
	 * @param withTransactions
	 *            if true, add transactions. If false, only return the block header.
	 * @return the block with the maximum value in the index column.
	 */
	private Block getBlockWithMaxIndex(final boolean withTransactions) {
		Block maxBlock = null;
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			if (maxBlock == null) {
				maxBlock = getBlock(mockBlock, withTransactions);
			} else {
				if (mockBlock.getLong(INDEX) > maxBlock.getIndexAsLong()) {
					maxBlock = getBlock(mockBlock, withTransactions);
				}
			}
		}
		return maxBlock;
	}

	@Override
	public final long getFileSize() {
		return 0;
	}

	@Override
	public final Block getFullBlockFromHash(final UInt256 hash) {
		return getBlock(hash, true);
	}

	@Override
	public final Block getFullBlockFromHeight(final long blockHeight) {
		return getBlock(blockHeight, true);
	}

	@Override
	public final Block getHeaderOfBlockFromHash(final UInt256 hash) {
		return getBlock(hash, false);
	}

	@Override
	public final Block getHeaderOfBlockFromHeight(final long blockHeight) {
		return getBlock(blockHeight, false);
	}

	@Override
	public final Block getHeaderOfBlockWithMaxIndex() {
		return getBlockWithMaxIndex(false);
	}

	/**
	 * return the mock block database.
	 *
	 * @return the mock block database.
	 */
	public abstract JSONArray getMockBlockDb();

	@Override
	public <K, V> Map<K, V> getStates(final Class<K> keyClass, final Class<V> valueClass) {
		return new TreeMap<>();
	}

	@Override
	public List<Transaction> getTransactionWithAccountList(final UInt160 account) {
		final List<Transaction> transactionList = new ArrayList<>();
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			final Block block = getBlock(mockBlock, true);
			for (final Transaction transaction : block.getTransactionList()) {
				boolean transactionHasAccount = false;
				for (final TransactionOutput output : transaction.outputs) {
					if (output.scriptHash.equals(account)) {
						transactionHasAccount = true;
					}
				}
				if (transactionHasAccount) {
					transactionList.add(transaction);
				}
			}
		}
		return transactionList;
	}

	@Override
	public final Transaction getTransactionWithHash(final UInt256 hash) {
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			final Block block = getBlock(mockBlock, true);
			for (final Transaction transaction : block.getTransactionList()) {
				if (transaction.getHash().equals(hash)) {
					return transaction;
				}
			}
		}
		throw new RuntimeException("no transaction with hash:" + hash);
	}

	@Override
	public Map<UInt256, Map<TransactionOutput, CoinReference>> getUnspentTransactionOutputListMap(
			final UInt160 account) {
		final Map<UInt256, Map<TransactionOutput, CoinReference>> assetIdTxoMap = new TreeMap<>();
		final JSONArray mockBlockDb = getMockBlockDb();
		for (int ix = 0; ix < mockBlockDb.length(); ix++) {
			final JSONObject mockBlock = mockBlockDb.getJSONObject(ix);
			final Block block = getBlock(mockBlock, true);
			for (int txIx = 0; txIx < block.getTransactionList().size(); txIx++) {
				final Transaction transaction = block.getTransactionList().get(txIx);
				for (final TransactionOutput output : transaction.outputs) {
					if (output.scriptHash.equals(account)) {
						if (!assetIdTxoMap.containsKey(output.assetId)) {
							assetIdTxoMap.put(output.assetId, new TreeMap<>());
						}
						final CoinReference cr = new CoinReference(transaction.getHash(), new UInt16(txIx));
						assetIdTxoMap.get(output.assetId).put(output, cr);
					}
				}
			}
		}
		return assetIdTxoMap;
	}

	@Override
	public final void put(final boolean forceSynch, final Block... blocks) {
		for (final Block block : blocks) {
			if (!containsBlockWithHash(block.hash)) {
				final JSONObject mockBlock = new JSONObject();
				mockBlock.put(HASH, block.hash.toHexString());
				mockBlock.put(INDEX, block.getIndexAsLong());
				mockBlock.put(BLOCK, ModelUtil.toHexString(block.toByteArray()));
				getMockBlockDb().put(mockBlock);
			}
		}
	}

	@Override
	public void validate() {
	}
}
