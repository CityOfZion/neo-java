package neo.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.main.ui.RemotePeerDataModel;
import neo.main.ui.StatsModel;
import neo.network.LocalControllerNode;
import neo.network.LocalNodeDataSynchronizedUtil;

/**
 * The main class for the neo-java application.
 *
 */
public final class NeoMain {

	/**
	 * the name of the config file.
	 */
	private static final File CONFIG_FILE = new File("config.json");

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(NeoMain.class);

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
		final int nonce = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
		final JSONObject controllerNodeConfig = new JSONObject(
				FileUtils.readFileToString(CONFIG_FILE, Charset.defaultCharset()));
		controllerNodeConfig.put(LocalControllerNode.NONCE, nonce);

		LOG.info("INTERIM config.length : {}", controllerNodeConfig.length());
		final LocalControllerNode controller = new LocalControllerNode(controllerNodeConfig);
		controller.loadNodeFile();

		final StatsModel statsTableModel = new StatsModel();
		final RemotePeerDataModel peerTableModel = new RemotePeerDataModel();

		final JFrame frame = new JFrame("NEO Main");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent evt) {
				try {
					statsTableModel.stop();
					peerTableModel.stop();
					LOG.info("STARTED SHUTTING DOWN DB");
					controller.getLocalNodeData().getBlockDb().close();
					LOG.info("SUCCESS SHUTTING DOWN DB");
				} catch (final SQLException | InterruptedException ex) {
					LOG.error("error closing", ex);
				}
				System.exit(0);
			}
		});
		final JPanel mainPanel = new JPanel();
		final JTabbedPane tabbedPane = new JTabbedPane();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.add(tabbedPane);
		{
			controller.addPeerChangeListener(statsTableModel);
			final JTable table = new JTable(statsTableModel);
			final JScrollPane scrollPane = new JScrollPane(table);
			tabbedPane.add("Connection Stats", scrollPane);
		}
		{
			controller.addPeerChangeListener(peerTableModel);
			final JTable table = new JTable(peerTableModel);
			final JScrollPane scrollPane = new JScrollPane(table);
			tabbedPane.add("Connection Details", scrollPane);
		}
		{
			final JPanel panel = new JPanel();
			{
				final JButton button = new JButton("Compress Db");
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent evt) {
						LocalNodeDataSynchronizedUtil.compressDb(controller.getLocalNodeData());
					}
				});
				panel.add(button);
			}

			tabbedPane.add("Controls", panel);
		}

		frame.getContentPane().add(mainPanel);
		frame.setSize(512, 1024);
		frame.setVisible(true);

		controller.startThreadPool();
		controller.startRefreshThread();

		LOG.info("SUCCESS main");
	}

	/**
	 * the constructor.
	 */
	private NeoMain() {
	}
}
