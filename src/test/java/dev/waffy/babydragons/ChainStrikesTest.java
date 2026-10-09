package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.ChainStrikes;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ChainStrikesTest {
 @Test void prefersUniqueThenBalancesRepeatedTargets() {
  for(int count=1;count<=6;count++) {
   var chain=new ChainStrikes(List.of(8,16,24,32));var options=new ArrayList<UUID>();
   for(int i=0;i<count;i++) options.add(UUID.randomUUID());
   Set<UUID> selected=new HashSet<>();
   for(int i=0;i<4;i++) { UUID next=chain.select(options);if(i<count) assertTrue(selected.add(next));chain.strike(next); }
   assertEquals(4,chain.index());assertEquals(Math.min(4,count),chain.targetCount());assertTrue(chain.complete());
  }
 }
 @Test void lostTargetsStillConsumeFourVisualMomentsAndNeverGetSelectedAgain() {
  var chain=new ChainStrikes(List.of(8,16,24,32));UUID a=UUID.randomUUID();
  assertFalse(chain.due(7));assertTrue(chain.due(8));chain.strike(a);chain.hit(a);chain.hit(a);
  assertNull(chain.select(List.of()));
  for(int tick=9;tick<=40;tick++) while(chain.due(tick)) chain.strike(null);
  assertEquals(4,chain.index());assertEquals(Set.of(a),chain.hit());assertFalse(chain.due(1000));
 }
}
