package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.DragonImmunity;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class DragonImmunityTest {
 @Test void protectsOnlyActiveOwnerCleansesNegativesAndEnds() throws Exception {
  TestRegistries.initialize();
  var plugin=mock(BaByDragonsPlugin.class);var dragons=mock(DragonManager.class);var owner=mock(Player.class);var server=mock(Server.class);
  when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));when(plugin.dragons()).thenReturn(dragons);when(plugin.getServer()).thenReturn(server);
  var data=new DragonData();data.dragonName="Nova";when(dragons.data()).thenReturn(data);
  UUID id=UUID.randomUUID();when(owner.getUniqueId()).thenReturn(id);when(server.getPlayer(id)).thenReturn(owner);
  var immunity=new DragonImmunity(plugin);UUID token=UUID.randomUUID();immunity.begin(owner,token);immunity.begin(owner,token);assertTrue(immunity.active());
  immunity.end(UUID.randomUUID());assertTrue(immunity.active());
  verify(owner).removePotionEffect(PotionEffectType.POISON);verify(owner).removePotionEffect(PotionEffectType.WITHER);
  verify(owner,never()).removePotionEffect(PotionEffectType.STRENGTH);
  verify(owner).sendMessage(argThat((net.kyori.adventure.text.Component message)->message.toString().contains("Nova")));
  var hit=mock(EntityDamageByEntityEvent.class);when(hit.getEntity()).thenReturn(owner);when(hit.getDamager()).thenReturn(mock(Arrow.class));
  immunity.damage(hit);verify(hit).setCancelled(true);
  immunity.end();assertFalse(immunity.active());reset(hit);when(hit.getEntity()).thenReturn(owner);when(hit.getDamager()).thenReturn(mock(Zombie.class));
  immunity.damage(hit);verify(hit,never()).setCancelled(anyBoolean());
 }
}
