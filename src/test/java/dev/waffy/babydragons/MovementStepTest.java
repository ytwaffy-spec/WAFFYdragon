package dev.waffy.babydragons;
import dev.waffy.babydragons.movement.DragonMovementController;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementStepTest {
 @Test void yawCrossesWrapByShortestArc() {
  assertEquals(180,DragonMovementController.smoothYaw(179,-179,.5),1e-6);
  assertEquals(-180,DragonMovementController.smoothYaw(-179,179,.5),1e-6);
 }
 @Test void accelerationAndArrivalAreBoundedAndMonotonic() {
  double distance=10,speed=0;
  for(int i=0;i<500;i++) {
   double next=DragonMovementController.smoothSpeed(speed,distance,3,.35,.2);
   assertTrue(next>=0 && next<=.35 && next<=distance-3+1e-9);
   distance-=next;speed=next;
  }
  assertTrue(distance>=3 && distance<=3.001);
 }
 @Test void clampsLongDistanceAndMovesVertically() {
  Vector delta=new Vector(0,10,0);
  assertEquals(.28,DragonMovementController.step(delta,.28,3).length(),1e-9);
  assertEquals(10,delta.getY());
 }
 @Test void stopsAtBoundaryWithoutOvershooting() {
  Vector delta=new Vector(3.1,0,0);
  assertEquals(.1,DragonMovementController.step(delta,.28,3).getX(),1e-9);
  assertEquals(0,DragonMovementController.step(new Vector(2,0,0),.28,3).length());
  assertEquals(0,DragonMovementController.step(new Vector(),.28,0).length());
 }
 @Test void repeatedStepsReachExactSeat() {
  Vector remaining=new Vector(-8,4,12);
  for(int i=0;i<100;i++) remaining.subtract(DragonMovementController.step(remaining,.45,0));
  assertEquals(0,remaining.length(),1e-9);
 }
}
