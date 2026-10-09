package dev.waffy.babydragons.listener;
import dev.waffy.babydragons.*;
import dev.waffy.babydragons.gui.DragonMenuHolder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
public final class InventoryListener implements Listener {
 private final BaByDragonsPlugin plugin;
 public InventoryListener(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 @EventHandler(priority=EventPriority.HIGHEST)
 public void click(InventoryClickEvent event) {
  if(!(event.getView().getTopInventory().getHolder() instanceof DragonMenuHolder holder)) return;
  event.setCancelled(true); // All top/bottom transfers, shift, hotbar, collect and drop.
  if(!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(holder.owner)
   || !plugin.dragons().owns(p) || !plugin.dragons().data().dragonId.equals(holder.dragon) || !p.hasPermission("babydragons.use")) return;
  if(event.getClickedInventory()!=event.getView().getTopInventory() || !(event.getClick()==ClickType.LEFT || event.getClick()==ClickType.RIGHT)) return;
  int slot=event.getRawSlot();
  plugin.getServer().getScheduler().runTask(plugin,()->{
   if(!p.isOnline() || !p.hasPermission("babydragons.use") || !plugin.dragons().owns(p)
    || !plugin.dragons().data().dragonId.equals(holder.dragon) || p.getOpenInventory().getTopInventory().getHolder()!=holder) return;
   p.playSound(p.getLocation(),Sound.UI_BUTTON_CLICK,.25f,1.3f);
   try {
    if(holder.kind==DragonMenuHolder.Kind.STORAGE) {
     if(slot==49) plugin.gui().open(p);else if(slot==53) p.closeInventory();return;
    }
    switch(slot) {
     case 10 -> { plugin.toggleEffects();plugin.gui().open(p); }
     case 11 -> { p.closeInventory();plugin.renameHelp(p); }
     case 12,14 -> {
      DragonState state=slot==12?DragonState.SITTING:DragonState.FOLLOWING;p.closeInventory();
      if(plugin.dragons().data().state==state && !plugin.attacks().busy()) p.sendMessage("Your dragon is already "+state.name().toLowerCase(java.util.Locale.ROOT)+".");
      else { plugin.dragons().state(state);p.sendActionBar(plugin.mm("<#CCAAFF>Dragon is "+state.name().toLowerCase(java.util.Locale.ROOT)+".")); }
     }
     case 13,15,16 -> {
      if(!p.hasPermission("babydragons.attack")) { p.sendMessage("No attack permission.");return; }
      if(slot==13) plugin.setAssist(!plugin.dragons().data().assistAttackEnabled);
      else if(slot==15) plugin.setDefense(!plugin.dragons().data().defenseAttackEnabled);
      else plugin.setManual(!plugin.dragons().data().manualAttackEnabled);
      plugin.gui().open(p);
     }
     case 22 -> plugin.storageGui().open(p);
     case 26 -> p.closeInventory();
     default -> { }
    }
   } catch(Exception error) { p.sendMessage("Dragon action failed: "+error.getMessage()); }
  });
 }
 @EventHandler(priority=EventPriority.HIGHEST)
 public void drag(InventoryDragEvent event) { if(event.getView().getTopInventory().getHolder() instanceof DragonMenuHolder) event.setCancelled(true); }
 @EventHandler(priority=EventPriority.HIGHEST)
 public void hopper(InventoryMoveItemEvent event) {
  if(event.getSource().getHolder() instanceof DragonMenuHolder || event.getDestination().getHolder() instanceof DragonMenuHolder) event.setCancelled(true);
 }
}
