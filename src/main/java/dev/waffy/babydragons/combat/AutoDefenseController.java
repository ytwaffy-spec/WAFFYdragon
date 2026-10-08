package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.BaByDragonsPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
public final class AutoDefenseController implements Listener {
 private final BaByDragonsPlugin plugin;
 public AutoDefenseController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void damage(EntityDamageByEntityEvent event) {
  if(!(event.getEntity() instanceof Player owner) || !plugin.dragons().owns(owner) || event.getFinalDamage()<=0) return;
  Entity source=event.getDamager();
  if(source instanceof Projectile projectile) {
   if(!(projectile.getShooter() instanceof LivingEntity shooter)) return;
   source=shooter;
  }
  if(!(source instanceof LivingEntity attacker) || attacker.equals(owner) || plugin.dragons().managed(attacker)) return;
  plugin.attacks().startAuto(attacker);
 }
}
