package neo.model.db;

import neo.model.bytes.UInt256;
import neo.model.core.Block;

public interface BlockDb {

	void close();

	boolean containsHash(UInt256 hash);

	Block getBlock(long blockHeight);

	Block getBlock(UInt256 hash);

	long getBlockCount();

	Block getBlockWithMaxIndex();

	long getFileSize();

	void put(Block block);

}
