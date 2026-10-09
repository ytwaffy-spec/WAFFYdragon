package dev.waffy.babydragons;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.*;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** API registry doubles only; these tests do not run a Paper server. */
final class TestRegistries {
 private static boolean initialized;
 @SuppressWarnings({"unchecked","rawtypes"})
 static void initialize() throws Exception {
  if(initialized) return;
  try(var bridge=mockStatic(RegistryAccess.class)) {
   RegistryAccess access=mock(RegistryAccess.class);bridge.when(RegistryAccess::registryAccess).thenReturn(access);
   Map<String,PotionEffectType> effects=new HashMap<>();
   org.mockito.stubbing.Answer<Object> registry=invocation->mock(Registry.class,call->{
    if((call.getMethod().getName().equals("getOrThrow") || call.getMethod().getName().equals("get")) && call.getArgument(0) instanceof Key key)
     return effects.computeIfAbsent(key.value(),name->{
      PotionEffectType type=mock(PotionEffectType.class);when(type.getName()).thenReturn(name.toUpperCase(Locale.ROOT));return type;
     });
    return RETURNS_DEFAULTS.answer(call);
   });
   doAnswer(registry).when(access).getRegistry(any(RegistryKey.class));
   doAnswer(registry).when(access).getRegistry(any(Class.class));
   Class.forName("org.bukkit.potion.PotionEffectType");initialized=true;
  }
 }
}
