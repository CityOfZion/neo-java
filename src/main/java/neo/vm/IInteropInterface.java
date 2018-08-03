package neo.vm;

/**
 * the internal-operations interface.
 *
 * @author coranos
 *
 */
public interface IInteropInterface extends Comparable<IInteropInterface> {

	@Override
	int compareTo(IInteropInterface object);
}
