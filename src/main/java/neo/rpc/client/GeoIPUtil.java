package neo.rpc.client;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the Geographic IP address utilities.
 *
 * @author coranos
 *
 */
public final class GeoIPUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(GeoIPUtil.class);

	/**
	 * returns the location of a host.
	 *
	 * @param canonicalHostName
	 *            the host name to use.
	 * @return the location, as JSON.
	 */
	public static JSONObject getLocation(final String canonicalHostName) {
		try {
			final String urlStr = "https://freegeoip.net/json/" + canonicalHostName;
			final HttpGet get = new HttpGet(urlStr);
			final RequestConfig requestConfig = RequestConfig.custom().build();
			get.setConfig(requestConfig);
			final CloseableHttpClient client = HttpClients.createDefault();
			final CloseableHttpResponse response = client.execute(get);
			final HttpEntity entity = response.getEntity();
			final String str = EntityUtils.toString(entity);
			final JSONObject outputJson = new JSONObject(str);
			LOG.info("outputJson:{}", outputJson);
			return outputJson;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * the constructor.
	 */
	private GeoIPUtil() {

	}

}
