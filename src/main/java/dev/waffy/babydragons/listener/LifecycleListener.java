package dev.waffy.babydragons.listener;
import dev.waffy.babydragons.BaByDragonsPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.world.EntitiesLoadEvent;
public final class LifecycleListener implements Listener {
 private final BaByDragonsPlugin plugin;
 public LifecycleListener(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 private void interrupt(Player p) {
  if(!plugin.dragons().owns(p)) return;
  try { plugin.attacks().cancel(); }
  finally { plugin.effects().clearAll();plugin.dragons().save();p.closeInventory(); }
 }
 @EventHandler public void quit(PlayerQuitEvent e) {
  try { interrupt(e.getPlayer()); }
  finally { plugin.manual().forget(e.getPlayer());plugin.interactions().forget(e.getPlayer()); }
 }
 @EventHandler public void death(PlayerDeathEvent e) { interrupt(e.getEntity()); }
 @EventHandler public void world(PlayerChangedWorldEvent e) { interrupt(e.getPlayer()); }
 @EventHandler public void entities(EntitiesLoadEvent e) { for(Entity entity:e.getEntities()) plugin.dragons().cleanLoaded(entity); }
 @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageEvent e) { if(plugin.dragons().managed(e.getEntity())) e.setCancelled(true); }
 @EventHandler(priority=EventPriority.HIGHEST) public void outgoing(EntityDamageByEntityEvent e) { if(plugin.dragons().managed(e.getDamager())) e.setCancelled(true); }
 private boolean crystal(Entity e) { return e!=null && plugin.dragons().managed(e) && "crystal".equals(plugin.dragons().tag(e,"role")); }
 @EventHandler(priority=EventPriority.HIGHEST) public void prime(ExplosionPrimeEvent e) { if(crystal(e.getEntity())) e.setCancelled(true); }
 @EventHandler(priority=EventPriority.HIGHEST) public void explode(EntityExplodeEvent e) {
  if(crystal(e.getEntity())) { e.blockList().clear();e.setCancelled(true); }
 }
 @EventHandler(priority=EventPriority.HIGHEST) public void ignite(BlockIgniteEvent e) { if(crystal(e.getIgnitingEntity())) e.setCancelled(true); }
}
