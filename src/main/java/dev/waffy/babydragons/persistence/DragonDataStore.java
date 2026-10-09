package dev.waffy.babydragons.persistence;
import dev.waffy.babydragons.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;
public final class DragonDataStore {
 private final Path path;
 public DragonDataStore(Path path) { this.path=path; }
 public Optional<DragonData> load() throws IOException {
  if(!Files.exists(path)) return Optional.empty();
  try {
   var y=new YamlConfiguration();y.load(path.toFile());
   int schema=y.getInt("schema");
   if(schema!=1 && schema!=2) throw new IllegalArgumentException("Unsupported schema");
   var d=new DragonData();
   d.dragonId=uuid(y,"dragonId");d.ownerUuid=uuid(y,"ownerUuid");
   d.controllerEntityUuid=uuid(y,"controllerEntityUuid");d.nameEntityUuid=uuid(y,"nameEntityUuid");
   d.dragonName=NameRules.name(y.getString("dragonName"));d.nameColor=NameRules.color(y.getString("nameColor")).asHexString();
   d.state=DragonState.valueOf(y.getString("state",""));d.location=position(y,"location");d.sittingLocation=position(y,"sittingLocation");
   d.effectsEnabled=bool(y,"effectsEnabled");d.manualAttackEnabled=bool(y,"manualAttackEnabled");
   d.assistAttackEnabled=schema==1?bool(y,"autoAttackEnabled"):bool(y,"assistAttackEnabled");
   d.defenseAttackEnabled=schema==1?bool(y,"autoAttackEnabled"):bool(y,"defenseAttackEnabled");
   d.autoAttackCooldownUntil=timestamp(y,"autoAttackCooldownUntil");d.manualAttackCooldownUntil=timestamp(y,"manualAttackCooldownUntil");
   d.baseModelId=y.getString("baseModelId");d.validate();return Optional.of(d);
  } catch(Exception e) { throw new IOException("Invalid data.yml; original preserved and recovery stopped",e); }
 }
 public void save(DragonData d) throws IOException {
  d.validate();var y=new YamlConfiguration();
  y.set("schema",2);y.set("dragonId",str(d.dragonId));y.set("ownerUuid",str(d.ownerUuid));
  y.set("controllerEntityUuid",str(d.controllerEntityUuid));y.set("nameEntityUuid",str(d.nameEntityUuid));
  y.set("dragonName",d.dragonName);y.set("nameColor",d.nameColor);y.set("state",d.state.name());
  position(y,"location",d.location);position(y,"sittingLocation",d.sittingLocation);
  y.set("effectsEnabled",d.effectsEnabled);y.set("assistAttackEnabled",d.assistAttackEnabled);y.set("defenseAttackEnabled",d.defenseAttackEnabled);y.set("manualAttackEnabled",d.manualAttackEnabled);
  y.set("autoAttackCooldownUntil",d.autoAttackCooldownUntil);y.set("manualAttackCooldownUntil",d.manualAttackCooldownUntil);
  y.set("baseModelId",d.baseModelId);
  Path parent=path.toAbsolutePath().getParent();Files.createDirectories(parent);
  Path tmp=Files.createTempFile(parent,"dragon-",".tmp");
  try {
   Files.writeString(tmp,y.saveToString());
   try { Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
   catch(AtomicMoveNotSupportedException e) { Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING); }
  } finally { Files.deleteIfExists(tmp); }
 }
 public void delete() throws IOException { Files.deleteIfExists(path); }
 private static String str(UUID id) { return id==null?null:id.toString(); }
 private static UUID uuid(YamlConfiguration y,String key) { String s=y.getString(key);return s==null?null:UUID.fromString(s); }
 private static boolean bool(YamlConfiguration y,String key) {
  if(!(y.get(key) instanceof Boolean)) throw new IllegalArgumentException("Missing/invalid "+key);
  return y.getBoolean(key);
 }
 private static long timestamp(YamlConfiguration y,String key) {
  Object raw=y.get(key);
  if(!(raw instanceof Integer || raw instanceof Long)) throw new IllegalArgumentException("Missing/invalid "+key);
  return ((Number)raw).longValue();
 }
 private static DragonData.Position position(YamlConfiguration y,String key) {
  if(!y.contains(key)) return null;
  for(String axis:List.of("x","y","z","yaw"))
   if(!(y.get(key+"."+axis) instanceof Number)) throw new IllegalArgumentException("Invalid "+key+"."+axis);
  return new DragonData.Position(uuid(y,key+".world"),y.getDouble(key+".x"),y.getDouble(key+".y"),y.getDouble(key+".z"),(float)y.getDouble(key+".yaw"));
 }
 private static void position(YamlConfiguration y,String k,DragonData.Position p) {
  if(p==null) return;
  y.set(k+".world",str(p.world()));y.set(k+".x",p.x());y.set(k+".y",p.y());y.set(k+".z",p.z());y.set(k+".yaw",p.yaw());
 }
}
