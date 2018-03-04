package neo.rpc.client.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * the main test suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
		/** */
		TestPayload.class,
		/** */
		TestBlockSerialization.class,
		/** */
		TestRpcServer.class,
		/** */
		TestCoreToJson.class,
		/** */
		TestCoreExceptions.class,
		/** */
		TestCoreEnums.class,
		/** */
		TestNEP5.class,
		/** */
		TestRpcServerInit.class,
		/** */
		TestNetwork.class,
		/** */
		TestDBH2.class,
		/** */
		TestDBMapDb.class, })

public class MainUnitTestSuite {
}
