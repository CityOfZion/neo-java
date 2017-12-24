package neo.perfmon;

import java.text.NumberFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spf4j.perf.MeasurementRecorderSource;
import org.spf4j.perf.impl.RecorderFactory;

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
	 * the performance monitor recorder.
	 */
	private static final MeasurementRecorderSource RECORDER = RecorderFactory
			.createScalableQuantizedRecorderSource("response time", "ms", 60000, 10, -3, 3, 5);

	/**
	 * the start time.
	 */
	private final long startTime;

	/**
	 * the name.
	 */
	private final String name;

	/**
	 * the api logging name.
	 */
	private final String apiLogName;

	/**
	 * the constructor.
	 *
	 * @param name
	 *            the name of the monitor.
	 */
	public PerformanceMonitor(final String name) {
		startTime = System.currentTimeMillis();
		this.name = name;
		apiLogName = name + "Millis";
		LOG.debug("STARTED {}", name);
	}

	@Override
	public void close() {
		final long measurement = System.currentTimeMillis() - startTime;
		RECORDER.getRecorder(name).recordAt(startTime, measurement);
		MapUtil.increment(LocalNodeData.API_CALL_MAP, apiLogName, measurement);
		MapUtil.increment(LocalNodeData.API_CALL_MAP, name, measurement);
		LOG.debug("SUCCESS {}, {} ms", name, NumberFormat.getIntegerInstance().format(measurement));
	}

}
