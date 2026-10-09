package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.DragonData;
import java.util.UUID;
public final class AttackGate {
 public enum Kind { AUTO, MANUAL }
 private UUID token;
 public UUID acquire(DragonData d,Kind kind,long now) {
  boolean enabled=kind==Kind.AUTO?(d.assistAttackEnabled || d.defenseAttackEnabled):d.manualAttackEnabled;
  long until=kind==Kind.AUTO?d.autoAttackCooldownUntil:0;
  if(token!=null || !enabled || now<until) return null;
  return token=UUID.randomUUID();
 }
 public void complete(DragonData d,Kind kind,UUID owner,boolean executed,long now,long cooldown) {
  if(token==null || !token.equals(owner) || !executed) return;
  long until=Math.addExact(now,cooldown);
  if(kind==Kind.AUTO) d.autoAttackCooldownUntil=until;else d.manualAttackCooldownUntil=0;
 }
 public void release(UUID owner) { if(owner!=null && owner.equals(token)) token=null; }
 public boolean busy() { return token!=null; }
 public static long remainingSeconds(long until,long now) {
  if(until<=now) return 0;
  long difference=until-now;return difference/1000+(difference%1000==0?0:1);
 }
}
