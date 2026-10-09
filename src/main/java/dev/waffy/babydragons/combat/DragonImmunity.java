package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.*;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
public final class DragonImmunity implements Listener {
 private final BaByDragonsPlugin plugin;
 private UUID ownerId;
 public DragonImmunity(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public boolean active() { return ownerId!=null; }
 public void begin(Player owner) {
  if(!plugin.settings().b("dragon-immunity.enabled")) return;
  ownerId=owner.getUniqueId();
  if(plugin.settings().b("dragon-immunity.remove-negative-effects"))
   for(String name:plugin.settings().strings("dragon-immunity.harmful-effects")) {
    PotionEffectType type=PotionEffectType.getByName(name);if(type!=null) owner.removePotionEffect(type);
   }
  if(plugin.settings().b("dragon-immunity.notify-chat")) {
   var d=plugin.dragons().data();
   owner.sendMessage(Component.text("Immunity granted by ").append(Component.text(d.dragonName,NameRules.color(d.nameColor))));
  }
 }
 public void end() {
  UUID previous=ownerId;ownerId=null;
  Player owner=previous==null?null:plugin.getServer().getPlayer(previous);
  if(owner!=null && plugin.settings().b("dragon-immunity.notify-end")) owner.sendActionBar(Component.text("Dragon Immunity ended."));
 }
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
 public void damage(EntityDamageByEntityEvent event) {
  if(ownerId==null || !event.getEntity().getUniqueId().equals(ownerId)) return;
  if(event.getDamager() instanceof LivingEntity || event.getDamager() instanceof Projectile) event.setCancelled(true);
 }
}
