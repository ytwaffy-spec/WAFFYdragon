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
  assertTrue(config.allowedItem("AIR"));assertTrue(config.allowedItem("STICK"));assertFalse(config.allowedItem("TNT"));
 }
 @Test void refusesTerrainDamageRealStorageAndUnsafeNumericValues() throws Exception {
  for(String path:new String[]{"manual-attack.break-blocks","storage.enabled"}) {
   var yaml=defaults();yaml.set(path,true);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
  for(Object value:new Object[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1000,"oops"}) {
   var yaml=defaults();yaml.set("manual-attack.damage",value);assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  }
 }
 @Test void refusesModelCollisionAndEffectRefreshGaps() throws Exception {
  var yaml=defaults();yaml.set("models.fire.id",yaml.getString("models.galaxy.id"));assertThrows(IllegalArgumentException.class,()->new Settings(yaml));
  var shortEffects=defaults();shortEffects.set("effects.duration-ticks",40);assertThrows(IllegalArgumentException.class,()->new Settings(shortEffects));
 }
}
