package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.*;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class FireGlowTest {
 @Test void acquiredGlowClearsButExternalReplacementIsPreserved() throws Exception {
  TestRegistries.initialize();
  var plugin=mock(BaByDragonsPlugin.class);var dragons=mock(DragonManager.class);var damage=mock(DamageService.class);var owner=mock(Player.class);
  var inventory=mock(PlayerInventory.class);var item=mock(ItemStack.class);var target=mock(LivingEntity.class);var world=mock(World.class);
  when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));when(plugin.dragons()).thenReturn(dragons);when(plugin.damage()).thenReturn(damage);
  var sounds=mock(CombatSounds.class);when(plugin.sounds()).thenReturn(sounds);when(dragons.owner()).thenReturn(owner);when(dragons.valid()).thenReturn(true);
  when(dragons.data()).thenReturn(new DragonData());when(owner.isOnline()).thenReturn(true);when(owner.hasPermission(anyString())).thenReturn(true);
  when(owner.getInventory()).thenReturn(inventory);when(inventory.getHeldItemSlot()).thenReturn(8);when(inventory.getItemInMainHand()).thenReturn(item);when(item.getType()).thenReturn(Material.STICK);
  when(owner.getWorld()).thenReturn(world);when(owner.getEyeLocation()).thenAnswer(i->new Location(world,0,65,0));when(owner.hasLineOfSight(target)).thenReturn(true);
  when(target.getWorld()).thenReturn(world);when(target.getUniqueId()).thenReturn(UUID.randomUUID());when(target.getEyeLocation()).thenReturn(new Location(world,0,65,100));
  when(target.addPotionEffect(any())).thenReturn(true);when(damage.allowed(target,owner,true)).thenReturn(true);
  when(world.rayTrace(any(),any(),eq(100.0),any(),eq(true),anyDouble(),any())).thenReturn(new RayTraceResult(new Vector(0,65,100),target));
  var controller=new ManualFireAttackController(plugin);controller.tick();controller.tick();
  verify(sounds,times(1)).play(eq("fire-lock"),any(),eq(owner));controller.clear();verify(target).removePotionEffect(PotionEffectType.GLOWING);
  controller.tick();var event=mock(EntityPotionEffectEvent.class);when(event.getEntity()).thenReturn(target);when(event.getModifiedType()).thenReturn(PotionEffectType.GLOWING);
  controller.externalGlow(event);controller.clear();verify(target,times(1)).removePotionEffect(PotionEffectType.GLOWING);
 }
}
