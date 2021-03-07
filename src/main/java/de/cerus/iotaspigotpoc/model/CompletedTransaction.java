package de.cerus.iotaspigotpoc.model;

import java.util.UUID;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Completed transaction model
 */
public class CompletedTransaction {

    @BsonId
    private final UUID transactionId;
    @BsonProperty("linked_transaction")
    private final UUID linkedPendingTransactionId;
    @BsonProperty("player")
    private final UUID playerUuid;
    @BsonProperty("product")
    private final int productId;
    @BsonProperty("iota")
    private final long iotaAmount;
    @BsonProperty("hash")
    private final String transactionHash;
    @BsonProperty("timestamp_started")
    private final long startedTimestamp;
    @BsonProperty("timestamp_completed")
    private final long completedTimestamp;

    @BsonCreator
    public CompletedTransaction(@BsonId final UUID transactionId,
                                @BsonProperty("linked_transaction") final UUID linkedPendingTransactionId,
                                @BsonProperty("player") final UUID playerUuid,
                                @BsonProperty("product") final int productId,
                                @BsonProperty("iota") final long iotaAmount,
                                @BsonProperty("hash") final String transactionHash,
                                @BsonProperty("timestamp_started") final long startedTimestamp,
                                @BsonProperty("timestamp_completed") final long completedTimestamp) {
        this.transactionId = transactionId;
        this.linkedPendingTransactionId = linkedPendingTransactionId;
        this.playerUuid = playerUuid;
        this.productId = productId;
        this.iotaAmount = iotaAmount;
        this.transactionHash = transactionHash;
        this.startedTimestamp = startedTimestamp;
        this.completedTimestamp = completedTimestamp;
    }

    public UUID getTransactionId() {
        return this.transactionId;
    }

    public UUID getLinkedPendingTransactionId() {
        return this.linkedPendingTransactionId;
    }

    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    public int getProductId() {
        return this.productId;
    }

    public long getIotaAmount() {
        return this.iotaAmount;
    }

    public String getTransactionHash() {
        return this.transactionHash;
    }

    public long getStartedTimestamp() {
        return this.startedTimestamp;
    }

    public long getCompletedTimestamp() {
        return this.completedTimestamp;
    }

}
