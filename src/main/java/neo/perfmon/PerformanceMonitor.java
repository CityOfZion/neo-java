package neo.perfmon;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

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
	 * the API Call map, used to track call stats.
	 */
	public static final Map<String, Long> PERF_DATA_MAP = Collections.synchronizedMap(new TreeMap<>());

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
		MapUtil.increment(PERF_DATA_MAP, totalMillisName, measurement);
		MapUtil.increment(PERF_DATA_MAP, name);
		final long count = PERF_DATA_MAP.get(name);
		final long averageMillis = PERF_DATA_MAP.get(totalMillisName) / count;
		LocalNodeData.API_CALL_MAP.put(name, count);
		LocalNodeData.API_CALL_MAP.put(averageMillisName, Math.max(1, averageMillis));
		LOG.debug("SUCCESS {}, {} ms", name, NumberFormat.getIntegerInstance().format(measurement));
	}

}
