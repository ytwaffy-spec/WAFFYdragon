package dev.waffy.babydragons.effects;
import dev.waffy.babydragons.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.*;
public final class DragonEffectController implements Listener {
 private final BaByDragonsPlugin plugin;
 private final Map<UUID,Map<PotionEffectType,EffectLease>> leases=new HashMap<>();
 private boolean ownChange;
 public DragonEffectController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void tick() {
  DragonData d=plugin.dragons().data();Player player=plugin.dragons().owner();
  if(d==null || !d.effectsEnabled || d.state!=DragonState.FOLLOWING || !plugin.dragons().valid()
    || player==null || !player.isOnline() || player.isDead()) { clearAll();return; }
  apply(player,PotionEffectType.STRENGTH,"strength");apply(player,PotionEffectType.HASTE,"haste");apply(player,PotionEffectType.ABSORPTION,"absorption");
 }
 private void apply(Player player,PotionEffectType type,String config) {
  var owned=leases.computeIfAbsent(player.getUniqueId(),ignored->new HashMap<>());
  if(!plugin.settings().b("effects."+config+".enabled")) { clear(player,type,owned);return; }
  long tick=Bukkit.getCurrentTick();PotionEffect current=player.getPotionEffect(type);EffectLease lease=owned.get(type);
  boolean ours=current!=null && lease!=null && matches(lease,current,tick);
  if(current!=null && !ours) { owned.remove(type);return; }
  int duration=plugin.settings().i("effects.duration-ticks"),level=plugin.settings().i("effects."+config+".amplifier");
  double absorptionBefore=player.getAbsorptionAmount();ownChange=true;
  try {
   boolean applied=player.addPotionEffect(new PotionEffect(type,duration,level,true,false,true));
   PotionEffect actual=player.getPotionEffect(type);
   if(applied && actual!=null && actual.getAmplifier()==level && actual.getDuration()==duration)
    owned.put(type,new EffectLease(level,tick+duration,true,false,true));
   else owned.remove(type);
   if(ours && type.equals(PotionEffectType.ABSORPTION) && player.getAbsorptionAmount()>absorptionBefore)
    player.setAbsorptionAmount(absorptionBefore);
  } finally { ownChange=false; }
 }
 private static boolean matches(EffectLease l,PotionEffect e,long tick) {
  return l.matches(e.getAmplifier(),e.getDuration(),tick,e.isAmbient(),e.hasParticles(),e.hasIcon());
 }
 private void clear(Player player,PotionEffectType type,Map<PotionEffectType,EffectLease> owned) {
  EffectLease lease=owned.remove(type);PotionEffect effect=player.getPotionEffect(type);
  if(lease==null || effect==null || !matches(lease,effect,Bukkit.getCurrentTick())) return;
  ownChange=true;try { player.removePotionEffect(type); } finally { ownChange=false; }
 }
 public void clearAll() {
  for(var entry:leases.entrySet()) {
   Player p=Bukkit.getPlayer(entry.getKey());
   if(p!=null) for(PotionEffectType t:new ArrayList<>(entry.getValue().keySet())) clear(p,t,entry.getValue());
  }
  leases.clear();
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void externalChange(EntityPotionEffectEvent event) {
  if(ownChange) return;
  var owned=leases.get(event.getEntity().getUniqueId());if(owned!=null) owned.remove(event.getModifiedType());
 }
}
