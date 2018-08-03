package neo.rpc.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Hex;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import neo.model.bytes.Fixed8;
import neo.model.bytes.UInt160;
import neo.model.bytes.UInt256;
import neo.model.core.Block;
import neo.model.core.CoinReference;
import neo.model.core.Transaction;
import neo.model.core.TransactionOutput;
import neo.model.core.TransactionType;
import neo.model.db.BlockDb;
import neo.model.util.MapUtil;
import neo.model.util.ModelUtil;
import neo.network.LocalControllerNode;
import neo.network.model.LocalNodeData;
import neo.network.model.NodeConnectionPhaseEnum;
import neo.network.model.RemoteNodeData;

/**
 * examples.
 *
 * http://cityofzion.io/neon-js/api/index.html
 * <p>
 * https://github.com/CityOfZion/neon-wallet-db/blob/042d2d00c4fb1a657e2268280c46fb900b4645ce/README.md
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/address/balance/ANrL4vPnQCCi5Mro4fqKK1rxrkxEHqmp2E
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/address/history/ALpwWoxKLwbfCTkRpK2iXrXpaMHgWGcrDV
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v2/transaction/ec4dc0092d5adf8cdf30eadf5116dbb6f138b2e35ca2f1a26d992d69388e0b95
 * <p>
 * curl
 * http://testnet-api.neonwallet.com/v1/address/claims/AJ3yzTLc5jebUskHtphKi1rb2FNoZjbpkz
 * <p>
 * curl http://api.neonwallet.com/v1/network/nodes
 * <p>
 *
 * @author coranos
 *
 *         TODO: rename CoZ API to REST api, as it's REST vs RPC not CORE vs
 *         COZ.
 *
 *         TODO: rename account to address.
 */
public final class RpcServerUtil {

	/**
	 * the JSON key, "sysfee".
	 */
	private static final String SYSFEE = "sysfee";

	/**
	 * the JSON key, "start".
	 */
	private static final String START = "start";

	/**
	 * the JSON key, "end".
	 */
	private static final String END = "end";

	/**
	 * the JSON key, "claims".
	 */
	private static final String CLAIMS = "claims";

	/**
	 * the JSON key, "value".
	 */
	private static final String VALUE = "value";

	/**
	 * the JSON key, "index".
	 */
	private static final String INDEX = "index";

	/**
	 * the JSON key, "txid".
	 */
	private static final String TXID = "txid";

	/**
	 * the JSON key, "address".
	 */
	private static final String ADDRESS = "address";

	/**
	 * the JSON key, "history".
	 */
	private static final String HISTORY = "history";

	/**
	 * the JSON key, "unspent".
	 */
	private static final String UNSPENT = "unspent";

	/**
	 * the JSON key, "net".
	 */
	private static final String NET = "net";

	/**
	 * the JSON key, "NEO".
	 */
	private static final String NEO = "NEO";

	/**
	 * the JSON key, "GAS".
	 */
	private static final String GAS = "GAS";

	/**
	 * the JSON key, "balance".
	 */
	private static final String BALANCE = "balance";

	/**
	 * first timestamp.
	 */
	private static final String FIRST_TS = "first_ts";

	/**
	 * last timestamp.
	 */
	private static final String LAST_TS = "last_ts";

	/**
	 * gas transaction.
	 */
	private static final String GAS_TX = "gas_tx";

	/**
	 * neo transaction.
	 */
	private static final String NEO_TX = "neo_tx";

	/**
	 * gas out.
	 */
	private static final String GAS_OUT = "gas_out";

	/**
	 * gas in.
	 */
	private static final String GAS_IN = "gas_in";

	/**
	 * neo out.
	 */
	private static final String NEO_OUT = "neo_out";

	/**
	 * neo in.
	 */
	private static final String NEO_IN = "neo_in";

	/**
	 * the logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(RpcServerUtil.class);

	/**
	 * error, no blocks in block chain.
	 */
	private static final String ERROR_NO_BLOCKS_IN_BLOCKCHAIN = "no blocks in blockchain";

	/**
	 * a generic expected value for a hex string "0x...".
	 */
	private static final String EXPECTED_GENERIC_HEX = "0x...";

	/**
	 * the string "null".
	 */
	private static final String NULL = "null";

	/**
	 * the response tag for the result.
	 */
	private static final String RESULT = "result";

	/**
	 * the response tag for an error, for the actual value of a field.
	 */
	private static final String ACTUAL = "actual";

	/**
	 * the response tag for an error, for the expected value of a field.
	 */
	private static final String EXPECTED = "expected";

	/**
	 * the error response tag.
	 */
	private static final String ERROR = "error";

	/**
	 * the ID request tag.
	 */
	public static final String ID = "id";

	/**
	 * the parameters tag..
	 */
	public static final String PARAMS = "params";

	/**
	 * the jsonrpc version 2.0.
	 */
	public static final String VERSION_2_0 = "2.0";

	/**
	 * the JSON RPC version request tag.
	 */
	public static final String JSONRPC = "jsonrpc";

	/**
	 * the method request tag.
	 */
	public static final String METHOD = "method";

	/**
	 * gas generation amount.
	 */
	private static final long[] GENERATION_AMOUNT = new long[] { 8, 7, 6, 5, 4, 3, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1 };

	/**
	 * gas generation length.
	 */
	private static final long GENERATION_LENGTH = 22;

	/**
	 * gas generation decrement interval.
	 */
	private static final long DECREMENT_INTERVAL = 2000000;

	/**
	 * calculates the bonus for the given claim.
	 *
	 * @param claim
	 *            the claim to use.
	 * @return the bonus.
	 */
	private static long calculateBonus(final JSONObject claim) {
		long amountClaimed = 0;
		final long startHeight = claim.getLong(START);
		final long endHeight = claim.getLong(END);
		long amount = 0;
		long ustart = startHeight / DECREMENT_INTERVAL;
		if (ustart < GENERATION_LENGTH) {
			long istart = startHeight % DECREMENT_INTERVAL;
			long uend = endHeight / DECREMENT_INTERVAL;
			long iend = endHeight % DECREMENT_INTERVAL;
			if (uend >= GENERATION_LENGTH) {
				uend = GENERATION_LENGTH;
				iend = 0;
			}
			if (iend == 0) {
				uend = uend - 1;
				iend = DECREMENT_INTERVAL;
			}
			while (ustart < uend) {
				amount += (DECREMENT_INTERVAL - istart) * GENERATION_AMOUNT[(int) ustart];
				ustart += 1;
				istart = 0;
			}

			amount += (iend - istart) * GENERATION_AMOUNT[(int) ustart];
		}

		amount += claim.getLong(SYSFEE);
		amountClaimed += claim.getInt(VALUE) * amount;
		return amountClaimed;
	}

	/**
	 * calculates the system fee for the given blocks.
	 *
	 * @param systemFeeMap
	 *            the map of system fees by transaction type.
	 *
	 * @param blockDb
	 *            the block database.
	 * @param startBlockIx
	 *            the start block index.
	 *
	 * @param endBlockIx
	 *            the end block index.
	 *
	 * @return the system fee.
	 */
	private static long computeSysFee(final Map<TransactionType, Fixed8> systemFeeMap, final BlockDb blockDb,
			final long startBlockIx, final long endBlockIx) {
		long sysFee = 0;

		for (long blockIx = startBlockIx; blockIx <= endBlockIx; blockIx++) {
			final Block block = blockDb.getFullBlockFromHeight(blockIx);
			for (final Transaction tx : block.getTransactionList()) {
				sysFee += systemFeeMap.get(tx.type).value;
			}
		}
		return sysFee;
	}

	/**
	 * returns the address asset map.
	 *
	 * @param blockDb
	 *            the block database to use.
	 * @param transaction
	 *            the transaction to use.
	 * @return the address asset map.
	 */
	public static Map<UInt160, Map<UInt256, Long>> getAddressAssetMap(final BlockDb blockDb,
			final Transaction transaction) {
		final Map<UInt160, Map<UInt256, Long>> friendAssetMap = new TreeMap<>();

		for (final CoinReference cr : transaction.inputs) {
			final UInt256 prevHashReversed = cr.prevHash.reverse();
			final Transaction tiTx = blockDb.getTransactionWithHash(prevHashReversed);

			if (tiTx == null) {
				throw new RuntimeException("no transaction with prevHash:" + prevHashReversed);
			}

			final TransactionOutput ti = tiTx.outputs.get(cr.prevIndex.asInt());
			final UInt160 input = ti.scriptHash;
			if ((ti.assetId.equals(ModelUtil.NEO_HASH)) || (ti.assetId.equals(ModelUtil.GAS_HASH))) {
				MapUtil.increment(friendAssetMap, input, ti.assetId, ti.value.value, TreeMap.class);
			}
		}

		for (final TransactionOutput to : transaction.outputs) {
			final UInt160 output = to.scriptHash;
			if ((to.assetId.equals(ModelUtil.NEO_HASH)) || (to.assetId.equals(ModelUtil.GAS_HASH))) {
				MapUtil.increment(friendAssetMap, output, to.assetId, -to.value.value, TreeMap.class);
			}
		}
		return friendAssetMap;
	}

	/**
	 * finds a block with a given timestamp.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param level
	 *            the level at which we are searching.
	 * @param minHeight
	 *            the min height to find.
	 * @param maxHeight
	 *            the max height to find.
	 * @param ts
	 *            the timestamp to find.
	 * @return the block height closest to the given timestamp.
	 */
	private static long getHeightOfTs(final LocalControllerNode controller, final long level, final long minHeight,
			final long maxHeight, final long ts) {
		final long midHeight = minHeight + ((maxHeight - minHeight) / 2);
		if ((midHeight == minHeight) || (midHeight == maxHeight)) {
			return minHeight;
		}
		final Block midBlock = controller.getLocalNodeData().getBlockDb().getHeaderOfBlockFromHeight(midHeight);
		if (midBlock == null) {
			LOG.trace("getHeightOfTs[null]level:{};minHeight:{};midHeight:{};", level, minHeight, midHeight);
			return getHeightOfTs(controller, level + 1, minHeight, maxHeight - 1, ts);
		}
		final long midBlockTs = midBlock.timestamp.asLong();
		if (ts == midBlockTs) {
			return midHeight;
		} else if (ts < midBlockTs) {
			LOG.trace("getHeightOfTs[upper]level:{};minHeight:{};midHeight:{};midBlock.Timestamp:{};", level, minHeight,
					midHeight, midBlock.timestamp);
			return getHeightOfTs(controller, level + 1, minHeight, midHeight, ts);
		} else {
			LOG.trace("getHeightOfTs[lower]level:{};minHeight:{};midHeight:{};midBlock.Timestamp:{};", level, midHeight,
					maxHeight, midBlock.timestamp);
			return getHeightOfTs(controller, level + 1, midHeight, maxHeight, ts);
		}
	}

	/**
	 * returns the account list for accounts that were active between the given
	 * timestamps.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the list of account data.
	 */
	private static JSONObject onGetAccountList(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		try {
			LOG.trace("getaccountlist 0");

			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();
			final long fromTs = params.getLong(0);
			final long toTs = params.getLong(1);
			final long minHeight = 0;
			final long maxHeight = blockDb.getBlockCount();
			final long fromHeight = getHeightOfTs(controller, 0, minHeight, maxHeight, fromTs);
			final long toHeight = getHeightOfTs(controller, 0, fromHeight, maxHeight, toTs);

			LOG.trace("getaccountlist 1 fromHeight:{};toHeight:{};", fromHeight, toHeight);

			LOG.trace("getaccountlist 2 accountStateCache STARTED");

			final Map<UInt160, Map<UInt256, Fixed8>> addressStateCache = blockDb.getAccountAssetValueMap();
			LOG.trace("getaccountlist 2 accountStateCache SUCCESS, count:{}", addressStateCache.size());

			final Map<UInt160, Long> neoTxByAddress = new TreeMap<>();
			final Map<UInt160, Long> gasTxByAddress = new TreeMap<>();

			final Map<UInt160, Long> neoInByAddress = new TreeMap<>();
			final Map<UInt160, Long> gasInByAddress = new TreeMap<>();

			final Map<UInt160, Long> neoOutByAddress = new TreeMap<>();
			final Map<UInt160, Long> gasOutByAddress = new TreeMap<>();
			final Map<UInt160, Long> firstTsByAddress = new TreeMap<>();
			final Map<UInt160, Long> lastTsByAddress = new TreeMap<>();

			for (long index = fromHeight; index < toHeight; index++) {
				LOG.trace("getaccountlist 3 fromHeight:{};toHeight:{};index:{};", fromHeight, toHeight, index);
				final Block block = blockDb.getFullBlockFromHeight(index);

				for (final Transaction t : block.getTransactionList()) {
					final Map<UInt160, Map<UInt256, Long>> addressAssetMap = getAddressAssetMap(blockDb, t);

					for (final UInt160 friend : addressAssetMap.keySet()) {
						if (!firstTsByAddress.containsKey(friend)) {
							firstTsByAddress.put(friend, block.timestamp.asLong());
						}
						lastTsByAddress.put(friend, block.timestamp.asLong());

						if (addressAssetMap.get(friend).containsKey(ModelUtil.NEO_HASH)) {
							MapUtil.increment(neoTxByAddress, friend);
							final long value = addressAssetMap.get(friend).get(ModelUtil.NEO_HASH);
							if (value < 0) {
								MapUtil.increment(neoInByAddress, friend, -value);
							} else {
								MapUtil.increment(neoOutByAddress, friend, value);
							}
						}
						if (addressAssetMap.get(friend).containsKey(ModelUtil.GAS_HASH)) {
							MapUtil.increment(gasTxByAddress, friend);
							final long value = addressAssetMap.get(friend).get(ModelUtil.GAS_HASH);
							if (value < 0) {
								MapUtil.increment(gasInByAddress, friend, -value);
							} else {
								MapUtil.increment(gasOutByAddress, friend, value);
							}
						}
					}
				}
			}

			LOG.trace("getaccountlist 4 addressByAccount STARTED");

			final Map<UInt160, String> addressByScriptHash = new TreeMap<>();

			for (final UInt160 key : addressStateCache.keySet()) {
				final String address = ModelUtil.scriptHashToAddress(key);
				addressByScriptHash.put(key, address);
			}
			LOG.trace("getaccountlist 4 addressByAccount SUCCESS, address count:{};", addressByScriptHash.size());

			LOG.trace("getaccountlist 5 returnList STARTED");
			final JSONArray returnList = new JSONArray();

			for (final UInt160 key : addressStateCache.keySet()) {
				LOG.trace("getaccountlist 6 key:{};", key);
				if (addressByScriptHash.containsKey(key)) {
					final Map<UInt256, Fixed8> addressState = addressStateCache.get(key);
					final String address = addressByScriptHash.get(key);

					LOG.trace("getaccountlist 7 key:{}; address:{};", key, address);

					final JSONObject entry = new JSONObject();
					entry.put("account", address);

					if (addressState.containsKey(ModelUtil.NEO_HASH)) {
						entry.put(ModelUtil.NEO, ModelUtil.toRoundedLong(addressState.get(ModelUtil.NEO_HASH).value));
					} else {
						entry.put(ModelUtil.NEO, 0);
					}

					if (addressState.containsKey(ModelUtil.GAS_HASH)) {
						entry.put(ModelUtil.GAS, ModelUtil.toRoundedDouble(addressState.get(ModelUtil.GAS_HASH).value));
					} else {
						entry.put(ModelUtil.GAS, 0);
					}

					if (neoInByAddress.containsKey(key)) {
						entry.put(NEO_IN, ModelUtil.toRoundedLong(neoInByAddress.get(key)));
					} else {
						entry.put(NEO_IN, 0);
					}

					if (neoOutByAddress.containsKey(key)) {
						entry.put(NEO_OUT, ModelUtil.toRoundedLong(neoOutByAddress.get(key)));
					} else {
						entry.put(NEO_OUT, 0);
					}

					if (gasInByAddress.containsKey(key)) {
						entry.put(GAS_IN, ModelUtil.toRoundedDouble(gasInByAddress.get(key)));
					} else {
						entry.put(GAS_IN, 0);
					}

					if (gasOutByAddress.containsKey(key)) {
						entry.put(GAS_OUT, ModelUtil.toRoundedDouble(gasOutByAddress.get(key)));
					} else {
						entry.put(GAS_OUT, 0);
					}

					if (neoTxByAddress.containsKey(key)) {
						entry.put(NEO_TX, neoTxByAddress.get(key));
					} else {
						entry.put(NEO_TX, 0);
					}

					if (gasTxByAddress.containsKey(key)) {
						entry.put(GAS_TX, gasTxByAddress.get(key));
					} else {
						entry.put(GAS_TX, 0);
					}

					if (firstTsByAddress.containsKey(key)) {
						entry.put(FIRST_TS, firstTsByAddress.get(key));
					} else {
						entry.put(FIRST_TS, 0);
					}

					if (lastTsByAddress.containsKey(key)) {
						entry.put(LAST_TS, lastTsByAddress.get(key));
					} else {
						entry.put(LAST_TS, 0);
					}

					returnList.put(entry);
				}
			}
			LOG.trace("getaccountlist 5 returnList SUCCESS, returnList.size:{};", returnList.length());

			LOG.trace("getaccountlist 6 return");

			final JSONObject response = new JSONObject();
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			response.put(RESULT, returnList);

			return response;
		} catch (final RuntimeException e) {
			LOG.error("error in onGetAccountList:", e);
			final JSONObject response = new JSONObject();
			response.put(ERROR, e.getMessage());
			response.put(EXPECTED, new JSONArray());
			response.put(ACTUAL, new JSONArray());
			return response;
		}
	}

	/**
	 * responds to a "getbestblockhash" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetBestBlockHash(final LocalControllerNode controller, final int id) {
		final Block block = controller.getLocalNodeData().getBlockDb().getHeaderOfBlockWithMaxIndex();
		if (block == null) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, ERROR_NO_BLOCKS_IN_BLOCKCHAIN);
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final JSONObject response = new JSONObject();
			final String hashHex = block.hash.toHexString();
			response.put(RESULT, "0x" + hashHex);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		}
	}

	/**
	 * responds to a "getblock" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlock(final LocalControllerNode controller, final int id, final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a hash or an index");
			response.put(EXPECTED, 0);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final boolean verbose;
			if (params.length() >= 2) {
				if (params.get(1) instanceof Number) {
					final long index = params.getLong(1);
					verbose = index == 1;
				} else {
					verbose = false;
				}
			} else {
				verbose = false;
			}

			final Block block;
			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();
			if (params.get(0) instanceof String) {
				final String hashStr = params.getString(0);
				final byte[] ba = ModelUtil.decodeHex(hashStr);
				final UInt256 hash = new UInt256(ByteBuffer.wrap(ba));
				try {
					block = blockDb.getFullBlockFromHash(hash);
				} catch (final RuntimeException e) {
					final JSONObject response = new JSONObject();
					response.put(ERROR, e.getMessage());
					response.put(EXPECTED, EXPECTED_GENERIC_HEX);
					response.put(ACTUAL, params.get(0));
					return response;
				}
			} else if (params.get(0) instanceof Number) {
				final long index = params.getLong(0);
				try {
					block = blockDb.getFullBlockFromHeight(index);
				} catch (final RuntimeException e) {
					final JSONObject response = new JSONObject();
					response.put(ERROR, e.getMessage());
					response.put(EXPECTED, 0);
					response.put(ACTUAL, params.get(0));
					return response;
				}
			} else {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "bad parameters, expected a hash or an index");
				response.put(EXPECTED, 0);
				response.put(ACTUAL, params.get(0));
				return response;
			}
			final JSONObject response = new JSONObject();
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);

			if (verbose) {
				response.put(RESULT, block.toJSONObject());
			} else {
				response.put(RESULT, Hex.encodeHexString(block.toByteArray()));
			}
			return response;
		}
	}

	/**
	 * responds to a "getblockcount" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlockCount(final LocalControllerNode controller, final int id) {
		final Block block = controller.getLocalNodeData().getBlockDb().getHeaderOfBlockWithMaxIndex();
		if (block == null) {
			final JSONObject response = new JSONObject();
			response.put(RESULT, 0);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		} else {
			final JSONObject response = new JSONObject();
			final long index = block.getIndexAsLong();
			response.put(RESULT, index + 1);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		}
	}

	/**
	 * responds to a "getblockhash" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetBlockHash(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected an index");
			response.put(EXPECTED, 0);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final long index = params.getLong(0);
			try {
				final Block block = controller.getLocalNodeData().getBlockDb().getHeaderOfBlockFromHeight(index);
				final JSONObject response = new JSONObject();
				response.put(ID, id);
				response.put(JSONRPC, VERSION_2_0);
				response.put(RESULT, block.hash.toHexString());
				return response;
			} catch (final RuntimeException e) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, e.getMessage());
				response.put(EXPECTED, 0);
				response.put(ACTUAL, NULL);
				return response;
			}
		}
	}

	/**
	 * return the balance of the address.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param address
	 *            the address to use.
	 * @return the balance of the address.
	 */
	private static JSONObject onGetCityOfZionBalance(final LocalControllerNode controller, final String address) {
		final UInt160 scriptHash = ModelUtil.addressToScriptHash(address);
		if (LOG.isTraceEnabled()) {
			LOG.trace("onGetCityOfZionBalance.scriptHash:{}", scriptHash);
		}

		try {
			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();
			final Map<UInt256, Fixed8> assetValueMap = blockDb.getAssetValueMap(scriptHash);

			final Map<UInt256, Map<TransactionOutput, CoinReference>> transactionOutputListMap = blockDb
					.getUnspentTransactionOutputListMap(scriptHash);

			if (assetValueMap == null) {
				final JSONObject response = new JSONObject();
				response.put(GAS, new JSONObject());
				response.put(NEO, new JSONObject());
				response.put(NET, controller.getLocalNodeData().getNetworkName());
				return response;
			}

			final Fixed8 neo = assetValueMap.get(ModelUtil.NEO_HASH);
			final Fixed8 gas = assetValueMap.get(ModelUtil.GAS_HASH);

			final JSONObject response = new JSONObject();
			final JSONObject neoJo = new JSONObject();

			neoJo.put(UNSPENT, toUnspentJSONArray(transactionOutputListMap.get(ModelUtil.NEO_HASH), false));
			neoJo.put(BALANCE, neo);

			final JSONObject gasJo = new JSONObject();
			gasJo.put(UNSPENT, toUnspentJSONArray(transactionOutputListMap.get(ModelUtil.GAS_HASH), true));
			gasJo.put(BALANCE, gas);

			response.put(GAS, gasJo);
			response.put(NEO, neoJo);
			response.put(NET, controller.getLocalNodeData().getNetworkName());
			return response;
		} catch (final RuntimeException e) {
			LOG.error("onGetCityOfZionBalance", e);
			final JSONObject response = new JSONObject();
			if (e.getMessage() == null) {
				response.put(ERROR, e.getClass().getName());
			} else {
				response.put(ERROR, e.getMessage());
			}
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, address);
			return response;
		}
	}

	/**
	 * return the available claims of the address.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param address
	 *            the address to use.
	 * @return the balance of the address.
	 */
	private static JSONObject onGetCityOfZionClaims(final LocalControllerNode controller, final String address) {
		final UInt160 scriptHash = ModelUtil.addressToScriptHash(address);
		if (LOG.isTraceEnabled()) {
			LOG.trace("onGetCityOfZionClaims.scriptHash:{}", scriptHash);
		}

		try {
			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();
			final Map<UInt256, Map<TransactionOutput, CoinReference>> transactionOutputListMap = controller
					.getLocalNodeData().getBlockDb().getUnspentTransactionOutputListMap(scriptHash);

			final JSONArray claimJa = new JSONArray();

			if (transactionOutputListMap != null) {
				final Map<TransactionOutput, CoinReference> neoTransactionOutputListMap = transactionOutputListMap
						.get(ModelUtil.NEO_HASH);

				final Map<TransactionOutput, Long> blockIxByTxoMap = new TreeMap<>();

				final List<Transaction> transactionList = blockDb.getTransactionWithAccountList(scriptHash);
				for (final Transaction transaction : transactionList) {
					final long blockIx = blockDb.getBlockIndexFromTransactionHash(transaction.getHash());
					for (final TransactionOutput to : transaction.outputs) {
						if (neoTransactionOutputListMap.containsKey(to)) {
							blockIxByTxoMap.put(to, blockIx);
						}
					}
				}

				for (final TransactionOutput output : neoTransactionOutputListMap.keySet()) {
					final CoinReference cr = neoTransactionOutputListMap.get(output);
					final JSONObject unspent = toUnspentJSONObject(false, output, cr);

					final JSONObject claim = new JSONObject();
					final String txHashStr = unspent.getString(TXID);
					claim.put(TXID, txHashStr);
					claim.put(INDEX, unspent.getLong(INDEX));
					claim.put(VALUE, unspent.getLong(VALUE));

					final UInt256 txHash = ModelUtil.getUInt256(ByteBuffer.wrap(ModelUtil.decodeHex(txHashStr)), true);
					final long start = blockDb.getBlockIndexFromTransactionHash(txHash);
					claim.put(START, start);

					final long end = blockIxByTxoMap.get(output);
					claim.put(END, end);
					claim.put(SYSFEE, computeSysFee(controller.getLocalNodeData().getTransactionSystemFeeMap(), blockDb,
							start, end));
					claim.put("claim", calculateBonus(claim));

					claimJa.put(claim);
				}
			}
			final JSONObject response = new JSONObject();
			response.put(ADDRESS, address);
			response.put(CLAIMS, claimJa);
			response.put(NET, controller.getLocalNodeData().getNetworkName());
			return response;
		} catch (final RuntimeException e) {
			LOG.error("onGetCityOfZionClaims", e);
			final JSONObject response = new JSONObject();
			if (e.getMessage() == null) {
				response.put(ERROR, e.getClass().getName());
			} else {
				response.put(ERROR, e.getMessage());
			}
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, address);
			return response;
		}
	}

	/**
	 * return the transaction history of the address.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param address
	 *            the address to use.
	 * @return the balance of the address.
	 */
	private static JSONObject onGetCityOfZionHistory(final LocalControllerNode controller, final String address) {
		final UInt160 scriptHash = ModelUtil.addressToScriptHash(address);
		if (LOG.isTraceEnabled()) {
			LOG.trace("onGetCityOfZionHistory.scriptHash:{}", scriptHash);
		}

		try {
			final BlockDb blockDb = controller.getLocalNodeData().getBlockDb();
			final List<Transaction> transactionList = blockDb.getTransactionWithAccountList(scriptHash);

			final JSONArray historyJa = new JSONArray();
			if (transactionList != null) {
				for (final Transaction transaction : transactionList) {
					Fixed8 neo = ModelUtil.FIXED8_ZERO;
					Fixed8 gas = ModelUtil.FIXED8_ZERO;
					for (final TransactionOutput to : transaction.outputs) {
						if (to.scriptHash.equals(scriptHash)) {
							if (to.assetId.equals(ModelUtil.NEO_HASH)) {
								neo = ModelUtil.add(neo, to.value);
							}
							if (to.assetId.equals(ModelUtil.GAS_HASH)) {
								gas = ModelUtil.add(gas, to.value);
							}
						}
					}
					final JSONObject transactionResponse = new JSONObject();

					transactionResponse.put(GAS, ModelUtil.toRoundedDouble(gas.value));
					transactionResponse.put(NEO, ModelUtil.toRoundedLong(neo.value));

					final Long blockIndex = blockDb.getBlockIndexFromTransactionHash(transaction.getHash());
					transactionResponse.put("block_index", blockIndex);
					transactionResponse.put(TXID, transaction.getHash().toString());
					historyJa.put(transactionResponse);
				}
			}
			final JSONObject response = new JSONObject();
			response.put(ADDRESS, address);
			response.put(HISTORY, historyJa);
			response.put(NET, controller.getLocalNodeData().getNetworkName());
			return response;
		} catch (final RuntimeException e) {
			LOG.error("onGetCityOfZionHistory", e);
			final JSONObject response = new JSONObject();
			if (e.getMessage() == null) {
				response.put(ERROR, e.getClass().getName());
			} else {
				response.put(ERROR, e.getMessage());
			}
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, address);
			return response;
		}
	}

	/**
	 * return the transaction as a JSON object.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param transactionHex
	 *            the transaction hex to use.
	 * @return the transaction as a JSON object.
	 */
	private static JSONObject onGetCityOfZionTransaction(final LocalControllerNode controller,
			final String transactionHex) {
		final Transaction transaction;
		try {
			final byte[] ba = ModelUtil.decodeHex(transactionHex);
			final UInt256 txId = new UInt256(ByteBuffer.wrap(ba));
			transaction = controller.getLocalNodeData().getBlockDb().getTransactionWithHash(txId);
		} catch (final RuntimeException e) {
			final JSONObject response = new JSONObject();
			if (e.getMessage() == null) {
				response.put(ERROR, e.getClass().getName());
			} else {
				response.put(ERROR, e.getMessage());
			}
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, transactionHex);
			return response;
		}

		return transaction.toJSONObject();

	}

	/**
	 * responds to a "getconnectioncount" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @return the response.
	 */
	private static JSONObject onGetConnectionCount(final LocalControllerNode controller, final int id) {
		int connectionCount = 0;
		synchronized (controller) {
			final LocalNodeData localNodeData = controller.getLocalNodeData();
			synchronized (localNodeData) {
				final List<RemoteNodeData> peerDataList = new ArrayList<>();
				controller.addPeerDataSetToList(peerDataList);
				for (final RemoteNodeData data : peerDataList) {
					if (data.getConnectionPhase() == NodeConnectionPhaseEnum.ACKNOWLEDGED) {
						connectionCount++;
					}
				}

			}
		}
		final JSONObject response = new JSONObject();
		response.put(RESULT, connectionCount);
		response.put(ID, id);
		response.put(JSONRPC, VERSION_2_0);
		return response;
	}

	/**
	 * return the transactions in the unverified transaction pool.
	 *
	 * @param controller
	 *            the controllers to use
	 * @param id
	 *            the id to use.
	 * @return the transactions in the unverified transaction pool.
	 */
	private static JSONObject onGetRawMempool(final LocalControllerNode controller, final int id) {
		try {
			final JSONArray resultArray = new JSONArray();
			for (final Transaction transaction : controller.getLocalNodeData().getUnverifiedTransactionSet()) {
				final String hex = ModelUtil.toHexString(transaction.toByteArray());
				resultArray.put(hex);
			}
			final JSONObject response = new JSONObject();
			response.put(RESULT, resultArray);
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			return response;
		} catch (final RuntimeException e) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, e.getMessage());
			final JSONArray expectedArray = new JSONArray();
			expectedArray.put(EXPECTED_GENERIC_HEX);
			response.put(EXPECTED, expectedArray);
			response.put(ACTUAL, expectedArray);
			return response;
		}
	}

	/**
	 * responds to a "getrawtransaction" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetRawTransaction(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a txid");
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, NULL);
			return response;
		} else {
			final boolean verbose;
			if (params.length() >= 2) {
				if (params.get(1) instanceof Number) {
					final long index = params.getLong(1);
					verbose = index == 1;
				} else {
					verbose = false;
				}
			} else {
				verbose = false;
			}

			final String txIdStr = params.getString(0);
			final byte[] ba = ModelUtil.decodeHex(txIdStr);
			final UInt256 txId = new UInt256(ByteBuffer.wrap(ba));
			final Transaction transaction;
			try {
				transaction = controller.getLocalNodeData().getBlockDb().getTransactionWithHash(txId);
			} catch (final RuntimeException e) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, e.getMessage());
				response.put(EXPECTED, EXPECTED_GENERIC_HEX);
				response.put(ACTUAL, params.get(0));
				return response;
			}

			final JSONObject response = new JSONObject();
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);

			if (verbose) {
				response.put(RESULT, transaction.toJSONObject());
			} else {
				response.put(RESULT, Hex.encodeHexString(transaction.toByteArray()));
			}
			return response;
		}
	}

	/**
	 * responds to a "gettxout" command.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the request id to use.
	 * @param params
	 *            the parameters to use.
	 * @return the response.
	 */
	private static JSONObject onGetTransactionOutput(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a txid and an index");
			final JSONArray expectedParams = new JSONArray();
			expectedParams.put(EXPECTED_GENERIC_HEX);
			expectedParams.put(0);
			response.put(EXPECTED, expectedParams);
			response.put(ACTUAL, new JSONArray());
			return response;
		} else if (params.length() == 1) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "only one parameter, expected a txid and an index");
			final JSONArray expectedParams = new JSONArray();
			expectedParams.put(EXPECTED_GENERIC_HEX);
			expectedParams.put(0);
			response.put(EXPECTED, expectedParams);
			response.put(ACTUAL, params);
			return response;
		} else {
			final String txIdStr = params.getString(0);
			final int outputsIndex = params.getInt(1);
			final byte[] ba = ModelUtil.decodeHex(txIdStr);
			final UInt256 txId = new UInt256(ByteBuffer.wrap(ba));
			final Transaction transaction;
			try {
				transaction = controller.getLocalNodeData().getBlockDb().getTransactionWithHash(txId);
			} catch (final RuntimeException e) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, e.getMessage());
				final JSONArray expectedParams = new JSONArray();
				expectedParams.put(EXPECTED_GENERIC_HEX);
				expectedParams.put(0);
				response.put(ACTUAL, params);
				return response;
			}

			if (transaction.outputs.isEmpty()) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "transaction with hex \"" + txIdStr + "\" has no outputs.");
				final JSONArray expectedParams = new JSONArray();
				expectedParams.put(EXPECTED_GENERIC_HEX);
				expectedParams.put(0);
				response.put(EXPECTED, 1);
				response.put(ACTUAL, 0);
				return response;
			}

			if (outputsIndex >= transaction.outputs.size()) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "requested index \"" + outputsIndex + "\" is is too large, needs to be less than \""
						+ transaction.outputs.size() + "\"");
				final JSONArray expectedParams = new JSONArray();
				expectedParams.put(EXPECTED_GENERIC_HEX);
				expectedParams.put(0);
				response.put(EXPECTED, transaction.outputs.size() - 1);
				response.put(ACTUAL, outputsIndex);
				return response;
			}

			final JSONObject response = new JSONObject();
			response.put(ID, id);
			response.put(JSONRPC, VERSION_2_0);
			response.put(RESULT, transaction.outputs.get(outputsIndex).toJSONObject());

			return response;
		}
	}

	/**
	 * sends a raw transaction to the blockchain.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the id to use.
	 * @param params
	 *            the parameters to use.
	 * @return true if successful
	 */
	private static JSONObject onSendRawTransaction(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a hex encoded transaction");
			final JSONArray expectedParams = new JSONArray();
			expectedParams.put(EXPECTED_GENERIC_HEX);
			expectedParams.put(0);
			response.put(EXPECTED, expectedParams);
			response.put(ACTUAL, new JSONArray());
			return response;
		}
		try {
			final String hex = params.getString(0);
			final byte[] ba = ModelUtil.decodeHex(hex);
			final Transaction tx = new Transaction(ByteBuffer.wrap(ba));
			controller.getLocalNodeData().getUnverifiedTransactionSet().add(tx);
		} catch (final RuntimeException e) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, e.getMessage());
			response.put(EXPECTED, true);
			response.put(ACTUAL, params.get(0));
			return response;
		}
		final JSONObject response = new JSONObject();
		response.put(RESULT, true);
		response.put(ID, id);
		response.put(JSONRPC, VERSION_2_0);
		return response;
	}

	/**
	 * submit a block.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param id
	 *            the id to use.
	 * @param params
	 *            the parameters to use.
	 * @return true if the block validated, false if it did not.
	 */
	private static JSONObject onSubmitBlock(final LocalControllerNode controller, final int id,
			final JSONArray params) {
		if (params.length() == 0) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, "no parameters, expected a hashed block");
			response.put(EXPECTED, EXPECTED_GENERIC_HEX);
			response.put(ACTUAL, NULL);
			return response;
		}
		try {
			final String hex = params.getString(0);
			final byte[] ba = ModelUtil.decodeHex(hex);
			final Block block = new Block(ByteBuffer.wrap(ba));
			controller.getLocalNodeData().getBlockDb().put(false, block);
		} catch (final RuntimeException e) {
			final JSONObject response = new JSONObject();
			response.put(ERROR, e.getMessage());
			response.put(EXPECTED, true);
			response.put(ACTUAL, params.get(0));
			return response;
		}
		final JSONObject response = new JSONObject();
		response.put(RESULT, true);
		response.put(ID, id);
		response.put(JSONRPC, VERSION_2_0);
		return response;
	}

	/**
	 * process the request.
	 *
	 * @param controller
	 *            the controller to use.
	 * @param uri
	 *            the uri to process
	 * @param requestStr
	 *            the request to process
	 *
	 * @return the response.
	 */
	public static JSONObject process(final LocalControllerNode controller, final String uri, final String requestStr) {
		LOG.trace("process uri:{};requestStr:{};", uri, requestStr);
		if (uri.equals("/")) {
			final JSONObject request = new JSONObject(requestStr);

			final String versionStr = request.getString(JSONRPC);
			if (!versionStr.equals(VERSION_2_0)) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unexpected version");
				response.put(EXPECTED, VERSION_2_0);
				response.put(ACTUAL, versionStr);
				return response;
			}
			final String methodStr = request.getString(METHOD);
			final Set<String> disabledMethods = controller.getLocalNodeData().getRpcDisabledCalls();
			if (disabledMethods.contains(methodStr)) {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "method disabled");
				response.put(EXPECTED, methodStr + " enabled");
				response.put(ACTUAL, methodStr + " disabled");
				return response;
			}

			final int id = request.getInt(ID);
			final CoreRpcCommandEnum coreRpcCommand = CoreRpcCommandEnum.fromName(methodStr);

			switch (coreRpcCommand) {
			case GETBESTBLOCKHASH: {
				return onGetBestBlockHash(controller, id);
			}
			case GETBLOCKCOUNT: {
				return onGetBlockCount(controller, id);
			}
			case GETBLOCK: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetBlock(controller, id, params);
			}
			case GETBLOCKHASH: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetBlockHash(controller, id, params);
			}
			case GETCONNECTIONCOUNT: {
				return onGetConnectionCount(controller, id);
			}
			case GETRAWMEMPOOL: {
				return onGetRawMempool(controller, id);
			}
			case GETRAWTRANSACTION: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetRawTransaction(controller, id, params);
			}
			case GETTXOUT: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetTransactionOutput(controller, id, params);
			}
			case SENDRAWTRANSACTION: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onSendRawTransaction(controller, id, params);
			}
			case SUBMITBLOCK: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onSubmitBlock(controller, id, params);
			}
			case GETACCOUNTLIST: {
				final JSONArray params = request.getJSONArray(PARAMS);
				return onGetAccountList(controller, id, params);
			}
			default: {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unknown method");
				response.put(EXPECTED, CoreRpcCommandEnum.getValuesJSONArray());
				response.put(ACTUAL, methodStr);
				return response;
			}
			}
		} else {
			final CityOfZionCommandEnum cityOfZionCommand = CityOfZionCommandEnum.getCommandStartingWith(uri);
			final String remainder = uri.substring(cityOfZionCommand.getUriPrefix().length());
			switch (cityOfZionCommand) {
			case BALANCE: {
				return onGetCityOfZionBalance(controller, remainder);
			}
			case CLAIMS: {
				return onGetCityOfZionClaims(controller, remainder);
			}
			case HISTORY: {
				return onGetCityOfZionHistory(controller, remainder);
			}
			case TRANSACTION: {
				return onGetCityOfZionTransaction(controller, remainder);
			}
			default: {
				final JSONObject response = new JSONObject();
				response.put(ERROR, "unknown URI");
				response.put(EXPECTED, CityOfZionCommandEnum.getValuesJSONArray());
				final JSONObject actual = new JSONObject();
				actual.put("uri", uri);
				actual.put("command", cityOfZionCommand.getUriPrefix());
				response.put(ACTUAL, actual);
				return response;
			}
			}
		}
	}

	/**
	 * converts a map of TransactionOutputs and CoinReferences to a json array of
	 * unspent transaction outputs.
	 *
	 * @param map
	 *            the map to use.
	 * @param withDecimals
	 *            if true, the value should have decimals.
	 * @return the json array of unspent transaction outputs.
	 */
	private static JSONArray toUnspentJSONArray(final Map<TransactionOutput, CoinReference> map,
			final boolean withDecimals) {

		if (map == null) {
			return null;
		}

		final JSONArray array = new JSONArray();

		for (final TransactionOutput output : map.keySet()) {
			final CoinReference cr = map.get(output);
			final JSONObject elt = toUnspentJSONObject(withDecimals, output, cr);
			array.put(elt);
		}

		return array;
	}

	/**
	 * converts a TransactionOutput and CoinReference to a json object of the
	 * unspent transaction output.
	 *
	 * @param output
	 *            the TransactionOutput to use.
	 * @param cr
	 *            the CoinReference to use.
	 * @param withDecimals
	 *            if true, the value should have decimals.
	 * @return the json object of the unspent transaction output.
	 */
	public static JSONObject toUnspentJSONObject(final boolean withDecimals, final TransactionOutput output,
			final CoinReference cr) {
		final JSONObject elt = new JSONObject();
		elt.put(INDEX, cr.prevIndex.asInt());
		elt.put(TXID, cr.prevHash.toHexString());

		if (withDecimals) {
			elt.put(VALUE, ModelUtil.toRoundedDouble(output.value.value));
		} else {
			elt.put(VALUE, ModelUtil.toRoundedLong(output.value.value));
		}
		return elt;
	}

	/**
	 * the constructor.
	 */
	private RpcServerUtil() {

	}
}
