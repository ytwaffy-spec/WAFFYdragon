package dev.waffy.babydragons;
import java.util.Locale;
import net.kyori.adventure.text.format.*;
public final class NameRules {
 private NameRules() {}
 public static String name(String input) {
  if(input==null) throw new IllegalArgumentException("Name is required");
  String text=input.strip();
  if(text.isEmpty() || text.codePointCount(0,text.length())>32 || !text.matches("[\\p{L}\\p{N} _.'-]+"))
   throw new IllegalArgumentException("Use 1-32 letters, numbers, spaces, apostrophes, dots, underscores or hyphens.");
  return text;
 }
 public static TextColor color(String input) {
  if(input==null) throw new IllegalArgumentException("Color is required");
  String value=input.toLowerCase(Locale.ROOT);
  if(value.equals("purple")) value="#b026ff";
  if(value.equals("pink")) value="#ff4dff";
  TextColor result=value.matches("#[a-f0-9]{6}")?TextColor.fromHexString(value):NamedTextColor.NAMES.value(value);
  if(result==null) throw new IllegalArgumentException("Use a named color or #RRGGBB.");
  return result;
 }
}
