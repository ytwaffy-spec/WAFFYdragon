package dev.waffy.babydragons;
import java.io.InputStreamReader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class SettingsTest {
 YamlConfiguration defaults() throws Exception {
  try(var reader=new InputStreamReader(getClass().getResourceAsStream("/config.yml"),java.nio.charset.StandardCharsets.UTF_8)) {
   var yaml=new YamlConfiguration();yaml.load(reader);return yaml;
  }
 }
 @Test void packagedDefaultsAreValidAndUseProvidedModelIds() throws Exception {
  var config=new Settings(defaults());
  assertEquals("cubee-galaxy_dragon",config.s("models.galaxy.id"));assertEquals("cubee-fire_dragon",config.s("models.fire.id"));
  assertFalse(config.allowedItem("AIR"));assertTrue(config.allowedItem("STICK"));assertFalse(config.allowedItem("TNT"));
 }
 @Test void refusesTerrainDamageRealStorageAndUnsafeNumericValues() throws Exception {
  for(String path:new String[]{"manual-attack.break-blocks","storage.enabled"}) {
   var yaml=defaults();yaml.set(path,true);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
  for(Object value:new Object[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1000,"oops"}) {
   var yaml=defaults();yaml.set("fire-attack.player-damage",value);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
 }
 @Test void refusesModelCollisionAndEffectRefreshGaps() throws Exception {
  var yaml=defaults();yaml.set("models.fire.id",yaml.getString("models.galaxy.id"));assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  var shortEffects=defaults();shortEffects.set("effects.duration-ticks",40);assertThrows(IllegalArgumentException.class,()->new Settings(shortEffects));
 }
 @Test void rejectsUnsafeCombatSettingsAndBadTimelines() throws Exception {
  for(String path:new String[]{"fire-attack.player-damage","fire-attack.target-range","fire-attack.cooldown-seconds","galaxy-attack.max-strikes"}) {
   var yaml=defaults();yaml.set(path,101);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
  for(var times:java.util.List.of(java.util.List.of(8,16),java.util.List.of(8,8,24,32),java.util.List.of(8,16,32,24))) {
   var yaml=defaults();yaml.set("galaxy-attack.strike-ticks",times);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
 }
 @Test void migrationFillsNewFieldsAndPreservesUserChoices() throws Exception {
  var old=defaults();old.set("config-version",null);old.set("galaxy-attack",null);old.set("fire-attack",null);
  old.set("auto-attack.enabled-by-default",false);old.set("manual-attack.enabled-by-default",false);
  old.set("movement.follow.distance",3.7);Settings.migrate(old,defaults());var result=new Settings(old);
  assertFalse(result.b("galaxy-attack.assist-enabled-by-default"));assertFalse(result.b("galaxy-attack.defense-enabled-by-default"));
  assertFalse(result.b("fire-attack.enabled-by-default"));assertEquals(3.7,result.d("movement.follow.distance"));
  assertEquals(100,result.d("fire-attack.target-range"));assertEquals(0,result.i("fire-attack.cooldown-seconds"));
 }
}
