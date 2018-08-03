package neo.rpc.client.test.local;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.time.FastDateFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.db.BlockDb;
import neo.model.util.ConfigurationUtil;
import neo.model.util.MapUtil;
import neo.model.util.ModelUtil;
import neo.network.LocalControllerNode;
import neo.network.model.LocalNodeData;

/**
 * tests the RPC server.
 *
 * @author coranos
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAccountPng {

	private static final int HEIGHT_SUBPIXELS = 8;

	private static final int HEIGHT = 1080 * 2;

	private static final int WIDTH = 1920 * 2;

	/**
	 * the integer format.
	 */
	private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance();

	/**
	 * the date format.
	 */
	private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy.MM.dd");

	/**
	 * the controller.
	 */
	private static final LocalControllerNode CONTROLLER;

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(TestAccountPng.class);

	static {
		final JSONObject controllerNodeConfig = ConfigurationUtil.getConfiguration();
		CONTROLLER = new LocalControllerNode(controllerNodeConfig);
	}

	/**
	 * method for after class disposal.
	 */
	@AfterClass
	public static void afterClass() {
		LOG.debug("afterClass");
	}

	/**
	 * method for before class setup.
	 */
	@BeforeClass
	public static void beforeClass() {
		LOG.debug("beforeClass");
	}

	/**
	 * first test, blank, so beforeClass() time doesnt throw off the metrics.
	 */
	@Test
	public void aaaFirstTest() {
	}

	public int getScaleLength(final Map<UInt160, Fixed8> accountNeoValueMap, final UInt160 account, final long total) {
		final long value = accountNeoValueMap.get(account).value;
		final double scaleValue = (value * 1.0) / total;
		final int scaleLength = (int) (scaleValue * HEIGHT * HEIGHT_SUBPIXELS);
		return scaleLength;
	}

	@Test
	public void test001GetAccountSankey() throws JSONException, IOException {
		LOG.debug("STARTED png");
		final LocalNodeData localNodeData = CONTROLLER.getLocalNodeData();
		final BlockDb blockDb = localNodeData.getBlockDb();
		final long maxIndex = blockDb.getHeaderOfBlockWithMaxIndex().getIndexAsLong();

		final CoinData neoData = new CoinData();
		final CoinData gasData = new CoinData();

		final Map<UInt256, CoinData> coinDataMap = new TreeMap<>();
		coinDataMap.put(ModelUtil.NEO_HASH, neoData);
		coinDataMap.put(ModelUtil.GAS_HASH, gasData);

		final File dir = new File("/Users/dps/git-coranos.github.io/neo/charts");
		final Map<UInt256, File> coinFileMap = new TreeMap<>();
		coinFileMap.put(ModelUtil.NEO_HASH, new File(dir, "neo-account.png"));
		coinFileMap.put(ModelUtil.GAS_HASH, new File(dir, "gas-account.png"));

		long startMs = -1;
		for (long blockIx = 0; blockIx <= maxIndex; blockIx++) {
			LOG.debug("STARTED png {} of {} ", blockIx, maxIndex);
			final Block block = blockDb.getFullBlockFromHeight(blockIx);

			for (final Transaction t : block.getTransactionList()) {
				// update assets based on tx inputs.
				for (final CoinReference cr : t.inputs) {
					final TransactionOutput ti = ModelUtil.getTransactionOutput(blockDb, cr);
					if (coinDataMap.containsKey(ti.assetId)) {
						final CoinData coinData = coinDataMap.get(ti.assetId);
						final UInt160 input = ti.scriptHash;
						if (!coinData.accountValueMap.containsKey(input)) {
							coinData.accountValueMap.put(input, ModelUtil.FIXED8_ZERO);
							coinData.newAccountSet.add(input);
						}
						final Fixed8 oldValue = coinData.accountValueMap.get(input);
						final Fixed8 newValue = ModelUtil.subtract(ti.value, oldValue);

						if (newValue.equals(ModelUtil.FIXED8_ZERO)) {
							coinData.accountValueMap.remove(input);
						} else {
							coinData.accountValueMap.put(input, newValue);
						}
					}
				}

				// update assets based on tx outputs.
				for (int outputIx = 0; outputIx < t.outputs.size(); outputIx++) {
					final TransactionOutput to = t.outputs.get(outputIx);
					final UInt160 output = to.scriptHash;
					if (coinDataMap.containsKey(to.assetId)) {
						final CoinData coinData = coinDataMap.get(to.assetId);
						coinData.txAccountSet.add(output);
						if (!coinData.accountValueMap.containsKey(output)) {
							coinData.accountValueMap.put(output, ModelUtil.FIXED8_ZERO);
							coinData.newAccountSet.add(output);
						}
						final Fixed8 oldValue = coinData.accountValueMap.get(output);
						final Fixed8 newValue = ModelUtil.add(oldValue, to.value);
						if (newValue.equals(ModelUtil.FIXED8_ZERO)) {
							coinData.accountValueMap.remove(output);
						} else {
							coinData.accountValueMap.put(output, newValue);
						}
					}
				}

				for (final UInt256 assetId : coinDataMap.keySet()) {
					final CoinData coinData = coinDataMap.get(assetId);
					for (final UInt160 txAccount : coinData.txAccountSet) {
						MapUtil.increment(coinData.newAccountTxCountMap, txAccount);
					}
					coinData.txAccountSet.clear();
				}
			}

			final Timestamp blockTs = block.getTimestamp();
			if (startMs < 0) {
				startMs = blockTs.getTime();
			}
			final long ms = blockTs.getTime() - startMs;
			if (ms > (86400 * 1000)) {
				final String targetDateStr = DATE_FORMAT.format(blockTs);

				for (final UInt256 assetId : coinDataMap.keySet()) {
					final CoinData coinData = coinDataMap.get(assetId);

					final List<UInt160> accountList = new ArrayList<>();
					accountList.addAll(coinData.accountValueMap.keySet());

					Collections.sort(accountList, new Comparator<UInt160>() {
						@Override
						public int compare(final UInt160 account1, final UInt160 account2) {
							final Long value1 = coinData.accountValueMap.get(account1).value;
							final Long value2 = coinData.accountValueMap.get(account2).value;
							return value1.compareTo(value2);
						}
					});
					Collections.reverse(accountList);

					long maxTx = 0;
					long totalValue = 0;
					{
						final List<DrawingData> drawingData = new ArrayList<>();
						coinData.drawingDataList.add(drawingData);
						int startDayPixel = 0;
						int visibleAccountCount = 0;
						for (final UInt160 account : accountList) {
							totalValue += coinData.accountValueMap.get(account).value;
						}
						for (final UInt160 account : accountList) {
							final int scaleLength = getScaleLength(coinData.accountValueMap, account, totalValue);
							if (scaleLength != 0) {
								visibleAccountCount++;
							}
							MapUtil.touch(coinData.newAccountTxCountMap, account);
							maxTx = Math.max(maxTx, coinData.newAccountTxCountMap.get(account));
						}
						final float greenStep = 1.0f / visibleAccountCount;
						final float redStep = 1.0f / maxTx;
						float green = 1.0f;
						for (final UInt160 account : accountList) {
							final int scaleLength = getScaleLength(coinData.accountValueMap, account, totalValue);
							if (scaleLength != 0) {
								final float blue;
								if (coinData.newAccountSet.contains(account)) {
									blue = 1.0f;
								} else {
									blue = 0.0f;
								}
								float red = 0.0f;
								if (coinData.newAccountTxCountMap.containsKey(account)) {
									red = redStep * coinData.newAccountTxCountMap.get(account);
								} else {
									red = 0.0f;
								}
								drawingData.add(new DrawingData(startDayPixel, scaleLength, red, green, blue));
							}

							green -= greenStep;
							startDayPixel += scaleLength;
						}
					}

					final BufferedImage im = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
					final Graphics2D g = (Graphics2D) im.getGraphics();
					g.scale((WIDTH * 1.0) / coinData.drawingDataList.size(), 1.0 / HEIGHT_SUBPIXELS);
					for (int x = 0; x < coinData.drawingDataList.size(); x++) {
						final List<DrawingData> drawingData = coinData.drawingDataList.get(x);
						for (final DrawingData drawing : drawingData) {
							g.setColor(new Color(drawing.r, drawing.g, drawing.b));
							g.fillRect(x, drawing.y, 1, drawing.h);
							g.setColor(Color.black);
							g.drawLine(x, drawing.y, x + 1, drawing.y);
						}
					}
					g.dispose();
					final File file = coinFileMap.get(assetId);
					ImageIO.write(im, "png", file);

					LOG.info("INTERIM file {}, {} of {}, date {}, accountList {}, tx {}, value {}", file.getName(),
							INTEGER_FORMAT.format(blockIx), INTEGER_FORMAT.format(maxIndex), targetDateStr,
							INTEGER_FORMAT.format(accountList.size()), INTEGER_FORMAT.format(maxTx),
							INTEGER_FORMAT.format(totalValue / ModelUtil.DECIMAL_DIVISOR));

					startMs = blockTs.getTime();
					coinData.newAccountSet.clear();
					coinData.newAccountTxCountMap.clear();
				}
			}
		}
		LOG.debug("SUCCESS png");
	}

	/**
	 * last test, blank, so afterClass() time doesnt throw off the metrics.
	 */
	@Test
	public void zzzLastTest() {
	}

	private static class CoinData {
		final Map<UInt160, Fixed8> accountValueMap = new TreeMap<>();
		final Map<UInt160, Long> newAccountTxCountMap = new TreeMap<>();
		final Set<UInt160> newAccountSet = new TreeSet<>();
		final List<List<DrawingData>> drawingDataList = new ArrayList<>();
		final Set<UInt160> txAccountSet = new TreeSet<>();
	}

	private static class DrawingData {

		private final int y;
		private final int h;
		private final float r;
		private final float g;
		private final float b;

		public DrawingData(final int y, final int h, final float r, final float g, final float b) {
			this.y = y;
			this.h = h;
			this.r = r;
			this.g = g;
			this.b = b;
		}

	}
}
