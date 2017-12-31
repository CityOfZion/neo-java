package neo.model.db.mapdb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
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
	private static final DB DB;

	/**
	 * the directory.
	 */
	private static final File DIR = new File("java-chain/db");

	/**
	 * the file.
	 */
	private static final File FILE = new File(DIR, "db.mapdb");

	static {
		DIR.mkdirs();
		DB = DBMaker.fileDB(FILE).transactionEnable().make();

	}

	/**
	 * the closed flag.
	 */
	private boolean closed = false;

	/**
	 * the constructor.
	 */
	public BlockDbMapDbImpl() {
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
		DB.close();
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
		final HTreeMap<byte[], Long> map = getBlockIndexByHashMap();
		return map.containsKey(hash.toByteArray());
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		final Map<UInt160, Map<UInt256, Fixed8>> accountAssetValueMap = new TreeMap<>();

		final TransactionOutputFactory objectFactory = new TransactionOutputFactory();

		final HTreeMap<Long, byte[]> map = getByteArrayByBlockIndexMap(TRANSACTION_OUTPUTS_BY_INDEX);
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
		final HTreeMap<Long, byte[]> map = DB.hashMap(BLOCK_HEADER_BY_INDEX, Serializer.LONG, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		return map;
	}

	/**
	 * return the map of block indexes by block hash.
	 *
	 * @return the map of block indexes by block hash.
	 */
	private HTreeMap<byte[], Long> getBlockIndexByHashMap() {
		return DB.hashMap(BLOCK_INDEX_BY_HASH, Serializer.BYTE_ARRAY, Serializer.LONG).createOrOpen();
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
		final HTreeMap<Long, byte[]> map = DB.hashMap(mapName, Serializer.LONG, Serializer.BYTE_ARRAY).counterEnable()
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
		return FileUtils.sizeOfDirectory(DIR);
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
		final HTreeMap<Long, byte[]> map = getByteArrayByBlockIndexMap(mapName);
		final List<byte[]> baList = getByteArrayList(map, blockIndex);
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
	private long getMaxBlockIndex() {
		final long retval = DB.atomicLong(MAX_BLOCK_INDEX, 0).createOrOpen().get();
		return retval;
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
		final HTreeMap<byte[], byte[]> map = DB
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
		final HTreeMap<byte[], byte[]> map = DB
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

		final HTreeMap<Long, byte[]> txKeyListMap = getByteArrayByBlockIndexMap(TRANSACTION_KEYS_BY_BLOCK_INDEX);
		final List<byte[]> txKeyBaList = getByteArrayList(txKeyListMap, blockIndex);

		final HTreeMap<byte[], byte[]> txMap = getTransactionsByKeyMap();
		for (final byte[] txKey : txKeyBaList) {
			final byte[] data = txMap.get(txKey);
			final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
			block.getTransactionList().add(transaction);
		}

		getTransactionOutputsWithIndex(block);

		getTransactionInputsWithIndex(block);

		getTransactionScriptsWithIndex(block);
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		final HTreeMap<byte[], byte[]> map = getTransactionKeyByTransactionHashMap();
		final byte[] hashBa = hash.toByteArray();
		if (!map.containsKey(hashBa)) {
			return null;
		}
		return new Transaction(ByteBuffer.wrap(map.get(hashBa)));
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

		final long blockIndex = block.getIndexAsLong();
		DB.atomicLong(MAX_BLOCK_INDEX, blockIndex).createOrOpen().set(blockIndex);

		final byte[] prevHashBa = block.prevHash.toByteArray();
		ArrayUtils.reverse(prevHashBa);

		final HTreeMap<byte[], Long> blockIndexByHashMap = getBlockIndexByHashMap();
		blockIndexByHashMap.put(block.hash.toByteArray(), blockIndex);
		final HTreeMap<Long, byte[]> blockHeaderByIndexMap = getBlockHeaderByIndexMap();
		blockHeaderByIndexMap.put(blockIndex, block.toHeaderByteArray());

		int transactionIndex = 0;

		final Map<Long, List<byte[]>> txKeyByBlockIxMap = new TreeMap<>();
		final Map<ByteBuffer, byte[]> txByKeyMap = new TreeMap<>();
		final Map<ByteBuffer, byte[]> txKeyByTxHashMap = new TreeMap<>();

		final Map<ByteBuffer, byte[]> txInputByTxKeyAndIndexMap = new TreeMap<>();
		final Map<ByteBuffer, byte[]> txOutputByTxKeyAndIndexMap = new TreeMap<>();
		final Map<ByteBuffer, byte[]> txScriptByTxKeyAndIndexMap = new TreeMap<>();

		for (final Transaction transaction : block.getTransactionList()) {
			final byte[] transactionBaseBa = transaction.toBaseByteArray();
			final byte[] transactionKeyBa = getTransactionKey(blockIndex, transactionIndex);

			putList(txKeyByBlockIxMap, blockIndex, transactionKeyBa);

			final ByteBuffer transactionKeyBb = ByteBuffer.wrap(transactionKeyBa);
			txByKeyMap.put(transactionKeyBb, transactionBaseBa);

			txKeyByTxHashMap.put(ByteBuffer.wrap(transaction.hash.toByteArray()), transactionKeyBa);

			for (int inputIx = 0; inputIx < transaction.inputs.size(); inputIx++) {
				final byte[] txInputIxByte = new UInt32(inputIx).toByteArray();
				final CoinReference input = transaction.inputs.get(inputIx);

				txInputByTxKeyAndIndexMap.put(transactionKeyBb,
						toByteArray(txInputIxByte, input.prevHash.toByteArray(), input.prevIndex.toByteArray()));

			}

			for (int outputIx = 0; outputIx < transaction.outputs.size(); outputIx++) {
				final byte[] txOutputIxByte = new UInt16(outputIx).toByteArray();
				final TransactionOutput output = transaction.outputs.get(outputIx);

				txInputByTxKeyAndIndexMap.put(transactionKeyBb, toByteArray(txOutputIxByte,
						output.assetId.toByteArray(), output.value.toByteArray(), output.scriptHash.toByteArray()));
			}

			for (int scriptIx = 0; scriptIx < transaction.scripts.size(); scriptIx++) {
				final byte[] txScriptIxByte = new UInt32(scriptIx).toByteArray();
				final Witness script = transaction.scripts.get(scriptIx);

				txInputByTxKeyAndIndexMap.put(transactionKeyBb, toByteArray(txScriptIxByte,
						script.getCopyOfInvocationScript(), script.getCopyOfVerificationScript()));
			}

			transactionIndex++;
		}

		putWithByteBufferKey(TRANSACTION_KEY_BY_HASH, txKeyByTxHashMap);
		putWithByteBufferKey(TRANSACTION_BY_KEY, txByKeyMap);

		final Map<Long, byte[]> txKeyBaByBlockIxMap = new TreeMap<>();

		for (final long blockIx : txKeyByBlockIxMap.keySet()) {
			final List<byte[]> txKeyList = txKeyByBlockIxMap.get(blockIx);
			txKeyBaByBlockIxMap.put(blockIx, toByteArray(txKeyList));
		}

		putWithLongKey(TRANSACTION_KEYS_BY_BLOCK_INDEX, txKeyBaByBlockIxMap);
		putWithByteBufferKey(TRANSACTION_INPUTS_BY_INDEX, txInputByTxKeyAndIndexMap);
		putWithByteBufferKey(TRANSACTION_OUTPUTS_BY_INDEX, txOutputByTxKeyAndIndexMap);
		putWithByteBufferKey(TRANSACTION_SCRIPTS_BY_INDEX, txScriptByTxKeyAndIndexMap);

	}

	/**
	 * adds teh value to the map, using the key.
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
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<>());
		}
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
		final HTreeMap<byte[], byte[]> map = DB.hashMap(destMapName, Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		for (final ByteBuffer key : sourceMap.keySet()) {
			final byte[] ba = sourceMap.get(key);
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
		final HTreeMap<Long, byte[]> map = DB.hashMap(destMapName, Serializer.LONG, Serializer.BYTE_ARRAY)
				.counterEnable().createOrOpen();
		for (final Long key : sourceMap.keySet()) {
			final byte[] ba = sourceMap.get(key);
			map.put(key, ba);
		}
	}

	/**
	 * returns the list of byte arrays as a encoded byte array.
	 *
	 * @param baList
	 *            the byte array list.
	 * @return the encoded byte array.
	 */
	private byte[] toByteArray(final byte[]... baList) {
		return toByteArray(Arrays.asList(baList));
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
