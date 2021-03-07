package de.cerus.iotaspigotpoc.storage.pending.impl;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.cerus.iotaspigotpoc.model.PendingTransaction;
import de.cerus.iotaspigotpoc.storage.pending.PendingTransactionStorageService;
import de.cerus.iotaspigotpoc.util.CallbackSubscriber;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.bson.conversions.Bson;
import org.bukkit.plugin.java.JavaPlugin;

public class MongoDbPendingTransactionStorageService implements PendingTransactionStorageService {

    private final JavaPlugin plugin;
    private final MongoCollection<PendingTransaction> collection;

    public MongoDbPendingTransactionStorageService(final MongoDatabase database, final JavaPlugin plugin) {
        this.plugin = plugin;
        this.collection = database.getCollection("pending", PendingTransaction.class);
    }

    /**
     * Stores a pending transaction
     *
     * @param transaction A transaction
     *
     * @return A callback
     */
    @Override
    public CompletableFuture<Void> storePendingTransaction(final PendingTransaction transaction) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final CallbackSubscriber<Object> subscriber = new CallbackSubscriber<>();
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to store pending transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });
        subscriber.doOnComplete(() -> future.complete(null));

        this.collection.insertOne(transaction).subscribe(subscriber);

        return future;
    }

    /**
     * Cancels a pending transaction
     *
     * @param transaction The transaction to be cancelled
     *
     * @return A callback
     */
    @Override
    public CompletableFuture<Void> cancelPendingTransaction(final PendingTransaction transaction) {
        final CompletableFuture<Void> future = new CompletableFuture<>();

        final CallbackSubscriber<Object> subscriber = new CallbackSubscriber<>();
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to delete pending transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });
        subscriber.doOnComplete(() -> future.complete(null));

        this.collection.deleteOne(Filters.eq("_id", transaction.getTransactionId())).subscribe(subscriber);

        return future;
    }

    /**
     * Retrieves a transaction by its id
     *
     * @param transactionId The id
     *
     * @return A pending transaction or null
     */
    @Override
    public CompletableFuture<PendingTransaction> retrieveById(final UUID transactionId) {
        final CompletableFuture<PendingTransaction> future = new CompletableFuture<>();

        final AtomicReference<PendingTransaction> transactionRef = new AtomicReference<>(null);
        final CallbackSubscriber<PendingTransaction> subscriber = new CallbackSubscriber<>();
        subscriber.doOnNext(transactionRef::set);
        subscriber.doOnComplete(() -> future.complete(transactionRef.get()));
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to retrieve pending transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });

        this.collection.find(Filters.eq("_id", transactionId)).subscribe(subscriber);

        return future;
    }

    /**
     * Retrieves all pending transactions by a player
     *
     * @param playerUuid The players uuid
     *
     * @return A collection of pending transactions
     */
    @Override
    public CompletableFuture<Collection<PendingTransaction>> retrieveAll(final UUID playerUuid) {
        return this.retrieveBulk(Filters.eq("player", playerUuid));
    }

    /**
     * Retrieves all pending transactions of a specific product
     *
     * @param productId The product id
     *
     * @return A collection of pending transactions
     */
    @Override
    public CompletableFuture<Collection<PendingTransaction>> retrieveAll(final int productId) {
        return this.retrieveBulk(Filters.eq("product", productId));
    }

    /**
     * Retrieves all pending transactions that were / will be handled by a specific address
     *
     * @param address A address
     *
     * @return A collection of pending transactions
     */
    @Override
    public CompletableFuture<Collection<PendingTransaction>> retrieveAll(final String address) {
        return this.retrieveBulk(Filters.eq("address", address));
    }

    /**
     * Retrieves a collection of transactions
     *
     * @param filters Filters to search by
     *
     * @return A collection of transactions
     */
    private CompletableFuture<Collection<PendingTransaction>> retrieveBulk(final Bson filters) {
        final CompletableFuture<Collection<PendingTransaction>> future = new CompletableFuture<>();

        final List<PendingTransaction> transactions = new ArrayList<>();
        final CallbackSubscriber<PendingTransaction> subscriber = new CallbackSubscriber<>();
        subscriber.doOnNext(transactions::add);
        subscriber.doOnComplete(() -> future.complete(transactions));
        subscriber.doOnError(throwable -> {
            this.plugin.getLogger().severe("Failed to retrieve pending transaction");
            this.plugin.getLogger().severe(throwable.getMessage());
        });

        this.collection.find(filters).subscribe(subscriber);

        return future;
    }

}
