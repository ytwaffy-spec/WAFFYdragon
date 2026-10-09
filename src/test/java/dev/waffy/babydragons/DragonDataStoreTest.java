package dev.waffy.babydragons;
import dev.waffy.babydragons.persistence.DragonDataStore;
import java.nio.file.*;import java.io.IOException;import java.util.UUID;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import static org.junit.jupiter.api.Assertions.*;
class DragonDataStoreTest {
 @TempDir Path directory;
 DragonData sample() {
  var d=new DragonData();d.dragonId=UUID.randomUUID();d.ownerUuid=UUID.randomUUID();d.controllerEntityUuid=UUID.randomUUID();d.nameEntityUuid=UUID.randomUUID();
  d.location=new DragonData.Position(UUID.randomUUID(),2.5,70,-4.5,32);
  d.sittingLocation=new DragonData.Position(d.location.world(),5,69,-8,180);d.state=DragonState.SITTING;
  d.dragonName="Nova 星";d.nameColor="#B026FF";d.baseModelId="cubee-galaxy_dragon";
  d.effectsEnabled=false;d.assistAttackEnabled=false;d.defenseAttackEnabled=true;d.manualAttackEnabled=true;
  d.autoAttackCooldownUntil=1800000000000L;d.manualAttackCooldownUntil=1800000012345L;return d;
 }
 @Test void restartRoundTripPreservesIdentitySeatTogglesAndIndependentCooldowns() throws Exception {
  var file=directory.resolve("data.yml");var expected=sample();new DragonDataStore(file).save(expected);
  var actual=new DragonDataStore(file).load().orElseThrow();
  assertAll(()->assertEquals(expected.dragonId,actual.dragonId),()->assertEquals(expected.ownerUuid,actual.ownerUuid),
   ()->assertEquals(expected.controllerEntityUuid,actual.controllerEntityUuid),()->assertEquals(expected.nameEntityUuid,actual.nameEntityUuid),
   ()->assertEquals(expected.location,actual.location),()->assertEquals(expected.sittingLocation,actual.sittingLocation),
   ()->assertEquals(expected.state,actual.state),()->assertEquals(expected.dragonName,actual.dragonName),
   ()->assertEquals(expected.nameColor,actual.nameColor),()->assertEquals(expected.baseModelId,actual.baseModelId),
   ()->assertFalse(actual.effectsEnabled),()->assertFalse(actual.assistAttackEnabled),()->assertTrue(actual.defenseAttackEnabled),()->assertTrue(actual.manualAttackEnabled),
   ()->assertEquals(expected.autoAttackCooldownUntil,actual.autoAttackCooldownUntil),()->assertEquals(expected.manualAttackCooldownUntil,actual.manualAttackCooldownUntil));
  try(var files=Files.list(directory)){assertEquals(1,files.count());}
 }
 @Test void malformedDataIsNeverSilentlyReplaced() throws Exception {
  var file=directory.resolve("data.yml");var store=new DragonDataStore(file);
  for(String bad:new String[]{"[broken yaml","schema: 2\n","schema: 1\ndragonId: nope\n"}) {
   Files.writeString(file,bad);assertThrows(IOException.class,store::load);assertEquals(bad,Files.readString(file));
  }
 }
 @Test void legacyToggleMigratesBothWaysWithoutTouchingIdentity() throws Exception {
  var file=directory.resolve("data.yml");var store=new DragonDataStore(file);
  for(boolean old:new boolean[]{false,true}) {
   var original=sample();store.save(original);
   var yaml=new YamlConfiguration();yaml.load(file.toFile());yaml.set("schema",1);yaml.set("autoAttackEnabled",old);
   yaml.set("assistAttackEnabled",null);yaml.set("defenseAttackEnabled",null);yaml.save(file.toFile());
   var actual=store.load().orElseThrow();assertEquals(old,actual.assistAttackEnabled);assertEquals(old,actual.defenseAttackEnabled);
   assertEquals(original.ownerUuid,actual.ownerUuid);assertEquals(original.sittingLocation,actual.sittingLocation);
   store.save(actual);assertFalse(Files.readString(file).contains("autoAttackEnabled:"));
  }
 }
 @Test void rejectsMissingSeatAndInvalidCooldownWithoutReplacingGoodFile() throws Exception {
  var file=directory.resolve("data.yml");var store=new DragonDataStore(file);var d=sample();store.save(d);
  String original=Files.readString(file);d.sittingLocation=null;
  assertThrows(IllegalArgumentException.class,()->store.save(d));assertEquals(original,Files.readString(file));
  var invalid=sample();invalid.manualAttackCooldownUntil=-1;
  assertThrows(IllegalArgumentException.class,()->store.save(invalid));assertEquals(original,Files.readString(file));
 }
 @Test void removingDragonAllowsCleanNewStart() throws Exception {
  var store=new DragonDataStore(directory.resolve("data.yml"));assertTrue(store.load().isEmpty());
  store.save(sample());store.delete();assertTrue(store.load().isEmpty());
 }
 @Test void rejectsMissingOrMalformedTogglesTimestampsAndCoordinates() throws Exception {
  var file=directory.resolve("data.yml");var store=new DragonDataStore(file);
  for(String key:new String[]{"effectsEnabled","autoAttackCooldownUntil","location.x"}) {
   store.save(sample());var yaml=new YamlConfiguration();yaml.load(file.toFile());yaml.set(key,"invalid");yaml.save(file.toFile());
   String before=Files.readString(file);assertThrows(IOException.class,store::load);assertEquals(before,Files.readString(file));
  }
 }
}
