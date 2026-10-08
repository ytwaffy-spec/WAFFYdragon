package dev.waffy.babydragons.gui;
import java.util.UUID;
import org.bukkit.inventory.*;
public final class DragonMenuHolder implements InventoryHolder {
 public enum Kind { CONTROL, STORAGE }
 public final UUID owner,dragon;
 public final Kind kind;
 private Inventory inventory;
 public DragonMenuHolder(UUID owner,UUID dragon,Kind kind) { this.owner=owner;this.dragon=dragon;this.kind=kind; }
 public void bind(Inventory inventory) { this.inventory=inventory; }
 @Override public Inventory getInventory() { return inventory; }
}
