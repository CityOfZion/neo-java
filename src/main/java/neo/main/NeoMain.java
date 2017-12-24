package neo.main;

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

import neo.main.ui.ApiCallModel;
import neo.main.ui.RemotePeerDataModel;
import neo.main.ui.StatsModel;
import neo.model.util.ConfigurationUtil;
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
	 * @param statsTableModel
	 *            the table model to use.
	 * @param tabbedPane
	 *            the tabbed pane to use.
	 */
	private static void addBlockchainStatsPanel(final LocalControllerNode controller, final StatsModel statsTableModel,
			final JTabbedPane tabbedPane) {
		controller.addPeerChangeListener(statsTableModel);
		final JTable table = new JTable(statsTableModel);
		final JScrollPane scrollPane = new JScrollPane(table);
		tabbedPane.add("Blockchain Stats", scrollPane);
	}

	/**
	 * adds the connection details panel.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param peerTableModel
	 *            the table model to use.
	 * @param tabbedPane
	 *            the tabbed pane to use.
	 */
	private static void addRemotePeerDetailsPanel(final LocalControllerNode controller,
			final RemotePeerDataModel peerTableModel, final JTabbedPane tabbedPane) {
		controller.addPeerChangeListener(peerTableModel);
		final JTable table = new JTable(peerTableModel);
		final JScrollPane scrollPane = new JScrollPane(table);
		tabbedPane.add("Remote Peer Details", scrollPane);
	}

	/**
	 * gets the window closing adapter.
	 *
	 * @param controller
	 *            the controller.
	 * @param statsTableModel
	 *            the stats table model.
	 * @param peerTableModel
	 *            the peers table model.
	 * @param apiCallModel
	 *            the api call model.
	 * @return the WindowAdapter.
	 */
	private static WindowAdapter getWindowClosingAdapter(final LocalControllerNode controller,
			final StatsModel statsTableModel, final RemotePeerDataModel peerTableModel,
			final ApiCallModel apiCallModel) {
		return new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent evt) {
				try {
					statsTableModel.stop();
					peerTableModel.stop();
					apiCallModel.stop();
					controller.stop();
					LOG.info("STARTED SHUTTING DOWN DB");
					controller.getLocalNodeData().getBlockDb().close();
					LOG.info("SUCCESS SHUTTING DOWN DB");
				} catch (final InterruptedException ex) {
					LOG.error("error closing", ex);
				}
				System.exit(0);
			}
		};
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
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		final LocalControllerNode controller = new LocalControllerNode(controllerNodeConfig);
		controller.loadNodeFiles();

		final StatsModel statsTableModel = new StatsModel();
		final ApiCallModel apiCallModel = new ApiCallModel();
		final RemotePeerDataModel peerTableModel = new RemotePeerDataModel();

		final JFrame frame = new JFrame("NEO Main");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(getWindowClosingAdapter(controller, statsTableModel, peerTableModel, apiCallModel));
		final JPanel mainPanel = new JPanel();
		final JTabbedPane tabbedPane = new JTabbedPane();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.add(tabbedPane);

		addBlockchainStatsPanel(controller, statsTableModel, tabbedPane);
		addRemotePeerDetailsPanel(controller, peerTableModel, tabbedPane);
		addApiStatsPanel(controller, apiCallModel, tabbedPane);

		frame.getContentPane().add(mainPanel);

		controller.startThreadPool();
		controller.startRefreshThread();

		frame.setSize(480, 960);
		frame.setVisible(true);

		LOG.info("SUCCESS main");
	}

	/**
	 * the constructor.
	 */
	private NeoMain() {
	}
}
