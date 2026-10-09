package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import org.bukkit.Server;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class CombatTriggerTest {
 @Test void independentTogglesAndDragonDamageRecursionGuard() {
  var plugin=mock(BaByDragonsPlugin.class);var manager=mock(DragonManager.class);var damage=mock(DamageService.class);var attacks=mock(AttackCoordinator.class);
  var server=mock(Server.class);var scheduler=mock(BukkitScheduler.class);var owner=mock(Player.class);var enemy=mock(Zombie.class);
  var data=new DragonData();data.ownerUuid=java.util.UUID.randomUUID();when(owner.getUniqueId()).thenReturn(data.ownerUuid);when(enemy.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
  when(plugin.dragons()).thenReturn(manager);when(manager.data()).thenReturn(data);when(manager.owns(owner)).thenReturn(true);
  when(plugin.damage()).thenReturn(damage);when(plugin.attacks()).thenReturn(attacks);when(plugin.getServer()).thenReturn(server);when(server.getScheduler()).thenReturn(scheduler);
  when(scheduler.runTask(eq(plugin),any(Runnable.class))).thenAnswer(i->{((Runnable)i.getArgument(1)).run();return null;});
  var listener=new AutoDefenseController(plugin);var event=mock(EntityDamageByEntityEvent.class);when(event.getFinalDamage()).thenReturn(4.0);
  for(boolean assist:new boolean[]{false,true}) for(boolean defense:new boolean[]{false,true}) {
   data.assistAttackEnabled=assist;data.defenseAttackEnabled=defense;clearInvocations(attacks);
   when(event.getEntity()).thenReturn(enemy);when(event.getDamager()).thenReturn(owner);listener.damage(event);
   verify(attacks,times(assist?1:0)).startAuto(enemy);clearInvocations(attacks);
   when(event.getEntity()).thenReturn(owner);when(event.getDamager()).thenReturn(enemy);listener.damage(event);
   verify(attacks,times(defense?1:0)).startAuto(enemy);
  }
  clearInvocations(attacks);when(damage.applyingDragonDamage()).thenReturn(true);listener.damage(event);verifyNoInteractions(attacks);
 }
}
