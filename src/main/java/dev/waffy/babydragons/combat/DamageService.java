package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.BaByDragonsPlugin;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Team;
public final class DamageService implements Listener {
 private final BaByDragonsPlugin plugin;
 private LivingEntity pending;
 private Player source;
 private boolean accepted;
 public boolean applyingDragonDamage() { return pending!=null; }
 public DamageService(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public boolean allowed(LivingEntity target,Player owner,boolean manual) {
  if(!target.isValid() || target.isDead() || target.isInvulnerable() || target.equals(owner)
   || plugin.dragons().managed(target) || !target.getWorld().equals(owner.getWorld()) || target instanceof ArmorStand) return false;
  if(target instanceof Tameable tame && tame.isTamed()) return false;
  if(target instanceof Player p) {
   if(!owner.getWorld().getPVP() || p.getGameMode()==org.bukkit.GameMode.SPECTATOR || p.getGameMode()==org.bukkit.GameMode.CREATIVE) return false;
   Team team=owner.getScoreboard().getEntryTeam(owner.getName());
   if(team!=null && team.hasEntry(p.getName()) && !team.allowFriendlyFire()) return false;
   Team global=owner.getServer().getScoreboardManager().getMainScoreboard().getEntryTeam(owner.getName());
   return global==null || !global.hasEntry(p.getName()) || global.allowFriendlyFire();
  }
  return true;
 }
 public boolean damage(LivingEntity target,Player owner,double amount,boolean manual) {
  if(!allowed(target,owner,manual)) return false;
  // Reject nested damage contexts rather than corrupting acceptance tracking.
  if(pending!=null) return false;
  pending=target;source=owner;accepted=false;
  int hurtTicks=target.getNoDamageTicks();
  try {
   // Eight-tick chain moments must still reach Bukkit's damage/protection pipeline.
   target.setNoDamageTicks(0);
   target.damage(amount,owner);return accepted;
  } finally {
   if(!accepted) target.setNoDamageTicks(hurtTicks);
   pending=null;source=null;
  }
 }
 @EventHandler(priority=EventPriority.MONITOR)
 public void record(EntityDamageByEntityEvent event) {
  if(event.getEntity().equals(pending) && event.getDamager().equals(source))
   accepted=!event.isCancelled() && event.getFinalDamage()>0;
 }
}
