package dev.waffy.babydragons.model;
import dev.waffy.babydragons.BaByDragonsPlugin;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.inventory.EquipmentSlot;

/** Cached linkage to documented public ModelEngine 4 APIs; no proprietary binaries or stubs bundled. */
public final class ModelEngineController {
 private final BaByDragonsPlugin plugin;
 private final Method blueprint,create,get,remove,active,models,add,removeModel,visible,saved,destroy;
 private final Method animationHandler,play,stop,isPlaying,blueprintAnimations,activeDestroy,isDestroyed;
 private final Method animationLength,forceLoop,propertyFinished,autoRenderer;
 private final Object once;
 private Object galaxyAttack,fireAttack;
 private final Method eventPlayer,eventBase,eventAction,eventSlot,baseId;
 private final Class<? extends Event> interactionClass;
 private Object modeled,galaxy,fire;
 private Object fireModeled;
 private Vex fireAnchor;
 private UUID controllerId;
 private String fireId;
 private boolean baseReady,fireReady;
 public ModelEngineController(BaByDragonsPlugin plugin) throws ReflectiveOperationException {
  this.plugin=plugin;
  var engine=Objects.requireNonNull(plugin.getServer().getPluginManager().getPlugin("ModelEngine"));
  ClassLoader loader=engine.getClass().getClassLoader();
  Class<?> api=loader.loadClass("com.ticxo.modelengine.api.ModelEngineAPI");
  Class<?> me=loader.loadClass("com.ticxo.modelengine.api.model.ModeledEntity");
  Class<?> am=loader.loadClass("com.ticxo.modelengine.api.model.ActiveModel");
  Class<?> ah=loader.loadClass("com.ticxo.modelengine.api.animation.handler.AnimationHandler");
  Class<?> bp=loader.loadClass("com.ticxo.modelengine.api.generator.blueprint.ModelBlueprint");
  blueprint=api.getMethod("getBlueprint",String.class);create=api.getMethod("createModeledEntity",Entity.class);
  get=api.getMethod("getModeledEntity",UUID.class);remove=api.getMethod("removeModeledEntity",UUID.class);
  active=api.getMethod("createActiveModel",String.class);models=me.getMethod("getModels");
  add=me.getMethod("addModel",am,boolean.class);removeModel=me.getMethod("removeModel",String.class);
  visible=me.getMethod("setBaseEntityVisible",boolean.class);saved=me.getMethod("setSaved",boolean.class);destroy=me.getMethod("destroy");
  animationHandler=am.getMethod("getAnimationHandler");activeDestroy=am.getMethod("destroy");isDestroyed=am.getMethod("isDestroyed");
  play=ah.getMethod("playAnimation",String.class,double.class,double.class,double.class,boolean.class);
  stop=ah.getMethod("forceStopAllAnimations");isPlaying=ah.getMethod("isPlayingAnimation",String.class);
  blueprintAnimations=bp.getMethod("getAnimations");
  animationLength=loader.loadClass("com.ticxo.modelengine.api.animation.BlueprintAnimation").getMethod("getLength");
  Class<?> property=loader.loadClass("com.ticxo.modelengine.api.animation.property.IAnimationProperty");
  Class<?> loop=loader.loadClass("com.ticxo.modelengine.api.animation.BlueprintAnimation$LoopMode");
  once=java.util.Arrays.stream(loop.getEnumConstants()).filter(x->((Enum<?>)x).name().equals("ONCE")).findFirst().orElseThrow();
  forceLoop=property.getMethod("setForceLoopMode",loop);propertyFinished=property.getMethod("isFinished");
  autoRenderer=am.getMethod("setAutoRendererInitialization",boolean.class);
  interactionClass=loader.loadClass("com.ticxo.modelengine.api.events.BaseEntityInteractEvent").asSubclass(Event.class);
  eventPlayer=interactionClass.getMethod("getPlayer");eventBase=interactionClass.getMethod("getBaseEntity");
  eventAction=interactionClass.getMethod("getAction");eventSlot=interactionClass.getMethod("getSlot");
  baseId=loader.loadClass("com.ticxo.modelengine.api.entity.BaseEntity").getMethod("getUUID");
  plugin.getLogger().info("ModelEngine public API linked: "+engine.getPluginMeta().getVersion());
 }
 private static Object call(Method method,Object receiver,Object... args) {
  try { return method.invoke(receiver,args); }
  catch(ReflectiveOperationException e) {
   Throwable cause=e instanceof InvocationTargetException i?i.getCause():e;
   throw new IllegalStateException("ModelEngine API call failed: "+method.getName(),cause);
  }
 }
 public void validateModels() {
  baseReady=validate("models.galaxy.id",List.of("animations.idle","animations.follow","animations.pet","galaxy-attack.animation.id"));
  fireReady=validate("models.fire.id",List.of("fire-attack.animation.id"));
 }
 private boolean validate(String key,List<String> animations) {
  String id=plugin.settings().s(key);Object bp=call(blueprint,null,id);
  if(bp==null) {
   plugin.getLogger().severe("ModelEngine blueprint '"+id+"' is unavailable ("+key+"). Check installed blueprints, then /dragonadmin reload.");
   return false;
  }
  Map<?,?> clips=(Map<?,?>)call(blueprintAnimations,bp);
  for(String logical:animations) {
   String clip=plugin.settings().s(logical);
   if(!clips.containsKey(clip)) { plugin.getLogger().severe("ModelEngine blueprint '"+id+"' has no animation '"+clip+"'.");return false; }
  }
  plugin.getLogger().info("Validated installed blueprint: "+id);return true;
 }
 public boolean baseReady() { return baseReady; }
 public boolean fireReady() { return fireReady; }
 public boolean isFireAnchor(Entity entity) { return fireAnchor!=null && fireAnchor.getUniqueId().equals(entity.getUniqueId()); }
 @SuppressWarnings("unchecked")
 public void attach(Entity entity) {
  if(!baseReady) throw new IllegalStateException("Galaxy blueprint/animations unavailable; see console.");
  if(controllerId!=null && !controllerId.equals(entity.getUniqueId())) detach();
  controllerId=entity.getUniqueId();modeled=call(get,null,controllerId);
  if(modeled==null) modeled=call(create,null,entity);
  call(visible,modeled,false);call(saved,modeled,false);
  Map<String,Object> existing=(Map<String,Object>)call(models,modeled);
  String id=plugin.settings().s("models.galaxy.id");
  for(String other:new ArrayList<>(existing.keySet())) if(!other.equals(id)) removeActive(other);
  galaxy=existing.get(id);
  if(galaxy==null) {
   galaxy=call(active,null,id);
   if(galaxy==null) throw new IllegalStateException("ModelEngine did not create Galaxy ActiveModel");
   call(add,modeled,galaxy,true);
  }
  // Preserve the installed model's default size: no scale/hitbox-scale API.
 }
 public boolean playGalaxy(String clip,boolean force) {
  if(galaxy==null) return false;
  call(play,call(animationHandler,galaxy),clip,.15,.15,1.0,force);
  boolean playing=isGalaxyPlaying(clip);
  if(!playing) plugin.getLogger().warning("Galaxy animation did not start: "+clip);
  return playing;
 }
 public boolean isGalaxyPlaying(String clip) { return galaxy!=null && Boolean.TRUE.equals(call(isPlaying,call(animationHandler,galaxy),clip)); }
 public void stopGalaxy() { if(galaxy!=null) call(stop,call(animationHandler,galaxy)); }
 public boolean beginFire(org.bukkit.Location center) {
  if(!fireReady || modeled==null) return false;
  endFire();fireId=plugin.settings().s("models.fire.id");fire=call(active,null,fireId);
  if(fire==null) return false;
  fireAnchor=center.getWorld().spawn(center,Vex.class,e->{
   plugin.dragons().mark(e,"fire-anchor");e.setAI(false);e.setGravity(false);e.setCollidable(false);
   e.setInvisible(true);e.setInvulnerable(true);e.setSilent(true);e.setPersistent(false);e.setLimitedLifetime(false);
  });
  fireModeled=call(create,null,fireAnchor);call(visible,fireModeled,false);call(saved,fireModeled,false);
  call(autoRenderer,fire,true);
  call(add,fireModeled,fire,true);
  Object handler=call(animationHandler,fire);call(stop,handler);
  String clip=plugin.settings().s("fire-attack.animation.id");
  fireAttack=call(play,handler,clip,0.0,.15,plugin.settings().d("fire-attack.animation.speed"),true);
  if(fireAttack!=null) call(forceLoop,fireAttack,once);
  boolean result=Boolean.TRUE.equals(call(isPlaying,handler,clip));
  if(!result) { plugin.getLogger().warning("Fire execute did not start; inspect /dragonadmin animationstatus");endFire(); }
  return result;
 }
 public boolean beginGalaxyAttack() {
  galaxyAttack=null;
  if(recoverGalaxy()) {
   Object handler=call(animationHandler,galaxy);call(stop,handler);
   String clip=plugin.settings().s("galaxy-attack.animation.id");
   galaxyAttack=call(play,handler,clip,0.0,.15,1.0,true);
   if(galaxyAttack!=null) {
    call(forceLoop,galaxyAttack,once);
    if(Boolean.TRUE.equals(call(isPlaying,handler,clip))) return true;
   }
  }
  plugin.getLogger().warning("Galaxy execute failed to start.");return false;
 }
 private boolean recoverGalaxy() {
  Entity controller=plugin.dragons().controller();
  if(controller==null || !controller.isValid()) return false;
  UUID id=controller.getUniqueId();Object current=call(get,null,id);
  if(current==null) current=call(create,null,controller);
  String modelId=plugin.settings().s("models.galaxy.id");
  Object attached=((Map<?,?>)call(models,current)).get(modelId);
  if(attached==null || Boolean.TRUE.equals(call(isDestroyed,attached))) {
   if(attached!=null) call(removeModel,current,modelId);
   attached=call(active,null,modelId);if(attached==null) return false;
   call(autoRenderer,attached,true);call(add,current,attached,true);
  }
  controllerId=id;modeled=current;galaxy=attached;
  call(visible,modeled,false);call(saved,modeled,false);
  return !Boolean.TRUE.equals(call(isDestroyed,galaxy));
 }
 public boolean galaxyFinished(long elapsed,int duration) {
  if(elapsed<duration) return false;
  boolean playing=isGalaxyPlaying(plugin.settings().s("galaxy-attack.animation.id"));
  boolean finished=galaxyAttack!=null && Boolean.TRUE.equals(call(propertyFinished,galaxyAttack));
  return (finished && !playing) || elapsed>=duration+4;
 }
 public boolean attackFinished(boolean manual) {
  Object property=manual?fireAttack:galaxyAttack;
  return property==null || Boolean.TRUE.equals(call(propertyFinished,property));
 }
 public int attackTicks(boolean manual) {
  String kind=manual?"fire-attack":"galaxy-attack";
  Object bp=call(blueprint,null,plugin.settings().s(manual?"models.fire.id":"models.galaxy.id"));
  Object clip=((Map<?,?>)call(blueprintAnimations,bp)).get(plugin.settings().s(kind+".animation.id"));
  double seconds=((Number)call(animationLength,clip)).doubleValue();
  if(!Double.isFinite(seconds) || seconds<=0 || seconds>120) throw new IllegalStateException("Invalid execute clip duration: "+seconds);
  return (int)Math.ceil(seconds*20/plugin.settings().d(kind+".animation.speed"))+4;
 }
 public void status(org.bukkit.command.CommandSender sender) {
  Object current=controllerId==null?null:call(get,null,controllerId);
  boolean attached=current!=null && galaxy!=null && ((Map<?,?>)call(models,current)).get(plugin.settings().s("models.galaxy.id"))==galaxy;
  sender.sendMessage("Galaxy model attached: "+attached+" | Galaxy model destroyed: "+(galaxy!=null && Boolean.TRUE.equals(call(isDestroyed,galaxy))));
  sender.sendMessage("execute configured: "+plugin.settings().s("galaxy-attack.animation.id")+" | execute property exists: "+(galaxyAttack!=null)
   +" | execute isPlaying: "+isGalaxyPlaying(plugin.settings().s("galaxy-attack.animation.id")));
  sender.sendMessage(plugin.attacks().galaxyDebug());
  describe(sender,"Galaxy "+plugin.settings().s("models.galaxy.id"),galaxy);
  describe(sender,"Fire "+fireId,fire);
 }
 private void describe(org.bukkit.command.CommandSender sender,String id,Object model) {
  sender.sendMessage(id+" attached="+(model!=null));if(model==null) return;
  Object handler=call(animationHandler,model);sender.sendMessage("AnimationHandler: "+handler.getClass().getName());
  for(String clip:List.of("idle","walk","pet","execute")) sender.sendMessage(clip+" playing="+call(isPlaying,handler,clip));
 }
 private void removeActive(String id) {
  Object result=call(removeModel,modeled,id);
  if(result instanceof Optional<?> removed) removed.ifPresent(m->{
   if(!Boolean.TRUE.equals(call(isDestroyed,m))) call(activeDestroy,m);
  });
 }
 public void endFire() {
  try { if(fireModeled!=null) call(destroy,fireModeled);else if(fire!=null) call(activeDestroy,fire); }
  finally {
   try { if(fireAnchor!=null) call(remove,null,fireAnchor.getUniqueId()); }
   finally {
    if(fireAnchor!=null) fireAnchor.remove();
    fireAnchor=null;fireModeled=null;fire=null;fireId=null;fireAttack=null;
   }
  }
 }
 public void detach() {
  try {
   try { endFire(); }
   finally {
    try { if(modeled!=null) call(destroy,modeled); }
    finally { if(controllerId!=null) call(remove,null,controllerId); }
   }
  } finally { modeled=null;galaxy=null;fire=null;fireId=null;controllerId=null; }
 }
 public void removeDuplicate(UUID id) {
  Object stale=call(get,null,id);
  if(stale!=null) { try { call(destroy,stale); } finally { call(remove,null,id); } }
 }
 public record Interaction(Player player,UUID base,EquipmentSlot hand,String action) {}
 public void registerInteractions(Listener listener,Consumer<Interaction> callback) {
  plugin.getServer().getPluginManager().registerEvent(interactionClass,listener,EventPriority.HIGH,(ignored,event)->{
   if(event instanceof Cancellable c && c.isCancelled()) return;
   Object base=call(eventBase,event);if(base==null) return;
   Interaction i=new Interaction((Player)call(eventPlayer,event),(UUID)call(baseId,base),
     (EquipmentSlot)call(eventSlot,event),((Enum<?>)call(eventAction,event)).name());
   if(!plugin.getServer().isPrimaryThread()) plugin.getServer().getScheduler().runTask(plugin,()->callback.accept(i));
   else callback.accept(i);
  },plugin,true);
 }
}
