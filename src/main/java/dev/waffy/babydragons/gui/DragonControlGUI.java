package dev.waffy.babydragons.gui;
import dev.waffy.babydragons.*;
import dev.waffy.babydragons.combat.AttackGate;
import java.util.Arrays;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
public final class DragonControlGUI {
 private final BaByDragonsPlugin plugin;
 public DragonControlGUI(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void open(Player player) {
  if(!player.hasPermission("babydragons.use")) return;
  if(!plugin.dragons().owns(player)) { plugin.message(player,"not-owner");return; }
  DragonData d=plugin.dragons().data();
  var holder=new DragonMenuHolder(player.getUniqueId(),d.dragonId,DragonMenuHolder.Kind.CONTROL);
  Inventory inventory=Bukkit.createInventory(holder,27,plugin.mm(plugin.settings().s("gui.title")));holder.bind(inventory);render(inventory,d);
  player.openInventory(inventory);
 }
 public void refreshOpen() {
  for(Player p:Bukkit.getOnlinePlayers()) {
   Inventory inventory=p.getOpenInventory().getTopInventory();
   if(inventory.getHolder() instanceof DragonMenuHolder holder && holder.kind==DragonMenuHolder.Kind.CONTROL && plugin.dragons().owns(p)) render(inventory,plugin.dragons().data());
  }
 }
 private void render(Inventory inventory,DragonData d) {
  fill(inventory);
  inventory.setItem(10,item(Material.BEACON,title("effects","<#B77AFF>✦ Dragon Effects"),
   "<gray>Passive powers granted by your dragon.","","<white>Strength II","<white>Haste II","<white>Absorption","",status(d.effectsEnabled),toggle(d.effectsEnabled)));
  inventory.setItem(11,item(Material.NAME_TAG,title("rename","<#FF99DD>✦ Rename Dragon"),
   "<gray>Choose a name and a color.","","<#CCAAFF>Click for command examples"));
  inventory.setItem(12,item(Material.OAK_STAIRS,title("sit","<#D4B8FF>✦ Sit"),
   "<gray>Remain at the current location.","",d.state==DragonState.SITTING?"<#55FF88>Currently sitting":"<#CCAAFF>Click to sit"));
  inventory.setItem(14,item(Material.FEATHER,title("follow","<#D4B8FF>✦ Follow"),
   "<gray>Travel alongside your owner.","",d.state==DragonState.FOLLOWING?"<#55FF88>Currently following":"<#CCAAFF>Click to follow"));
  inventory.setItem(13,item(Material.IRON_SWORD,title("assist","<#B777FF>⚔ Combat Assist"),
   "<gray>When you attack an enemy,","<gray>your dragon joins the fight.","",
   status(d.assistAttackEnabled),attackStatus(d.autoAttackCooldownUntil),toggle(d.assistAttackEnabled)));
  inventory.setItem(15,item(Material.SHIELD,title("defense","<#77BBFF>🛡 Guardian Defense"),
   "<gray>When an enemy attacks you,","<gray>your dragon retaliates.","",
   status(d.defenseAttackEnabled),attackStatus(d.autoAttackCooldownUntil),toggle(d.defenseAttackEnabled)));
  inventory.setItem(16,item(Material.FIRE_CHARGE,title("manual","<#FF9666>✦ Inferno Strike"),
   "<gray>Slot 9 • stick • locked entity","<gray>Sneak + right-click to strike.","<gray>Cooldown: None — Phase 1",
   status(d.manualAttackEnabled),plugin.attacks().busy()?"<#FFCC88>ATTACKING":plugin.manual().locked()==null?"<gray>NO TARGET":"<#55FF88>READY",toggle(d.manualAttackEnabled)));
  inventory.setItem(22,item(Material.CHEST,title("storage","<#DD99FF>✦ Dragon Storage"),
   "<gray>Storage is locked in Phase 1.","<#CCAAFF>Click to preview"));
  inventory.setItem(26,item(Material.BARRIER,title("close","<#FF7777>Close")));
 }
 private String title(String key,String fallback) { return plugin.text("gui."+key+".name",fallback); }
 public ItemStack item(Material material,String name,String... lore) {
  ItemStack item=new ItemStack(material);
  item.editMeta(meta->{
   meta.displayName(plugin.mm(name).decoration(TextDecoration.ITALIC,false));
   meta.lore(Arrays.stream(lore).map(line->plugin.mm(line).decoration(TextDecoration.ITALIC,false)).toList());
  });
  return item;
 }
 public void fill(Inventory inventory) {
  for(int i=0;i<inventory.getSize();i++) inventory.setItem(i,item(i%9==0||i%9==8?Material.PURPLE_STAINED_GLASS_PANE:Material.BLACK_STAINED_GLASS_PANE," "));
 }
 private static String status(boolean enabled) { return enabled?"<#55FF88>Status: ENABLED":"<#FF5555>Status: DISABLED"; }
 private static String toggle(boolean enabled) { return "<gray>Click to "+(enabled?"disable":"enable"); }
 private static String cooldown(long until) {
  long left=AttackGate.remainingSeconds(until,System.currentTimeMillis());return left==0?"<#55FF88>Ready":"<#FFCC88>Cooldown: "+left+"s";
 }
 private String attackStatus(long until) { return plugin.attacks().busy()?"<#FFCC88>ATTACKING":cooldown(until); }
}
