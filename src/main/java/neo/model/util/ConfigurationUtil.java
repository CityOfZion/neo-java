package neo.model.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import neo.network.LocalControllerNode;

/**
 * a utility class to handle saving and loading the configuration files.
 *
 * @author coranos
 *
 */
public final class ConfigurationUtil {

	/**
	 * the name of the config file.
	 */
	private static final File CONFIG_FILE = new File("config.json");

	/**
	 * @return the configuration JSON object.
	 * @throws IOException
	 *             if an error occurs.
	 */
	public static JSONObject getConfiguration() {
		try {
			final JSONObject controllerNodeConfig = new JSONObject(
					FileUtils.readFileToString(CONFIG_FILE, Charset.defaultCharset()));
			final int nonce = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
			controllerNodeConfig.put(LocalControllerNode.NONCE, nonce);
			return controllerNodeConfig;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * the constructor.
	 */
	private ConfigurationUtil() {
	}
}
