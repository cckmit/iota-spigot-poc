package de.cerus.iotaspigotpoc.product;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.cerus.iotaspigotpoc.model.Product;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Very simple registry to cache products / packages.
 */
public class ProductRegistry {

    private final Map<Integer, Product> productMap = new LinkedHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void load(final File file) {
        try {
            for (final JsonElement jsonElement : new JsonParser().parse(new FileReader(file)).getAsJsonArray()) {
                final Product product = this.gson.fromJson(jsonElement, Product.class);
                this.productMap.put(product.getId(), product);
            }
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a product by its id
     *
     * @param id The product id
     *
     * @return The product or null
     */
    public Product getById(final int id) {
        return this.productMap.get(id);
    }

    /**
     * Retrieves all products
     *
     * @return all products
     */
    public List<Product> getAllProducts() {
        return new ArrayList<>(this.productMap.values());
    }

}
