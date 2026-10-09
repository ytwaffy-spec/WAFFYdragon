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
  if(plugin.damage().applyingDragonDamage() || plugin.dragons().data()==null || event.getFinalDamage()<=0) return;
  Entity source=event.getDamager();
  if(source instanceof Projectile projectile) {
   if(!(projectile.getShooter() instanceof LivingEntity shooter)) return;
   source=shooter;
  }
  if(!(source instanceof LivingEntity attacker) || plugin.dragons().managed(attacker)) return;
  var data=plugin.dragons().data();
  LivingEntity target=null;
  if(event.getEntity() instanceof Player owner && plugin.dragons().owns(owner) && !attacker.equals(owner) && data.defenseAttackEnabled) target=attacker;
  else if(attacker instanceof Player owner && plugin.dragons().owns(owner) && data.assistAttackEnabled && event.getEntity() instanceof LivingEntity living) target=living;
  if(target==null) return;
  LivingEntity selected=target;
  // Commit the original hit before cleansing or retaliating; plugin damage cannot recurse here.
  plugin.getServer().getScheduler().runTask(plugin,()->{
   if(!event.isCancelled() && event.getFinalDamage()>0 && plugin.dragons().data()==data
    && (attacker.getUniqueId().equals(data.ownerUuid)?data.assistAttackEnabled:data.defenseAttackEnabled)) plugin.attacks().startAuto(selected);
  });
 }
}
