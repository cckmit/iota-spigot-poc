package de.cerus.iotaspigotpoc.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.lib.expiringmap.ExpiringMap;
import de.cerus.iotaspigotpoc.model.PendingTransaction;
import de.cerus.iotaspigotpoc.model.Product;
import de.cerus.iotaspigotpoc.product.ProductRegistry;
import de.cerus.iotaspigotpoc.storage.completed.CompletedTransactionStorageService;
import de.cerus.iotaspigotpoc.storage.pending.PendingTransactionStorageService;
import de.cerus.iotaspigotpoc.transaction.TransactionController;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;

@CommandAlias("iotaspigotpoc|iota")
public class IotaSpigotPocCommand extends BaseCommand {

    private final ExpiringMap<UUID, Long> cooldownMap = ExpiringMap.builder()
            .expiration(30, TimeUnit.SECONDS)
            .build();

    @Dependency
    private TransactionController transactionController;
    @Dependency
    private CompletedTransactionStorageService completedTransactionStorageService;
    @Dependency
    private PendingTransactionStorageService pendingTransactionStorageService;
    @Dependency
    private ProductRegistry productRegistry;

    @Default
    public void handle(final Player player) {
        player.sendMessage("§e/iota view");
        player.sendMessage("§e/iota buy <product>");
        player.sendMessage("§e/iota confirm <transaction hash>");
        player.sendMessage("§e/iota cancel");
        player.sendMessage("§e/iota products");
    }

    @Subcommand("products")
    public void handleProducts(final Player player) {
        // Send the player a list of products
        for (final Product product : this.productRegistry.getAllProducts()) {
            final TextComponent component = Component.text()
                    .content("§6" + product.getName() + " (#" + product.getId() + "): §f" + product.getDescription())
                    .hoverEvent(HoverEvent.showText(Component.text("§7Click to purchase")))
                    .clickEvent(ClickEvent.runCommand("/iota buy " + product.getId()))
                    .build();
            player.sendMessage(component);
        }
    }

    @Subcommand("cancel")
    public void handleCancel(final Player player) {
        if (this.cooldownMap.containsKey(player.getUniqueId())) {
            // Player is on cooldown
            player.sendMessage("§ePlease wait, you are currently on cooldown. §7("
                    + (this.cooldownMap.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000 + "s remaining)");
            return;
        }
        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + 30 * 1000);

        // Cancel pending transaction
        this.getPendingTransaction(player).whenComplete((transaction, throwable) -> {
            this.pendingTransactionStorageService.cancelPendingTransaction(transaction).whenComplete((unused, otherThrowable) -> {
                player.sendMessage("§aYour pending transaction was cancelled.");
            });
        });
    }

    @Subcommand("view")
    public void handleView(final Player player) {
        if (this.cooldownMap.containsKey(player.getUniqueId())) {
            // Player is on cooldown
            player.sendMessage("§ePlease wait, you are currently on cooldown. §7("
                    + (this.cooldownMap.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000 + "s remaining)");
            return;
        }
        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + 30 * 1000);

        // Show the deposit details
        this.sendTransactionMessage(player);
    }

    @Subcommand("confirm")
    public void handleConfirm(final Player player, final String transactionHash) {
        if (this.cooldownMap.containsKey(player.getUniqueId())) {
            // Player is on cooldown
            player.sendMessage("§ePlease wait, you are currently on cooldown. §7("
                    + (this.cooldownMap.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000 + "s remaining)");
            return;
        }
        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + 30 * 1000);

        player.sendMessage("§7§oPlease wait...");
        this.getPendingTransaction(player).whenComplete((pendingTransaction, throwable) -> {
            // Complete the transaction
            this.transactionController.completeTransaction(pendingTransaction, transactionHash)
                    .whenComplete((pair, err) -> {
                        final Integer code = pair.getLow();
                        final String additional = pair.getHi();
                        switch (code) {
                            case TransactionController.CODE_ERROR:
                                // General error
                                player.sendMessage("§cFailed to complete transaction: §7" + additional);
                                break;
                            case TransactionController.CODE_SUCCESS:
                                // Action successful
                                player.sendMessage("§aThank you for your purchase!");
                                break;
                            case TransactionController.CODE_HAS_NO_PENDING:
                                // No pending transactions
                                player.sendMessage("§cYou don't have a pending transaction.");
                                break;
                            default:
                                // Oh no
                                player.sendMessage("§7Unknown code");
                                break;
                        }
                    });
        });
    }

    @Subcommand("buy")
    public void handleBuy(final Player player, final int productId, @Optional final String unused) {
        final Product product = this.productRegistry.getById(productId);
        if (product == null) {
            // Unknown product
            player.sendMessage("§cThis product does not exist");
            return;
        }

        if (unused == null) {
            // Command not confirmed
            player.sendMessage("§eAre you sure you want to buy " + product.getName() + " for " + this.formatIota(product.getPrice()) + "?");
            player.sendMessage("§7Run §d/iota buy " + productId + " confirm");
            return;
        }

        if (this.cooldownMap.containsKey(player.getUniqueId())) {
            // Player is on cooldown
            player.sendMessage("§ePlease wait, you are currently on cooldown. §7("
                    + (this.cooldownMap.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000 + "s remaining)");
            return;
        }
        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + 30 * 1000);

        player.sendMessage("§7§oPlease wait...");
        this.transactionController.startTransaction(player.getUniqueId(), product).whenComplete((pair, throwable) -> {
            final Integer code = pair.getLow();
            final String additional = pair.getHi();

            switch (code) {
                case TransactionController.CODE_ERROR:
                    // General error
                    player.sendMessage("§cFailed to start a transaction: §7" + additional);
                    break;
                case TransactionController.CODE_SUCCESS:
                    // Action successful
                    this.sendTransactionMessage(player);
                    break;
                case TransactionController.CODE_HAS_PENDING:
                    // Player has pending transaction
                    player.sendMessage("§cYou already have a pending transaction.");
                    break;
                default:
                    // Oh no
                    player.sendMessage("§7Unknown code");
                    break;
            }
        });
    }

    /**
     * If the player has a pending transaction this method will return it, otherwise the player gets a little error message
     *
     * @param player The player
     *
     * @return A callback
     */
    private CompletableFuture<PendingTransaction> getPendingTransaction(final Player player) {
        final CompletableFuture<PendingTransaction> future = new CompletableFuture<>();
        this.pendingTransactionStorageService.retrieveAll(player.getUniqueId()).whenComplete((pendingTransactions, throwable) -> {
            if (throwable != null) {
                player.sendMessage("§cError: §7" + throwable.getMessage());
                return;
            }

            this.completedTransactionStorageService.retrieveAll(player.getUniqueId()).whenComplete((completedTransactions, otherThrowable) -> {
                if (otherThrowable != null) {
                    player.sendMessage("§cError: §7" + otherThrowable.getMessage());
                    return;
                }

                final java.util.Optional<PendingTransaction> optional = pendingTransactions.stream()
                        .filter(transaction -> completedTransactions.stream()
                                .noneMatch(completedTransaction ->
                                        completedTransaction.getLinkedPendingTransactionId().equals(transaction.getTransactionId())))
                        .findAny();
                if (optional.isEmpty()) {
                    player.sendMessage("§7You don't have any pending purchases.");
                    return;
                }

                future.complete(optional.get());
            });
        });
        return future;
    }

    /**
     * Sends deposit details to a player
     *
     * @param player The player
     */
    private void sendTransactionMessage(final Player player) {
        this.getPendingTransaction(player).whenComplete((transaction, throwable) -> {
            final TextComponent component = Component.join(
                    Component.empty(),
                    Component.text()
                            .content("§aPlease deposit §e" + this.formatIota(transaction.getIotaAmount()) + " §ato address ")
                            .build(),
                    Component.text()
                            .content("§b" + transaction.getAssignedAddress())
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(transaction.getAssignedAddress()))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("§7Click to copy address")))
                            .build(),
                    Component.text()
                            .content(" §awith message §7\"")
                            .build(),
                    Component.text()
                            .content("§7" + player.getUniqueId() + " " + transaction.getTransactionId())
                            .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(player.getUniqueId() + " " + transaction.getTransactionId()))
                            .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("§7Click to copy message")))
                            .build(),
                    Component.text()
                            .content("§7\" §ato complete your transaction.")
                            .build()
            );
            player.sendMessage(component);
            player.sendMessage("§7Warning: Please make sure to send the exact amount of IOTA! Otherwise your IOTA §c§lwill be lost!");
        });
    }

    /**
     * Formats a number into proper iota format
     *
     * @param price A number
     *
     * @return A formatted number
     */
    private String formatIota(final long price) {
        return price < 1000
                ? price + " i" : price < 1_000_000
                ? (price / 1000d) + " Ki" : (price / 1_000_000d) + " Mi";
    }

}
