package dev.waffy.babydragons;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;
public final class Settings {
 private final FileConfiguration c;
 public Settings(FileConfiguration c) {
  this.c=c;
  integer("config-version",2,2);
  number("movement.follow.distance",2.5,4);number("movement.teleport-distance",8,128);
  number("movement.follow.smooth-factor",.01,1);number("movement.follow.rotation-smooth-factor",.01,1);
  number("movement.follow.max-speed",.05,.65);number("movement.attack.max-speed",.1,1);
  number("movement.unstuck-seconds",1,10);integer("movement.update-ticks",1,5);
  number("movement.attack-stop-distance",1.5,2.5);
  number("movement.attack-timeout-seconds",5,120);number("movement.name-height",.1,32);
  integer("animations.pet-ticks",1,600);
  integer("effects.refresh-ticks",20,200);integer("effects.duration-ticks",40,1200);
  if(i("effects.duration-ticks")<=i("effects.refresh-ticks")*2) throw new IllegalArgumentException("Effect duration must exceed twice refresh-ticks");
  for(String e:Set.of("strength","haste","absorption")) integer("effects."+e+".amplifier",0,4);
  number("galaxy-attack.cooldown-seconds",0,3600);number("galaxy-attack.chain-radius",.1,5);
  number("galaxy-attack.max-chase-distance",5,100);integer("galaxy-attack.max-strikes",4,4);
  number("galaxy-attack.player-damage-per-strike",.1,4);number("galaxy-attack.mob-damage-per-strike",.1,100);
  number("galaxy-attack.levitation-seconds",.1,30);integer("galaxy-attack.levitation-amplifier",0,4);
  integer("fire-attack.cooldown-seconds",0,0);number("fire-attack.target-range",1,100);
  integer("fire-attack.target-lock-grace-millis",0,2000);
  number("fire-attack.player-damage",.1,14);number("fire-attack.mob-damage",.1,100);
  for(String axis:List.of("x","y","z")) number("fire-attack.blast-radius."+axis,.1,2);
  integer("fire-attack.animation.impact-delay-ticks",1,600);
  integer("fire-attack.required-slot",9,9);
  if(!s("fire-attack.required-item").equals("STICK")) throw new IllegalArgumentException("Fire requires STICK");
  for(String kind:List.of("galaxy-attack","fire-attack")) {
   number(kind+".animation.speed",.1,3);
   if(s(kind+".animation.id").isBlank()) throw new IllegalArgumentException(kind+" animation is required");
  }
  var times=c.getList("galaxy-attack.strike-ticks");
  if(times==null || times.size()!=4) throw new IllegalArgumentException("Exactly four strike ticks are required");
  int last=0;
  for(Object raw:times) {
   if(!(raw instanceof Integer n) || n<=last || n>600) throw new IllegalArgumentException("Strike ticks must be increasing integers from 1 to 600");
   last=(Integer)raw;
  }
  integer("persistence.save-interval-ticks",100,1200);
  if(b("manual-attack.break-blocks") || b("fire-attack.break-blocks") || b("storage.enabled"))
   throw new IllegalArgumentException("Phase 1 requires terrain damage and storage disabled");
  for(String p:Set.of("models.galaxy.id","models.fire.id","animations.idle","animations.follow","animations.pet"))
   if(s(p).isBlank()) throw new IllegalArgumentException(p+" is required");
  if(s("models.galaxy.id").equals(s("models.fire.id"))) throw new IllegalArgumentException("Model IDs must be distinct");
  for(String key:List.of("galaxy-execute-start","galaxy-strike","fire-lock","fire-execute","crystal-pop")) {
   if(!(c.get("sounds."+key+".enabled") instanceof Boolean)) throw new IllegalArgumentException("Invalid sound toggle: "+key);
   number("sounds."+key+".volume",0,2);number("sounds."+key+".pitch",.5,2);
   if(!s("sounds."+key+".sound").matches("[A-Z0-9_]+")) throw new IllegalArgumentException("Invalid sound: "+key);
  }
  if(!(c.get("dragon-immunity.harmful-effects") instanceof List<?>)) throw new IllegalArgumentException("Harmful effects must be a list");
  for(String effect:strings("dragon-immunity.harmful-effects"))
   if(!Set.of("POISON","WITHER","WEAKNESS","SLOWNESS","MINING_FATIGUE","BLINDNESS","DARKNESS","HUNGER","NAUSEA","LEVITATION","BAD_OMEN","UNLUCK").contains(effect))
    throw new IllegalArgumentException("Unsupported harmful effect: "+effect);
  for(String flag:List.of("galaxy-attack.assist-enabled-by-default","galaxy-attack.defense-enabled-by-default","fire-attack.enabled-by-default",
   "dragon-immunity.enabled","dragon-immunity.remove-negative-effects","dragon-immunity.notify-chat","dragon-immunity.notify-end"))
   if(!(c.get(flag) instanceof Boolean)) throw new IllegalArgumentException("Invalid boolean: "+flag);
 }
 public static void migrate(FileConfiguration c,FileConfiguration defaults) {
  if(!c.contains("config-version",true)) {
   c.set("movement.update-ticks",1);
   copy(c,"movement.follow-distance","movement.follow.distance");
   copy(c,"auto-attack.enabled-by-default","galaxy-attack.assist-enabled-by-default");
   copy(c,"auto-attack.enabled-by-default","galaxy-attack.defense-enabled-by-default");
   copy(c,"manual-attack.enabled-by-default","fire-attack.enabled-by-default");
   copy(c,"animations.auto-attack","galaxy-attack.animation.id");
   copy(c,"animations.fire-attack","fire-attack.animation.id");
  }
  for(String key:defaults.getKeys(true))
   if(!defaults.isConfigurationSection(key) && !c.contains(key,true)) c.set(key,defaults.get(key));
 }
 private static void copy(FileConfiguration c,String old,String next) {
  if(c.contains(old,true) && !c.contains(next,true)) c.set(next,c.get(old));
 }
 public String s(String p) { return c.getString(p,""); }
 public List<String> strings(String p) { return c.getStringList(p); }
 public List<Integer> strikes() { return c.getIntegerList("galaxy-attack.strike-ticks"); }
 public double d(String p) { return c.getDouble(p); }
 public int i(String p) { return c.getInt(p); }
 public boolean b(String p) { return c.getBoolean(p); }
 public boolean allowedItem(String item) { return "STICK".equals(item); }
 private void number(String p,double min,double max) {
  double v=c.getDouble(p,Double.NaN);
  if(!(c.get(p) instanceof Number) || !Double.isFinite(v) || v<min || v>max) throw new IllegalArgumentException(p+" must be between "+min+" and "+max);
 }
 private void integer(String p,int min,int max) { number(p,min,max);if(d(p)!=Math.rint(d(p))) throw new IllegalArgumentException(p+" must be an integer"); }
}
