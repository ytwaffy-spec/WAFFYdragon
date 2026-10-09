package dev.waffy.babydragons;
import dev.waffy.babydragons.movement.DragonMovementController;
import dev.waffy.babydragons.persistence.DragonDataStore;
import java.io.IOException;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;

public final class DragonManager {
 private final BaByDragonsPlugin plugin;
 private final DragonDataStore store;
 private DragonData data;
 private Vex controller;
 private TextDisplay label;
 private Chunk ticket;
 private boolean recovering;
 private Location lastLabel;
 public DragonManager(BaByDragonsPlugin plugin) throws IOException {
  this.plugin=plugin;store=new DragonDataStore(plugin.getDataFolder().toPath().resolve("data.yml"));
  data=store.load().orElse(null);
 }
 public DragonData data() { return data; }
 public Vex controller() { return controller; }
 public boolean valid() { return data!=null && controller!=null && controller.isValid() && plugin.models().baseReady(); }
 public Player owner() { return data==null?null:Bukkit.getPlayer(data.ownerUuid); }
 public boolean owns(Player p) { return data!=null && data.ownerUuid.equals(p.getUniqueId()); }
 public boolean isController(UUID id) { return data!=null && Objects.equals(data.controllerEntityUuid,id); }
 public void mark(Entity entity,String role) {
  entity.getPersistentDataContainer().set(plugin.key("dragon_id"),PersistentDataType.STRING,data.dragonId.toString());
  entity.getPersistentDataContainer().set(plugin.key("role"),PersistentDataType.STRING,role);
  entity.getPersistentDataContainer().set(plugin.key("owner"),PersistentDataType.STRING,data.ownerUuid.toString());
  entity.setPersistent(true);
 }
 public String tag(Entity entity,String field) { return entity.getPersistentDataContainer().get(plugin.key(field),PersistentDataType.STRING); }
 public boolean managed(Entity entity) {
  return tag(entity,"dragon_id")!=null && Set.of("controller","name","crystal").contains(Objects.toString(tag(entity,"role"),""));
 }
 public void spawnOrRecover(Player player) {
  if(!plugin.models().baseReady()) throw new IllegalStateException("Galaxy model unavailable; check console and /dragonadmin reload.");
  plugin.attacks().cancel();plugin.manual().clear();plugin.effects().clearAll();
  boolean fresh=data==null;
  if(fresh) {
   Location where=plugin.movement().safeNear(player);
   data=new DragonData();data.dragonId=UUID.randomUUID();data.ownerUuid=player.getUniqueId();
   data.location=DragonMovementController.position(where);data.baseModelId=plugin.settings().s("models.galaxy.id");
   data.assistAttackEnabled=plugin.settings().b("galaxy-attack.assist-enabled-by-default");
   data.defenseAttackEnabled=plugin.settings().b("galaxy-attack.defense-enabled-by-default");
   data.manualAttackEnabled=plugin.settings().b("fire-attack.enabled-by-default");
  }
  try { recover();data.ownerUuid=player.getUniqueId();mark(controller,"controller");mark(label,"name");save(); }
  catch(RuntimeException error) {
   if(fresh) {
    try { plugin.models().detach(); }
    finally {
     if(controller!=null) controller.remove();if(label!=null) label.remove();
     releaseTicket();data=null;controller=null;label=null;
    }
   }
   throw error;
  }
 }
 private void configure(Vex e) {
  // Vex travel physics requires AI ticking; removing all goals prevents vanilla wandering/attacks.
  e.setAI(true);plugin.getServer().getMobGoals().removeAllGoals(e);
  e.setInvisible(true);e.setSilent(true);e.setInvulnerable(true);e.setCollidable(false);
  e.setGravity(false);e.setRemoveWhenFarAway(false);e.setLimitedLifetime(false);e.setCanPickupItems(false);
 }
 public void recover() {
  if(data==null || !plugin.models().baseReady()) return;
  recovering=true;
  try {
   Location where=DragonMovementController.location(data.state==DragonState.SITTING?data.sittingLocation:data.location);
   holdChunk(where);
   List<Entity> candidates=new ArrayList<>();
   for(World world:Bukkit.getWorlds()) for(Chunk chunk:world.getLoadedChunks()) for(Entity e:chunk.getEntities())
    if(data.dragonId.toString().equals(tag(e,"dragon_id"))) candidates.add(e);
   controller=candidates.stream().filter(Vex.class::isInstance).filter(e->"controller".equals(tag(e,"role")))
    .sorted(Comparator.comparing(e->!e.getUniqueId().equals(data.controllerEntityUuid))).map(Vex.class::cast).findFirst().orElse(null);
   if(controller==null) {
    controller=where.getWorld().spawn(where,Vex.class,e->{mark(e,"controller");configure(e);});
    plugin.getLogger().info("Created missing controller for dragon "+data.dragonId);
   } else { configure(controller);plugin.getLogger().info("Recovered controller "+controller.getUniqueId()); }
   data.controllerEntityUuid=controller.getUniqueId();plugin.movement().teleport(controller,where);
   label=candidates.stream().filter(TextDisplay.class::isInstance).filter(e->"name".equals(tag(e,"role")))
    .sorted(Comparator.comparing(e->!e.getUniqueId().equals(data.nameEntityUuid))).map(TextDisplay.class::cast).findFirst().orElse(null);
   if(label==null) label=where.getWorld().spawn(where,TextDisplay.class,e->mark(e,"name"));
   data.nameEntityUuid=label.getUniqueId();label.setInvulnerable(true);label.setGravity(false);label.setPersistent(true);
   label.setBillboard(Display.Billboard.CENTER);label.setShadowed(true);label.setTeleportDuration(plugin.settings().i("movement.update-ticks"));
   lastLabel=null;updateName();plugin.models().attach(controller);plugin.animations().reset();
   data.baseModelId=plugin.settings().s("models.galaxy.id");
   for(Entity e:candidates) if(!e.getUniqueId().equals(data.controllerEntityUuid) && !e.getUniqueId().equals(data.nameEntityUuid)) {
    if("controller".equals(tag(e,"role"))) plugin.models().removeDuplicate(e.getUniqueId());
    e.remove();plugin.getLogger().warning("Removed duplicate/stale dragon part "+e.getUniqueId());
   }
  } finally { recovering=false; }
 }
 public void cleanLoaded(Entity e) {
  if(recovering || !managed(e)) return;
  if(plugin.attacks()!=null && plugin.attacks().isCrystal(e)) return;
  if("crystal".equals(tag(e,"role"))) { e.remove();return; }
  if(data!=null && controller==null) return;
  if(data!=null && (e.getUniqueId().equals(data.controllerEntityUuid)||e.getUniqueId().equals(data.nameEntityUuid))) return;
  if("controller".equals(tag(e,"role"))) plugin.models().removeDuplicate(e.getUniqueId());
  e.remove();plugin.getLogger().info("Removed stale dragon part "+e.getUniqueId());
 }
 public void cleanupLoaded() {
  for(World world:Bukkit.getWorlds()) for(Chunk chunk:world.getLoadedChunks()) for(Entity e:chunk.getEntities()) cleanLoaded(e);
 }
 public void tick(long tick) {
  if(data==null || !plugin.models().baseReady()) return;
  if(!valid()) { plugin.attacks().cancel();recover(); }
  if(label==null || !label.isValid()) { plugin.attacks().cancel();recover(); }
  holdChunk(controller.getLocation());
  boolean moving=false;
  if(plugin.attacks().busy()) plugin.attacks().tick(tick);
  else {
   Player owner=owner();
   if(data.state==DragonState.SITTING) {
    plugin.movement().stop(controller);Location seat=DragonMovementController.location(data.sittingLocation);
    if(!seat.equals(controller.getLocation()))
     plugin.movement().teleport(controller,seat);
   } else if(owner!=null && owner.isOnline() && !owner.isDead()) moving=plugin.movement().follow(controller,owner);
   else plugin.movement().stop(controller);
   if(data.state==DragonState.SITTING) plugin.animations().sitting(tick);
   else plugin.animations().normal(moving,tick);
  }
  data.location=DragonMovementController.position(controller.getLocation());
  Location target=controller.getLocation().add(0,plugin.settings().d("movement.name-height"),0);target.setYaw(0);target.setPitch(0);
  if(!target.equals(lastLabel)) { label.teleport(target);lastLabel=target.clone(); }
 }
 public void holdChunk(Location location) {
  Chunk next=location.getChunk();if(next.equals(ticket)) return;
  next.addPluginChunkTicket(plugin);Chunk previous=ticket;ticket=next;
  if(previous!=null) previous.removePluginChunkTicket(plugin);
 }
 public void releaseTicket() { if(ticket!=null) ticket.removePluginChunkTicket(plugin);ticket=null; }
 public void state(DragonState state) {
  requireValid();plugin.attacks().cancel();
  Location seat=state==DragonState.SITTING?plugin.movement().ground(controller.getLocation()):null;
  if(seat!=null) plugin.movement().teleport(controller,seat);
  data.state=state;
  if(state==DragonState.SITTING) {
   plugin.movement().stop(controller);data.sittingLocation=DragonMovementController.position(controller.getLocation());
   plugin.effects().clearAll();
  }
  plugin.animations().reset();save();
 }
 public void summon(Player player) {
  requireValid();plugin.attacks().cancel();plugin.movement().teleport(controller,plugin.movement().safeNear(player));
  data.state=DragonState.FOLLOWING;plugin.animations().reset();save();
 }
 public void rename(String name,String color) {
  requireValid();String validName=NameRules.name(name),validColor=NameRules.color(color).asHexString();
  data.dragonName=validName;data.nameColor=validColor;updateName();save();
 }
 private void updateName() { label.text(Component.text(data.dragonName,NameRules.color(data.nameColor))); }
 public void requireValid() { if(!valid()) throw new IllegalStateException("No valid dragon. Use /dragonadmin spawn or check ModelEngine."); }
 public void save() {
  if(data==null) return;
  if(controller!=null && controller.isValid()) data.location=DragonMovementController.position(controller.getLocation());
  try { store.save(data); }
  catch(IOException e) { plugin.getLogger().log(java.util.logging.Level.SEVERE,"Could not save dragon data",e); }
 }
 public void remove() throws IOException {
  plugin.attacks().cancel();plugin.manual().clear();store.delete();plugin.effects().clearAll();
  try { plugin.models().detach(); }
  finally {
   if(controller!=null) controller.remove();if(label!=null) label.remove();
   controller=null;label=null;data=null;releaseTicket();cleanupLoaded();
  }
 }
 public void shutdown() {
  try { plugin.attacks().cancel(); }
  finally {
   try { if(controller!=null && controller.isValid()) plugin.movement().stop(controller);plugin.effects().clearAll();save(); }
   finally { try { plugin.models().detach(); } finally { releaseTicket(); } }
  }
 }
}
