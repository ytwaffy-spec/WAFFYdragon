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
 DragonMovementController movement;DamageService damage;DragonImmunity immunity;Player owner;Vex base;LivingEntity enemy;
 World world;AttackCoordinator attacks;DragonData data;
 @BeforeEach void setup() throws Exception {
  TestRegistries.initialize();
  plugin=mock(BaByDragonsPlugin.class);dragons=mock(DragonManager.class);models=mock(ModelEngineController.class);
  animations=mock(DragonAnimationController.class);movement=mock(DragonMovementController.class);
  damage=mock(DamageService.class);immunity=mock(DragonImmunity.class);world=mock(World.class);
  owner=mock(Player.class);base=mock(Vex.class);enemy=mock(LivingEntity.class);
  data=new DragonData();data.ownerUuid=UUID.randomUUID();
  when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));when(plugin.dragons()).thenReturn(dragons);
  when(plugin.models()).thenReturn(models);when(plugin.animations()).thenReturn(animations);when(plugin.movement()).thenReturn(movement);
  when(plugin.damage()).thenReturn(damage);when(plugin.immunity()).thenReturn(immunity);when(plugin.sounds()).thenReturn(mock(CombatSounds.class));
  when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());when(dragons.valid()).thenReturn(true);
  when(dragons.data()).thenReturn(data);when(dragons.owner()).thenReturn(owner);when(dragons.controller()).thenReturn(base);
  when(owner.getUniqueId()).thenReturn(data.ownerUuid);when(owner.isOnline()).thenReturn(true);when(owner.hasPermission(anyString())).thenReturn(true);
  when(owner.getWorld()).thenReturn(world);when(owner.getLocation()).thenReturn(new Location(world,0,64,0));
  when(base.getWorld()).thenReturn(world);when(base.isValid()).thenReturn(true);when(base.getLocation()).thenAnswer(i->new Location(world,2,64,0));
  when(enemy.getWorld()).thenReturn(world);when(enemy.getUniqueId()).thenReturn(UUID.randomUUID());when(enemy.getLocation()).thenAnswer(i->new Location(world,4,64,0));
  when(damage.allowed(eq(enemy),eq(owner),anyBoolean())).thenReturn(true);
  when(models.attackTicks(anyBoolean())).thenReturn(44);when(models.fireReady()).thenReturn(true);when(models.beginFire()).thenReturn(true);
  when(animations.attack(anyString())).thenReturn(true);when(models.attackFinished(anyBoolean())).thenReturn(true);
  when(movement.toward(any(),any(),anyDouble(),anyDouble())).thenReturn(true);
  when(world.getNearbyEntities(any(Location.class),anyDouble(),anyDouble(),anyDouble())).thenReturn(List.of());
  attacks=new AttackCoordinator(plugin);
 }
 @Test void fullExecuteFourMomentsThenReturnAndOnlyThenCooldown() {
  assertTrue(attacks.startAuto(enemy));verify(immunity).begin(owner);
  attacks.tick(1);verify(animations,times(1)).attack("execute");
  for(int tick=2;tick<=33;tick++) attacks.tick(tick);
  verify(damage,times(4)).damage(enemy,owner,20,false);assertEquals(0,data.autoAttackCooldownUntil);
  when(models.attackFinished(false)).thenReturn(false);
  attacks.tick(50);assertTrue(attacks.busy());verify(immunity,never()).end();
  when(models.attackFinished(false)).thenReturn(true);attacks.tick(51);assertTrue(attacks.busy());
  attacks.tick(52);assertFalse(attacks.busy());assertTrue(data.autoAttackCooldownUntil>System.currentTimeMillis());
  verify(immunity).end();verify(animations,times(1)).attack("execute");
 }
 @Test void deadOrProtectedTargetDoesNotCancelRemainingVisualStrikes() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);attacks.tick(9);
  when(damage.allowed(eq(enemy),eq(owner),anyBoolean())).thenReturn(false);
  for(int tick=10;tick<=45;tick++) attacks.tick(tick);
  assertTrue(attacks.status().contains("strike=4"));verify(damage,times(1)).damage(enemy,owner,20,false);
  attacks.tick(46);assertFalse(attacks.busy());verify(immunity).end();
 }
 @Test void cancelAlwaysReleasesImmunityAndAllowsAnotherAttack() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);attacks.cancel();
  verify(immunity).end();assertFalse(attacks.busy());assertEquals(0,data.autoAttackCooldownUntil);
  assertTrue(attacks.startManual(enemy));assertFalse(attacks.startAuto(enemy));attacks.cancel();
 }
 @Test void cannotStartWhenOwnerOfflineOrAnimationTimingInvalid() {
  when(owner.isOnline()).thenReturn(false);assertFalse(attacks.startAuto(enemy));verifyNoInteractions(immunity);
  when(owner.isOnline()).thenReturn(true);when(models.attackTicks(false)).thenReturn(20);
  assertThrows(IllegalStateException.class,()->attacks.startAuto(enemy));assertFalse(attacks.busy());
 }
 @Test void ownerWorldChangeCancelsWithoutWaitingForAnimation() {
  assertTrue(attacks.startAuto(enemy));attacks.tick(1);when(owner.getWorld()).thenReturn(mock(World.class));attacks.tick(2);
  assertFalse(attacks.busy());verify(immunity).end();
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
 @Test void fireUsesEntityImpactDelayBoxDamageAndNoCooldown() {
  EnderCrystal crystal=mock(EnderCrystal.class);Player victim=mock(Player.class);
  when(world.spawn(any(Location.class),eq(EnderCrystal.class),any(java.util.function.Consumer.class))).thenAnswer(inv->{
   java.util.function.Consumer<EnderCrystal> setup=inv.getArgument(2);setup.accept(crystal);return crystal;
  });
  when(world.getNearbyEntities(any(Location.class),eq(2.0),eq(2.0),eq(2.0))).thenReturn(List.of(enemy,victim));
  data.manualAttackCooldownUntil=Long.MAX_VALUE;
  assertTrue(attacks.startManual(enemy));attacks.tick(1);verify(models).beginFire();
  for(int tick=2;tick<=20;tick++) attacks.tick(tick);
  verify(damage,never()).damage(any(),any(),anyDouble(),anyBoolean());
  attacks.tick(21);verify(damage).damage(enemy,owner,20,true);verify(damage).damage(victim,owner,14,true);
  verify(crystal).setInvulnerable(true);verify(crystal).setPersistent(false);verify(crystal).setGravity(false);
  attacks.tick(23);verify(crystal).remove();attacks.tick(45);attacks.tick(46);
  assertFalse(attacks.busy());assertEquals(0,data.manualAttackCooldownUntil);assertTrue(attacks.startManual(enemy));
  verify(immunity,never()).begin(any());
 }
 @Test void stalledReturnUsesEmergencyPositionAndReleasesLock() {
  attacks.startAuto(enemy);attacks.tick(1);for(int t=2;t<=45;t++) attacks.tick(t);
  when(movement.toward(any(),any(),anyDouble(),anyDouble())).thenReturn(false);
  Location safe=new Location(world,0,64,3);when(movement.safeNear(owner)).thenReturn(safe);
  attacks.tick(86);verify(movement).teleport(base,safe);assertFalse(attacks.busy());verify(immunity).end();
 }
}
