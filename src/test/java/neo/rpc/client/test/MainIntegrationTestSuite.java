package neo.rpc.client.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * the main integration test suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		/** */
		TestLoadSave.class,
		/** */
		TestPayload.class,
		/** */
})

public class MainIntegrationTestSuite {
}
