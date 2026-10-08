package dev.waffy.babydragons;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
public final class Settings {
 private final FileConfiguration c;
 public Settings(FileConfiguration c) {
  this.c=c;
  number("movement.follow-distance",2.5,4);number("movement.teleport-distance",8,128);
  number("movement.attack-stop-distance",1.5,3);integer("movement.update-ticks",1,5);
  number("movement.speed-per-tick",.05,1);number("movement.attack-speed-per-tick",.05,1.5);
  number("movement.attack-timeout-seconds",5,120);number("movement.name-height",.1,32);
  integer("animations.pet-ticks",1,600);integer("animations.execute-windup-ticks",1,600);
  integer("animations.execute-recovery-ticks",1,600);
  integer("effects.refresh-ticks",20,200);integer("effects.duration-ticks",40,1200);
  if(i("effects.duration-ticks")<=i("effects.refresh-ticks")*2)
   throw new IllegalArgumentException("Effect duration must exceed twice refresh-ticks");
  for(String e:Set.of("strength","haste","absorption")) integer("effects."+e+".amplifier",0,4);
  for(String a:Set.of("auto-attack","manual-attack")) { number(a+".cooldown-seconds",1,3600);number(a+".damage",.1,100); }
  number("auto-attack.levitation-seconds",.1,30);integer("auto-attack.levitation-amplifier",0,4);
  number("auto-attack.breath-radius",.1,8);number("manual-attack.radius",.1,8);
  number("manual-attack.target-range",1,64);integer("manual-attack.crystal-ticks",1,40);
  integer("persistence.save-interval-ticks",100,1200);
  if(i("manual-attack.required-hotbar-slot")!=9) throw new IllegalArgumentException("Phase 1 requires hotbar slot 9");
  if(c.getStringList("manual-attack.allowed-items").isEmpty()
    || !Set.of("AIR","STICK").containsAll(c.getStringList("manual-attack.allowed-items")))
   throw new IllegalArgumentException("Allowed items must be AIR and/or STICK");
  if(b("manual-attack.break-blocks") || b("storage.enabled"))
   throw new IllegalArgumentException("Phase 1 requires break-blocks and storage.enabled false");
  for(String p:Set.of("models.galaxy.id","models.fire.id","animations.idle","animations.follow",
    "animations.pet","animations.auto-attack","animations.fire-attack"))
   if(s(p).isBlank()) throw new IllegalArgumentException(p+" is required");
  if(s("models.galaxy.id").equals(s("models.fire.id"))) throw new IllegalArgumentException("Model IDs must be distinct");
 }
 public String s(String p) { return c.getString(p,""); }
 public double d(String p) { return c.getDouble(p); }
 public int i(String p) { return c.getInt(p); }
 public boolean b(String p) { return c.getBoolean(p); }
 public boolean allowedItem(String item) { return c.getStringList("manual-attack.allowed-items").contains(item); }
 private void number(String p,double min,double max) {
  double v=c.getDouble(p,Double.NaN);
  if(!(c.get(p) instanceof Number) || !Double.isFinite(v) || v<min || v>max)
   throw new IllegalArgumentException(p+" must be between "+min+" and "+max);
 }
 private void integer(String p,int min,int max) { number(p,min,max);if(d(p)!=Math.rint(d(p))) throw new IllegalArgumentException(p+" must be an integer"); }
}
