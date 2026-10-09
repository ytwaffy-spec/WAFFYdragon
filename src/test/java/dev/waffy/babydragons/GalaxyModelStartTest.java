package dev.waffy.babydragons;
import dev.waffy.babydragons.model.ModelEngineController;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.entity.Vex;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GalaxyModelStartTest {
 public static class Bridge {
  Object model=new Object();Map<String,Object> attached=new HashMap<>();
  boolean playing,finished=true;int stops,plays;Object property=new Object();
  public Object get(UUID id) { return this; }
  public Map<String,Object> models() { return attached; }
  public boolean destroyed() { return false; }
  public Object handler() { return this; }
  public void stop() { stops++; }
  public Object play(String clip,double in,double out,double speed,boolean force) {
   assertEquals("execute",clip);assertEquals(1,speed);assertTrue(force);plays++;return property;
  }
  public void loop(Object once) {}
  public boolean playing(String clip) { return playing; }
  public boolean finished() { return finished; }
  public void flag(boolean value) {}
 }
 private static void field(Object object,String name,Object value) throws Exception {
  Field field=ModelEngineController.class.getDeclaredField(name);field.setAccessible(true);field.set(object,value);
 }
 @Test void refetchesAttachedGalaxyAndRequiresPropertyAndPlaying() throws Exception {
  var plugin=mock(BaByDragonsPlugin.class);var manager=mock(DragonManager.class);var entity=mock(Vex.class);
  when(plugin.dragons()).thenReturn(manager);when(manager.controller()).thenReturn(entity);when(entity.isValid()).thenReturn(true);
  when(entity.getUniqueId()).thenReturn(UUID.randomUUID());when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));
  when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
  var controller=mock(ModelEngineController.class,CALLS_REAL_METHODS);field(controller,"plugin",plugin);
  // Reflection API doubles exercise the production linkage without proprietary binaries.
  var bridge=new Bridge();bridge.attached.put("cubee-galaxy_dragon",bridge);bridge.property=bridge;
  field(controller,"galaxy",new Object());field(controller,"once",new Object());
  field(controller,"get",StaticBridge.class.getMethod("get",UUID.class));StaticBridge.current=bridge;
  for(String[] pair:new String[][]{{"models","models"},{"isDestroyed","destroyed"},{"animationHandler","handler"},{"stop","stop"},{"propertyFinished","finished"}})
   field(controller,pair[0],Bridge.class.getMethod(pair[1]));
  field(controller,"play",Bridge.class.getMethod("play",String.class,double.class,double.class,double.class,boolean.class));
  field(controller,"forceLoop",Bridge.class.getMethod("loop",Object.class));
  field(controller,"isPlaying",Bridge.class.getMethod("playing",String.class));
  field(controller,"visible",Bridge.class.getMethod("flag",boolean.class));field(controller,"saved",Bridge.class.getMethod("flag",boolean.class));
  assertFalse(controller.beginGalaxyAttack());assertEquals(1,bridge.plays);
  bridge.playing=true;assertTrue(controller.beginGalaxyAttack());assertEquals(2,bridge.stops);
  assertFalse(controller.galaxyFinished(1,44));
  assertFalse(controller.galaxyFinished(44,44));
  bridge.playing=false;assertTrue(controller.galaxyFinished(44,44));
  bridge.playing=true;bridge.property=null;assertFalse(controller.beginGalaxyAttack());
 }
 public static class StaticBridge {
  static Bridge current;
  public static Object get(UUID id) { return current; }
 }
}
