package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.BaByDragonsPlugin;
import org.bukkit.*;
import org.bukkit.entity.Player;
public final class CombatSounds {
 private final BaByDragonsPlugin plugin;
 public CombatSounds(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void play(String key,Location location,Player audience) {
  String p="sounds."+key;
  if(!plugin.settings().b(p+".enabled")) return;
  Sound sound=Sound.valueOf(plugin.settings().s(p+".sound"));
  float volume=(float)plugin.settings().d(p+".volume"),pitch=(float)plugin.settings().d(p+".pitch");
  if(audience!=null) audience.playSound(location,sound,volume,pitch);
  else location.getWorld().playSound(location,sound,volume,pitch);
 }
}
