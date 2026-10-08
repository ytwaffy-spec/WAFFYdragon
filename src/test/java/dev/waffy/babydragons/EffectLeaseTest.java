package dev.waffy.babydragons;
import dev.waffy.babydragons.effects.EffectLease;import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class EffectLeaseTest {
 @Test void recognizesOurNaturallyAgingEffectWithTickTolerance() {
  var lease=new EffectLease(1,200,true,false,true);
  assertTrue(lease.matches(1,120,80,true,false,true));assertTrue(lease.matches(1,118,80,true,false,true));
 }
 @Test void refusesToRemoveStrongerLongerOrDifferentlyFlaggedEffects() {
  var lease=new EffectLease(1,200,true,false,true);
  assertFalse(lease.matches(2,120,80,true,false,true));assertFalse(lease.matches(1,1200,80,true,false,true));
  assertFalse(lease.matches(1,120,80,false,false,true));assertFalse(lease.matches(1,120,80,true,true,true));
  assertFalse(lease.matches(1,120,80,true,false,false));
 }
}
