package neo.model.db.mapdb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONObject;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.Witness;
import neo.model.db.BlockDb;
import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.model.util.ModelUtil;
import neo.model.util.NetworkUtil;

/**
 * the block database.
 *
 * @author coranos
 *
 */
public final class BlockDbMapDbImpl implements BlockDb {

	/**
	 * the allocation increment size.
	 */
	private static final int ALLOCATION_INCREMENT_SIZE = 8 * 1024 * 1024;

	/**
	 * transaction scripts by transaction hash.
	 */
	private static final String TRANSACTION_SCRIPTS_BY_HASH = "TransactionScriptsByHash";

	/**
	 * transaction outputs by transaction hash.
	 */
	private static final String TRANSACTION_OUTPUTS_BY_HASH = "TransactionOutputsByHash";

	/**
	 * transaction inputs by transaction hash.
	 */
	private static final String TRANSACTION_INPUTS_BY_HASH = "TransactionInputsByHash";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockDbMapDbImpl.class);

	/**
	 * the block header primary index.
	 */
	private static final String BLOCK_HEADER_BY_INDEX = "blockHeaderByIndex";

	/**
	 * the block header primary index.
	 */
	private static final String BLOCK_INDEX_BY_HASH = "blockIndexByHash";

	/**
	 * transaction keys by block index.
	 */
	private static final String TRANSACTION_KEYS_BY_BLOCK_INDEX = "transactionKeysByBlockIndex";

	/**
	 * transaction key by transaction hash.
	 */
	private static final String TRANSACTION_KEY_BY_HASH = "transactionKeyByHash";

	/**
	 * transaction by keys.
	 */
	private static final String TRANSACTION_BY_KEY = "transactionByKey";

	/**
	 * the max block index.
	 */
	private static final String ASSET_AND_VALUE_BY_ACCOUNT = "assetAndValueByAccount";

	/**
	 * the max block index.
	 */
	private static final String MAX_BLOCK_INDEX = "maxBlockIndex";

	/**
	 * the database.
	 */
	private final DB db;

	/**
	 * the directory.
	 */
	private final File fileSizeDir;

	/**
	 * the closed flag.
	 */
	private boolean closed = false;

	/**
	 * the constructor.
	 *
	 * @param config
	 *            the configuration to use.
	 */
	public BlockDbMapDbImpl(final JSONObject config) {
		fileSizeDir = new File(config.getString(ConfigurationUtil.FILE_SIZE_DIR));
		final String url = config.getString(ConfigurationUtil.URL);
		final File dbFile = new File(url);
		dbFile.getParentFile().mkdirs();
		db = DBMaker.fileDB(dbFile).transactionEnable().closeOnJvmShutdown().fileMmapEnableIfSupported()
				.fileMmapPreclearDisable().allocateIncrement(ALLOCATION_INCREMENT_SIZE).make();
	}

	/**
	 * add data from a given map in the db to the list.
	 *
	 * @param mapName
	 *            the map name to use.
	 * @param keyBa
	 *            the key ba to use.
	 * @param list
	 *            the list to add to.
	 * @param factory
	 *            the factory to use.
	 * @param <T>
	 *            the type of object the factory makes.
	 */
	private <T> void addMapDataToList(final String mapName, final byte[] keyBa, final List<T> list,
			final AbstractByteBufferFactory<T> factory) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("addMapDataToList {} keyBa:{}", mapName, ModelUtil.toHexString(keyBa));
		}
		final HTreeMap<byte[], byte[]> map = db.hashMap(mapName, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		final byte[] listBa = map.get(keyBa);
		if (LOG.isTraceEnabled()) {
			LOG.trace("addMapDataToList {} listBa:{}", mapName, ModelUtil.toHexString(listBa));
		}

		final List<byte[]> baList = ModelUtil.toByteArrayList(listBa);
		for (final byte[] ba : baList) {
			final ByteBuffer bb = ByteBuffer.wrap(ba);
			final T t = factory.toObject(bb);
			list.add(t);
		}
	}

	/**
	 * close the database.
	 *
	 * @throws SQLException
	 *             if an error occurs.
	 */
	@Override
	public void close() {
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
		}
		LOG.debug("STARTED shutdown");
		db.close();
		LOG.debug("SUCCESS shutdown");
	}

	/**
	 * returns true if the hash is in the database. <br>
	 * checks both the "hash to block index" and "block index to header" map, in
	 * case the header was deleted but the hash wasn't.
	 *
	 * @param hash
	 *            the hash to use.
	 *
	 * @return true if the hash is in the database.
	 */
	@Override
	public boolean containsBlockWithHash(final UInt256 hash) {
		synchronized (this) {
			if (closed) {
				return false;
			}
		}
		final HTreeMap<byte[], Long> blockIndexByHashMap = getBlockIndexByHashMap();
		final HTreeMap<Long, byte[]> blockHeaderByIndexMap = getBlockHeaderByIndexMap();
		final byte[] hashBa = hash.toByteArray();
		if (blockIndexByHashMap.containsKey(hashBa)) {
			final long index = blockIndexByHashMap.get(hashBa);
			return blockHeaderByIndexMap.containsKey(index);
		} else {
			return false;
		}
	}

	/**
	 * used to get blocks unstuck, during debugging.
	 *
	 * @param blockHeight
	 *            the block height to remove.
	 */
	private void deleteBlockAtHeight(final long blockHeight) {
		final HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap();
		map.remove(blockHeight);
	}

	@Override
	public void deleteHighestBlock() {
		LOG.info("STARTED deleteHighestBlock");
		try {
			long blockHeight = getHeaderOfBlockWithMaxIndex().getIndexAsLong();
			Block block = getBlock(blockHeight, false);
			while (block == null) {
				LOG.error("INTERIM INFO deleteHighestBlock height:{} block is null, decrementing by 1 and retrying");
				blockHeight--;
				block = getBlock(blockHeight, false);
			}
			LOG.info("INTERIM INFO deleteHighestBlock height:{};hash:{};", blockHeight, block.hash);
			deleteBlockAtHeight(blockHeight);
			setBlockIndex(blockHeight - 1);
			db.commit();
		} catch (final Exception e) {
			LOG.error("FAILURE deleteHighestBlock", e);
			db.rollback();
		}
		LOG.info("SUCCESS deleteHighestBlock");
	}

	/**
	 * makes sure that an account exists with zeroed neo and gas assets.
	 *
	 * @param assetAndValueByAccountMap
	 *            the account asset value map.
	 * @param account
	 *            the account to use.account
	 * @return the account map.
	 */
	private Map<UInt256, Fixed8> ensureAccountExists(final HTreeMap<byte[], byte[]> assetAndValueByAccountMap,
			final UInt160 account) {
		final byte[] accountBa = account.toByteArray();
		if (!assetAndValueByAccountMap.containsKey(accountBa)) {
			final Map<UInt256, Fixed8> friendAssetValueMap = new TreeMap<>();
			friendAssetValueMap.put(ModelUtil.NEO_HASH_FORWARD, ModelUtil.getFixed8(BigInteger.ZERO));
			friendAssetValueMap.put(ModelUtil.GAS_HASH_FORWARD, ModelUtil.getFixed8(BigInteger.ZERO));
			putAssetValueMap(assetAndValueByAccountMap, accountBa, friendAssetValueMap);
		}
		return getAssetValueMapFromByteArray(assetAndValueByAccountMap.get(accountBa));
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		LOG.error("getAccountAssetValueMap STARTED");
		final Map<UInt160, Map<UInt256, Fixed8>> accountAssetValueMap = new TreeMap<>();

		final HTreeMap<byte[], byte[]> assetAndValueByAccountMap = getAssetAndValueByAccountMap();
		LOG.error("getAccountAssetValueMap INTERIM assetAndValueByAccountMap.size:{};",
				assetAndValueByAccountMap.size());

		for (final byte[] key : assetAndValueByAccountMap.getKeys()) {
			final byte[] value = assetAndValueByAccountMap.get(key);
			final UInt160 account = new UInt160(key);
			final Map<UInt256, Fixed8> map = getAssetValueMapFromByteArray(value);
			accountAssetValueMap.put(account, map);
		}

		LOG.error("getAccountAssetValueMap SUCCESS, count:{}", accountAssetValueMap.size());
		return accountAssetValueMap;
	}

	/**
	 * return the map of transactions by key.
	 *
	 * @return the map of transactions by key.
	 */
	private HTreeMap<byte[], byte[]> getAssetAndValueByAccountMap() {
		final HTreeMap<byte[], byte[]> map = db
				.hashMap(ASSET_AND_VALUE_BY_ACCOUNT, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).counterEnable()
				.createOrOpen();
		return map;
	}

	/**
	 * converts a byte array into a map of assets and values.
	 *
	 * @param ba
	 *            the byte array to use.
	 * @return the map.
	 */
	private Map<UInt256, Fixed8> getAssetValueMapFromByteArray(final byte[] ba) {
		final Map<UInt256, Fixed8> map = new TreeMap<>();
		final ByteBuffer bb = ByteBuffer.wrap(ba);
		final int size = ModelUtil.getBigInteger(bb).intValue();

		while (map.size() < size) {
			final UInt256 key = new UInt256(ModelUtil.getVariableLengthByteArray(bb));
			final Fixed8 value = new Fixed8(ByteBuffer.wrap(ModelUtil.getVariableLengthByteArray(bb)));
			map.put(key, value);
		}
		return map;
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
		synchronized (this) {
			if (closed) {
				return null;
			}
		}

		final HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap();
		if (!map.containsKey(blockHeight)) {
			return null;
		}

		final Block block = new Block(ByteBuffer.wrap(map.get(blockHeight)));
		if (withTransactions) {
			getTransactionsForBlock(block);
		}
		return block;
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
		synchronized (this) {
			if (closed) {
				return null;
			}
		}

		final HTreeMap<byte[], Long> map = getBlockIndexByHashMap();
		final byte[] hashBa = hash.toByteArray();
		if (!map.containsKey(hashBa)) {
			return null;
		}
		final long index = map.get(hashBa);
		return getBlock(index, withTransactions);

	}

	/**
	 * return the block count.
	 *
	 * @return the block count.
	 */
	@Override
	public long getBlockCount() {
		synchronized (this) {
			if (closed) {
				return 0;
			}
		}
		final HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap();
		return map.sizeLong();
	}

	/**
	 * return the map of block headers by block indexes.
	 *
	 * @return the map of block headers by block indexes.
	 */
	public HTreeMap<Long, byte[]> getBlockHeaderByIndexMap() {
		final HTreeMap<Long, byte[]> map = db.hashMap(BLOCK_HEADER_BY_INDEX, Serializer.LONG, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		return map;
	}

	/**
	 * return the map of block indexes by block hash.
	 *
	 * @return the map of block indexes by block hash.
	 */
	private HTreeMap<byte[], Long> getBlockIndexByHashMap() {
		return db.hashMap(BLOCK_INDEX_BY_HASH, Serializer.BYTE_ARRAY, Serializer.LONG).createOrOpen();
	}

	/**
	 * return the block with the maximum value in the index column.
	 *
	 * @param withTransactions
	 *            if true, add transactions. If false, only return the block header.
	 * @return the block with the maximum value in the index column.
	 */
	private Block getBlockWithMaxIndex(final boolean withTransactions) {
		synchronized (this) {
			if (closed) {
				return null;
			}
		}

		final long blockHeight = getMaxBlockIndex();
		return getBlock(blockHeight, withTransactions);
	}

	/**
	 * return the map of byte arrays by block index.
	 *
	 * @param mapName
	 *            the name of the map to get.
	 *
	 * @return the map of byte arrays keys by block index.
	 */
	private HTreeMap<Long, byte[]> getByteArrayByBlockIndexMap(final String mapName) {
		final HTreeMap<Long, byte[]> map = db.hashMap(mapName, Serializer.LONG, Serializer.BYTE_ARRAY).counterEnable()
				.createOrOpen();
		return map;
	}

	/**
	 * converts a map of assets and values into a byte array.
	 *
	 * @param friendAssetValueMap
	 *            the map to use.
	 * @return the byte array.
	 */
	private byte[] getByteArrayFromAssetValueMap(final Map<UInt256, Fixed8> friendAssetValueMap) {
		final byte[] mapBa;
		final ByteArrayOutputStream bout;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();) {
			NetworkUtil.writeVarInt(out, friendAssetValueMap.size());
			for (final UInt256 key : friendAssetValueMap.keySet()) {
				final Fixed8 value = friendAssetValueMap.get(key);
				NetworkUtil.writeByteArray(out, key.toByteArray());
				final byte[] valueBa = value.toByteArray();
				ArrayUtils.reverse(valueBa);
				NetworkUtil.writeByteArray(out, valueBa);
			}
			bout = out;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		mapBa = bout.toByteArray();
		return mapBa;
	}

	/**
	 * returns a list of byte arrays from the map.
	 *
	 * @param map
	 *            the map to use.
	 * @param key
	 *            the key to use.
	 * @param <K>
	 *            the key type.
	 * @return the list of byte arrays.
	 */
	private <K> List<byte[]> getByteArrayList(final HTreeMap<K, byte[]> map, final K key) {
		if (map.containsKey(key)) {
			final byte[] keyListBa = map.get(key);
			final List<byte[]> keyBaList = ModelUtil.toByteArrayList(keyListBa);
			return keyBaList;
		}
		return Collections.emptyList();
	}

	/**
	 * return the file size.
	 *
	 * @return the file size.
	 */
	@Override
	public long getFileSize() {
		return FileUtils.sizeOfDirectory(fileSizeDir);
	}

	@Override
	public Block getFullBlockFromHash(final UInt256 hash) {
		return getBlock(hash, true);
	}

	@Override
	public Block getFullBlockFromHeight(final long blockHeight) {
		return getBlock(blockHeight, true);
	}

	@Override
	public Block getHeaderOfBlockFromHash(final UInt256 hash) {
		return getBlock(hash, false);
	}

	@Override
	public Block getHeaderOfBlockFromHeight(final long blockHeight) {
		return getBlock(blockHeight, false);
	}

	/**
	 * return the block with the maximum value in the index column.
	 *
	 * @return the block with the maximum value in the index column.
	 */
	@Override
	public Block getHeaderOfBlockWithMaxIndex() {
		return getBlockWithMaxIndex(false);
	}

	/**
	 * return the max blockindex as an atomic long.
	 *
	 * @return the max blockindex as an atomic long.
	 */
	private long getMaxBlockIndex() {
		final long retval = db.atomicLong(MAX_BLOCK_INDEX, 0).createOrOpen().get();
		return retval;
	}

	/**
	 * gets the inputs for a transaction, and adds them to the transaction.
	 *
	 * @param transactionKey
	 *            the transaction key to use
	 * @param transaction
	 *            the transaction to use
	 */
	private void getTransactionInputs(final byte[] transactionKey, final Transaction transaction) {
		addMapDataToList(TRANSACTION_INPUTS_BY_HASH, transactionKey, transaction.inputs, new CoinReferenceFactory());
	}

	/**
	 * gets the transaction key for the given block index and transaction index.
	 *
	 * @param blockIndex
	 *            the block index.
	 * @param transactionIndex
	 *            the transaction index.
	 * @return the transaction key.
	 */
	private byte[] getTransactionKey(final long blockIndex, final int transactionIndex) {
		final ByteArrayOutputStream bout;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			NetworkUtil.writeLong(out, blockIndex);
			NetworkUtil.writeLong(out, transactionIndex);
			bout = out;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return bout.toByteArray();
	}

	/**
	 * return the map of transaction keys by transaction hash.
	 *
	 * @return the map of transaction keys by transaction hash.
	 */
	private HTreeMap<byte[], byte[]> getTransactionKeyByTransactionHashMap() {
		final HTreeMap<byte[], byte[]> map = db
				.hashMap(TRANSACTION_KEY_BY_HASH, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).counterEnable()
				.createOrOpen();
		return map;
	}

	/**
	 * gets the outputs for a transaction, and adds them to the transaction.
	 *
	 * @param transactionKey
	 *            the transaction key to use
	 * @param transaction
	 *            the transaction to use
	 */
	private void getTransactionOutputs(final byte[] transactionKey, final Transaction transaction) {
		addMapDataToList(TRANSACTION_OUTPUTS_BY_HASH, transactionKey, transaction.outputs,
				new TransactionOutputFactory());
	}

	/**
	 * return the map of transactions by key.
	 *
	 * @return the map of transactions by key.
	 */
	private HTreeMap<byte[], byte[]> getTransactionsByKeyMap() {
		final HTreeMap<byte[], byte[]> map = db
				.hashMap(TRANSACTION_BY_KEY, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).counterEnable()
				.createOrOpen();
		return map;
	}

	/**
	 * gets the inputs for a transaction, and adds them to the transaction.
	 *
	 * @param transactionKey
	 *            the transaction key to use
	 * @param transaction
	 *            the transaction to use
	 */
	private void getTransactionScripts(final byte[] transactionKey, final Transaction transaction) {
		addMapDataToList(TRANSACTION_SCRIPTS_BY_HASH, transactionKey, transaction.scripts, new WitnessFactory());
	}

	/**
	 * return the block, with transactions added.
	 *
	 * @param block
	 *            the block, to add transactions to.
	 */
	private void getTransactionsForBlock(final Block block) {
		final long blockIndex = block.getIndexAsLong();

		final HTreeMap<Long, byte[]> txKeyListMap = getByteArrayByBlockIndexMap(TRANSACTION_KEYS_BY_BLOCK_INDEX);
		final List<byte[]> txKeyBaList = getByteArrayList(txKeyListMap, blockIndex);

		final HTreeMap<byte[], byte[]> txMap = getTransactionsByKeyMap();
		for (final byte[] txKey : txKeyBaList) {
			if (LOG.isTraceEnabled()) {
				LOG.trace("getTransactionsForBlock {} txKey:{}", blockIndex, ModelUtil.toHexString(txKey));
			}
			final byte[] data = txMap.get(txKey);
			final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
			getTransactionOutputs(txKey, transaction);
			getTransactionInputs(txKey, transaction);
			getTransactionScripts(txKey, transaction);
			transaction.recalculateHash();
			block.getTransactionList().add(transaction);
		}
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		final HTreeMap<byte[], byte[]> map = getTransactionKeyByTransactionHashMap();
		final byte[] hashBa = hash.toByteArray();
		if (!map.containsKey(hashBa)) {
			return null;
		}
		final byte[] txKey = map.get(hashBa);
		final HTreeMap<byte[], byte[]> txMap = getTransactionsByKeyMap();
		final byte[] data = txMap.get(txKey);
		final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
		getTransactionOutputs(txKey, transaction);
		getTransactionInputs(txKey, transaction);
		getTransactionScripts(txKey, transaction);
		transaction.recalculateHash();
		return transaction;
	}

	/**
	 * puts the block into the database.
	 *
	 * @param blocks
	 *            the blocks to use.
	 */
	@Override
	public void put(final Block... blocks) {
		synchronized (this) {
			if (closed) {
				return;
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("STARTED put, {} blocks", NumberFormat.getIntegerInstance().format(blocks.length));
		}
		try {
			final HTreeMap<byte[], Long> blockIndexByHashMap = getBlockIndexByHashMap();
			final HTreeMap<Long, byte[]> blockHeaderByIndexMap = getBlockHeaderByIndexMap();

			for (final Block block : blocks) {
				synchronized (this) {
					if (closed) {
						db.rollback();
						return;
					}
				}
				final long blockIndex = block.getIndexAsLong();
				final long maxBlockIndex = getMaxBlockIndex();
				final boolean duplicateBlock;
				if ((blockIndex <= maxBlockIndex) && (blockIndex != 0) && (maxBlockIndex != 0)) {
					duplicateBlock = true;
				} else {
					duplicateBlock = false;
				}

				if (duplicateBlock) {
					LOG.error("duplicate block,blockIndex:{};maxBlockIndex:{};hash:{};", blockIndex, maxBlockIndex,
							block.hash);
				} else {
					updateMaxBlockIndex(blockIndex);

					final byte[] prevHashBa = block.prevHash.toByteArray();
					ArrayUtils.reverse(prevHashBa);

					blockIndexByHashMap.put(block.hash.toByteArray(), blockIndex);
					blockHeaderByIndexMap.put(blockIndex, block.toHeaderByteArray());

					int transactionIndex = 0;

					final Map<Long, List<byte[]>> txKeyByBlockIxMap = new TreeMap<>();
					final Map<ByteBuffer, byte[]> txByKeyMap = new TreeMap<>();
					final Map<ByteBuffer, byte[]> txKeyByTxHashMap = new TreeMap<>();

					final Map<ByteBuffer, List<byte[]>> txInputByTxKeyAndIndexMap = new TreeMap<>();
					final Map<ByteBuffer, List<byte[]>> txOutputByTxKeyAndIndexMap = new TreeMap<>();
					final Map<ByteBuffer, List<byte[]>> txScriptByTxKeyAndIndexMap = new TreeMap<>();

					txKeyByBlockIxMap.put(blockIndex, new ArrayList<>());

					final TransactionOutputFactory transactionOutputFactory = new TransactionOutputFactory();
					final CoinReferenceFactory coinReferenceFactory = new CoinReferenceFactory();
					final WitnessFactory witnessFactory = new WitnessFactory();

					for (final Transaction transaction : block.getTransactionList()) {
						final byte[] transactionBaseBa = transaction.toBaseByteArray();
						final byte[] transactionKeyBa = getTransactionKey(blockIndex, transactionIndex);

						putList(txKeyByBlockIxMap, blockIndex, transactionKeyBa);

						final ByteBuffer transactionKeyBb = ByteBuffer.wrap(transactionKeyBa);
						txByKeyMap.put(transactionKeyBb, transactionBaseBa);

						txKeyByTxHashMap.put(ByteBuffer.wrap(transaction.getHash().toByteArray()), transactionKeyBa);

						txInputByTxKeyAndIndexMap.put(transactionKeyBb, new ArrayList<>());
						txOutputByTxKeyAndIndexMap.put(transactionKeyBb, new ArrayList<>());
						txScriptByTxKeyAndIndexMap.put(transactionKeyBb, new ArrayList<>());

						for (int inputIx = 0; inputIx < transaction.inputs.size(); inputIx++) {
							final CoinReference input = transaction.inputs.get(inputIx);
							putList(txInputByTxKeyAndIndexMap, transactionKeyBb,
									coinReferenceFactory.fromObject(input).array());
						}

						for (int outputIx = 0; outputIx < transaction.outputs.size(); outputIx++) {
							final TransactionOutput output = transaction.outputs.get(outputIx);
							putList(txOutputByTxKeyAndIndexMap, transactionKeyBb,
									transactionOutputFactory.fromObject(output).array());
						}

						for (int scriptIx = 0; scriptIx < transaction.scripts.size(); scriptIx++) {
							final Witness script = transaction.scripts.get(scriptIx);
							putList(txScriptByTxKeyAndIndexMap, transactionKeyBb,
									witnessFactory.fromObject(script).array());
						}

						transactionIndex++;
					}

					putWithByteBufferKey(TRANSACTION_KEY_BY_HASH, txKeyByTxHashMap);
					putWithByteBufferKey(TRANSACTION_BY_KEY, txByKeyMap);

					putWithLongKey(TRANSACTION_KEYS_BY_BLOCK_INDEX, toByteBufferValue(txKeyByBlockIxMap));
					putWithByteBufferKey(TRANSACTION_INPUTS_BY_HASH, toByteBufferValue(txInputByTxKeyAndIndexMap));
					putWithByteBufferKey(TRANSACTION_OUTPUTS_BY_HASH, toByteBufferValue(txOutputByTxKeyAndIndexMap));
					putWithByteBufferKey(TRANSACTION_SCRIPTS_BY_HASH, toByteBufferValue(txScriptByTxKeyAndIndexMap));

					updateAssetAndValueByAccountMap(block);
				}
			}

			db.commit();
		} catch (final Exception e) {
			LOG.error("FAILURE put, {} blocks", NumberFormat.getIntegerInstance().format(blocks.length));
			LOG.error("FAILURE put", e);
			db.rollback();
			throw new RuntimeException(e);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("SUCCESS put, {} blocks", NumberFormat.getIntegerInstance().format(blocks.length));
		}
	}

	/**
	 * puts the asset value map into the account map.
	 *
	 * @param assetAndValueByAccountMap
	 *            the account map to use.
	 * @param accountBa
	 *            the account key byte array.
	 * @param friendAssetValueMap
	 *            the asset value map to use.
	 */
	private void putAssetValueMap(final HTreeMap<byte[], byte[]> assetAndValueByAccountMap, final byte[] accountBa,
			final Map<UInt256, Fixed8> friendAssetValueMap) {
		final byte[] mapBa = getByteArrayFromAssetValueMap(friendAssetValueMap);
		assetAndValueByAccountMap.put(accountBa, mapBa);
	}

	/**
	 * adds the value to the map, using the key.
	 *
	 * @param map
	 *            the map to use.
	 * @param key
	 *            the key.
	 * @param value
	 *            the value.
	 * @param <K>
	 *            the type of key.
	 */
	private <K> void putList(final Map<K, List<byte[]>> map, final K key, final byte[] value) {
		map.get(key).add(value);
	}

	/**
	 * put the data in the map into the named database map.
	 *
	 * @param destMapName
	 *            the destination map name to use.
	 * @param sourceMap
	 *            the source map to use.
	 */
	private void putWithByteBufferKey(final String destMapName, final Map<ByteBuffer, byte[]> sourceMap) {
		final HTreeMap<byte[], byte[]> map = db.hashMap(destMapName, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		for (final ByteBuffer key : sourceMap.keySet()) {
			final byte[] ba = sourceMap.get(key);

			if (LOG.isTraceEnabled()) {
				LOG.trace("putWithByteBufferKey {} {} {}", destMapName, ModelUtil.toHexString(key.array()),
						ModelUtil.toHexString(ba));
			}

			map.put(key.array(), ba);
		}
	}

	/**
	 * put the data in the map into the named database map.
	 *
	 * @param destMapName
	 *            the destination map name to use.
	 * @param sourceMap
	 *            the source map to use.
	 */
	private void putWithLongKey(final String destMapName, final Map<Long, byte[]> sourceMap) {
		final HTreeMap<Long, byte[]> map = db.hashMap(destMapName, Serializer.LONG, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		for (final Long key : sourceMap.keySet()) {
			final byte[] ba = sourceMap.get(key);
			map.put(key, ba);
		}
	}

	/**
	 * sets the blockindex to be the given block index.
	 *
	 * @param blockIndex
	 *            the block index to use.
	 */
	private void setBlockIndex(final long blockIndex) {
		db.atomicLong(MAX_BLOCK_INDEX, blockIndex).createOrOpen().set(blockIndex);
	}

	/**
	 * serializes a list of byte array values into a single byte array.
	 *
	 * @param sourceMap
	 *            the map to use.
	 * @param <K>
	 *            the type of key.
	 * @return the map with byte array values.
	 */
	private <K> Map<K, byte[]> toByteBufferValue(final Map<K, List<byte[]>> sourceMap) {
		final Map<K, byte[]> destMap = new TreeMap<>();
		for (final K key : sourceMap.keySet()) {
			final List<byte[]> baList = sourceMap.get(key);
			destMap.put(key, ModelUtil.toByteArray(baList));
		}
		return destMap;
	}

	/**
	 * updates the asset and value by account map.
	 *
	 * @param block
	 *            the block to update.
	 */
	private void updateAssetAndValueByAccountMap(final Block block) {
		final HTreeMap<byte[], byte[]> assetAndValueByAccountMap = getAssetAndValueByAccountMap();
		LOG.debug("updateAssetAndValueByAccountMap STARTED block;{};numberOfAccounts:{}", block.getIndexAsLong(),
				assetAndValueByAccountMap.size());

		for (final Transaction t : block.getTransactionList()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("updateAssetAndValueByAccountMap INTERIM tx:{}", t.getHash());
			}
			for (final CoinReference cr : t.inputs) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateAssetAndValueByAccountMap INTERIM cr:{}", cr.toJSONObject());
				}
				final UInt256 prevHashReversed = cr.prevHash.reverse();
				final Transaction tiTx = getTransactionWithHash(prevHashReversed);

				if (tiTx == null) {
					throw new RuntimeException("no transaction with prevHash:" + prevHashReversed + " in block[1] "
							+ block.hash + " index[1] " + block.getIndexAsLong());
				}

				final int prevIndex = cr.prevIndex.asInt();
				if (prevIndex >= tiTx.outputs.size()) {
					throw new RuntimeException("prevIndex:" + prevIndex + " exceeds output size:" + tiTx.outputs.size()
							+ "; in block[2] " + block.hash + " index[2] " + block.getIndexAsLong());
				}
				final TransactionOutput ti = tiTx.outputs.get(prevIndex);
				final UInt160 input = ti.scriptHash;
				if ((ti.assetId.equals(ModelUtil.NEO_HASH_FORWARD))
						|| (ti.assetId.equals(ModelUtil.GAS_HASH_FORWARD))) {
					final Map<UInt256, Fixed8> accountAssetValueMap = ensureAccountExists(assetAndValueByAccountMap,
							input);
					if (LOG.isDebugEnabled()) {
						LOG.debug("TI beforeMap {}", accountAssetValueMap);
					}
					final Fixed8 oldValue = accountAssetValueMap.get(ti.assetId);
					final Fixed8 newValue = ModelUtil.subtract(oldValue, ti.value);
					if (LOG.isDebugEnabled()) {
						LOG.debug("updateAssetAndValueByAccountMap INTERIM input;{};", ModelUtil.toAddress(input));
						LOG.debug("updateAssetAndValueByAccountMap INTERIM ti.assetId:{} oldValue:{};", ti.assetId,
								oldValue);
						LOG.debug("updateAssetAndValueByAccountMap INTERIM ti.assetId:{} to.value:{};", ti.assetId,
								ti.value);
						LOG.debug("updateAssetAndValueByAccountMap INTERIM ti.assetId:{} newValue:{};", ti.assetId,
								newValue);
					}
					accountAssetValueMap.put(ti.assetId, newValue);
					putAssetValueMap(assetAndValueByAccountMap, input.toByteArray(), accountAssetValueMap);
					if (LOG.isDebugEnabled()) {
						LOG.debug("TI afterMap {}", ensureAccountExists(assetAndValueByAccountMap, input));
					}
				} else {
					LOG.error("updateAssetAndValueByAccountMap INTERIM NON NEO ti.assetId:{}", ti.assetId);
				}
			}

			for (final TransactionOutput to : t.outputs) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("updateAssetAndValueByAccountMap INTERIM to:{}", to.toJSONObject());
				}
				final UInt160 output = to.scriptHash;
				if ((to.assetId.equals(ModelUtil.NEO_HASH_FORWARD))
						|| (to.assetId.equals(ModelUtil.GAS_HASH_FORWARD))) {
					try {
						final Map<UInt256, Fixed8> accountAssetValueMap = ensureAccountExists(assetAndValueByAccountMap,
								output);
						if (LOG.isDebugEnabled()) {
							LOG.debug("TO beforeMap {}", accountAssetValueMap);
						}
						final Fixed8 oldValue = accountAssetValueMap.get(to.assetId);
						if (LOG.isDebugEnabled()) {
							LOG.debug("updateAssetAndValueByAccountMap INTERIM output;{};",
									ModelUtil.toAddress(output));
							LOG.debug("updateAssetAndValueByAccountMap INTERIM to.assetId:{} oldValue:{};", to.assetId,
									oldValue);
							LOG.debug("updateAssetAndValueByAccountMap INTERIM to.assetId:{} to.value:{};", to.assetId,
									to.value);
						}
						final Fixed8 newValue = ModelUtil.add(oldValue, to.value);
						accountAssetValueMap.put(to.assetId, newValue);
						putAssetValueMap(assetAndValueByAccountMap, output.toByteArray(), accountAssetValueMap);
						if (LOG.isDebugEnabled()) {
							LOG.debug("updateAssetAndValueByAccountMap INTERIM to.assetId:{} newValue:{};", to.assetId,
									newValue);
							LOG.debug("TO afterMap {}", ensureAccountExists(assetAndValueByAccountMap, output));
						}
					} catch (final RuntimeException e) {
						final String msg = "error processing transaction type " + t.type + " hash " + t.getHash();
						throw new RuntimeException(msg, e);
					}
				} else {
					LOG.error("updateAssetAndValueByAccountMap INTERIM NON NEO to.assetId:{}", to.assetId);
				}
			}
		}
		LOG.debug("updateAssetAndValueByAccountMap SUCCESS block;{};numberOfAccounts:{}", block.getIndexAsLong(),
				assetAndValueByAccountMap.size());
	}

	/**
	 * updates the block index, if the new block index is greater than the existing
	 * block index.
	 *
	 * @param blockIndex
	 *            the new block index.
	 */
	private void updateMaxBlockIndex(final long blockIndex) {
		if (blockIndex > getMaxBlockIndex()) {
			setBlockIndex(blockIndex);
		}
	}

	@Override
	public void validate() {
		LOG.info("STARTED validate");
		try {
			final Block block0 = getBlock(0, false);
			if (!block0.hash.equals(GenesisBlockUtil.GENESIS_HASH)) {
				throw new RuntimeException(
						"height 0 block hash \"" + block0.hash.toHexString() + "\" does not match genesis block hash \""
								+ GenesisBlockUtil.GENESIS_HASH.toHexString() + "\".");
			}

			long lastInfoMs = System.currentTimeMillis();

			long blockHeight = 0;
			long lastGoodBlockIndex = -1;
			final long maxBlockCount = getBlockCount();

			boolean blockHeightNoLongerValid = false;

			final String maxBlockCountStr;
			if (LOG.isDebugEnabled() || LOG.isErrorEnabled()) {
				maxBlockCountStr = NumberFormat.getIntegerInstance().format(maxBlockCount);
			} else {
				maxBlockCountStr = null;
			}

			LOG.info("INTERIM validate, clear account list STARTED");
			final HTreeMap<byte[], byte[]> assetAndValueByAccountMap = getAssetAndValueByAccountMap();
			assetAndValueByAccountMap.clear();
			LOG.info("INTERIM validate, clear account list SUCCESS");

			while (blockHeight < maxBlockCount) {
				final String blockHeightStr;
				if (LOG.isDebugEnabled() || LOG.isErrorEnabled()) {
					blockHeightStr = NumberFormat.getIntegerInstance().format(blockHeight);
				} else {
					blockHeightStr = null;
				}

				LOG.debug("INTERIM DEBUG validate {} of {} STARTED ", blockHeightStr, maxBlockCountStr);
				final Block block = getBlock(blockHeight, true);
				if (block == null) {
					LOG.error("INTERIM validate {} of {} FAILURE, block not found in blockchain.", blockHeightStr,
							maxBlockCountStr);
					blockHeightNoLongerValid = true;
				} else if ((blockHeight != 0) && (!containsBlockWithHash(block.prevHash))) {
					LOG.error("INTERIM validate {} of {} FAILURE, prevHash {} not found in blockchain.", blockHeightStr,
							maxBlockCountStr, block.prevHash.toHexString());
					deleteBlockAtHeight(blockHeight);
					blockHeightNoLongerValid = true;
				} else if (block.getIndexAsLong() != blockHeight) {
					LOG.error("INTERIM validate {} of {} FAILURE, indexAsLong {} does not match blockchain.",
							blockHeightStr, maxBlockCountStr, block.getIndexAsLong());
					deleteBlockAtHeight(blockHeight);
					blockHeightNoLongerValid = true;
				} else if (blockHeightNoLongerValid) {
					LOG.error("INTERIM validate {} of {} FAILURE, block height tainted.", blockHeightStr,
							maxBlockCountStr, block.getIndexAsLong());
					deleteBlockAtHeight(blockHeight);
				} else {
					if (System.currentTimeMillis() > (lastInfoMs + 1000)) {
						final String numberOfAccountsStr = NumberFormat.getIntegerInstance()
								.format(assetAndValueByAccountMap.size());
						LOG.info("INTERIM INFO  validate {} of {} SUCCESS, number of accounts:{};", blockHeightStr,
								maxBlockCountStr, numberOfAccountsStr);
						lastInfoMs = System.currentTimeMillis();
					} else {
						LOG.debug("INTERIM DEBUG validate {} of {} SUCCESS.", blockHeightStr, maxBlockCountStr);
					}

					final long blockIndex = block.getIndexAsLong();
					int transactionIndex = 0;
					final Map<ByteBuffer, byte[]> txKeyByTxHashMap = new TreeMap<>();
					for (final Transaction transaction : block.getTransactionList()) {
						final byte[] transactionKeyBa = getTransactionKey(blockIndex, transactionIndex);
						txKeyByTxHashMap.put(ByteBuffer.wrap(transaction.getHash().toByteArray()), transactionKeyBa);
						transactionIndex++;
					}
					putWithByteBufferKey(TRANSACTION_KEY_BY_HASH, txKeyByTxHashMap);

					updateAssetAndValueByAccountMap(block);

					lastGoodBlockIndex = block.getIndexAsLong();
				}
				blockHeight++;
			}
			setBlockIndex(lastGoodBlockIndex);

			db.commit();

			LOG.info("SUCCESS validate");
		} catch (final Exception e) {
			LOG.error("FAILURE validate", e);
			db.rollback();
		}
	}

}
