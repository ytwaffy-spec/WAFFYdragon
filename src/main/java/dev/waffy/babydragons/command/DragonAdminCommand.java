package dev.waffy.babydragons.command;
import dev.waffy.babydragons.*;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
public final class DragonAdminCommand implements CommandExecutor,TabCompleter {
 private final BaByDragonsPlugin plugin;
 private static final List<String> OPTIONS=List.of("spawn","remove","info","follow","sit","summon","animation","animationstatus","galaxytest","firetest","movementdebug","assist","defense","autoattack","manualattack","resetcooldowns","reload");
 public DragonAdminCommand(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
  if(!sender.hasPermission("babydragons.admin")) { sender.sendMessage("No permission.");return true; }
  if(args.length==0) { sender.sendMessage("/dragonadmin "+String.join(" | ",OPTIONS));return true; }
  String action=args[0].toLowerCase(Locale.ROOT);
  try {
   switch(action) {
    case "reload" -> { plugin.reloadSettings();sender.sendMessage("BaByDragons reloaded; model validation results are in console."); }
    case "info","movementdebug" -> plugin.info(sender);
    case "animationstatus" -> plugin.models().status(sender);
    case "firetest","galaxytest" -> {
     plugin.dragons().requireValid();
     if(plugin.attacks().busy()) { plugin.message(sender,"busy");return true; }
     LivingEntity target=null;
     if(args.length==2) {
      Entity found;
      try { found=plugin.getServer().getEntity(UUID.fromString(args[1])); }
      catch(IllegalArgumentException ignored) { found=plugin.getServer().getPlayerExact(args[1]); }
      if(found instanceof LivingEntity living) target=living;
     } else target=plugin.manual().locked();
     if(target==null) { sender.sendMessage("/dragonadmin "+action+" <entity UUID or player name>, or acquire a Fire lock first.");return true; }
     boolean started=action.equals("firetest")?plugin.attacks().startManual(target):plugin.attacks().startAuto(target);
     sender.sendMessage(started?"Attack test started (normal damage and safety rules apply).":"Attack not started; check toggles, cooldown, owner and target eligibility.");
    }
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
    case "autoattack","manualattack","assist","defense" -> {
     if(plugin.dragons().data()==null) { plugin.message(sender,"no-dragon");return true; }
     if(args.length!=2 || !(args[1].equalsIgnoreCase("on")||args[1].equalsIgnoreCase("off"))) { sender.sendMessage("/dragonadmin "+action+" <on|off>");return true; }
     boolean value=args[1].equalsIgnoreCase("on");
     switch(action) { case "autoattack" -> plugin.setAuto(value);case "assist" -> plugin.setAssist(value);case "defense" -> plugin.setDefense(value);default -> plugin.setManual(value); }
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
  return List.of(plugin.settings().s("animations.idle"),plugin.settings().s("animations.follow"),plugin.settings().s("animations.pet"),plugin.settings().s("galaxy-attack.animation.id"));
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args) {
  if(!s.hasPermission("babydragons.admin") || args.length==0) return List.of();
  List<String> options=args.length==1?OPTIONS:args.length==2 && args[0].equalsIgnoreCase("animation")?animations()
   :args.length==2 && Set.of("autoattack","manualattack","assist","defense").contains(args[0].toLowerCase(Locale.ROOT))?List.of("on","off"):List.of();
  return options.stream().filter(x->x.startsWith(args[args.length-1].toLowerCase(Locale.ROOT))).toList();
 }
}
