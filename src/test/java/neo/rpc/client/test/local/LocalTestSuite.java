package neo.rpc.client.test.local;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * a test suite for local tests, that only work with a full blockchain (and are
 * thus not suitable for unit or integration tests).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		/** */
		TestRpcServer.class,
		/** */
})

public class LocalTestSuite {

}
