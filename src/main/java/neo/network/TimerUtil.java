package neo.network;

import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.network.model.TimerData;
import neo.network.model.TimerTypeEnum;

public final class TimerUtil {

	private static final String SEND = "send";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TimerUtil.class);

	private static final String RESPONSE_COMMAND = "response-command";

	private static final Map<String, String> timersKeyMap = new TreeMap<>();

	private static final Map<String, String> responseCommandKeyMap = new TreeMap<>();

	private static final Map<CommandEnum, String> sendKeyMap = new TreeMap<>();

	public static String getResponseCommandKey(final String responseCommand) {
		if (!responseCommandKeyMap.containsKey(responseCommand)) {
			responseCommandKeyMap.put(responseCommand, RESPONSE_COMMAND + "-" + responseCommand);
		}
		return responseCommandKeyMap.get(responseCommand);
	}

	public static String getSendKey(final CommandEnum command) {
		if (!sendKeyMap.containsKey(command)) {
			sendKeyMap.put(command, SEND + "-" + command.getName());
		}
		return sendKeyMap.get(command);
	}

	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final CommandEnum command,
			final String subtype) {
		return getTimerData(timersMap, getSendKey(command), subtype);
	}

	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final String command,
			final String subtype) {
		final String key;
		if (subtype == null) {
			key = command;
		} else {
			key = getTimerKey(command, subtype);
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

	public static String getTimerKey(final String timerType, final String key) {
		final String timerKey = timerType + "-" + key;
		if (!timersKeyMap.containsKey(timerKey)) {
			timersKeyMap.put(timerKey, timerKey);
		}
		return timersKeyMap.get(timerKey);
	}

	public static Map<String, TimerData> getTimerMap(final JSONObject timersJson) {
		final Map<String, TimerData> timersMap = new TreeMap<>();
		for (final String timerType : timersJson.keySet()) {
			final JSONObject timerTypeJson = timersJson.getJSONObject(timerType);
			for (final String key : timerTypeJson.keySet()) {
				final JSONObject timerJson = timerTypeJson.getJSONObject(key);
				final String timersMapKey = getTimerKey(timerType, key);
				final TimerData timerData = new TimerData(timersMapKey, timerJson);
				timersMap.put(timersMapKey, timerData);

				if (timerJson.has(RESPONSE_COMMAND)) {
					final String responseCommand = timerJson.getString(RESPONSE_COMMAND);
					timersMap.put(getResponseCommandKey(responseCommand), timerData);
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
