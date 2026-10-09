package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.TargetLock;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class TargetLockTest {
 @Test void graceReacquisitionAndExpiryDoNotRepeatLockSounds() {
  var lock=new TargetLock();UUID a=UUID.randomUUID();assertTrue(lock.update(a,false,0,700));
  assertFalse(lock.update(null,true,699,700));assertEquals(a,lock.target());
  assertFalse(lock.update(a,true,699,700));assertFalse(lock.update(null,true,1398,700));
  assertTrue(lock.update(null,true,1399,700));assertNull(lock.target());
 }
 @Test void invalidTargetClearsImmediatelyAndNewTargetChangesLock() {
  var lock=new TargetLock();UUID a=UUID.randomUUID(),b=UUID.randomUUID();lock.update(a,false,0,700);
  assertTrue(lock.update(b,true,1,700));assertEquals(b,lock.target());
  assertTrue(lock.update(null,false,2,700));assertNull(lock.target());
 }
}
