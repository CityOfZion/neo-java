package neo.network.model;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.util.JsonUtil;

public class TimerData {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TimerData.class);

	private static final String RESPONSE_COMMAND = "response-command";

	private static final String RESPONSE_WAIT_TIMEOUT = "response-wait-timeout";

	private static final String SEND_INTERVAL = "send-interval";

	private final long waitTimeBeforeSendMs;

	private final long waitTimeBeforeResendMs;

	private final String responseCommand;

	private long lastSentTimeMs = 0;

	private boolean waitingForResponse = false;

	private final String logLabel;

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

	public String getResponseCommand() {
		return responseCommand;
	}

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
				LOG.debug("SUCCESS isReadyForSend {} true", logLabel);
				return true;
			}
		} else {
			final long sendTimeMs = lastSentTimeMs + waitTimeBeforeSendMs;
			LOG.trace("INTERIM isReadyForSend sendTimeMs:{};", sendTimeMs);
			LOG.trace("INTERIM isReadyForSend  (sendTimeMs < currentTimeMillis):{};", (sendTimeMs < currentTimeMillis));
			if (sendTimeMs < currentTimeMillis) {
				LOG.debug("SUCCESS isReadyForSend {} true", logLabel);
				return true;
			}
		}
		LOG.debug("SUCCESS isReadyForSend {} false", logLabel);
		return false;
	}

	public void requestSent() {
		lastSentTimeMs = System.currentTimeMillis();
		waitingForResponse = true;
	}

	public void responseReceived() {
		LOG.debug("STARTED responseReceived");
		waitingForResponse = false;
		LOG.debug("SUCCESS responseReceived");
	}

}
