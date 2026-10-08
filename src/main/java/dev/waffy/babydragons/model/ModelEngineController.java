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
 private final Method eventPlayer,eventBase,eventAction,eventSlot,baseId;
 private final Class<? extends Event> interactionClass;
 private Object modeled,galaxy,fire;
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
  baseReady=validate("models.galaxy.id",List.of("idle","follow","pet","auto-attack"));
  fireReady=validate("models.fire.id",List.of("fire-attack"));
 }
 private boolean validate(String key,List<String> animations) {
  String id=plugin.settings().s(key);Object bp=call(blueprint,null,id);
  if(bp==null) {
   plugin.getLogger().severe("ModelEngine blueprint '"+id+"' is unavailable ("+key+"). Check installed blueprints, then /dragonadmin reload.");
   return false;
  }
  Map<?,?> clips=(Map<?,?>)call(blueprintAnimations,bp);
  for(String logical:animations) {
   String clip=plugin.settings().s("animations."+logical);
   if(!clips.containsKey(clip)) { plugin.getLogger().severe("ModelEngine blueprint '"+id+"' has no animation '"+clip+"'.");return false; }
  }
  plugin.getLogger().info("Validated installed blueprint: "+id);return true;
 }
 public boolean baseReady() { return baseReady; }
 public boolean fireReady() { return fireReady; }
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
  return galaxy!=null && call(play,call(animationHandler,galaxy),clip,.15,.15,1.0,force)!=null;
 }
 public boolean isGalaxyPlaying(String clip) { return galaxy!=null && Boolean.TRUE.equals(call(isPlaying,call(animationHandler,galaxy),clip)); }
 public void stopGalaxy() { if(galaxy!=null) call(stop,call(animationHandler,galaxy)); }
 public boolean beginFire() {
  if(!fireReady || modeled==null) return false;
  endFire();fireId=plugin.settings().s("models.fire.id");fire=call(active,null,fireId);
  if(fire==null) return false;
  call(add,modeled,fire,false); // Secondary overlay; Galaxy retains main hitbox.
  boolean result=call(play,call(animationHandler,fire),plugin.settings().s("animations.fire-attack"),.1,.15,1.0,true)!=null;
  if(!result) endFire();return result;
 }
 private void removeActive(String id) {
  Object result=call(removeModel,modeled,id);
  if(result instanceof Optional<?> removed) removed.ifPresent(m->{
   if(!Boolean.TRUE.equals(call(isDestroyed,m))) call(activeDestroy,m);
  });
 }
 public void endFire() {
  if(fire!=null && modeled!=null) {
   try { removeActive(fireId); }
   finally { fire=null;fireId=null; }
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
