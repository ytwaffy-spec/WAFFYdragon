package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.BaByDragonsPlugin;
import java.util.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

public final class FireLaunchProtection implements Listener {
 private record Launch(UUID token,UUID world,long started,boolean airborne,FireRegion region) {}
 private boolean correcting;
 private final Map<UUID,Launch> launches=new HashMap<>();
 private final BaByDragonsPlugin plugin;
 public FireLaunchProtection(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public static double apex(double velocity) {
  double height=0;
  for(int tick=0;tick<200 && velocity>0;tick++) { height+=velocity;velocity=(velocity-.08)*.98; }
  return height;
 }
 public static double launchSpeed() {
  double low=0,high=4;
  for(int i=0;i<50;i++) { double mid=(low+high)/2;if(apex(mid)<20) low=mid;else high=mid; }
  return (low+high)/2;
 }
 public void launch(Player player,UUID token) {
  launch(player,token,null);
 }
 public void launch(Player player,UUID token,FireRegion region) {
  launches.put(player.getUniqueId(),new Launch(token,player.getWorld().getUID(),System.currentTimeMillis(),false,region));
  player.setFallDistance(0);player.setVelocity(new Vector(0,launchSpeed(),0));
 }
 public void protect(Player player,UUID token) {
  launches.put(player.getUniqueId(),new Launch(token,player.getWorld().getUID(),System.currentTimeMillis(),false,null));
  player.setFallDistance(0);
 }
 public void constrain(Player player,FireRegion region,boolean vertical) {
  var location=player.getLocation();var bounded=region.clamp(location,vertical);
  if(location.distanceSquared(bounded)>.0001) {
   correcting=true;try { if(!player.teleport(bounded)) return; } finally { correcting=false; }
   location=player.getLocation();
  }
  Vector velocity=player.getVelocity();Vector boundedVelocity=region.velocity(location,velocity,vertical);
  if(!boundedVelocity.equals(velocity)) player.setVelocity(boundedVelocity);
 }
 public void tick() {
  long now=System.currentTimeMillis();
  launches.entrySet().removeIf(entry->{
   Player player=plugin.getServer().getPlayer(entry.getKey());Launch launch=entry.getValue();
   if(player==null || !player.isOnline() || player.isDead() || !player.getWorld().getUID().equals(launch.world()) || now-launch.started()>10000) return true;
   if(player.isOnGround()) return launch.airborne() || now-launch.started()>500;
   if(launch.region()!=null) constrain(player,launch.region(),false);
   if(!launch.airborne()) entry.setValue(new Launch(launch.token(),launch.world(),launch.started(),true,launch.region()));
   return false;
  });
 }
 @EventHandler(priority=EventPriority.HIGHEST)
 public void damage(EntityDamageEvent event) {
  if(event.getCause()!=EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) return;
  Launch launch=launches.remove(player.getUniqueId());
  if(launch!=null && player.getWorld().getUID().equals(launch.world()) && System.currentTimeMillis()-launch.started()<=10000) event.setCancelled(true);
 }
 @EventHandler public void quit(PlayerQuitEvent event) { launches.remove(event.getPlayer().getUniqueId()); }
 @EventHandler public void death(PlayerDeathEvent event) { launches.remove(event.getEntity().getUniqueId()); }
 @EventHandler public void world(PlayerChangedWorldEvent event) { launches.remove(event.getPlayer().getUniqueId()); }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void teleport(PlayerTeleportEvent event) { if(!correcting) launches.remove(event.getPlayer().getUniqueId()); }
 public void clear() { launches.clear(); }
}
