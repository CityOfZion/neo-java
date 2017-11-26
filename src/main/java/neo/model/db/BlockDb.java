package neo.model.db;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import neo.model.bytes.UInt256;
import neo.model.bytes.UInt32;
import neo.model.core.Block;

public class BlockDb {

	private static final boolean EXPLAIN_PLAN_FOR = false;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(BlockDb.class);

	private final JDBCDataSource ds;

	private boolean closed = false;

	public BlockDb() {
		ds = new JDBCDataSource();
		ds.setUrl("jdbc:hsqldb:file:java-chain/db/db");

		final JdbcTemplate t = new JdbcTemplate(ds);
		t.execute(
				"CREATE CACHED TABLE IF NOT EXISTS block (hash BINARY(32) not null, prev_hash BINARY(32) not null, index BINARY(4) not null, block  LONGVARBINARY not null)");
		t.execute("CREATE INDEX IF NOT EXISTS block_hash ON block (hash)");
		t.execute("CREATE INDEX IF NOT EXISTS block_prev_hash ON block (prev_hash)");
		t.execute("CREATE INDEX IF NOT EXISTS block_index ON block (index)");
	}

	public void close() throws SQLException {
		synchronized (this) {
			closed = true;
		}
		LOG.debug("STARTED shutdown");
		final JdbcTemplate t = new JdbcTemplate(ds);
		// t.execute("CHECKPOINT DEFRAG");
		// t.execute("SHUTDOWN COMPACT");
		t.execute("SHUTDOWN");
		LOG.debug("SUCCESS shutdown");
	}

	public void compress() {
		synchronized (this) {
			if (closed) {
				return;
			}
		}

		LOG.debug("STARTED compress");
		final JdbcTemplate t = new JdbcTemplate(ds);
		t.execute("CHECKPOINT DEFRAG");
		LOG.debug("SUCCESS compress");
	}

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

	public void deleteBlocksAboveAndIncludingIndex(final UInt32 index) {
		LOG.debug("STARTED deleteBlocksAboveAndIncludingIndex {}", index.asLong());
		final String sql = "delete " + "from block where index >= ?";
		final JdbcTemplate t = new JdbcTemplate(ds);
		final int size = t.update(sql, index.toByteArray());
		LOG.debug("SUCCESS deleteBlocksAboveAndIncludingIndex, {} blocks cleared", size);
	}

	public long getBlockCount() {
		synchronized (this) {
			if (closed) {
				return 0;
			}
		}
		final JdbcTemplate t = new JdbcTemplate(ds);
		return t.queryForObject("select count(1) from block", Integer.class);
	}

	public List<Block> getBlocksWithMissingPrevHash(final byte[] byteArray, final int limit) {
		synchronized (this) {
			if (closed) {
				return Collections.emptyList();
			}
		}
		final String sql = "select found_block.block " + "from block as found_block " + "where found_block.hash != ? "
				+ "and not exists (select 1 from block where hash = found_block.prev_hash) "
				+ "order by found_block.index " + "asc limit ?";
		LOG.debug("STARTED getBlocksWithMissingPrevHash");
		final JdbcTemplate t = new JdbcTemplate(ds);

		if (EXPLAIN_PLAN_FOR) {
			final List<String> explainList = t.queryForList("explain plan for " + sql, String.class);
			LOG.debug("INTERIM getBlocksWithMissingPrevHash {}", explainList.size());
			int explainNbr = 0;
			for (final String explainRow : explainList) {
				explainNbr++;
				LOG.debug("INTERIM getBlocksWithMissingPrevHash ({}/{}):{}", explainNbr, explainList.size(),
						explainRow);
			}
		}

		final List<byte[]> dataList = t.queryForList(sql, byte[].class, byteArray, limit);

		final List<Block> blockList = new ArrayList<>();

		for (final byte[] data : dataList) {
			final Block block = new Block(ByteBuffer.wrap(data));
			blockList.add(block);
		}

		LOG.debug("SUCCESS getBlocksWithMissingPrevHash, count returned:{}", blockList.size());
		return blockList;
	}

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

	public long getFileSize() {
		final File dir = new File("java-chain/db");
		return FileUtils.sizeOfDirectory(dir);
	}

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
