package neo.model.core;

public class ValueAndTimestamp<T extends Comparable<T>> implements Comparable<ValueAndTimestamp<T>> {

	private final T value;

	private final Long timestamp;

	public ValueAndTimestamp(final T value, final Long timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(final ValueAndTimestamp<T> that) {
		if ((this.timestamp == null) || (that.timestamp == null)) {
			if ((this.timestamp == null) && (that.timestamp != null)) {
				return -1;
			}
			if ((this.timestamp != null) && (that.timestamp == null)) {
				return 1;
			}
		} else {
			final int tsCompare = this.timestamp.compareTo(that.timestamp);
			if (tsCompare != 0) {
				return tsCompare;
			}
		}
		final int valueCompare = this.value.compareTo(that.value);
		if (valueCompare != 0) {
			return valueCompare;
		}
		return 0;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public T getValue() {
		return value;
	}

}
