package dev.waffy.babydragons.gui;
import dev.waffy.babydragons.BaByDragonsPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
public final class DragonStorageGUI {
 private final BaByDragonsPlugin plugin;
 public DragonStorageGUI(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void open(Player player) {
  if(!plugin.dragons().owns(player) || !player.hasPermission("babydragons.use")) return;
  var holder=new DragonMenuHolder(player.getUniqueId(),plugin.dragons().data().dragonId,DragonMenuHolder.Kind.STORAGE);
  Inventory inventory=Bukkit.createInventory(holder,54,plugin.mm(plugin.settings().s("storage.title")));holder.bind(inventory);
  plugin.gui().fill(inventory);
  inventory.setItem(22,plugin.gui().item(Material.CHEST,"<#DD99FF>Dragon Storage",
   plugin.text("coming-soon","<gray>Coming in Phase 2"),"","<gray>No items can be stored here yet."));
  inventory.setItem(49,plugin.gui().item(Material.ARROW,"<#DD99FF>Back"));
  inventory.setItem(53,plugin.gui().item(Material.BARRIER,"<#FF7777>Close"));
  player.openInventory(inventory);
 }
}
