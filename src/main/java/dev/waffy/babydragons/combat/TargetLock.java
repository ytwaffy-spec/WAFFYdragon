package dev.waffy.babydragons.combat;
import java.util.UUID;
public final class TargetLock {
 private UUID target;
 private long lastSeen;
 public UUID target() { return target; }
 public boolean update(UUID aimed,boolean previousValid,long now,long grace) {
  UUID before=target;
  if(aimed!=null) { target=aimed;lastSeen=now; }
  else if(!previousValid || now-lastSeen>=grace) target=null;
  return !java.util.Objects.equals(before,target);
 }
 public void clear() { target=null;lastSeen=0; }
}
