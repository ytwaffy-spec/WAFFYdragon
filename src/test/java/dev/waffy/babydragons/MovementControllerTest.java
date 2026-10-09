package dev.waffy.babydragons;
import dev.waffy.babydragons.movement.DragonMovementController;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class MovementControllerTest {
 @Test void controlledMovementProgressesAndRestingDragonFacesOwnerWithoutTeleportJitter() throws Exception {
  var plugin=mock(BaByDragonsPlugin.class);var manager=mock(DragonManager.class);var world=mock(World.class);var base=mock(Vex.class);var owner=mock(Player.class);
  when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));when(plugin.dragons()).thenReturn(manager);
  var position=new AtomicReference<>(new Location(world,0,64,0));when(base.getLocation()).thenAnswer(i->position.get().clone());when(base.getWorld()).thenReturn(world);
  var velocity=new AtomicReference<>(new org.bukkit.util.Vector());
  doAnswer(i->{velocity.set(((org.bukkit.util.Vector)i.getArgument(0)).clone());return null;}).when(base).setVelocity(any());
  doAnswer(i->{position.get().setYaw(i.getArgument(0));return null;}).when(base).setRotation(anyFloat(),anyFloat());
  var controller=new DragonMovementController(plugin);Location target=new Location(world,6,68,0);
  double previous=position.get().distance(target);
  for(int i=0;i<300;i++) {
   Location before=position.get().clone();controller.toward(base,target,.35,3);position.get().add(velocity.get());
   double now=position.get().distance(target);assertTrue(now<=previous+1e-9);assertTrue(before.distance(position.get())<=.35+1e-9);previous=now;
  }
  assertTrue(previous>=3-1e-9 && previous<=3.001+1e-9,"Arrival distance: "+previous);
  when(owner.getVelocity()).thenReturn(new org.bukkit.util.Vector());
  when(owner.getWorld()).thenReturn(world);when(owner.getLocation()).thenAnswer(i->position.get().clone().add(1,0,0));
  when(owner.getEyeLocation()).thenAnswer(i->position.get().clone().add(1,1,0));
  for(int i=0;i<20;i++) assertFalse(controller.follow(base,owner));
  verify(base,never()).teleport(any(Location.class));verify(base,atLeastOnce()).setRotation(anyFloat(),eq(0f));
 }
}
