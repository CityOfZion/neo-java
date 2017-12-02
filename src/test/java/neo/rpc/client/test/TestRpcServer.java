package neo.rpc.client.test;

import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.rpc.client.test.util.TestUtil;

/**
 * tests serializing blocks.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRpcServer {

	/**
	 * the CoZ mainnet API.
	 */
	private static final String MAINNET_API = "http://api.wallet.cityofzion.io";

	/**
	 * the connection timeout, in milliseconds.
	 */
	private static final int TIMEOUT_MILLIS = 2000;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestRpcServer.class);

	/**
	 * method for after class disposal.
	 */
	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	/**
	 * method for before class setup.
	 */
	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	/**
	 * test reading address balance.
	 */
	@Test
	@Ignore
	public void test001CityOfZionAddressBalance() {
		try {
			final String input = "ANrL4vPnQCCi5Mro4fqKK1rxrkxEHqmp2E";
			final String method = "/v2/address/balance/";

			final String expectedStr = TestUtil.getJsonTestResourceAsString(getClass().getSimpleName(),
					"test001CityOfZionAddressBalance");

			final String url = MAINNET_API + method + input;

			LOG.debug("url:{}", url);

			final HttpGet httpRequest = new HttpGet(url);
			final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(TIMEOUT_MILLIS)
					.setConnectTimeout(TIMEOUT_MILLIS).setConnectionRequestTimeout(TIMEOUT_MILLIS).build();
			httpRequest.setConfig(requestConfig);
			final CloseableHttpClient client = HttpClients.createDefault();
			final String responseStr;
			try {
				final CloseableHttpResponse response = client.execute(httpRequest);
				LOG.debug("status:{}", response.getStatusLine());
				final HttpEntity entity = response.getEntity();
				responseStr = EntityUtils.toString(entity);
			} catch (final ConnectTimeoutException | SocketTimeoutException | NoHttpResponseException
					| SocketException e) {
				throw new RuntimeException(e);
			}

			final JSONObject responseJson = new JSONObject(responseStr);

			final String actualStr = responseJson.toString(2);

			Assert.assertEquals("responses must match", expectedStr, actualStr);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

}
