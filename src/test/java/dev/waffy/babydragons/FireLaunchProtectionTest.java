package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.FireLaunchProtection;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class FireLaunchProtectionTest {
 @Test void onlyOneLaunchFallIsProtectedAndVelocityTargetsTwentyBlocks() throws Exception {
  var plugin=mock(BaByDragonsPlugin.class);var player=mock(Player.class);var world=mock(World.class);
  when(player.getUniqueId()).thenReturn(UUID.randomUUID());when(world.getUID()).thenReturn(UUID.randomUUID());when(player.getWorld()).thenReturn(world);
  var protection=new FireLaunchProtection(plugin);protection.launch(player,UUID.randomUUID());
  verify(player).setVelocity(argThat(v->Math.abs(FireLaunchProtection.apex(v.getY())-20)<.001));
  var event=mock(EntityDamageEvent.class);when(event.getEntity()).thenReturn(player);
  when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);protection.damage(event);verify(event,never()).setCancelled(anyBoolean());
  when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);protection.damage(event);protection.damage(event);verify(event,times(1)).setCancelled(true);
  protection.launch(player,UUID.randomUUID());protection.clear();clearInvocations(event);protection.damage(event);verify(event,never()).setCancelled(anyBoolean());
  var settings=new Settings(new SettingsTest().defaults());assertEquals(1.0,settings.d("fire-attack.animation.speed"));assertEquals(4,settings.d("fire-attack.player-damage"));
 }
}
