package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.ManualFireAttackController;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ManualInputTest {
 @Test void onlyExplicitNinthSlotSneakRightClickWithAllowedHandTriggers() {
  for(String item:new String[]{"AIR","STICK"}) {
   assertTrue(ManualFireAttackController.input(true,true,true,8,true,item,true));
   assertFalse(ManualFireAttackController.input(false,true,true,8,true,item,true));
   assertFalse(ManualFireAttackController.input(true,false,true,8,true,item,true));
   assertFalse(ManualFireAttackController.input(true,true,false,8,true,item,true));
   assertFalse(ManualFireAttackController.input(true,true,true,8,false,item,true));
   assertFalse(ManualFireAttackController.input(true,true,true,8,true,item,false));
   for(int slot=0;slot<8;slot++) assertFalse(ManualFireAttackController.input(true,true,true,slot,true,item,true));
  }
  assertFalse(ManualFireAttackController.input(true,true,true,8,true,"DIAMOND_SWORD",true));
 }
}
