package neo.network;

import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.network.model.TimerData;
import neo.network.model.TimerTypeEnum;

public class TimerUtil {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TimerUtil.class);

	private static final String RESPONSE_COMMAND = "response-command";

	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final CommandEnum command,
			final String subtype) {
		return getTimerData(timersMap, "send-" + command.getName(), subtype);
	}

	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final String command,
			final String subtype) {
		final String key;
		if (subtype == null) {
			key = command;
		} else {
			key = command + "-" + subtype;
		}

		if (!timersMap.containsKey(key)) {
			throw new RuntimeException(
					"no polling data for key \"" + key + "\" found in timersMap.keySet():" + timersMap.keySet());
		}
		return timersMap.get(key);
	}

	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final TimerTypeEnum timerType,
			final String subtype) {
		return getTimerData(timersMap, timerType.getName(), subtype);
	}

	public static Map<String, TimerData> getTimerMap(final JSONObject timersJson) {
		final Map<String, TimerData> timersMap = new TreeMap<>();
		for (final String timerType : timersJson.keySet()) {
			final JSONObject timerTypeJson = timersJson.getJSONObject(timerType);
			for (final String key : timerTypeJson.keySet()) {
				final JSONObject timerJson = timerTypeJson.getJSONObject(key);
				final String timersMapKey = timerType + "-" + key;
				final TimerData timerData = new TimerData(timersMapKey, timerJson);
				timersMap.put(timersMapKey, timerData);

				if (timerJson.has(RESPONSE_COMMAND)) {
					final String responseCommand = timerJson.getString(RESPONSE_COMMAND);
					timersMap.put(RESPONSE_COMMAND + "-" + responseCommand, timerData);
				}
			}
		}
		return timersMap;
	}

	public static void responseReceived(final Map<String, TimerData> timersMap, final CommandEnum commandEnum) {
		LOG.trace("STARTED responseReceived {}", commandEnum);
		final String key = RESPONSE_COMMAND + "-" + commandEnum.getName();
		if (timersMap.containsKey(key)) {
			LOG.debug("INTERIM responseReceived {} key {}", commandEnum, key);
			timersMap.get(key).responseReceived();
		}
		LOG.trace("SUCCESS responseReceived {}", commandEnum);
	}
}
