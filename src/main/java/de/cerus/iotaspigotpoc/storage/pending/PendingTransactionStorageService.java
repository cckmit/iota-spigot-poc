package de.cerus.iotaspigotpoc.storage.pending;

import de.cerus.iotaspigotpoc.model.PendingTransaction;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PendingTransactionStorageService {

    /**
     * Stores a pending transaction
     *
     * @param transaction A transaction
     *
     * @return A callback
     */
    CompletableFuture<Void> storePendingTransaction(PendingTransaction transaction);

    /**
     * Cancels a pending transaction
     *
     * @param transaction The transaction to be cancelled
     *
     * @return A callback
     */
    CompletableFuture<Void> cancelPendingTransaction(PendingTransaction transaction);

    /**
     * Retrieves a transaction by its id
     *
     * @param transactionId The id
     *
     * @return A pending transaction or null
     */
    CompletableFuture<PendingTransaction> retrieveById(UUID transactionId);

    /**
     * Retrieves all pending transactions by a player
     *
     * @param playerUuid The players uuid
     *
     * @return A collection of pending transactions
     */
    CompletableFuture<Collection<PendingTransaction>> retrieveAll(UUID playerUuid);

    /**
     * Retrieves all pending transactions of a specific product
     *
     * @param productId The product id
     *
     * @return A collection of pending transactions
     */
    CompletableFuture<Collection<PendingTransaction>> retrieveAll(int productId);

    /**
     * Retrieves all pending transactions that were / will be handled by a specific address
     *
     * @param address A address
     *
     * @return A collection of pending transactions
     */
    CompletableFuture<Collection<PendingTransaction>> retrieveAll(String address);

}
