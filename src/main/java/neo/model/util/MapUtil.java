package neo.model.util;

import java.util.Map;

public class MapUtil {

	public static void increment(final Map<String, Long> destMap, final Map<String, Long> srcMap) {
		synchronized (srcMap) {
			for (final String key : srcMap.keySet()) {
				final long value = srcMap.get(key);
				increment(destMap, key, value);
			}
		}
	}

	public static long increment(final Map<String, Long> map, final String key) {
		return increment(map, key, 1L);
	}

	public static long increment(final Map<String, Long> map, final String key, final long amount) {
		if (map.containsKey(key)) {
			final long oldAmount = map.get(key);
			final long newAmount = oldAmount + amount;
			map.put(key, newAmount);
			return newAmount;
		} else {
			map.put(key, amount);
			return amount;
		}
	}
}
