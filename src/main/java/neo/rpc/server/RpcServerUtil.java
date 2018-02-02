package neo.rpc.server;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.NotImplementedException;
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
 */
public final class RpcServerUtil {

	/**
	 * first timestamp.
	 */
	private static final String FIRST_TS = "first_ts";

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
		final long midBlockTs = midBlock.timestamp.asLong();
		if (ts == midBlockTs) {
			return midHeight;
		} else if (ts < midBlockTs) {
			// #if DEBUG
			// Console.WriteLine($"level:{level};minHeight:{minHeight};midHeight:{midHeight};midBlock.Timestamp:{midBlock.Timestamp};");
			// #endif
			return getHeightOfTs(controller, level + 1, minHeight, midHeight, ts);
		} else {
			// #if DEBUG
			// Console.WriteLine($"level:{level};midHeight:{midHeight};maxHeight:{maxHeight};midBlock.Timestamp:{midBlock.Timestamp};");
			// #endif
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
		// Console.WriteLine("getaccountlist 0");

		final long fromTs = params.getLong(0);
		final long toTs = params.getLong(1);
		final long minHeight = 0;
		final long maxHeight = controller.getLocalNodeData().getBlockDb().getBlockCount();
		final long fromHeight = getHeightOfTs(controller, 0, minHeight, maxHeight, fromTs);
		final long toHeight = getHeightOfTs(controller, 0, fromHeight, maxHeight, toTs);

		// Console.WriteLine($"getaccountlist 1 fromHeight:{fromHeight};
		// toHeight:{toHeight};");
		// errorTrace["2"] = $"fromHeight:{fromHeight}; toHeight:{toHeight};";

		final Map<UInt160, Long> neoTxByAccount = new TreeMap<>();
		final Map<UInt160, Long> gasTxByAccount = new TreeMap<>();

		final Map<UInt160, Long> neoInByAccount = new TreeMap<>();
		final Map<UInt160, Long> gasInByAccount = new TreeMap<>();

		final Map<UInt160, Long> neoOutByAccount = new TreeMap<>();
		final Map<UInt160, Long> gasOutByAccount = new TreeMap<>();
		final Map<UInt160, Long> firstTsByAccount = new TreeMap<>();

		for (long index = fromHeight; index < toHeight; index++) {
			// Console.WriteLine($"getaccountlist 2 fromHeight:{fromHeight};
			// toHeight:{toHeight}; index:{index};");
			final Block block = controller.getLocalNodeData().getBlockDb().getFullBlockFromHeight(index);

			// Console.WriteLine("getaccountlist 2.1");
			for (final Transaction t : block.getTransactionList()) {
				// Console.WriteLine("getaccountlist 3");

				final Map<UInt160, Map<UInt256, Long>> friendAssetMap = new TreeMap<>();

				for (final CoinReference cr : t.inputs) {
					final Transaction tiTx = controller.getLocalNodeData().getBlockDb()
							.getTransactionWithHash(cr.prevHash);
					final TransactionOutput ti = tiTx.outputs.get(cr.prevIndex.asInt());
					final UInt160 input = ti.scriptHash;
					if ((ti.assetId.equals(ModelUtil.NEO_HASH)) || (ti.assetId.equals(ModelUtil.GAS_HASH))) {
						MapUtil.increment(friendAssetMap, input, ti.assetId, ti.value.value, TreeMap.class);
					}
				}

				for (final TransactionOutput to : t.outputs) {
					final UInt160 output = to.scriptHash;
					if ((to.assetId.equals(ModelUtil.NEO_HASH)) || (to.assetId.equals(ModelUtil.GAS_HASH))) {
						MapUtil.increment(friendAssetMap, output, to.assetId, -to.value.value, TreeMap.class);
					}
				}

				for (final UInt160 friend : friendAssetMap.keySet()) {
					if (!firstTsByAccount.containsKey(friend)) {
						firstTsByAccount.put(friend, block.timestamp.asLong());
					}

					if (friendAssetMap.get(friend).containsKey(ModelUtil.NEO_HASH)) {
						MapUtil.increment(neoTxByAccount, friend);
						final long value = friendAssetMap.get(friend).get(ModelUtil.NEO_HASH);
						if (value < 0) {
							MapUtil.increment(neoInByAccount, friend, -value);
						} else {
							MapUtil.increment(neoOutByAccount, friend, value);
						}
					}
					if (friendAssetMap.get(friend).containsKey(ModelUtil.GAS_HASH)) {
						MapUtil.increment(gasTxByAccount, friend);
						final long value = friendAssetMap.get(friend).get(ModelUtil.GAS_HASH);
						if (value < 0) {
							MapUtil.increment(gasInByAccount, friend, -value);
						} else {
							MapUtil.increment(gasOutByAccount, friend, value);
						}
					}
				}
			}
		}
		// errorTrace["3"] = $"accountStateCache";

		final Map<UInt160, Map<UInt256, Fixed8>> accountStateCache = controller.getLocalNodeData().getBlockDb()
				.getAccountAssetValueMap();

		// errorTrace["4"] = $"addressByAccount";
		final Map<UInt160, String> addressByAccount = new TreeMap<>();

		for (final UInt160 key : accountStateCache.keySet()) {
			final String address = ModelUtil.toAddress(key);
			addressByAccount.put(key, address);
		}

		// errorTrace["5"] = $"returnList";
		final JSONArray returnList = new JSONArray();

		for (final UInt160 key : accountStateCache.keySet()) {
			if (addressByAccount.containsKey(key)) {
				final Map<UInt256, Fixed8> accountState = accountStateCache.get(key);
				final String address = addressByAccount.get(key);
				// Console.WriteLine($"getaccountlist 7 key:{key}; address:{address};");
				final JSONObject entry = new JSONObject();
				entry.put("account", address);

				if (accountState.containsKey(ModelUtil.NEO_HASH)) {
					entry.put(ModelUtil.NEO, accountState.containsKey(ModelUtil.NEO_HASH));
				} else {
					entry.put(ModelUtil.NEO, 0);
				}

				if (accountState.containsKey(ModelUtil.GAS_HASH)) {
					entry.put(ModelUtil.GAS, accountState.containsKey(ModelUtil.GAS_HASH));
				} else {
					entry.put(ModelUtil.GAS, 0);
				}

				if (neoInByAccount.containsKey(key)) {
					entry.put(NEO_IN, neoInByAccount.get(key));
				} else {
					entry.put(NEO_IN, 0);
				}

				if (neoOutByAccount.containsKey(key)) {
					entry.put(NEO_OUT, neoOutByAccount.get(key));
				} else {
					entry.put(NEO_OUT, 0);
				}

				if (gasInByAccount.containsKey(key)) {
					entry.put(GAS_IN, ModelUtil.toRoundedDoubleAsString(gasInByAccount.get(key)));
				} else {
					entry.put(GAS_IN, 0);
				}

				if (gasOutByAccount.containsKey(key)) {
					entry.put(GAS_OUT, ModelUtil.toRoundedDoubleAsString(gasOutByAccount.get(key)));
				} else {
					entry.put(GAS_OUT, 0);
				}

				if (neoTxByAccount.containsKey(key)) {
					entry.put(NEO_TX, neoTxByAccount.get(key));
				} else {
					entry.put(NEO_TX, 0);
				}

				if (gasTxByAccount.containsKey(key)) {
					entry.put(GAS_TX, gasTxByAccount.get(key));
				} else {
					entry.put(GAS_TX, 0);
				}

				if (firstTsByAccount.containsKey(key)) {
					entry.put(FIRST_TS, firstTsByAccount.get(key));
				} else {
					entry.put(FIRST_TS, 0);
				}

				returnList.put(entry);
			}
		}
		// errorTrace["6"] = $"return";
		// Console.WriteLine($"getaccountlist 8 {returnList.Count()}");

		final JSONObject response = new JSONObject();
		response.put(ID, id);
		response.put(JSONRPC, VERSION_2_0);
		response.put(RESULT, returnList);

		return response;
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
			if (params.get(0) instanceof String) {
				final String hashStr = params.getString(0);
				final byte[] ba = ModelUtil.decodeHex(hashStr);
				final UInt256 hash = new UInt256(ByteBuffer.wrap(ba));
				try {
					block = controller.getLocalNodeData().getBlockDb().getFullBlockFromHash(hash);
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
					block = controller.getLocalNodeData().getBlockDb().getFullBlockFromHeight(index);
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
			controller.getLocalNodeData().getBlockDb().put(block);
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
				// TODO : implement.
				throw new NotImplementedException(coreRpcCommand.getName());
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
				// TODO : implement.
				throw new NotImplementedException(coreRpcCommand.getName());
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
				// TODO : implement.
				throw new NotImplementedException(cityOfZionCommand.getUriPrefix());
			}
			case CLAIMS: {
				// TODO : implement.
				throw new NotImplementedException(cityOfZionCommand.getUriPrefix());
			}
			case HISTORY: {
				// TODO : implement.
				throw new NotImplementedException(cityOfZionCommand.getUriPrefix());
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
	 * the constructor.
	 */
	private RpcServerUtil() {

	}
}
