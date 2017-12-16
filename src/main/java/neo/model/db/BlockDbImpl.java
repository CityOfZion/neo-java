package neo.model.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.hsqldb.jdbc.JDBCDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.core.Block;
import neo.model.core.Transaction;
import neo.model.util.SHA256HashUtil;

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
	private final JDBCDataSource ds;

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
			throw new RuntimeException("error reading resource\"" + SQL_CACHE_XML + "\"", e);
		}

		ds = new JDBCDataSource();
		ds.setUrl(sqlCache.getString("url"));

		final JdbcTemplate t = new JdbcTemplate(ds);

		executeSqlGroup(t, "create");
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
	public boolean containsHash(final UInt256 hash) {
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

	/**
	 * returns the block with the given index.
	 *
	 * @param index
	 *            the index to use.
	 * @return the block with the given index.
	 */
	@Override
	public Block getBlock(final long index) {
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

		return getTransactionsWithIndex(new Block(ByteBuffer.wrap(data.get(0))));
	}

	/**
	 * returns the block with the given hash.
	 *
	 * @param hash
	 *            the hash to use.
	 * @return the block with the given hash.
	 */
	@Override
	public Block getBlock(final UInt256 hash) {
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

		return getTransactionsWithIndex(new Block(ByteBuffer.wrap(data.get(0))));
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
	public Block getBlockWithMaxIndex() {
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

		return getTransactionsWithIndex(new Block(ByteBuffer.wrap(data.get(0))));
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
	 * @param block
	 *            the block, to add transactions to.
	 *
	 * @return the block, with transactions added.
	 */
	private Block getTransactionsWithIndex(final Block block) {
		final JdbcTemplate t = new JdbcTemplate(ds);
		final String sql = getSql("getTransactionsWithIndex");
		final List<byte[]> dataList = t.queryForList(sql, byte[].class, block.index.toByteArray());

		for (final byte[] data : dataList) {
			final Transaction transaction = new Transaction(ByteBuffer.wrap(data));
			block.getTransactionList().add(transaction);
		}

		return block;
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
		long transactionIndex = 0;
		for (final Transaction transaction : block.getTransactionList()) {
			final byte[] txBa = transaction.toByteArray();
			final byte[] txHashBa = SHA256HashUtil.getDoubleSHA256Hash(txBa);
			final byte[] txIxByte = new UInt32(transactionIndex).toByteArray();
			t.update(putTransactionSql, blockIndexBa, txIxByte, txHashBa, txBa);
			transactionIndex++;
		}
	}

}
