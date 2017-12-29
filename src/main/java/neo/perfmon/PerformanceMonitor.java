package neo.perfmon;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections4.BoundedCollection;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public static final Map<String, BoundedCollection<Long>> PERF_DATA_MAP = Collections
			.synchronizedMap(new TreeMap<>());

	/**
	 * max history of performance for stats.
	 */
	public static final int MAX_PERF_DATA_HISTORY = 10000;

	/**
	 * increments a value in the map.
	 *
	 * @param key
	 *            the key to use.
	 * @param amount
	 *            the amount to use.
	 */
	private static void addToPerfDataSumMap(final String key, final long amount) {
		synchronized (PERF_DATA_MAP) {
			if (!PERF_DATA_MAP.containsKey(key)) {
				PERF_DATA_MAP.put(key, new CircularFifoQueue<>(MAX_PERF_DATA_HISTORY));
			}
			final BoundedCollection<Long> values = PERF_DATA_MAP.get(key);
			values.add(amount);
		}
	}

	/**
	 * increments a value in the map.
	 *
	 * @param key
	 *            the key to use.
	 * @return the sum.
	 */
	private static long getSum(final String key) {
		synchronized (PERF_DATA_MAP) {
			final BoundedCollection<Long> values = PERF_DATA_MAP.get(key);
			long sum = 0;
			for (final long value : values) {
				sum += value;
			}
			return sum;
		}
	}

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
		addToPerfDataSumMap(totalMillisName, measurement);
		addToPerfDataSumMap(name, 1L);
		final long count = getSum(name);
		final long averageMillis = getSum(totalMillisName) / count;
		LocalNodeData.API_CALL_MAP.put(name, count);
		LocalNodeData.API_CALL_MAP.put(averageMillisName, Math.max(1, averageMillis));
		LOG.debug("SUCCESS {}, {} ms", name, NumberFormat.getIntegerInstance().format(measurement));
	}

}
