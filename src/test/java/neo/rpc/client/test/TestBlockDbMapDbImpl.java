package neo.rpc.client.test;

import java.io.IOException;

import org.apache.commons.codec.DecoderException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.db.BlockDb;
import neo.model.db.mapdb.BlockDbMapDbImpl;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public final class TestBlockDbMapDbImpl {

	private static final Logger LOG = LoggerFactory.getLogger(TestBlockDbMapDbImpl.class);

	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	@Test
	public void test001getHeaderOfBlockWithMaxIndex() throws IOException, DecoderException {
		final BlockDb blockDb = new BlockDbMapDbImpl(null);
		Assert.assertNotNull("header of block database should not be null", blockDb.getHeaderOfBlockWithMaxIndex());
	}

}
