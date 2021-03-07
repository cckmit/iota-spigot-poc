package de.cerus.iotaspigotpoc.transaction;

import de.cerus.iotaspigotpoc.model.PendingTransaction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.iota.jota.IotaAPI;
import org.iota.jota.builder.AddressRequest;
import org.iota.jota.dto.response.GetBundleResponse;
import org.iota.jota.utils.TrytesConverter;

/**
 * Simple layer on top of the IotaAPI client
 */
public class IotaCommunicator {

    private final JavaPlugin plugin;
    private IotaAPI iotaClient;

    public IotaCommunicator(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.startIotaClient();
    }

    /**
     * Initializes the client
     */
    private void startIotaClient() {
        this.iotaClient = new IotaAPI.Builder()
                .protocol("https")
                .host("nodes.thetangle.org")
                .port(443)
                .build();
    }

    /**
     * Returns whether a transaction is completed or not
     * <p>
     * This method could be implemented a little better but it works and its not too bad
     *
     * @param pendingTransaction The transaction to check
     * @param hash               The transaction hash
     *
     * @return Whether it is completed (null) or not (not null, error message)
     */
    public String transactionComplete(final PendingTransaction pendingTransaction, final String hash) {
        final GetBundleResponse bundleResponse;
        try {
            // Try to get transaction
            bundleResponse = this.iotaClient.getBundle(hash);
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to fetch transaction for hash '" + hash + "'");
            this.plugin.getLogger().warning("Usually this indicates that the player provided a invalid hash.");
            this.plugin.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            return "Invalid hash";
        }

        // Filter response
        return bundleResponse.getTransactions().stream()
                .filter(transaction -> (transaction.getHash()).equals(hash)) // Only check transaction matching our hash
                .filter(transaction -> {
                    // Check the message
                    // A message is considered valid if it starts with the uuid of the buyer and ends with the id of the transaction
                    final String msg = TrytesConverter.trytesToAscii(transaction.getSignatureFragments().substring(0, 2186)).trim();
                    final String realMsg = pendingTransaction.getPlayerUuid().toString() + " " + pendingTransaction.getTransactionId();
                    return realMsg.equals(msg);
                }) // Value has to exactly match the price
                .anyMatch(transaction -> transaction.getValue() == pendingTransaction.getIotaAmount()) ? null : "No completed transaction found";
    }

    /**
     * Retrieves a address for deposits
     * Depending on the settings this will either return a fixed address all the time
     * or it will return the next unspent address belonging to the set seed
     *
     * @return A address
     */
    public String getDepositAddress() {
        final FileConfiguration config = this.plugin.getConfig();
        switch (config.getString("address.mode", "SEED")) {
            case "SEED":
                // Return the next unspent address
                return this.iotaClient.generateNewAddresses(new AddressRequest.Builder(
                        config.getString("address.seed").replace(" ", ""),
                        2
                ).amount(1).checksum(true).build()).first();
            case "ADDRESS":
                // Return the fixed address
                return config.getString("address.address");
            default:
                // Print error
                this.plugin.getLogger().severe("Invalid address mode");
                return null;
        }
    }

}
