package neo.model.db.mapdb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt16;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.Witness;
import neo.model.db.BlockDb;
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
	 * transaction scripts by index.
	 */
	private static final String TRANSACTION_SCRIPTS_BY_INDEX = "TransactionScriptsByIndex";

	/**
	 * transaction outputs by index.
	 */
	private static final String TRANSACTION_OUTPUTS_BY_INDEX = "TransactionOutputsByIndex";

	/**
	 * transaction inputs by index.
	 */
	private static final String TRANSACTION_INPUTS_BY_INDEX = "TransactionInputsByIndex";

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
	private static final String MAX_BLOCK_INDEX = "maxBlockIndex";

	/**
	 * the database.
	 */
	private final DB db;

	/**
	 * the closed flag.
	 */
	private boolean closed = false;

	/**
	 * the directory.
	 */
	private final File dir = new File("java-chain/db");

	/**
	 * the constructor.
	 */
	public BlockDbMapDbImpl() {
		db = DBMaker.fileDB(dir).make();
	}

	/**
	 * add the values to the map using the given key.
	 *
	 * @param map
	 *            the map to use.
	 * @param key
	 *            the key to use.
	 * @param values
	 *            the values to add to the list.
	 */
	private void add(final Map<Long, List<byte[]>> map, final long key, final byte[]... values) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<>());
		}
		final List<byte[]> list = map.get(key);
		for (final byte[] value : values) {
			list.add(value);
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
	 * returns true if the hash is in the database.
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

		try (HTreeMap<byte[], Long> map = getBlockIndexByHashMap();) {
			return map.containsKey(hash.toByteArray());
		}
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		final Map<UInt160, Map<UInt256, Fixed8>> accountAssetValueMap = new TreeMap<>();

		final TransactionOutputFactory objectFactory = new TransactionOutputFactory();

		try (HTreeMap<Long, byte[]> map = getByteArrayByBlockIndexMap(TRANSACTION_OUTPUTS_BY_INDEX);) {
			for (final long key : map.getKeys()) {
				final byte[] listBa = map.get(key);
				final List<byte[]> baList = toByteArrayList(listBa);
				for (final byte[] ba : baList) {
					final TransactionOutput output = objectFactory.toObject(ByteBuffer.wrap(ba));

					if (!accountAssetValueMap.containsKey(output.scriptHash)) {
						accountAssetValueMap.put(output.scriptHash, new TreeMap<>());
					}
					final Map<UInt256, Fixed8> assetValueMap = accountAssetValueMap.get(output.scriptHash);
					assetValueMap.put(output.assetId, output.value);
				}
			}
		}

		return accountAssetValueMap;
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

		try (HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap();) {
			if (!map.containsKey(blockHeight)) {
				return null;
			}

			final Block block = new Block(ByteBuffer.wrap(map.get(blockHeight)));
			if (withTransactions) {
				getTransactionsForBlock(block);
			}
			return block;
		}
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

		try (HTreeMap<byte[], Long> map = getBlockIndexByHashMap();) {
			final byte[] hashBa = hash.toByteArray();
			if (!map.containsKey(hashBa)) {
				return null;
			}
			final long index = map.get(hashBa);
			return getBlock(index, withTransactions);
		}
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
		try (HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap();) {
			return map.sizeLong();
		}
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

		final Atomic.Long blockHeight = getMaxBlockIndex();
		return getBlock(blockHeight.get(), withTransactions);
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
			final List<byte[]> keyBaList = toByteArrayList(keyListBa);
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
		return FileUtils.sizeOfDirectory(dir);
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
	 * return a map of the objects, divided into their transactions indexes.
	 *
	 * @param mapName
	 *            the map name to use.
	 * @param blockIndex
	 *            the block index byte array to use.
	 * @param mapToObject
	 *            the mapToObject to use.
	 * @param <T>
	 *            the object type to use.
	 * @return a map of the objects, divided into their transactions indexes.
	 */
	private <T> Map<Integer, List<T>> getMapList(final String mapName, final long blockIndex,
			final AbstractByteBufferFactory<T> mapToObject) {
		final List<byte[]> baList;
		try (HTreeMap<Long, byte[]> map = getByteArrayByBlockIndexMap(mapName);) {
			baList = getByteArrayList(map, blockIndex);
		}

		final Map<Integer, List<T>> tMapList = new TreeMap<>();
		for (final byte[] ba : baList) {
			final ByteBuffer bb = ByteBuffer.wrap(ba);
			final int transactionIndex = bb.getInt();
			final T t = mapToObject.toObject(bb);
			if (!tMapList.containsKey(transactionIndex)) {
				tMapList.put(transactionIndex, new ArrayList<>());
			}
			tMapList.get(transactionIndex).add(t);
		}

		return tMapList;
	}

	/**
	 * return the max blockindex as an atomic long.
	 *
	 * @return the max blockindex as an atomic long.
	 */
	private Atomic.Long getMaxBlockIndex() {
		return db.atomicLong(MAX_BLOCK_INDEX).createOrOpen();
	}

	/**
	 * gets the inputs for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use.
	 */
	private void getTransactionInputsWithIndex(final Block block) {
		final Map<Integer, List<CoinReference>> inputsMap = getMapList(TRANSACTION_INPUTS_BY_INDEX,
				block.getIndexAsLong(), new CoinReferenceFactory());
		for (final int txIx : inputsMap.keySet()) {
			final List<CoinReference> inputs = inputsMap.get(txIx);

			if (txIx >= block.getTransactionList().size()) {
				throw new RuntimeException(
						"txIx \"" + txIx + "\" exceeds txList.size \"" + block.getTransactionList().size()
								+ "\" for block index \"" + block.getIndexAsLong() + "\" hash \"" + block.hash + "\"");
			} else {
				block.getTransactionList().get(txIx).inputs.addAll(inputs);
			}
		}
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
	 * gets the outputs for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use
	 */
	private void getTransactionOutputsWithIndex(final Block block) {
		final Map<Integer, List<TransactionOutput>> outputsMap = getMapList(TRANSACTION_OUTPUTS_BY_INDEX,
				block.getIndexAsLong(), new TransactionOutputFactory());
		for (final int txIx : outputsMap.keySet()) {
			final List<TransactionOutput> outputs = outputsMap.get(txIx);
			block.getTransactionList().get(txIx).outputs.addAll(outputs);
		}
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
	 * gets the scripts for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use
	 */
	private void getTransactionScriptsWithIndex(final Block block) {
		final Map<Integer, List<Witness>> scriptsMap = getMapList(TRANSACTION_SCRIPTS_BY_INDEX, block.getIndexAsLong(),
				new WitnessFactory());
		for (final int txIx : scriptsMap.keySet()) {
			final List<Witness> scripts = scriptsMap.get(txIx);
			block.getTransactionList().get(txIx).scripts.addAll(scripts);
		}
	}

	/**
	 * return the block, with transactions added.
	 *
	 * @param block
	 *            the block, to add transactions to.
	 */
	private void getTransactionsForBlock(final Block block) {
		final long blockIndex = block.getIndexAsLong();
		final List<byte[]> txKeyBaList;
		try (HTreeMap<Long, byte[]> map = getByteArrayByBlockIndexMap(TRANSACTION_KEYS_BY_BLOCK_INDEX);) {
			txKeyBaList = getByteArrayList(map, blockIndex);
		}

		try (HTreeMap<byte[], byte[]> map = getTransactionsByKeyMap();) {
			for (final byte[] txKey : txKeyBaList) {
				final byte[] data = map.get(txKey);
				final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
				block.getTransactionList().add(transaction);
			}
		}

		getTransactionOutputsWithIndex(block);

		getTransactionInputsWithIndex(block);

		getTransactionScriptsWithIndex(block);
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		try (HTreeMap<byte[], byte[]> map = getTransactionKeyByTransactionHashMap()) {
			final byte[] hashBa = hash.toByteArray();
			if (!map.containsKey(hashBa)) {
				return null;
			}
			return new Transaction(ByteBuffer.wrap(map.get(hashBa)));
		}
	}

	/**
	 * puts the block into the database.
	 *
	 * @param block
	 *            the block to use.
	 */
	@Override
	public void put(final Block block) {
		synchronized (this) {
			if (closed) {
				return;
			}
		}
		final byte[] prevHashBa = block.prevHash.toByteArray();
		ArrayUtils.reverse(prevHashBa);

		final long blockIndex = block.getIndexAsLong();
		try (HTreeMap<byte[], Long> map = getBlockIndexByHashMap()) {
			map.put(block.hash.toByteArray(), blockIndex);
		}
		try (HTreeMap<Long, byte[]> map = getBlockHeaderByIndexMap()) {
			map.put(blockIndex, block.toHeaderByteArray());
		}

		int transactionIndex = 0;

		final Map<Long, List<byte[]>> putTransactionMap = new TreeMap<>();
		final Map<Long, List<byte[]>> putTransactionInputMap = new TreeMap<>();
		final Map<Long, List<byte[]>> putTransactionOutputMap = new TreeMap<>();
		final Map<Long, List<byte[]>> putTransactionScriptMap = new TreeMap<>();

		for (final Transaction transaction : block.getTransactionList()) {
			final byte[] transactionBaseBa = transaction.toBaseByteArray();

			add(putTransactionMap, transactionIndex, transaction.hash.toByteArray(), transactionBaseBa);

			for (int inputIx = 0; inputIx < transaction.inputs.size(); inputIx++) {
				final byte[] txInputIxByte = new UInt32(inputIx).toByteArray();
				final CoinReference input = transaction.inputs.get(inputIx);
				add(putTransactionInputMap, transactionIndex, txInputIxByte, input.prevHash.toByteArray(),
						input.prevIndex.toByteArray());
			}

			for (int outputIx = 0; outputIx < transaction.outputs.size(); outputIx++) {
				final byte[] txOutputIxByte = new UInt16(outputIx).toByteArray();
				final TransactionOutput output = transaction.outputs.get(outputIx);
				add(putTransactionOutputMap, transactionIndex, txOutputIxByte, output.assetId.toByteArray(),
						output.value.toByteArray(), output.scriptHash.toByteArray());
			}

			for (int scriptIx = 0; scriptIx < transaction.scripts.size(); scriptIx++) {
				final byte[] txScriptIxByte = new UInt32(scriptIx).toByteArray();
				final Witness script = transaction.scripts.get(scriptIx);
				add(putTransactionScriptMap, transactionIndex, txScriptIxByte, script.getCopyOfInvocationScript(),
						script.getCopyOfVerificationScript());
			}

			transactionIndex++;
		}

		put(TRANSACTION_KEYS_BY_BLOCK_INDEX, putTransactionMap);
		put(TRANSACTION_INPUTS_BY_INDEX, putTransactionInputMap);
		put(TRANSACTION_OUTPUTS_BY_INDEX, putTransactionOutputMap);
		put(TRANSACTION_SCRIPTS_BY_INDEX, putTransactionScriptMap);

	}

	/**
	 * put the data in the map into the named database map.
	 *
	 * @param destMapName
	 *            the destination map name to use.
	 * @param sourceMap
	 *            the source map to use.
	 */
	private void put(final String destMapName, final Map<Long, List<byte[]>> sourceMap) {
		final HTreeMap<Long, byte[]> map = db.hashMap(destMapName, Serializer.LONG, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		for (final Long key : sourceMap.keySet()) {
			final List<byte[]> baList = sourceMap.get(key);
			final byte[] ba = toByteArray(baList);
			map.put(key, ba);
		}
	}

	/**
	 * converts a list of byte arrays into a byte array.
	 *
	 * @param baList
	 *            the byte array list to use.
	 * @return the byte array.
	 */
	private byte[] toByteArray(final List<byte[]> baList) {
		final ByteArrayOutputStream bout;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			NetworkUtil.writeLong(out, baList.size());
			for (final byte[] ba : baList) {
				NetworkUtil.writeByteArray(out, ba);
			}
			bout = out;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
		return bout.toByteArray();
	}

	/**
	 * converts a byte array into a list of byte arrays.
	 *
	 * @param ba
	 *            the byte array to use.
	 * @return the byte array.
	 */
	private List<byte[]> toByteArrayList(final byte[] ba) {
		final List<byte[]> baList = new ArrayList<>();
		final ByteBuffer listBb = ByteBuffer.wrap(ba);
		final long size = listBb.getLong();
		for (long ix = 0; ix < size; ix++) {
			final byte[] keyBa = ModelUtil.getByteArray(listBb);
			baList.add(keyBa);
		}
		return baList;
	}

}
