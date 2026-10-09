package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import org.bukkit.*;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class FireRegionTest {
 @Test void timelineHasThreeDistinctPulsesAndFinalSafetyMargin() {
  assertArrayEquals(new int[]{9,21,31},FireRegion.pulseTicks(40));
  for(int length=8;length<600;length++) {
   int[] ticks=FireRegion.pulseTicks(length);
   assertTrue(ticks[0]>0 && ticks[0]<ticks[1] && ticks[1]<ticks[2]);
   assertTrue(ticks[2]<=length-4);
  }
 }
 @Test void fixedCenterRelativeBlastsAndFinalVerticalRelease() {
  var world=mock(World.class);var center=new Location(world,10,64,20);
  var region=new FireRegion(center,new Vector(1,0,0));center.add(100,100,100);
  assertEquals(new Location(world,10,64,20),region.center());
  assertEquals(.65,region.blast(0).getZ(),1e-9);
  assertEquals(-.65,region.blast(1).getZ(),1e-9);
  Location edge=new Location(world,13.2,67.2,23.2);
  Vector velocity=region.velocity(edge,new Vector(2,2,2),true);
  assertEquals(.1,velocity.getX(),1e-9);assertEquals(.1,velocity.getY(),1e-9);
  Vector released=region.velocity(edge,new Vector(2,2,2),false);
  assertEquals(.1,released.getX(),1e-9);assertEquals(2,released.getY(),1e-9);
  assertEquals(20,FireLaunchProtection.apex(FireLaunchProtection.launchSpeed()),.001);
 }
}
