package de.cerus.iotaspigotpoc.transaction;

import de.cerus.iotaspigotpoc.IotaSpigotPocPlugin;
import de.cerus.iotaspigotpoc.model.CompletedTransaction;
import de.cerus.iotaspigotpoc.model.PendingTransaction;
import de.cerus.iotaspigotpoc.model.Product;
import de.cerus.iotaspigotpoc.product.ProductRegistry;
import de.cerus.iotaspigotpoc.storage.completed.CompletedTransactionStorageService;
import de.cerus.iotaspigotpoc.storage.pending.PendingTransactionStorageService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.iota.jota.utils.Pair;

/**
 * Handles the creation of new transactions and the completion of pending transactions
 */
public class TransactionController {

    // Bunch of return codes
    public static final int CODE_SUCCESS = 1;
    public static final int CODE_ERROR = 2;
    public static final int CODE_HAS_PENDING = 3;
    public static final int CODE_HAS_NO_PENDING = 4;

    private final PendingTransactionStorageService pendingTransactionStorageService;
    private final CompletedTransactionStorageService completedTransactionStorageService;
    private final IotaCommunicator iotaCommunicator;
    private final ProductRegistry productRegistry;

    public TransactionController(final PendingTransactionStorageService pendingTransactionStorageService,
                                 final CompletedTransactionStorageService completedTransactionStorageService,
                                 final IotaCommunicator iotaCommunicator, final ProductRegistry productRegistry) {
        this.pendingTransactionStorageService = pendingTransactionStorageService;
        this.completedTransactionStorageService = completedTransactionStorageService;
        this.iotaCommunicator = iotaCommunicator;
        this.productRegistry = productRegistry;
    }

    /**
     * Attempts to complete a pending transactions
     * <p>
     * The method will return a pair of <ReturnCode, ErrorMessage>. I'm not exactly happy with this design but I guess it's fine, eh?
     *
     * @param pendingTransaction The transaction to complete
     * @param transactionHash    The transaction hash
     *
     * @return <ReturnCode, ErrorMessage>
     */
    public CompletableFuture<Pair<Integer, String>> completeTransaction(final PendingTransaction pendingTransaction, final String transactionHash) {
        final String errorMsg;
        if ((errorMsg = this.iotaCommunicator.transactionComplete(pendingTransaction, transactionHash)) != null) {
            // Error out if we can't complete
            return CompletableFuture.completedFuture(new Pair<>(CODE_ERROR, errorMsg));
        }

        if (pendingTransaction.getProductId() != pendingTransaction.getProductId()) {
            // Error out if products don't match
            return CompletableFuture.completedFuture(new Pair<>(CODE_ERROR, "Product id does not match"));
        }

        final CompletableFuture<Pair<Integer, String>> future = new CompletableFuture<>();
        final CompletedTransaction completedTransaction = new CompletedTransaction(
                UUID.randomUUID(),
                pendingTransaction.getTransactionId(),
                pendingTransaction.getPlayerUuid(),
                pendingTransaction.getProductId(),
                pendingTransaction.getIotaAmount(),
                transactionHash,
                pendingTransaction.getTimestamp(),
                System.currentTimeMillis()
        );
        // Store transaction
        this.completedTransactionStorageService.storeCompletedTransaction(completedTransaction).whenComplete((unused, storeThrowable) -> {
            if (storeThrowable != null) {
                // Error out
                future.complete(new Pair<>(CODE_ERROR, storeThrowable.getMessage()));
                return;
            }

            final Product product = this.productRegistry.getById(pendingTransaction.getProductId());
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(IotaSpigotPocPlugin.class), () -> {
                // Run the product commands
                // This has to be done on the main thread because of Minecraft's shitty architecture
                product.getCommands().forEach(cmd -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd
                            .replace("{PLAYER_UUID}", pendingTransaction.getPlayerUuid().toString())
                            .replace("{PLAYER_NAME}", Bukkit.getOfflinePlayer(pendingTransaction.getPlayerUuid()).getName()));
                });
            });
            // Success!
            future.complete(new Pair<>(CODE_SUCCESS, null));
        });
        return future;
    }

    /**
     * Attempts to start a transaction
     * <p>
     * The method will return a pair of <ReturnCode, ErrorMessage>. I'm not exactly happy with this design but I guess it's fine, eh?
     *
     * @param playerUuid The players uuid
     * @param product    The product
     *
     * @return <ReturnCode, ErrorMessage>
     */
    public CompletableFuture<Pair<Integer, String>> startTransaction(final UUID playerUuid, final Product product) {
        final CompletableFuture<Pair<Integer, String>> future = new CompletableFuture<>();
        this.pendingTransactionStorageService.retrieveAll(playerUuid).whenComplete((pendingTransactions, throwable) -> {
            if (throwable != null) {
                // Error out
                future.complete(new Pair<>(CODE_ERROR, throwable.getMessage()));
                return;
            }

            this.completedTransactionStorageService.retrieveAll(playerUuid).whenComplete((completedTransactions, otherThrowable) -> {
                if (otherThrowable != null) {
                    // Error out
                    future.complete(new Pair<>(CODE_ERROR, otherThrowable.getMessage()));
                    return;
                }

                // Check if the player has any pending transactions
                final boolean hasPending = pendingTransactions.stream()
                        .anyMatch(transaction -> completedTransactions.stream()
                                .noneMatch(completedTransaction ->
                                        completedTransaction.getLinkedPendingTransactionId().equals(transaction.getTransactionId())));
                if (hasPending) {
                    // Error out
                    future.complete(new Pair<>(CODE_HAS_PENDING, null));
                    return;
                }

                final PendingTransaction pendingTransaction = new PendingTransaction(
                        UUID.randomUUID(),
                        playerUuid,
                        product.getId(),
                        product.getPrice(),
                        this.iotaCommunicator.getDepositAddress(),
                        System.currentTimeMillis()
                );
                // Store the new transaction
                this.pendingTransactionStorageService.storePendingTransaction(pendingTransaction).whenComplete((unused, storeThrowable) -> {
                    if (storeThrowable != null) {
                        future.complete(new Pair<>(CODE_ERROR, storeThrowable.getMessage()));
                        return;
                    }

                    // Return the assigned deposit address and the transaction id
                    // This is kinda shitty ngl
                    future.complete(new Pair<>(CODE_SUCCESS, pendingTransaction.getAssignedAddress()
                            + ";" + pendingTransaction.getTransactionId().toString()));
                });
            });
        });
        return future;
    }

}
