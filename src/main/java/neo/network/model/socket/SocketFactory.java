package neo.network.model.socket;

/**
 * the socket factory.
 *
 * @author coranos
 *
 */
public interface SocketFactory {

	/**
	 * return the socket wrapper.
	 *
	 * @return the socket wrapper.
	 */
	SocketWrapper newSocketWrapper();

}
