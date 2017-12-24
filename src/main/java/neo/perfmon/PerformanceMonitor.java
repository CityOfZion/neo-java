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
	 * the api logging name.
	 */
	private final String millisName;

	/**
	 * the constructor.
	 *
	 * @param name
	 *            the name of the monitor.
	 */
	public PerformanceMonitor(final String name) {
		startTime = System.currentTimeMillis();
		this.name = name;
		millisName = name + "Millis";
		LOG.debug("STARTED {}", name);
	}

	@Override
	public void close() {
		final long measurement = System.currentTimeMillis() - startTime;
		MapUtil.increment(LocalNodeData.API_CALL_MAP, millisName, measurement);
		MapUtil.increment(LocalNodeData.API_CALL_MAP, name);
		LOG.debug("SUCCESS {}, {} ms", name, NumberFormat.getIntegerInstance().format(measurement));
	}

}
