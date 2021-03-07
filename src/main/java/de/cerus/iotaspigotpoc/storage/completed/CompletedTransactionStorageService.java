package de.cerus.iotaspigotpoc.storage.completed;

import de.cerus.iotaspigotpoc.model.CompletedTransaction;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CompletedTransactionStorageService {

    /**
     * Stores a completed transaction
     *
     * @param transaction The transaction
     *
     * @return A callback
     */
    CompletableFuture<Void> storeCompletedTransaction(CompletedTransaction transaction);

    /**
     * Retrieves a transaction by id
     *
     * @param transactionId The id of the transaction
     *
     * @return A completed transaction or null
     */
    CompletableFuture<CompletedTransaction> retrieveById(UUID transactionId);

    /**
     * Retrieves a transaction by the id of its linked pending transaction
     *
     * @param linkedTransactionId The id of the linked pending transaction
     *
     * @return A completed transaction or null
     */
    CompletableFuture<CompletedTransaction> retrieveByLinkedId(UUID linkedTransactionId);

    /**
     * Retrieves a transaction by a transaction hash
     *
     * @param transactionHash The transaction hash
     *
     * @return A completed transaction or null
     */
    CompletableFuture<CompletedTransaction> retrieveByHash(String transactionHash);

    /**
     * Retrieves all completed transactions of a player
     *
     * @param playerUuid The uuid of a player
     *
     * @return A collection of completed transactions
     */
    CompletableFuture<Collection<CompletedTransaction>> retrieveAll(UUID playerUuid);

    /**
     * Retrieves all completed transactions of a specific product
     *
     * @param productId The id of a product
     *
     * @return A collection of completed transactions
     */
    CompletableFuture<Collection<CompletedTransaction>> retrieveAll(int productId);

    /**
     * Retrieves all completed transactions that were handled by a specific address
     *
     * @param address A address
     *
     * @return A collection of completed transactions
     */
    CompletableFuture<Collection<CompletedTransaction>> retrieveAll(String address);

}
