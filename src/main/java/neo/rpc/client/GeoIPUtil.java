package neo.rpc.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

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

public class GeoIPUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(GeoIPUtil.class);

	public static JSONObject getLocation(final String canonicalHostName) throws MalformedURLException, IOException {
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
	}

	public static JSONObject getLocation2(final String canonicalHostName) throws MalformedURLException, IOException {
		final String urlStr = "https://freegeoip.net/json/" + canonicalHostName;
		final URL url = new URL(urlStr);
		final URLConnection urlc = url.openConnection();
		final byte[] ba = readFully(urlc);

		final JSONObject location;
		if (ba != null) {
			location = new JSONObject(new String(ba, Charset.defaultCharset()));
		} else {
			location = null;
		}
		return location;
	}

	private static byte[] readFully(final InputStream in) throws IOException {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final byte[] ba = new byte[1];
		while (in.read(ba) > 0) {
			bout.write(ba);
		}

		return bout.toByteArray();
	}

	private static byte[] readFully(final URLConnection urlc) {
		final byte[] ba;
		try (InputStream in = urlc.getInputStream()) {
			ba = readFully(in);
		} catch (final IOException e) {
			return null;
		}
		return ba;
	}
}
