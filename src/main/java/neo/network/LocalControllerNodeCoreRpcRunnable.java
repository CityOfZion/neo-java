package neo.network;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import neo.model.util.InputStreamUtil;
import neo.rpc.server.RpcServerUtil;

/**
 * the Runnable responsible for handling the CoreRpc server listener thread.
 *
 * @author coranos
 *
 */
public final class LocalControllerNodeCoreRpcRunnable implements Runnable {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(LocalControllerNodeCoreRpcRunnable.class);

	/**
	 * proesses the request from the given session.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param session
	 *            the session to use.
	 * @return the response.
	 */
	private static String processRequest(final LocalControllerNode controller, final IHTTPSession session) {
		try {
			final long readTimeOut = controller.getLocalNodeData().getRpcClientTimeoutMillis();
			final InputStream in = session.getInputStream();
			if (LOG.isDebugEnabled()) {
				LOG.debug("host:{};headers:{}", session.getRemoteHostName(), session.getHeaders());
			}
			final int contentLength = Integer.valueOf(session.getHeaders().get("content-length"));
			final byte[] ba = new byte[contentLength];
			InputStreamUtil.readUntilFull(readTimeOut, in, ba);
			final String requestStr = new String(ba);

			if (LOG.isDebugEnabled()) {
				LOG.debug("host:{};request:{}", session.getRemoteHostName(), requestStr);
			}
			final JSONObject response = RpcServerUtil.process(controller, session.getUri(), requestStr);
			final String responseStr = response.toString();
			if (LOG.isDebugEnabled()) {
				LOG.debug("host:{};response:{}", session.getRemoteHostName(), responseStr);
			}
			return responseStr;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * if true, stop running.
	 */
	private final boolean stopped = false;

	/**
	 * if true, socket has started listening.
	 */
	private boolean started = false;

	/**
	 * the local controller node.
	 */
	private final LocalControllerNode localControllerNode;

	/**
	 * the server socket.
	 */
	private final NanoHTTPD httpServer;

	/**
	 * the constructor.
	 *
	 * @param localControllerNode
	 *            the local controller node to use.
	 */
	public LocalControllerNodeCoreRpcRunnable(final LocalControllerNode localControllerNode) {
		this.localControllerNode = localControllerNode;
		httpServer = new NanoHTTPD(localControllerNode.getLocalNodeData().getRpcPort()) {
			@Override
			public Response serve(final IHTTPSession session) {
				return newFixedLengthResponse(processRequest(localControllerNode, session));
			}
		};
	}

	/**
	 * return true if server is up and ready for connections.
	 *
	 * @return true if server is up and ready for connections.
	 */
	public boolean isStarted() {
		return started;
	}

	/**
	 * return true if stopped.
	 *
	 * @return true if stopped.
	 */
	public boolean isStopped() {
		return stopped;
	}

	@Override
	public void run() {
		try {
			final long timeout = localControllerNode.getLocalNodeData().getRpcClientTimeoutMillis();
			httpServer.start((int) timeout);
			started = true;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * stop the server.
	 */
	public void stop() {
		httpServer.stop();
	}
}
