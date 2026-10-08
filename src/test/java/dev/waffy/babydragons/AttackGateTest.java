package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.AttackGate;
import static dev.waffy.babydragons.combat.AttackGate.Kind.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AttackGateTest {
 @Test void locksBothKindsUntilSequenceReturns() {
  var gate=new AttackGate();var d=new DragonData();var token=gate.acquire(d,AUTO,1000);
  assertNotNull(token);assertNull(gate.acquire(d,MANUAL,1000));gate.complete(d,AUTO,token,true,2000,30000);
  assertTrue(gate.busy());assertNull(gate.acquire(d,MANUAL,2000));
  gate.release(token);assertNotNull(gate.acquire(d,MANUAL,2000));
  assertEquals(32000,d.autoAttackCooldownUntil);assertEquals(0,d.manualAttackCooldownUntil);
 }
 @Test void failedAndCancelledAttacksDoNotConsumeCooldown() {
  var gate=new AttackGate();var d=new DragonData();var token=gate.acquire(d,MANUAL,1000);
  gate.complete(d,MANUAL,token,false,2000,30000);gate.release(token);
  assertEquals(0,d.manualAttackCooldownUntil);assertNotNull(gate.acquire(d,MANUAL,2000));
 }
 @Test void disabledAutoDoesNotDisableManualAndViceVersa() {
  var gate=new AttackGate();var d=new DragonData();d.autoAttackEnabled=false;
  assertNull(gate.acquire(d,AUTO,1000));var token=gate.acquire(d,MANUAL,1000);assertNotNull(token);
  gate.release(token);d.autoAttackEnabled=true;d.manualAttackEnabled=false;
  assertNull(gate.acquire(d,MANUAL,1000));assertNotNull(gate.acquire(d,AUTO,1000));
 }
 @Test void cooldownsSurviveNewLockAndBoundaryIsExact() {
  var d=new DragonData();d.autoAttackCooldownUntil=32000;d.manualAttackCooldownUntil=50000;var gate=new AttackGate();
  assertNull(gate.acquire(d,AUTO,31999));var token=gate.acquire(d,AUTO,32000);assertNotNull(token);gate.release(token);
  assertNull(gate.acquire(d,MANUAL,32000));assertNotNull(gate.acquire(d,MANUAL,50000));
  assertEquals(1,AttackGate.remainingSeconds(32000,31999));assertEquals(0,AttackGate.remainingSeconds(32000,32000));
  assertEquals(0,AttackGate.remainingSeconds(32000,50000));
 }
 @Test void staleCallbacksCannotReleaseOrCompleteNewAttack() {
  var gate=new AttackGate();var d=new DragonData();var old=gate.acquire(d,AUTO,0);gate.release(old);
  var current=gate.acquire(d,MANUAL,0);gate.release(old);gate.release(null);
  gate.complete(d,AUTO,old,true,10,30000);gate.complete(d,MANUAL,UUID.randomUUID(),true,10,30000);
  assertTrue(gate.busy());assertEquals(0,d.autoAttackCooldownUntil);assertEquals(0,d.manualAttackCooldownUntil);
  gate.complete(d,MANUAL,current,true,10,30000);assertEquals(30010,d.manualAttackCooldownUntil);
 }
}
