package de.cerus.iotaspigotpoc.storage.completed.impl;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.cerus.iotaspigotpoc.model.CompletedTransaction;
import de.cerus.iotaspigotpoc.storage.completed.CompletedTransactionStorageService;
import de.cerus.iotaspigotpoc.util.CallbackSubscriber;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bson.conversions.Bson;
import org.bukkit.plugin.java.JavaPlugin;

public class MongoDbCompletedTransactionStorageService implements CompletedTransactionStorageService {

    private final JavaPlugin plugin;
    private final MongoCollection<CompletedTransaction> collection;

    public MongoDbCompletedTransactionStorageService(final MongoDatabase database, final JavaPlugin plugin) {
        this.plugin = plugin;
        this.collection = database.getCollection("completed", CompletedTransaction.class);
    }

    /**
     * Stores a completed transaction
     *
     * @param transaction The transaction
     *
     * @return A callback
     */
    @Override
    public CompletableFuture<Void> storeCompletedTransaction(final CompletedTransaction transaction) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final CallbackSubscriber<Object> subscriber = new CallbackSubscriber<>();
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to store completed transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });
        subscriber.doOnComplete(() -> future.complete(null));

        this.collection.insertOne(transaction).subscribe(subscriber);

        return future;
    }

    /**
     * Retrieves a transaction by id
     *
     * @param transactionId The id of the transaction
     *
     * @return A completed transaction or null
     */
    @Override
    public CompletableFuture<CompletedTransaction> retrieveById(final UUID transactionId) {
        return this.retrieveSingle(Filters.eq("_id", transactionId));
    }

    /**
     * Retrieves a transaction by the id of its linked pending transaction
     *
     * @param linkedTransactionId The id of the linked pending transaction
     *
     * @return A completed transaction or null
     */
    @Override
    public CompletableFuture<CompletedTransaction> retrieveByLinkedId(final UUID linkedTransactionId) {
        return this.retrieveSingle(Filters.eq("linked_transaction", linkedTransactionId));
    }

    /**
     * Retrieves a transaction by a transaction hash
     *
     * @param transactionHash The transaction hash
     *
     * @return A completed transaction or null
     */
    @Override
    public CompletableFuture<CompletedTransaction> retrieveByHash(final String transactionHash) {
        return this.retrieveSingle(Filters.eq("hash", transactionHash));
    }

    /**
     * Retrieves all completed transactions of a player
     *
     * @param playerUuid The uuid of a player
     *
     * @return A collection of completed transactions
     */
    @Override
    public CompletableFuture<Collection<CompletedTransaction>> retrieveAll(final UUID playerUuid) {
        return this.retrieveBulk(Filters.eq("player", playerUuid));
    }

    /**
     * Retrieves all completed transactions of a specific product
     *
     * @param productId The id of a product
     *
     * @return A collection of completed transactions
     */
    @Override
    public CompletableFuture<Collection<CompletedTransaction>> retrieveAll(final int productId) {
        return this.retrieveBulk(Filters.eq("product", productId));
    }

    /**
     * Retrieves all completed transactions that were handled by a specific address
     *
     * @param address A address
     *
     * @return A collection of completed transactions
     */
    @Override
    public CompletableFuture<Collection<CompletedTransaction>> retrieveAll(final String address) {
        return this.retrieveBulk(Filters.eq("address", address));
    }

    /**
     * Retrieves a single transaction
     *
     * @param filters Filters to search by
     *
     * @return A single transaction or null
     */
    private CompletableFuture<CompletedTransaction> retrieveSingle(final Bson filters) {
        // A little hacky
        return this.retrieveBulk(filters).thenApply(completedTransactions ->
                completedTransactions.isEmpty() ? null : completedTransactions.iterator().next());
    }

    /**
     * Retrieves a collection of transactions
     *
     * @param filters Filters to search by
     *
     * @return A collection of transactions
     */
    private CompletableFuture<Collection<CompletedTransaction>> retrieveBulk(final Bson filters) {
        final CompletableFuture<Collection<CompletedTransaction>> future = new CompletableFuture<>();

        final List<CompletedTransaction> transactions = new ArrayList<>();
        final CallbackSubscriber<CompletedTransaction> subscriber = new CallbackSubscriber<>();
        subscriber.doOnNext(transactions::add);
        subscriber.doOnComplete(() -> future.complete(transactions));
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to retrieve completed transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });

        this.collection.find(filters).subscribe(subscriber);

        return future;
    }

}
