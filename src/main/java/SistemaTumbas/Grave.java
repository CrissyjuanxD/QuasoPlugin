package SistemaTumbas;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Grave {
    private final UUID id;
    private final UUID owner;
    private final String ownerName;
    private final Location location;
    private final long creationTime;
    private final long expiryTime;
    private Inventory inventory;

    // Los items van a un inventario de 54 slots, que es el que se abre al clickear la tumba
    public Grave(UUID id, UUID owner, String ownerName, Location location, long creationTime, long expiryTime, List<ItemStack> items) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.location = location;
        this.creationTime = creationTime;
        this.expiryTime = expiryTime;

        this.inventory = Bukkit.createInventory(null, 54, "Tumba de " + ownerName);
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), 54); i++) {
                if (items.get(i) != null) {
                    this.inventory.setItem(i, items.get(i));
                }
            }
        }
    }

    public UUID getId() { return id; }
    public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public Location getLocation() { return location; }
    public long getCreationTime() { return creationTime; }
    public long getExpiryTime() { return expiryTime; }
    public Inventory getInventory() { return inventory; }

    public boolean isEmpty() {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }

    public List<ItemStack> getItems() {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && !item.getType().isAir()) {
                list.add(item);
            }
        }
        return list;
    }
}