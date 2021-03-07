package de.cerus.iotaspigotpoc.model;

import java.util.UUID;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Pending transaction model
 */
public class PendingTransaction {

    @BsonId
    private final UUID transactionId;
    @BsonProperty("player")
    private final UUID playerUuid;
    @BsonProperty("product")
    private final int productId;
    @BsonProperty("iota")
    private final long iotaAmount;
    @BsonProperty("address")
    private final String assignedAddress;
    @BsonProperty("timestamp")
    private final long timestamp;

    @BsonCreator
    public PendingTransaction(
            @BsonId final UUID transactionId,
            @BsonProperty("player") final UUID playerUuid,
            @BsonProperty("product") final int productId,
            @BsonProperty("iota") final long iotaAmount,
            @BsonProperty("address") final String assignedAddress,
            @BsonProperty("timestamp") final long timestamp) {
        this.transactionId = transactionId;
        this.playerUuid = playerUuid;
        this.productId = productId;
        this.iotaAmount = iotaAmount;
        this.assignedAddress = assignedAddress;
        this.timestamp = timestamp;
    }

    public UUID getTransactionId() {
        return this.transactionId;
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

    public String getAssignedAddress() {
        return this.assignedAddress;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

}
