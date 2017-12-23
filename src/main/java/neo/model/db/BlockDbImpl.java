package neo.model.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.h2.jdbcx.JdbcDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

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

/**
 * the block database.
 *
 * @author coranos
 *
 */
public final class BlockDbImpl implements BlockDb {

	/**
	 * the JSON key "sql".
	 */
	private static final String SQL = "sql";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockDbImpl.class);

	/**
	 * the SQL cache XML file name.
	 */
	private static final String SQL_CACHE_XML = "BlockDbImpl.xml";

	/**
	 * the data source.
	 */
	private final JdbcDataSource ds;

	/**
	 * the SQL cache.
	 */
	private final JSONObject sqlCache;

	/**
	 * the closed flag.
	 */
	private boolean closed = false;

	/**
	 * the constructor.
	 */
	public BlockDbImpl() {
		try (InputStream resourceAsStream = BlockDbImpl.class.getResourceAsStream(SQL_CACHE_XML);) {
			final String jsonStr = IOUtils.toString(resourceAsStream, "UTF-8");
			sqlCache = XML.toJSONObject(jsonStr, true).getJSONObject("BlockDbImpl");
		} catch (final IOException | NullPointerException e) {
			throw new RuntimeException("error reading resource\"" + SQL_CACHE_XML + "\" ", e);
		}

		ds = new JdbcDataSource();
		ds.setUrl(sqlCache.getString("url"));

		final JdbcTemplate t = new JdbcTemplate(ds);

		executeSqlGroup(t, "create");
	}

	/**
	 * add the parameters to the list.
	 *
	 * @param list
	 *            the list to use.
	 * @param parms
	 *            the parameters to add to the list.
	 */
	private void add(final List<Object[]> list, final Object... parms) {
		list.add(parms);
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
		final JdbcTemplate t = new JdbcTemplate(ds);
		executeSqlGroup(t, "close");
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
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("containsHash");
		final List<Integer> data = t.queryForList(sql, Integer.class, hash.toByteArray());
		return !data.isEmpty();
	}

	/**
	 * executes the group of SQL in the SQL Cache.
	 *
	 * @param jdbc
	 *            the jdbcoperations to use.
	 *
	 * @param sqlGroup
	 *            the group of SQL to pull out of the sqlcache to execute.
	 */
	private void executeSqlGroup(final JdbcOperations jdbc, final String sqlGroup) {
		final JSONObject sqlGroupJo = sqlCache.getJSONObject(sqlGroup);
		if (!sqlGroupJo.has(SQL)) {
			throw new RuntimeException("no key \"" + SQL + "\" in " + sqlGroupJo.keySet());
		}

		if (sqlGroupJo.get(SQL) instanceof JSONArray) {
			final JSONArray createSqls = sqlGroupJo.getJSONArray(SQL);
			for (int createSqlIx = 0; createSqlIx < createSqls.length(); createSqlIx++) {
				final String sql = createSqls.getString(createSqlIx);
				jdbc.execute(sql);
			}
		} else if (sqlGroupJo.get(SQL) instanceof String) {
			final String sql = sqlGroupJo.getString(SQL);
			jdbc.execute(sql);
		} else {
			throw new RuntimeException(
					"no key of type String or JSONArray in \"" + SQL + "\" found in " + sqlGroupJo.keySet());
		}
	}

	@Override
	public Map<UInt160, Map<UInt256, Fixed8>> getAccountAssetValueMap() {
		final JdbcTemplate jdbcOperations = new JdbcTemplate(ds);
		final String sql = getSql("getAccountAssetValueMap");

		final List<Map<String, Object>> mapList = jdbcOperations.queryForList(sql);

		final Map<UInt160, Map<UInt256, Fixed8>> accountAssetValueMap = new TreeMap<>();

		final TransactionOutputMapToObject mapToObject = new TransactionOutputMapToObject();

		for (final Map<String, Object> map : mapList) {
			final TransactionOutput output = mapToObject.toObject(map);

			if (!accountAssetValueMap.containsKey(output.scriptHash)) {
				accountAssetValueMap.put(output.scriptHash, new TreeMap<>());
			}
			final Map<UInt256, Fixed8> assetValueMap = accountAssetValueMap.get(output.scriptHash);
			assetValueMap.put(output.assetId, output.value);
		}

		return accountAssetValueMap;
	}

	@Override
	public Block getBlock(final long index, final boolean withTransactions) {
		synchronized (this) {
			if (closed) {
				return null;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final UInt32 indexObj = new UInt32(index);
		final byte[] indexBa = indexObj.toByteArray();
		final String sql = getSql("getBlockWithIndex");
		final List<byte[]> data = t.queryForList(sql, byte[].class, indexBa);
		if (data.isEmpty()) {
			return null;
		}

		final Block block = new Block(ByteBuffer.wrap(data.get(0)));
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
	@Override
	public Block getBlock(final UInt256 hash, final boolean withTransactions) {
		synchronized (this) {
			if (closed) {
				return null;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getBlockWithHash");
		final List<byte[]> data = t.queryForList(sql, byte[].class, hash.toByteArray());
		if (data.isEmpty()) {
			return null;
		}

		final Block block = new Block(ByteBuffer.wrap(data.get(0)));
		if (withTransactions) {
			getTransactionsForBlock(block);
		}
		return block;
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
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getBlockCount");
		return t.queryForObject(sql, Integer.class);
	}

	/**
	 * return the block with the maximum value in the index column.
	 *
	 * @return the block with the maximum value in the index column.
	 */
	@Override
	public Block getBlockWithMaxIndex(final boolean withTransactions) {
		synchronized (this) {
			if (closed) {
				return null;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getBlockWithMaxIndex");
		final List<byte[]> data = t.queryForList(sql, byte[].class);
		if (data.isEmpty()) {
			return null;
		}

		final Block block = new Block(ByteBuffer.wrap(data.get(0)));
		if (withTransactions) {
			getTransactionsForBlock(block);
		}
		return block;
	}

	/**
	 * return the file size.
	 *
	 * @return the file size.
	 */
	@Override
	public long getFileSize() {
		final File dir = new File(sqlCache.getString("getFileSizeDir"));
		return FileUtils.sizeOfDirectory(dir);
	}

	/**
	 * return a map of the objects, divided into their transactions indexes.
	 *
	 * @param jdbcOperations
	 *            the jdbc operations to use.
	 * @param sqlKey
	 *            the sql key to use.
	 * @param blockIndexBa
	 *            the block index byte array to use.
	 * @param mapToObject
	 *            the mapToObject to use.
	 * @param <T>
	 *            the object type to use.
	 * @return a map of the objects, divided into their transactions indexes.
	 */
	private <T> Map<Integer, List<T>> getMapList(final JdbcOperations jdbcOperations, final String sqlKey,
			final byte[] blockIndexBa, final AbstractMapToObject<T> mapToObject) {
		final String sql = getSql(sqlKey);

		final List<Map<String, Object>> mapList = jdbcOperations.queryForList(sql, blockIndexBa);

		final Map<Integer, List<T>> tMapList = new TreeMap<>();
		for (final Map<String, Object> map : mapList) {
			final byte[] transactionIndexBa = (byte[]) map.get("transaction_index");
			final T t = mapToObject.toObject(map);
			final UInt16 transactionIndexObj = new UInt16(transactionIndexBa);
			final int transactionIndex = transactionIndexObj.asInt();
			if (!tMapList.containsKey(transactionIndex)) {
				tMapList.put(transactionIndex, new ArrayList<>());
			}
			tMapList.get(transactionIndex).add(t);
		}

		return tMapList;
	}

	/**
	 * returns the SQL in the sqlcache (must be a singleton SQL in the sqlGroup).
	 *
	 * @param sqlGroup
	 *            the group of SQL to pull out of the sqlcache to execute.
	 * @return the SQL in the sqlcache (must be a singleton SQL in the sqlGroup).
	 */
	private String getSql(final String sqlGroup) {
		return sqlCache.getJSONObject(sqlGroup).getString(SQL);
	}

	/**
	 * gets the inputs for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use
	 * @param jdbcOperations
	 *            the jdbc operations to use.
	 * @param blockIndexBa
	 *            the block index to use.
	 */
	private void getTransactionInputsWithIndex(final Block block, final JdbcOperations jdbcOperations,
			final byte[] blockIndexBa) {
		final Map<Integer, List<CoinReference>> inputsMap = getMapList(jdbcOperations, "getTransactionInputsWithIndex",
				blockIndexBa, new CoinReferenceMapToObject());
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
	 * gets the outputs for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use
	 * @param jdbcOperations
	 *            the jdbc operations to use.
	 * @param blockIndexBa
	 *            the block index to use.
	 */
	private void getTransactionOutputsWithIndex(final Block block, final JdbcOperations jdbcOperations,
			final byte[] blockIndexBa) {
		final Map<Integer, List<TransactionOutput>> outputsMap = getMapList(jdbcOperations,
				"getTransactionOutputsWithIndex", blockIndexBa, new TransactionOutputMapToObject());
		for (final int txIx : outputsMap.keySet()) {
			final List<TransactionOutput> outputs = outputsMap.get(txIx);
			block.getTransactionList().get(txIx).outputs.addAll(outputs);
		}
	}

	/**
	 * gets the scripts for each transaction in the block, and adds them to the
	 * transaction.
	 *
	 * @param block
	 *            the block to use
	 * @param jdbcOperations
	 *            the jdbc operations to use.
	 * @param blockIndexBa
	 *            the block index to use.
	 */
	private void getTransactionScriptsWithIndex(final Block block, final JdbcOperations jdbcOperations,
			final byte[] blockIndexBa) {
		final Map<Integer, List<Witness>> scriptsMap = getMapList(jdbcOperations, "getTransactionScriptsWithIndex",
				blockIndexBa, new WitnessMapToObject());
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
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getTransactionsWithIndex");
		final byte[] blockIndexBa = block.index.toByteArray();
		final List<byte[]> dataList = t.queryForList(sql, byte[].class, blockIndexBa);

		for (final byte[] data : dataList) {
			final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
			block.getTransactionList().add(transaction);
		}

		getTransactionOutputsWithIndex(block, t, blockIndexBa);

		getTransactionInputsWithIndex(block, t, blockIndexBa);

		getTransactionScriptsWithIndex(block, t, blockIndexBa);
	}

	@Override
	public Transaction getTransactionWithHash(final UInt256 hash) {
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getTransactionWithHash");
		final List<byte[]> dataList = t.queryForList(sql, byte[].class, hash.toByteArray());

		if (dataList.isEmpty()) {
			return null;
		}

		return new Transaction(ByteBuffer.wrap(dataList.get(0)));
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
		final JdbcTemplate t = new JdbcTemplate(ds);
		final byte[] prevHashBa = block.prevHash.toByteArray();
		ArrayUtils.reverse(prevHashBa);

		final String putBlockSql = getSql("putBlock");
		final byte[] blockIndexBa = block.index.toByteArray();
		t.update(putBlockSql, block.hash.toByteArray(), prevHashBa, blockIndexBa, block.toHeaderByteArray());

		final String putTransactionSql = getSql("putTransaction");
		final String putTransactionInputSql = getSql("putTransactionInput");
		final String putTransactionOutputSql = getSql("putTransactionOutput");
		final String putTransactionScriptSql = getSql("putTransactionScript");
		int transactionIndex = 0;

		final List<Object[]> putTransactionList = new ArrayList<>();
		final List<Object[]> putTransactionInputList = new ArrayList<>();
		final List<Object[]> putTransactionOutputList = new ArrayList<>();
		final List<Object[]> putTransactionScriptList = new ArrayList<>();

		for (final Transaction transaction : block.getTransactionList()) {
			final byte[] txIxByte = new UInt16(transactionIndex).toByteArray();
			final byte[] transactionBaseBa = transaction.toBaseByteArray();
			add(putTransactionList, blockIndexBa, txIxByte, transaction.hash.toByteArray(), transactionBaseBa);

			for (int inputIx = 0; inputIx < transaction.inputs.size(); inputIx++) {
				final byte[] txInputIxByte = new UInt32(inputIx).toByteArray();
				final CoinReference input = transaction.inputs.get(inputIx);
				add(putTransactionInputList, blockIndexBa, txIxByte, txInputIxByte, input.prevHash.toByteArray(),
						input.prevIndex.toByteArray());
			}

			for (int outputIx = 0; outputIx < transaction.outputs.size(); outputIx++) {
				final byte[] txOutputIxByte = new UInt16(outputIx).toByteArray();
				final TransactionOutput output = transaction.outputs.get(outputIx);
				add(putTransactionOutputList, blockIndexBa, txIxByte, txOutputIxByte, output.assetId.toByteArray(),
						output.value.toByteArray(), output.scriptHash.toByteArray());
			}

			for (int scriptIx = 0; scriptIx < transaction.scripts.size(); scriptIx++) {
				final byte[] txScriptIxByte = new UInt32(scriptIx).toByteArray();
				final Witness script = transaction.scripts.get(scriptIx);
				add(putTransactionScriptList, blockIndexBa, txIxByte, txScriptIxByte,
						script.getCopyOfInvocationScript(), script.getCopyOfVerificationScript());
			}

			transactionIndex++;
		}

		t.batchUpdate(putTransactionSql, putTransactionList);
		t.batchUpdate(putTransactionInputSql, putTransactionInputList);
		t.batchUpdate(putTransactionOutputSql, putTransactionOutputList);
		t.batchUpdate(putTransactionScriptSql, putTransactionScriptList);

	}

}
