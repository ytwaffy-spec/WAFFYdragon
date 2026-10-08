package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
public final class ManualFireAttackController implements Listener {
 private final BaByDragonsPlugin plugin;
 private final Map<UUID,Long> lastTrigger=new HashMap<>();
 private long lastCooldown=-1;
 public ManualFireAttackController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public static boolean input(boolean owner,boolean permission,boolean enabled,int slot,boolean sneaking,String item,boolean rightClick) {
  return owner && permission && enabled && slot==8 && sneaking && ("AIR".equals(item)||"STICK".equals(item)) && rightClick;
 }
 @EventHandler(priority=EventPriority.HIGH)
 public void click(PlayerInteractEvent event) {
  if(event.getHand()!=EquipmentSlot.HAND || plugin.dragons().data()==null) return;
  Player p=event.getPlayer();Material item=p.getInventory().getItemInMainHand().getType();
  boolean right=event.getAction()==Action.RIGHT_CLICK_AIR || event.getAction()==Action.RIGHT_CLICK_BLOCK;
  String name=item.isAir()?"AIR":item.name();
  if(!input(plugin.dragons().owns(p),p.hasPermission("babydragons.attack"),plugin.dragons().data().manualAttackEnabled,
   p.getInventory().getHeldItemSlot(),p.isSneaking(),name,right) || !plugin.settings().allowedItem(name)) return;
  // Vanilla pre-cancels empty air interaction; honor protection-denied block/item interactions.
  if(event.getAction()==Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock()==Event.Result.DENY) return;
  if(item==Material.STICK && event.useItemInHand()==Event.Result.DENY) return;
  long tick=plugin.tick();if(tick-lastTrigger.getOrDefault(p.getUniqueId(),-100L)<5) return;
  lastTrigger.put(p.getUniqueId(),tick);
  if(plugin.attacks().busy()) { plugin.message(p,"busy");return; }
  long left=AttackGate.remainingSeconds(plugin.dragons().data().manualAttackCooldownUntil,System.currentTimeMillis());
  if(left>0) { p.sendActionBar(Component.text("Inferno Strike • "+left+"s"));return; }
  double range=plugin.settings().d("manual-attack.target-range");
  var hit=p.rayTraceBlocks(range,FluidCollisionMode.NEVER);
  if(hit==null || hit.getHitBlock()==null) { p.sendActionBar(Component.text("No target block within "+(int)range+" blocks."));return; }
  try {
   if(plugin.attacks().startManual(hit.getHitBlock().getLocation().add(.5,1.1,.5))) {
    event.setUseInteractedBlock(Event.Result.DENY);event.setUseItemInHand(Event.Result.DENY);
   }
  } catch(IllegalStateException error) { p.sendMessage(Component.text(error.getMessage())); }
 }
 public void feedback() {
  Player owner=plugin.dragons().owner();
  if(owner==null || plugin.dragons().data()==null || !plugin.dragons().data().manualAttackEnabled) { lastCooldown=-1;return; }
  long left=AttackGate.remainingSeconds(plugin.dragons().data().manualAttackCooldownUntil,System.currentTimeMillis());
  if(owner.getInventory().getHeldItemSlot()==8 && owner.hasPermission("babydragons.attack")) {
   if(left>0) owner.sendActionBar(Component.text("Inferno Strike • "+left+"s"));
   else if(lastCooldown!=0) plugin.action(owner,"ready");
   lastCooldown=left;
  } else lastCooldown=-1;
 }
 public void forget(Player p) { lastTrigger.remove(p.getUniqueId());lastCooldown=-1; }
}
