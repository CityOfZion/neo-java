package neo.network;

import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.CommandEnum;
import neo.network.model.TimerData;
import neo.network.model.TimerTypeEnum;

/**
 * the timer utility class.
 *
 * @author coranos
 *
 */
public final class TimerUtil {

	/**
	 * a dash "-", used several times in making timer keys.
	 */
	private static final String DASH = "-";

	/**
	 * send.
	 */
	private static final String SEND = "send";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TimerUtil.class);

	/**
	 * response-command.
	 */
	private static final String RESPONSE_COMMAND = "response-command";

	/**
	 * the timer key deduplication map (so we only store one copy of the key in
	 * memory).
	 */
	private static final Map<String, String> TIMERS_KEY_MAP = new TreeMap<>();

	/**
	 * the response command key deduplication map (so we only store one copy of the
	 * key in memory).
	 */
	private static final Map<String, String> RESPONSE_COMMAND_KEY_MAP = new TreeMap<>();

	/**
	 * the send key deduplication map (so we only store one copy of the key in
	 * memory).
	 */
	private static final Map<CommandEnum, String> SEND_KEY_MAP = new TreeMap<>();

	/**
	 * returns the key for referring to a response from a command.
	 *
	 * @param responseCommand
	 *            get the key for referring to the response command timer.
	 * @return the key.
	 */
	private static String getResponseCommandKey(final String responseCommand) {
		if (!RESPONSE_COMMAND_KEY_MAP.containsKey(responseCommand)) {
			RESPONSE_COMMAND_KEY_MAP.put(responseCommand, RESPONSE_COMMAND + DASH + responseCommand);
		}
		return RESPONSE_COMMAND_KEY_MAP.get(responseCommand);
	}

	/**
	 * returns the key for referring to a "send" command.
	 *
	 * @param command
	 *            the command being sent.
	 * @return the key.
	 */
	private static String getSendKey(final CommandEnum command) {
		if (!SEND_KEY_MAP.containsKey(command)) {
			SEND_KEY_MAP.put(command, SEND + DASH + command.getName());
		}
		return SEND_KEY_MAP.get(command);
	}

	/**
	 * returns timer data for a given timer map, command, and command subtype.
	 *
	 * @param timersMap
	 *            the timer map.
	 * @param command
	 *            the command.
	 * @param subtype
	 *            the command subtype.
	 * @return the timer data.
	 */
	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final CommandEnum command,
			final String subtype) {
		return getTimerData(timersMap, getSendKey(command), subtype);
	}

	/**
	 * returns timer data for a given timer map, command, and command subtype.
	 *
	 * @param timersMap
	 *            the timer map.
	 * @param sendKey
	 *            the command send key.
	 * @param subtype
	 *            the command subtype.
	 * @return the timer data.
	 */
	private static TimerData getTimerData(final Map<String, TimerData> timersMap, final String sendKey,
			final String subtype) {
		final String key;
		if (subtype == null) {
			key = sendKey;
		} else {
			key = getTimerKey(sendKey, subtype);
		}

		if (!timersMap.containsKey(key)) {
			throw new RuntimeException(
					"no polling data for key \"" + key + "\" found in timersMap.keySet():" + timersMap.keySet());
		}
		return timersMap.get(key);
	}

	/**
	 * returns timer data for a given timer map, command, and command subtype.
	 *
	 * @param timersMap
	 *            the timer map.
	 * @param timerType
	 *            the timer type.
	 * @param subtype
	 *            the timer subtype.
	 * @return the timer data.
	 */
	public static TimerData getTimerData(final Map<String, TimerData> timersMap, final TimerTypeEnum timerType,
			final String subtype) {
		return getTimerData(timersMap, timerType.getName(), subtype);
	}

	/**
	 * return the timer key.
	 *
	 * @param timerType
	 *            the timer type.
	 * @param timerSubType
	 *            the timer subtype.
	 * @return the timer key.
	 */
	private static String getTimerKey(final String timerType, final String timerSubType) {
		final String timerKey = timerType + DASH + timerSubType;
		if (!TIMERS_KEY_MAP.containsKey(timerKey)) {
			TIMERS_KEY_MAP.put(timerKey, timerKey);
		}
		return TIMERS_KEY_MAP.get(timerKey);
	}

	/**
	 * parses the JSON into a timer map.
	 *
	 * @param timersJson
	 *            the timers JSON configuration.
	 * @return the timer data in a map.
	 */
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

	/**
	 * record that a response was recieved for a given timer, which resets the
	 * "Waiting for response" flag.
	 *
	 * @param timersMap
	 *            the timer map to use.
	 * @param commandEnum
	 *            the command to use.
	 */
	public static void responseReceived(final Map<String, TimerData> timersMap, final CommandEnum commandEnum) {
		LOG.trace("STARTED responseReceived {}", commandEnum);
		final String key = RESPONSE_COMMAND + DASH + commandEnum.getName();
		if (timersMap.containsKey(key)) {
			LOG.debug("INTERIM responseReceived {} key {}", commandEnum, key);
			timersMap.get(key).responseReceived();
		}
		LOG.trace("SUCCESS responseReceived {}", commandEnum);
	}

	/**
	 * the constructor.
	 */
	private TimerUtil() {

	}
}
