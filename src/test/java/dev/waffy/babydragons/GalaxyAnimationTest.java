package dev.waffy.babydragons;
import dev.waffy.babydragons.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class GalaxyAnimationTest {
 @Test void normalAndPetCannotInterruptGalaxyButFailureUnlocksThem() throws Exception {
  var plugin=mock(BaByDragonsPlugin.class);var models=mock(ModelEngineController.class);
  when(plugin.models()).thenReturn(models);when(plugin.settings()).thenReturn(new Settings(new SettingsTest().defaults()));
  when(models.beginGalaxyAttack()).thenReturn(true);var animation=new DragonAnimationController(plugin);
  assertTrue(animation.attack("execute"));clearInvocations(models);
  animation.normal(true,5);animation.normal(false,6);animation.sitting(7);
  assertFalse(animation.temporary("pet",8,40));verifyNoInteractions(models);
  animation.reset();when(models.beginGalaxyAttack()).thenReturn(false);
  assertFalse(animation.attack("execute"));animation.normal(false,9);
  verify(models).playGalaxy("idle",false);
 }
}
