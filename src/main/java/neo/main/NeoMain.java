package neo.main;

import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.export.BlockImportExportUtil;
import neo.main.ui.ApiCallModel;
import neo.main.ui.RemotePeerDataModel;
import neo.main.ui.StatsModel;
import neo.model.util.ConfigurationUtil;
import neo.model.util.GenesisBlockUtil;
import neo.network.LocalControllerNode;

/**
 * The main class for the neo-java application.
 */
public final class NeoMain {

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(NeoMain.class);

	/**
	 * adds the api statistics panel.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param apiTableModel
	 *            the table model to use.
	 * @param tabbedPane
	 *            the tabbed pane to use.
	 */
	private static void addApiStatsPanel(final LocalControllerNode controller, final ApiCallModel apiTableModel,
			final JTabbedPane tabbedPane) {
		controller.addPeerChangeListener(apiTableModel);
		final JTable table = new JTable(apiTableModel);
		final JScrollPane scrollPane = new JScrollPane(table);
		tabbedPane.add("API Stats", scrollPane);
	}

	/**
	 * adds the blockchain statistics panel.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param statsModel
	 *            the table model to use.
	 * @param tabbedPane
	 *            the tabbed pane to use.
	 */
	private static void addBlockchainStatsPanel(final LocalControllerNode controller, final StatsModel statsModel,
			final JTabbedPane tabbedPane) {
		controller.addPeerChangeListener(statsModel);
		final JTable table = new JTable(statsModel);
		final JScrollPane scrollPane = new JScrollPane(table);
		tabbedPane.add("Blockchain Stats", scrollPane);
	}

	/**
	 * adds the connection details panel.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param remotePeerDataModel
	 *            the table model to use.
	 * @param tabbedPane
	 *            the tabbed pane to use.
	 */
	private static void addRemotePeerDetailsPanel(final LocalControllerNode controller,
			final RemotePeerDataModel remotePeerDataModel, final JTabbedPane tabbedPane) {
		controller.addPeerChangeListener(remotePeerDataModel);
		final JTable table = new JTable(remotePeerDataModel);
		final JScrollPane scrollPane = new JScrollPane(table);
		tabbedPane.add("Remote Peer Details", scrollPane);
	}

	/**
	 * gets the window closing adapter.
	 *
	 * @param controller
	 *            the controller.
	 * @param statsModel
	 *            the stats table model.
	 * @param remotePeerDataModel
	 *            the peers table model.
	 * @param apiCallModel
	 *            the api call model.
	 * @return the WindowAdapter.
	 */
	private static WindowClosingAdapter getWindowClosingAdapter(final LocalControllerNode controller,
			final StatsModel statsModel, final RemotePeerDataModel remotePeerDataModel,
			final ApiCallModel apiCallModel) {
		return new WindowClosingAdapter(apiCallModel, remotePeerDataModel, statsModel, controller);
	}

	/**
	 * the main method.
	 *
	 * @param args
	 *            the application arguments (none currently).
	 * @throws Exception
	 *             if an error occurs.
	 */
	public static void main(final String[] args) throws Exception {
		LOG.info("STARTED main");
		LOG.info("INTERIM main os.arch:{};", System.getProperty("os.arch"));

		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		final LocalControllerNode controller = new LocalControllerNode(controllerNodeConfig);

		if (controller.getLocalNodeData().getBlockDb().getBlockCount() == 0) {
			LOG.info("DB is empty, addiing genesis block STARTED");
			controller.getLocalNodeData().getBlockDb().put(true, GenesisBlockUtil.GENESIS_BLOCK);
			LOG.info("DB is empty, addiing genesis block SUCCESS");
		}

		LOG.info("INTERIM main number of accounts:{};", controller.getLocalNodeData().getBlockDb().getAccountCount());

		for (final String arg : args) {
			if (arg.equals("/import")) {
				LOG.info("STARTED import");
				BlockImportExportUtil.importBlocks(controller);
				LOG.info("SUCCESS import");
			}
			if (arg.equals("/validate")) {
				LOG.info("STARTED validate");
				controller.getLocalNodeData().getBlockDb().validate();
				LOG.info("SUCCESS validate");
			}
			if (arg.equals("/export")) {
				LOG.info("STARTED export");
				BlockImportExportUtil.exportBlocks(controller);
				LOG.info("SUCCESS export");
			}
			if (arg.equals("/decapitate")) {
				LOG.info("STARTED decapitate");
				controller.getLocalNodeData().getBlockDb().deleteHighestBlock();
				LOG.info("SUCCESS decapitate");
			}
			if (arg.equals("/rpc")) {
				LOG.info("STARTED rpc");
				controller.startCoreRpcServer();
				LOG.info("SUCCESS rpc");
			}
		}

		final StatsModel statsModel = new StatsModel();
		final ApiCallModel apiCallModel = new ApiCallModel();
		final RemotePeerDataModel remotePeerDataModel = new RemotePeerDataModel();

		final JPanel mainPanel = new JPanel();
		final JTabbedPane tabbedPane = new JTabbedPane();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.add(tabbedPane);

		addBlockchainStatsPanel(controller, statsModel, tabbedPane);
		addRemotePeerDetailsPanel(controller, remotePeerDataModel, tabbedPane);
		addApiStatsPanel(controller, apiCallModel, tabbedPane);

		controller.startNodesInConfigFiles();
		controller.startThreadPool();

		if (!GraphicsEnvironment.isHeadless()) {
			final JFrame frame = new JFrame("NEO Main");
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			final WindowClosingAdapter windowClosingAdapter = getWindowClosingAdapter(controller, statsModel,
					remotePeerDataModel, apiCallModel);
			frame.addWindowListener(windowClosingAdapter);
			frame.getContentPane().add(mainPanel);
			frame.pack();
			frame.setSize(640, 960);
			frame.setVisible(true);
		}

		controller.startRefreshThread();

		LOG.info("SUCCESS main");
	}

	/**
	 * the constructor.
	 */
	private NeoMain() {
	}

	/**
	 * the adapter that handles the window closing event.
	 *
	 * @author coranos
	 *
	 */
	private static final class WindowClosingAdapter extends WindowAdapter {

		/**
		 * the api call model.
		 */
		private final ApiCallModel apiCallModel;

		/**
		 * the remote peer data model.
		 */
		private final RemotePeerDataModel remotePeerDataModel;

		/**
		 * the stats model.
		 */
		private final StatsModel statsModel;

		/**
		 * the controller.
		 */
		private final LocalControllerNode controller;

		/**
		 * the constructor.
		 *
		 * @param apiCallModel
		 *            the api call model.
		 * @param remotePeerDataModel
		 *            the remote peer data model.
		 * @param statsModel
		 *            the stats model.
		 * @param controller
		 *            the controller.
		 */
		private WindowClosingAdapter(final ApiCallModel apiCallModel, final RemotePeerDataModel remotePeerDataModel,
				final StatsModel statsModel, final LocalControllerNode controller) {
			this.apiCallModel = apiCallModel;
			this.remotePeerDataModel = remotePeerDataModel;
			this.statsModel = statsModel;
			this.controller = controller;
		}

		/**
		 * shuts down and exits.
		 */
		private void shutdown() {
			try {
				LOG.info("STARTED SHUTTING DOWN");
				LOG.info("STARTED SHUTTING DOWN GUI REFRESH");
				statsModel.stop();
				remotePeerDataModel.stop();
				apiCallModel.stop();
				LOG.info("SUCCESS SHUTTING DOWN GUI REFRESH");
				LOG.info("STARTED SHUTTING DOWN NETWORK");
				controller.stop();
				LOG.info("SUCCESS SHUTTING DOWN NETWORK");
				LOG.info("STARTED SHUTTING DOWN DATABASE");
				controller.getLocalNodeData().getBlockDb().close();
				LOG.info("SUCCESS SHUTTING DOWN DATABASE");
				LOG.info("SUCCESS SHUTTING DOWN");
			} catch (final InterruptedException ex) {
				LOG.error("error closing", ex);
			}
		}

		@Override
		public void windowClosing(final WindowEvent evt) {
			shutdown();
			System.exit(0);
		}

	}
}
