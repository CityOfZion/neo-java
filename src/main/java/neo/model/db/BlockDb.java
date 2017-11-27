package neo.model.db;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import neo.model.bytes.UInt256;
import neo.model.core.Block;

/**
 * the block database.
 *
 * @author coranos
 *
 */
public final class BlockDb {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockDb.class);

	/**
	 * the static singleton instance.
	 */
	private static final BlockDb INSTANCE = new BlockDb();

	/**
	 * return the static instance of the db connection.
	 *
	 * @return the static instance of the db connection.
	 */
	public static BlockDb getInstance() {
		return INSTANCE;
	}

	/**
	 * the data source.
	 */
	private final JDBCDataSource ds;

	/**
	 * the closed flag.
	 */
	private boolean closed = false;

	/**
	 * the constructor.
	 */
	private BlockDb() {
		ds = new JDBCDataSource();
		ds.setUrl("jdbc:hsqldb:file:java-chain/db/db");

		final JdbcTemplate t = new JdbcTemplate(ds);
		t.execute("CREATE CACHED TABLE IF NOT EXISTS"
				+ " block (hash BINARY(32) not null, prev_hash BINARY(32) not null, index BINARY(4) not null, block  LONGVARBINARY not null)");
		t.execute("CREATE INDEX IF NOT EXISTS block_hash ON block (hash)");
		t.execute("CREATE INDEX IF NOT EXISTS block_prev_hash ON block (prev_hash)");
		t.execute("CREATE INDEX IF NOT EXISTS block_index ON block (index)");
	}

	/**
	 * close the database.
	 *
	 * @throws SQLException
	 *             if an error occurs.
	 */
	public void close() throws SQLException {
		synchronized (this) {
			closed = true;
		}
		LOG.debug("STARTED shutdown");
		final JdbcTemplate t = new JdbcTemplate(ds);
		t.execute("SHUTDOWN");
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
	public boolean containsHash(final UInt256 hash) {
		synchronized (this) {
			if (closed) {
				return false;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final List<Integer> data = t.queryForList("select 1 from block where hash = ?", Integer.class,
				hash.toByteArray());
		return !data.isEmpty();
	}

	/**
	 * return the block count.
	 *
	 * @return the block count.
	 */
	public long getBlockCount() {
		synchronized (this) {
			if (closed) {
				return 0;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		return t.queryForObject("select count(1) from block", Integer.class);
	}

	/**
	 * return the block with the maximum value in the index column.
	 *
	 * @return the block with the maximum value in the index column.
	 */
	public Block getBlockWithMaxIndex() {
		synchronized (this) {
			if (closed) {
				return null;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final List<byte[]> data = t.queryForList(
				"select block from block where index = (select max(index) from block) limit 1", byte[].class);
		if (data.isEmpty()) {
			return null;
		}

		return new Block(ByteBuffer.wrap(data.get(0)));
	}

	/**
	 * return the file size.
	 *
	 * @return the file size.
	 */
	public long getFileSize() {
		final File dir = new File("java-chain/db");
		return FileUtils.sizeOfDirectory(dir);
	}

	/**
	 * puts the block into the database.
	 *
	 * @param block
	 *            the block to use.
	 */
	public void put(final Block block) {
		synchronized (this) {
			if (closed) {
				return;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		final byte[] prevHashBa = block.prevHash.toByteArray();
		ArrayUtils.reverse(prevHashBa);

		t.update("insert into block (hash,prev_hash,index,block) values (?,?,?,?)", block.hash.toByteArray(),
				prevHashBa, block.index.toByteArray(), block.toByteArray());
	}

}
