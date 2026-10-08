package dev.waffy.babydragons;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class NameRulesTest {
 @Test void acceptsLiteralUnicodeAndSpaces() { assertEquals("Nova 星-2",NameRules.name("  Nova 星-2  ")); }
 @Test void rejectsMarkupControlAndOverlongNames() {
  for(String value:new String[]{""," ","<red>Nova","Nova\nattack","§cNova","x".repeat(33)})
   assertThrows(IllegalArgumentException.class,()->NameRules.name(value),value);
 }
 @Test void resolvesAliasesNamedAndHexColors() {
  assertEquals("#B026FF",NameRules.color("purple").asHexString());assertEquals("#FF4DFF",NameRules.color("pink").asHexString());
  assertEquals("#B026FF",NameRules.color("#b026ff").asHexString());assertEquals("#55FFFF",NameRules.color("AQUA").asHexString());
  for(String value:new String[]{"<red>","red><click:run_command>","#fff","#GGGGGG",""})
   assertThrows(IllegalArgumentException.class,()->NameRules.color(value));
 }
}
