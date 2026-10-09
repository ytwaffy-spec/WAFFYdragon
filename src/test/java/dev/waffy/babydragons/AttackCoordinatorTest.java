package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import dev.waffy.babydragons.model.*;
import dev.waffy.babydragons.movement.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AttackCoordinatorTest {
 BaByDragonsPlugin plugin;DragonManager dragons;ModelEngineController models;DragonAnimationController animations;
 DragonMovementController movement;DamageService damage;Player owner;Vex base;LivingEntity enemy;
 World world;AttackCoordinator attacks;DragonData data;
 @BeforeEach void setup() throws Exception {
  TestRegistries.initialize();
  plugin=mock(BaByDragonsPlugin.class);dragons=mock(DragonManager.class);models=mock(ModelEngineController.class);
  animations=mock(DragonAnimationController.class);movement=mock(DragonMovementController.class);
  damage=mock(DamageService.class);world=mock(World.class);
  owner=mock(Player.class);base=mock(Vex.class);enemy=mock(LivingEntity.class);
  data=new DragonData();data.ownerUuid=UUID.randomUUID();
  when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));when(plugin.dragons()).thenReturn(dragons);
  when(plugin.models()).thenReturn(models);when(plugin.animations()).thenReturn(animations);when(plugin.movement()).thenReturn(movement);
  when(plugin.damage()).thenReturn(damage);when(plugin.manual()).thenReturn(mock(ManualFireAttackController.class));when(plugin.launches()).thenReturn(mock(FireLaunchProtection.class));when(plugin.sounds()).thenReturn(mock(CombatSounds.class));
  when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());when(dragons.valid()).thenReturn(true);
  when(dragons.data()).thenReturn(data);when(dragons.owner()).thenReturn(owner);when(dragons.controller()).thenReturn(base);
  when(owner.getUniqueId()).thenReturn(data.ownerUuid);when(owner.isOnline()).thenReturn(true);when(owner.hasPermission(anyString())).thenReturn(true);
  when(owner.getWorld()).thenReturn(world);when(owner.getLocation()).thenReturn(new Location(world,0,64,0));
  when(base.getWorld()).thenReturn(world);when(base.isValid()).thenReturn(true);when(base.getLocation()).thenAnswer(i->new Location(world,2,64,0));
  when(enemy.getWorld()).thenReturn(world);when(enemy.getUniqueId()).thenReturn(UUID.randomUUID());when(enemy.getLocation()).thenAnswer(i->new Location(world,4,64,0));
  when(damage.allowed(eq(enemy),eq(owner),anyBoolean())).thenReturn(true);
  when(models.attackTicks(anyBoolean())).thenReturn(44);when(models.fireReady()).thenReturn(true);when(models.beginFire(any(Location.class))).thenReturn(true);
  when(animations.attack(anyString())).thenReturn(true);when(models.attackFinished(anyBoolean())).thenReturn(true);
  when(models.galaxyFinished(anyLong(),anyInt())).thenReturn(true);
  when(movement.toward(any(),any(),anyDouble(),anyDouble())).thenReturn(true);
  when(world.getNearbyEntities(any(Location.class),anyDouble(),anyDouble(),anyDouble())).thenReturn(List.of());
  when(movement.toward(any(),any(),anyDouble(),anyDouble(),anyString())).thenReturn(true);
  when(movement.ground(any())).thenReturn(new Location(world,4,63.05,0));
  attacks=new AttackCoordinator(plugin);
 }
 @Test void fullExecuteFourMomentsThenReturnAndOnlyThenCooldown() {
  assertTrue(attacks.startAuto(enemy));
  attacks.tick(1);verify(animations,times(1)).attack("execute");
  for(int tick=2;tick<=33;tick++) attacks.tick(tick);
  verify(damage,times(4)).damage(enemy,owner,20,false);assertEquals(0,data.autoAttackCooldownUntil);
  when(models.galaxyFinished(anyLong(),anyInt())).thenReturn(false);
  attacks.tick(50);assertTrue(attacks.busy());
  when(models.galaxyFinished(anyLong(),anyInt())).thenReturn(true);attacks.tick(51);assertTrue(attacks.busy());
  attacks.tick(52);assertFalse(attacks.busy());assertTrue(data.autoAttackCooldownUntil>System.currentTimeMillis());
  verify(animations,times(1)).attack("execute");
 }
 @Test void deadOrProtectedTargetDoesNotCancelRemainingVisualStrikes() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);attacks.tick(9);
  when(damage.allowed(eq(enemy),eq(owner),anyBoolean())).thenReturn(false);
  for(int tick=10;tick<=45;tick++) attacks.tick(tick);
  assertTrue(attacks.status().contains("strike=4"));verify(damage,times(1)).damage(enemy,owner,20,false);
  attacks.tick(46);assertFalse(attacks.busy());
 }
 @Test void cancelAlwaysReleasesGateAndAllowsAnotherAttack() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);attacks.cancel();
  assertFalse(attacks.busy());assertEquals(0,data.autoAttackCooldownUntil);
  assertTrue(attacks.startManual(enemy));assertFalse(attacks.startAuto(enemy));attacks.cancel();
 }
 @Test void offlineOwnerCannotStartAndShortClipsStillGetFourMoments() {
  when(owner.isOnline()).thenReturn(false);assertFalse(attacks.startAuto(enemy));
  when(owner.isOnline()).thenReturn(true);when(models.attackTicks(false)).thenReturn(20);
  assertTrue(attacks.startAuto(enemy));for(int tick=1;tick<=20;tick++) attacks.tick(tick);
  assertTrue(attacks.status().contains("strike=4"));verify(animations,times(1)).attack("execute");
 }
 @Test void ownerWorldChangeCancelsWithoutWaitingForAnimation() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);when(owner.getWorld()).thenReturn(mock(World.class));attacks.tick(2);
  assertFalse(attacks.busy());
 }
 @Test void staleAttackTokenIsRejectedAfterCancellation() {
  assertTrue(attacks.startAuto(enemy));UUID old=attacks.token();assertTrue(attacks.active(old));
  attacks.cancel();assertTrue(attacks.startManual(enemy));assertFalse(attacks.active(old));
  assertNotEquals(old,attacks.token());
 }
 @Test void survivingTargetReceivesLevitationOnceOnlyAfterFullAnimation() {
  Server server=mock(Server.class);when(plugin.getServer()).thenReturn(server);when(server.getEntity(enemy.getUniqueId())).thenReturn(enemy);
  when(damage.damage(enemy,owner,20,false)).thenReturn(true);
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);
  for(int tick=2;tick<=44;tick++) attacks.tick(tick);
  verify(enemy,never()).addPotionEffect(any());attacks.tick(45);attacks.tick(46);
  verify(enemy,times(1)).addPotionEffect(argThat(effect->effect.getType()==org.bukkit.potion.PotionEffectType.LEVITATION && effect.getDuration()==100));
 }
 @Test void deadHitTargetNeverReceivesPostAnimationEffect() {
  Server server=mock(Server.class);when(plugin.getServer()).thenReturn(server);when(server.getEntity(enemy.getUniqueId())).thenReturn(enemy);
  when(damage.damage(enemy,owner,20,false)).thenReturn(true);attacks.startAuto(enemy);attacks.tick(1);attacks.tick(9);
  when(damage.allowed(eq(enemy),eq(owner),anyBoolean())).thenReturn(false);
  for(int tick=10;tick<=46;tick++) attacks.tick(tick);
  verify(enemy,never()).addPotionEffect(any());assertFalse(attacks.busy());
 }
 @Test void fireCommitsFixedGroundRegionAndThreePulsesBeforeClipEnds() {
  Player victim=mock(Player.class);
  Location center=new Location(world,4,63.05,0,-90,0);
  when(world.getNearbyEntities(eq(center),eq(3.5),eq(3.5),eq(3.5))).thenReturn(List.of(enemy,victim));
  data.manualAttackCooldownUntil=Long.MAX_VALUE;
  assertTrue(attacks.startManual(enemy));attacks.tick(1);verify(models).beginFire(center);
  when(enemy.getLocation()).thenReturn(new Location(world,12,64,0));
  for(int tick=2;tick<=9;tick++) attacks.tick(tick);
  verify(damage,never()).damage(any(),any(),anyDouble(),anyBoolean());
  attacks.tick(10);verify(damage,times(1)).damage(enemy,owner,8,true);
  attacks.tick(22);verify(damage,times(2)).damage(enemy,owner,8,true);
  attacks.tick(32);attacks.tick(40);
  verify(damage,times(3)).damage(enemy,owner,8,true);verify(damage,times(3)).damage(victim,owner,4,true);
  verify(models,never()).endFire();verify(movement,never()).toward(any(),any(),anyDouble(),anyDouble());
  attacks.tick(45);attacks.tick(46);assertFalse(attacks.busy());
  assertEquals(0,data.manualAttackCooldownUntil);assertTrue(attacks.startManual(enemy));
 }
 @Test void stalledReturnUsesEmergencyPositionAndReleasesLock() {
  attacks.startAuto(enemy);attacks.tick(1);for(int t=2;t<=45;t++) attacks.tick(t);
  when(movement.toward(any(),any(),anyDouble(),anyDouble(),anyString())).thenReturn(false);
  Location safe=new Location(world,0,64,3);when(movement.safeNear(owner)).thenReturn(safe);
  attacks.tick(86);verify(movement).teleport(base,safe);assertFalse(attacks.busy());
 }
 @Test void failedExecuteAndReleasesGate() {
  when(animations.attack(anyString())).thenReturn(false);assertTrue(attacks.startAuto(enemy));attacks.tick(1);
  assertFalse(attacks.busy());assertTrue(attacks.startAuto(enemy));
  verify(plugin.sounds(),never()).play(eq("galaxy-execute-start"),any(),any());
 }
 @Test void galaxyUsesRequiredParticlePayloadAndNeverChasesDuringExecute() {
  assertEquals(Float.class,Particle.DRAGON_BREATH.getDataType());
  doAnswer(i->{throw new IllegalArgumentException("Missing Dragon Breath Float data");}).when(world)
   .spawnParticle(eq(Particle.DRAGON_BREATH),any(Location.class),anyInt(),anyDouble(),anyDouble(),anyDouble(),anyDouble());
  assertTrue(attacks.startAuto(enemy));attacks.tick(10);
  var order=inOrder(animations,plugin.sounds());
  order.verify(animations).attack("execute");order.verify(plugin.sounds()).play(eq("galaxy-execute-start"),any(),isNull());
  clearInvocations(movement);
  when(enemy.getLocation()).thenReturn(new Location(world,6.9,64,0));
  for(int tick=11;tick<=42;tick++) attacks.tick(tick);
  assertTrue(attacks.busy());verify(damage,times(4)).damage(enemy,owner,20,false);
  verify(world,times(4)).spawnParticle(eq(Particle.DRAGON_BREATH),any(Location.class),eq(16),eq(.6),eq(.3),eq(.6),eq(.01),eq(1.0f));
  verify(plugin.sounds(),times(4)).play(eq("galaxy-strike"),any(),isNull());
  verify(movement,never()).toward(any(),any(),anyDouble(),anyDouble());
  verify(animations,times(1)).attack("execute");
 }
}
