package dev.waffy.babydragons.listener;
import dev.waffy.babydragons.*;
import dev.waffy.babydragons.model.ModelEngineController;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
public final class DragonInteractionListener implements Listener {
 private final BaByDragonsPlugin plugin;
 private final Map<UUID,Long> lastClick=new HashMap<>();
 public DragonInteractionListener(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void modelInteract(ModelEngineController.Interaction event) {
  if(event.hand()!=EquipmentSlot.HAND || event.action().equals("ATTACK") || !plugin.dragons().isController(event.base())) return;
  interact(event.player());
 }
 @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
 public void bukkitInteract(PlayerInteractEntityEvent event) {
  if(event.getHand()!=EquipmentSlot.HAND || !plugin.dragons().isController(event.getRightClicked().getUniqueId())) return;
  event.setCancelled(true);interact(event.getPlayer());
 }
 private void interact(Player player) {
  if(!player.getInventory().getItemInMainHand().getType().isAir()) return;
  long now=plugin.tick();if(now-lastClick.getOrDefault(player.getUniqueId(),-100L)<5) return;
  lastClick.put(player.getUniqueId(),now);
  if(!plugin.dragons().owns(player)) { plugin.message(player,"not-owner");return; }
  if(!player.hasPermission("babydragons.use")) return;
  if(player.isSneaking()) {
   if(plugin.attacks().busy()) { plugin.message(player,"busy");return; }
   if(!plugin.dragons().valid()) return;
   if(plugin.animations().temporary(plugin.settings().s("animations.pet"),now,plugin.settings().i("animations.pet-ticks"))) {
    Location where=plugin.dragons().controller().getLocation().add(0,1,0);
    where.getWorld().spawnParticle(Particle.HEART,where,5,.5,.3,.5,0);
    where.getWorld().playSound(where,Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM,.2f,1.2f);
   }
  } else plugin.gui().open(player);
 }
 public void forget(Player p) { lastClick.remove(p.getUniqueId()); }
}
