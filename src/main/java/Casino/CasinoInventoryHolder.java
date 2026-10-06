package Casino;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Identifica el slot de apuesta para que las fichas sigan funcionando en el casino. */
public final class CasinoInventoryHolder implements InventoryHolder {
    private final int tokenSlot;
    private Inventory inventory;

    public CasinoInventoryHolder(int tokenSlot) {
        this.tokenSlot = tokenSlot;
    }

    public int getTokenSlot() { return tokenSlot; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override
    public Inventory getInventory() { return inventory; }
}
