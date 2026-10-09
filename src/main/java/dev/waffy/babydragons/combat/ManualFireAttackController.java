package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.*;

public final class ManualFireAttackController implements Listener {
 private final BaByDragonsPlugin plugin;
 private final TargetLock lock=new TargetLock();
 private LivingEntity target;
 private boolean ownsGlow,changingGlow;
 private long lastTrigger=-100;
 public ManualFireAttackController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public static boolean input(boolean owner,boolean permission,boolean enabled,int slot,boolean sneaking,String item,boolean rightClick) {
  return owner && permission && enabled && slot==8 && sneaking && "STICK".equals(item) && rightClick;
 }
 private boolean equipped(Player p) {
  return p!=null && p.isOnline() && !p.isDead() && plugin.dragons().valid() && plugin.dragons().data().manualAttackEnabled
   && p.hasPermission("babydragons.attack") && p.getInventory().getHeldItemSlot()==8
   && p.getInventory().getItemInMainHand().getType()==Material.STICK;
 }
 private boolean valid(LivingEntity entity,Player owner) {
  double range=plugin.settings().d("fire-attack.target-range");
  return entity!=null && plugin.damage().allowed(entity,owner,true)
   && owner.getEyeLocation().distanceSquared(entity.getEyeLocation())<=range*range && owner.hasLineOfSight(entity);
 }
 public void tick() {
  Player owner=plugin.dragons().owner();
  if(!equipped(owner)) { clear();return; }
  Location eye=owner.getEyeLocation();
  var hit=owner.getWorld().rayTrace(eye,eye.getDirection(),plugin.settings().d("fire-attack.target-range"),
   FluidCollisionMode.NEVER,true,.2,e->e instanceof LivingEntity living && plugin.damage().allowed(living,owner,true));
  LivingEntity aimed=hit!=null && hit.getHitEntity() instanceof LivingEntity living?living:null;
  if(aimed!=null && !valid(aimed,owner)) aimed=null;
  boolean changed=lock.update(aimed==null?null:aimed.getUniqueId(),valid(target,owner),System.currentTimeMillis(),plugin.settings().i("fire-attack.target-lock-grace-millis"));
  if(changed) {
   releaseGlow();target=aimed;
   if(target!=null) { glow();plugin.sounds().play("fire-lock",owner.getLocation(),owner); }
  } else if(target!=null && ownsGlow && plugin.tick()%5==0) glow();
 }
 private void glow() {
  if(!ownsGlow && (target.isGlowing() || target.hasPotionEffect(PotionEffectType.GLOWING))) return;
  changingGlow=true;
  try { ownsGlow=target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,12,0,false,false,true)); }
  finally { changingGlow=false; }
 }
 private void releaseGlow() {
  if(target!=null && ownsGlow) {
   changingGlow=true;
   try { target.removePotionEffect(PotionEffectType.GLOWING); }
   finally { changingGlow=false; }
  }
  ownsGlow=false;
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
 public void externalGlow(EntityPotionEffectEvent event) {
  if(!changingGlow && target!=null && event.getEntity().getUniqueId().equals(target.getUniqueId())
   && event.getModifiedType().equals(PotionEffectType.GLOWING)) ownsGlow=false;
 }
 public void clear() { releaseGlow();target=null;lock.clear(); }
 public LivingEntity locked() { return target; }
 public String status() {
  Player owner=plugin.dragons().owner();
  return target==null?"NO TARGET":"locked="+target.getUniqueId()+" | distance="+(owner!=null && owner.getWorld().equals(target.getWorld())?owner.getEyeLocation().distance(target.getEyeLocation()):"n/a");
 }
 @EventHandler(priority=EventPriority.HIGH)
 public void click(PlayerInteractEvent event) {
  if(event.getHand()!=EquipmentSlot.HAND) return;
  boolean right=event.getAction()==Action.RIGHT_CLICK_AIR || event.getAction()==Action.RIGHT_CLICK_BLOCK;
  if(!right || (event.getAction()==Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock()==Event.Result.DENY)
   || event.useItemInHand()==Event.Result.DENY) return;
  if(trigger(event.getPlayer())) { event.setUseInteractedBlock(Event.Result.DENY);event.setUseItemInHand(Event.Result.DENY); }
 }
 @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
 public void entityClick(PlayerInteractEntityEvent event) {
  if(event.getHand()==EquipmentSlot.HAND && trigger(event.getPlayer())) event.setCancelled(true);
 }
 private boolean trigger(Player p) {
  if(plugin.dragons().data()==null || !input(plugin.dragons().owns(p),p.hasPermission("babydragons.attack"),plugin.dragons().data().manualAttackEnabled,
   p.getInventory().getHeldItemSlot(),p.isSneaking(),p.getInventory().getItemInMainHand().getType().name(),true)) return false;
  if(plugin.tick()-lastTrigger<2) return false;lastTrigger=plugin.tick();
  if(plugin.attacks().busy()) { plugin.busy(p);return true; }
  tick();
  if(!valid(target,p)) { p.sendActionBar(Component.text("No target locked."));return true; }
  try { plugin.attacks().startManual(target); }
  catch(IllegalStateException error) { p.sendMessage(Component.text(error.getMessage())); }
  return true;
 }
 public void feedback() {
  Player owner=plugin.dragons().owner();if(!equipped(owner)) return;
  owner.sendActionBar(Component.text(plugin.attacks().busy()?plugin.dragons().data().dragonName+" is already attacking.":target==null?"No target locked.":"Inferno Strike READY | "+target.getName()));
 }
 public void forget(Player p) { if(plugin.dragons().owns(p)) { clear();lastTrigger=-100; } }
}
