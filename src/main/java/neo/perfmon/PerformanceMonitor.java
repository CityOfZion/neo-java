package neo.perfmon;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.util.MapUtil;
import neo.network.model.LocalNodeData;

/**
 * the performance monitor.
 *
 * @author coranos
 *
 */
public final class PerformanceMonitor implements AutoCloseable {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceMonitor.class);

	/**
	 * the start time.
	 */
	private final long startTime;

	/**
	 * the name.
	 */
	private final String name;

	/**
	 * the name for logging total milliseconds used.
	 */
	private final String totalMillisName;

	/**
	 * the name for logging average milliseconds used.
	 */
	private final String averageMillisName;

	/**
	 * the constructor.
	 *
	 * @param name
	 *            the name of the monitor.
	 */
	public PerformanceMonitor(final String name) {
		startTime = System.currentTimeMillis();
		this.name = name;
		totalMillisName = name + "TotalMillis";
		averageMillisName = name + "AverageMillis";
		LOG.debug("STARTED {}", name);
	}

	@Override
	public void close() {
		final long measurement = System.currentTimeMillis() - startTime;
		MapUtil.increment(LocalNodeData.API_CALL_MAP, totalMillisName, measurement);
		MapUtil.increment(LocalNodeData.API_CALL_MAP, name);
		final long averageMillis = LocalNodeData.API_CALL_MAP.get(totalMillisName)
				/ LocalNodeData.API_CALL_MAP.get(name);
		LocalNodeData.API_CALL_MAP.put(averageMillisName, averageMillis);
		LOG.debug("SUCCESS {}, {} ms", name, NumberFormat.getIntegerInstance().format(measurement));
	}

}
