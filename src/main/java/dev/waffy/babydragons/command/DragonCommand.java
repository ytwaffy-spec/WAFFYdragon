package dev.waffy.babydragons.command;
import dev.waffy.babydragons.*;
import java.util.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.format.NamedTextColor;
public final class DragonCommand implements CommandExecutor,TabCompleter {
 private final BaByDragonsPlugin plugin;
 private static final List<String> OPTIONS=List.of("gui","sit","follow","summon","name","info");
 public DragonCommand(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 @Override public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args) {
  if(!(sender instanceof Player p)) { sender.sendMessage("Run this command in game.");return true; }
  if(!p.hasPermission("babydragons.use")) { sender.sendMessage("No permission.");return true; }
  if(plugin.dragons().data()==null) { plugin.message(sender,"no-dragon");return true; }
  if(!plugin.dragons().owns(p)) { plugin.message(sender,"not-owner");return true; }
  String action=args.length==0?"gui":args[0].toLowerCase(Locale.ROOT);
  try {
   switch(action) {
    case "gui" -> plugin.gui().open(p);
    case "sit","follow" -> {
     DragonState state=action.equals("sit")?DragonState.SITTING:DragonState.FOLLOWING;
     if(plugin.dragons().data().state==state && !plugin.attacks().busy()) sender.sendMessage("Your dragon is already "+state.name().toLowerCase(Locale.ROOT)+".");
     else { plugin.dragons().state(state);sender.sendMessage("Dragon state: "+state); }
    }
    case "summon" -> { plugin.dragons().summon(p);sender.sendMessage("Your existing dragon has been summoned."); }
    case "name" -> {
     if(!p.hasPermission("babydragons.name")) { sender.sendMessage("No rename permission.");return true; }
     if(args.length<3) { plugin.renameHelp(p);return true; }
     plugin.dragons().rename(String.join(" ",Arrays.copyOfRange(args,1,args.length-1)),args[args.length-1]);sender.sendMessage("Dragon name updated.");
    }
    case "info" -> plugin.info(sender);
    default -> sender.sendMessage("/dragon "+String.join(" | ",OPTIONS));
   }
  } catch(Exception error) { sender.sendMessage("Dragon action failed: "+error.getMessage()); }
  return true;
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String alias,String[] args) {
  if(!s.hasPermission("babydragons.use") || args.length==0) return List.of();
  List<String> choices=args.length==1?OPTIONS:args.length>=3 && args[0].equalsIgnoreCase("name")?new ArrayList<>(NamedTextColor.NAMES.keys()):List.of();
  if(args.length>=3 && args[0].equalsIgnoreCase("name")) { choices.add("purple");choices.add("pink");choices.add("#B026FF"); }
  String prefix=args[args.length-1].toLowerCase(Locale.ROOT);return choices.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
 }
}
