package de.cerus.iotaspigotpoc;

import co.aikar.commands.PaperCommandManager;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.cerus.iotaspigotpoc.command.IotaSpigotPocCommand;
import de.cerus.iotaspigotpoc.model.Product;
import de.cerus.iotaspigotpoc.product.ProductRegistry;
import de.cerus.iotaspigotpoc.storage.completed.CompletedTransactionStorageService;
import de.cerus.iotaspigotpoc.storage.completed.impl.MongoDbCompletedTransactionStorageService;
import de.cerus.iotaspigotpoc.storage.pending.PendingTransactionStorageService;
import de.cerus.iotaspigotpoc.storage.pending.impl.MongoDbPendingTransactionStorageService;
import de.cerus.iotaspigotpoc.transaction.IotaCommunicator;
import de.cerus.iotaspigotpoc.transaction.TransactionController;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class IotaSpigotPocPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        final boolean firstStart = !new File(this.getDataFolder(), "config.yml").exists();
        this.saveDefaultConfig();

        if (firstStart) {
            this.getLogger().info("");
            this.getLogger().info("Welcome to the Iota <-> Spigot Proof Of Concept.");
            this.getLogger().info("Please edit the config accordingly and restart your server.");
            this.getLogger().info("");
            this.getPluginLoader().disablePlugin(this);
            return;
        }

        // Init mongo
        final MongoClient mongoClient = this.getMongoClient();
        final MongoDatabase database = mongoClient.getDatabase("iota-spigot-poc");

        // Init transaction stuff
        final PendingTransactionStorageService pendingTransactionStorageService = new MongoDbPendingTransactionStorageService(database, this);
        final CompletedTransactionStorageService completedTransactionStorageService = new MongoDbCompletedTransactionStorageService(database, this);
        final IotaCommunicator iotaCommunicator = new IotaCommunicator(this);
        final ProductRegistry productRegistry = new ProductRegistry();
        final TransactionController transactionController = new TransactionController(pendingTransactionStorageService, completedTransactionStorageService, iotaCommunicator, productRegistry);

        // Init products
        this.loadProducts(productRegistry);

        // Init command stuff
        final PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerDependency(PendingTransactionStorageService.class, pendingTransactionStorageService);
        commandManager.registerDependency(CompletedTransactionStorageService.class, completedTransactionStorageService);
        commandManager.registerDependency(IotaCommunicator.class, iotaCommunicator);
        commandManager.registerDependency(ProductRegistry.class, productRegistry);
        commandManager.registerDependency(TransactionController.class, transactionController);
        commandManager.registerCommand(new IotaSpigotPocCommand());
    }

    /**
     * Load products from a file
     *
     * @param productRegistry The registry
     */
    private void loadProducts(final ProductRegistry productRegistry) {
        final File file = new File(this.getDataFolder(), "products.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
                // Save defaults
                final String str = new GsonBuilder().setPrettyPrinting().create().toJson(Arrays.asList(
                        new Product(0, 1337, "Test", "This is a test", Arrays.asList(
                                "say Hello there"
                        )),
                        new Product(1, 10, "Test 2", "This is also a test", Arrays.asList(
                                "give {PLAYER_NAME} apple 16"
                        ))
                ));
                Files.write(file.toPath(), str.getBytes(StandardCharsets.UTF_8));
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        productRegistry.load(file);
    }

    /**
     * Create a new mongo client
     *
     * @return A mongo client
     */
    private MongoClient getMongoClient() {
        // Initialize the POJO codec registry
        final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        // Initialize the MongoDB client
        final MongoClientSettings settings = MongoClientSettings.builder()
                .codecRegistry(codecRegistry)
                .applyConnectionString(new ConnectionString(this.getConfig().getString("storage.mongo.uri", "")))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build();
        return MongoClients.create(settings);
    }

}
