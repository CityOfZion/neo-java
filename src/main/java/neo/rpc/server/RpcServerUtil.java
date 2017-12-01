package neo.rpc.server;

/**
 * http://cityofzion.io/neon-js/api/index.html
 * <p>
 * https://github.com/CityOfZion/neon-wallet-db/blob/042d2d00c4fb1a657e2268280c46fb900b4645ce/README.md
 * <p>
 * curl http://testnet-api.neonwallet.com/v2/address/balance/ANrL4vPnQCCi5Mro4fqKK1rxrkxEHqmp2E
 * <p>
 * curl http://testnet-api.neonwallet.com/v2/address/history/ALpwWoxKLwbfCTkRpK2iXrXpaMHgWGcrDV
 * <p>
 * curl http://testnet-api.neonwallet.com/v2/transaction/ec4dc0092d5adf8cdf30eadf5116dbb6f138b2e35ca2f1a26d992d69388e0b95
 * <p>
 * curl http://testnet-api.neonwallet.com/v1/address/claims/AJ3yzTLc5jebUskHtphKi1rb2FNoZjbpkz
 * <p>
 * curl http://api.neonwallet.com/v1/network/nodes
 * <p>
 * https://github.com/neo-project/neo/wiki/API-Reference
 * <p>
 * getbalance <asset_id> yes return balance info on the assets in the wallet
 * based on designated asset num.
 * <p>
 * getbestblockhash acquire the hash of the highest block of the main chain
 * <p>
 * getblock <hash> [verbose=0] return block info based on designated hash value
 * <p>
 * getblock <index> [verbose=0] return block info based on designated index
 * <p>
 * getblockcount acquire the number of blocks of the main chain
 * <p>
 * getblockhash <index> return block hash value based on designated index
 * <p>
 * getconnectioncount acquire the number of connections on your node
 * <p>
 * getrawmempool acquire unconfirmed transactions in the raw memory
 * <p>
 * getrawtransaction <txid> [verbose=0] return transaction info based on
 * designated hash value
 * <p>
 * gettxout <txid> <n> return change info based on designated hash and index
 * <p>
 * sendrawtransaction <hex> broadcast the transaction
 * <p>
 * sendtoaddress <asset_id> <address> <value> [fee=0] yes transfer to designated
 * address
 * <p>
 * submitblock <hex> submit a new block
 * <p>
 * 
 * @author coranos
 *
 */
public class RpcServerUtil {

}
