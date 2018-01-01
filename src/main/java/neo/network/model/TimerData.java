package neo.network.model;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.util.JsonUtil;

/**
 * timer data.
 *
 * @author coranos
 *
 */
public class TimerData {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TimerData.class);

	/**
	 * the response command.
	 */
	private static final String RESPONSE_COMMAND = "response-command";

	/**
	 * the response wait timeout.
	 */
	private static final String RESPONSE_WAIT_TIMEOUT = "response-wait-timeout";

	/**
	 * the send interval.
	 */
	private static final String SEND_INTERVAL = "send-interval";

	/**
	 * the wait time before sending, in ms.
	 */
	private final long waitTimeBeforeSendMs;

	/**
	 * the wait time before re-sending, in ms.
	 */
	private final long waitTimeBeforeResendMs;

	/**
	 * the response command.
	 */
	private final String responseCommand;

	/**
	 * the last sent time.
	 */
	private long lastSentTimeMs = 0;

	/**
	 * waiting for response flag.
	 */
	private boolean waitingForResponse = false;

	/**
	 * the label for logging.
	 */
	private final String logLabel;

	/**
	 * the constructor.
	 *
	 * @param logLabel
	 *            the logging label.
	 * @param config
	 *            the configuration.
	 */
	public TimerData(final String logLabel, final JSONObject config) {
		this.logLabel = logLabel;
		waitTimeBeforeSendMs = JsonUtil.getTime(config, SEND_INTERVAL);
		waitTimeBeforeResendMs = JsonUtil.getTime(config, RESPONSE_WAIT_TIMEOUT);
		if (config.isNull(RESPONSE_COMMAND)) {
			responseCommand = null;
		} else {
			responseCommand = config.getString(RESPONSE_COMMAND);
		}
	}

	/**
	 * return the response command.
	 *
	 * @return the response command.
	 */
	public String getResponseCommand() {
		return responseCommand;
	}

	/**
	 * return true if the timer indicates that it is time to send a new command.
	 *
	 * @return true if the timer indicates that it is time to send a new command.
	 */
	public boolean isReadyForSend() {
		final long currentTimeMillis = System.currentTimeMillis();
		LOG.debug("STARTED isReadyForSend {}", logLabel);
		LOG.trace("INTERIM isReadyForSend waitingForResponse:{};", waitingForResponse);
		LOG.trace("INTERIM isReadyForSend lastSentTimeMs:{};", lastSentTimeMs);
		LOG.trace("INTERIM isReadyForSend waitTimeBeforeResendMs:{};", waitTimeBeforeResendMs);
		LOG.trace("INTERIM isReadyForSend waitTimeBeforeSendMs:{};", waitTimeBeforeSendMs);
		LOG.trace("INTERIM isReadyForSend currentTimeMillis:{};", currentTimeMillis);
		if (waitingForResponse) {
			final long resendTimeMs = lastSentTimeMs + waitTimeBeforeResendMs;
			LOG.trace("INTERIM isReadyForSend resendTimeMs:{};", resendTimeMs);
			LOG.trace("INTERIM isReadyForSend  (resendTimeMs < currentTimeMillis):{};",
					(resendTimeMs < currentTimeMillis));
			if (resendTimeMs < currentTimeMillis) {
				LOG.debug("SUCCESS isReadyForSend {} true, was waiting for response.", logLabel);
				return true;
			}
		} else {
			final long sendTimeMs = lastSentTimeMs + waitTimeBeforeSendMs;
			LOG.trace("INTERIM isReadyForSend sendTimeMs:{};", sendTimeMs);
			LOG.trace("INTERIM isReadyForSend  (sendTimeMs < currentTimeMillis):{};", (sendTimeMs < currentTimeMillis));
			if (sendTimeMs < currentTimeMillis) {
				LOG.debug("SUCCESS isReadyForSend {} true, was not waiting for response.", logLabel);
				return true;
			}
		}
		LOG.debug("SUCCESS isReadyForSend {} false", logLabel);
		return false;
	}

	/**
	 * tell the timer data object that the request was sent, so it should update the
	 * last sent time, and set the wait timer to true.
	 */
	public void requestSent() {
		lastSentTimeMs = System.currentTimeMillis();
		waitingForResponse = true;
	}

	/**
	 * tell the timer data object that the response was recieved, so it should
	 * update the wait timer to false.
	 */
	public void responseReceived() {
		LOG.debug("STARTED responseReceived");
		waitingForResponse = false;
		LOG.debug("SUCCESS responseReceived");
	}

}
