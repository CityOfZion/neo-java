package neo.network.model.socket;

/**
 * the socket factory.
 *
 * @author coranos
 */
public final class SocketFactoryImpl implements SocketFactory {

	@Override
	public SocketWrapper newSocketWrapper() {
		return new SocketWrapperImpl();
	}

}
