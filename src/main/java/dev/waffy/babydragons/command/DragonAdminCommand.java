package dev.waffy.babydragons.command;
import dev.waffy.babydragons.*;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
public final class DragonAdminCommand implements CommandExecutor,TabCompleter {
 private final BaByDragonsPlugin plugin;
 private static final List<String> OPTIONS=List.of("spawn","remove","info","follow","sit","summon","animation","autoattack","manualattack","resetcooldowns","reload");
 public DragonAdminCommand(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
  if(!sender.hasPermission("babydragons.admin")) { sender.sendMessage("No permission.");return true; }
  if(args.length==0) { sender.sendMessage("/dragonadmin "+String.join(" | ",OPTIONS));return true; }
  String action=args[0].toLowerCase(Locale.ROOT);
  try {
   switch(action) {
    case "reload" -> { plugin.reloadSettings();sender.sendMessage("BaByDragons reloaded; model validation results are in console."); }
    case "info" -> plugin.info(sender);
    case "spawn" -> {
     if(!(sender instanceof Player p)) { sender.sendMessage("Spawn requires an in-game owner.");return true; }
     plugin.dragons().spawnOrRecover(p);sender.sendMessage("One Galaxy dragon is active; you are its owner.");
    }
    case "remove" -> { plugin.dragons().remove();sender.sendMessage("Dragon removed; unloaded stale parts are cleaned when loaded."); }
    case "summon" -> {
     plugin.dragons().requireValid();Player owner=plugin.dragons().owner();
     if(owner==null) { sender.sendMessage("The owner is offline.");return true; }
     plugin.dragons().summon(owner);sender.sendMessage("Dragon summoned to its owner.");
    }
    case "sit","follow" -> { plugin.dragons().state(action.equals("sit")?DragonState.SITTING:DragonState.FOLLOWING);sender.sendMessage("Dragon state updated."); }
    case "animation" -> {
     plugin.dragons().requireValid();
     if(plugin.attacks().busy()) { plugin.message(sender,"busy");return true; }
     if(args.length!=2 || !animations().contains(args[1])) { sender.sendMessage("/dragonadmin animation <"+String.join("|",animations())+">");return true; }
     sender.sendMessage(plugin.animations().temporary(args[1],plugin.tick(),100)?"Animation requested: "+args[1]:"ModelEngine could not play that animation.");
    }
    case "autoattack","manualattack" -> {
     if(plugin.dragons().data()==null) { plugin.message(sender,"no-dragon");return true; }
     if(args.length!=2 || !(args[1].equalsIgnoreCase("on")||args[1].equalsIgnoreCase("off"))) { sender.sendMessage("/dragonadmin "+action+" <on|off>");return true; }
     boolean value=args[1].equalsIgnoreCase("on");
     if(action.equals("autoattack")) plugin.setAuto(value);else plugin.setManual(value);
     sender.sendMessage(action+": "+(value?"ON":"OFF"));
    }
    case "resetcooldowns" -> {
     if(plugin.dragons().data()==null) { plugin.message(sender,"no-dragon");return true; }
     plugin.dragons().data().autoAttackCooldownUntil=0;plugin.dragons().data().manualAttackCooldownUntil=0;plugin.dragons().save();
     sender.sendMessage("Both cooldowns reset.");
    }
    default -> sender.sendMessage("/dragonadmin "+String.join(" | ",OPTIONS));
   }
  } catch(Exception e) { sender.sendMessage("Dragon action failed: "+e.getMessage());plugin.getLogger().log(java.util.logging.Level.WARNING,"Admin command failed",e); }
  return true;
 }
 private List<String> animations() {
  return List.of(plugin.settings().s("animations.idle"),plugin.settings().s("animations.follow"),plugin.settings().s("animations.pet"),plugin.settings().s("animations.auto-attack"));
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args) {
  if(!s.hasPermission("babydragons.admin") || args.length==0) return List.of();
  List<String> options=args.length==1?OPTIONS:args.length==2 && args[0].equalsIgnoreCase("animation")?animations()
   :args.length==2 && (args[0].equalsIgnoreCase("autoattack")||args[0].equalsIgnoreCase("manualattack"))?List.of("on","off"):List.of();
  return options.stream().filter(x->x.startsWith(args[args.length-1].toLowerCase(Locale.ROOT))).toList();
 }
}
