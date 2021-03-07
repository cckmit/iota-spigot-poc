package de.cerus.iotaspigotpoc.model;

import java.util.List;

/**
 * Product model
 */
public class Product {

    private final int id;
    private final long price;
    private final String name;
    private final String description;
    private final List<String> commands;

    public Product(final int id, final long price, final String name, final String description, final List<String> commands) {
        this.id = id;
        this.price = price;
        this.name = name;
        this.description = description;
        this.commands = commands;
    }

    public int getId() {
        return this.id;
    }

    public long getPrice() {
        return this.price;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public List<String> getCommands() {
        return this.commands;
    }

}
