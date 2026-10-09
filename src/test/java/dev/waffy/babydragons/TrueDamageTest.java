package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class TrueDamageTest {
 @Test void galaxyAndFireEachSubtractExactlyFourWithoutSecondDamageCall() {
  for(boolean manual:new boolean[]{false,true}) {
   var plugin=mock(BaByDragonsPlugin.class);var server=mock(Server.class);var manager=mock(PluginManager.class);
   when(plugin.getServer()).thenReturn(server);when(server.getPluginManager()).thenReturn(manager);
   var owner=mock(Player.class);var victim=mock(Player.class);var service=spy(new DamageService(plugin));
   doReturn(true).when(service).allowed(victim,owner,manual);
   var health=new AtomicReference<Double>(20.0);when(victim.getHealth()).thenAnswer(i->health.get());
   doAnswer(i->{health.set(i.getArgument(0));return null;}).when(victim).setHealth(anyDouble());
   try(var events=mockConstruction(EntityDamageByEntityEvent.class,(event,context)->when(event.getDamage()).thenReturn(4.0))) {
    for(int n=0;n<(manual?3:4);n++) assertTrue(service.damage(victim,owner,4,manual));
    assertEquals(manual?8:4,health.get());verify(victim,never()).damage(anyDouble(),any(org.bukkit.entity.Entity.class));
    assertEquals(manual?3:4,events.constructed().size());
   }
  }
 }
 @Test void cancelledOrStaleEventsNeverSubtractHealth() {
  for(boolean stale:new boolean[]{false,true}) {
   var plugin=mock(BaByDragonsPlugin.class);var server=mock(Server.class);var manager=mock(PluginManager.class);
   when(plugin.getServer()).thenReturn(server);when(server.getPluginManager()).thenReturn(manager);
   var attacks=mock(AttackCoordinator.class);when(plugin.attacks()).thenReturn(attacks);
   UUID token=UUID.randomUUID();when(attacks.token()).thenReturn(token);when(attacks.active(token)).thenReturn(!stale);
   var owner=mock(Player.class);var victim=mock(Player.class);var service=spy(new DamageService(plugin));
   doReturn(true).when(service).allowed(victim,owner,true);
   try(var events=mockConstruction(EntityDamageByEntityEvent.class,(event,context)->{
    when(event.getDamage()).thenReturn(4.0);when(event.isCancelled()).thenReturn(!stale);
   })) {
    assertFalse(service.damage(victim,owner,4,true));verify(victim,never()).setHealth(anyDouble());
    assertFalse(service.applyingDragonDamage());
   }
  }
 }
}
