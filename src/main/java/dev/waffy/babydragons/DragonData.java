package dev.waffy.babydragons;
import java.util.UUID;
public final class DragonData {
 public UUID dragonId, ownerUuid, controllerEntityUuid, nameEntityUuid;
 public String dragonName="BabyDragon", nameColor="#B026FF", baseModelId;
 public DragonState state=DragonState.FOLLOWING;
 public Position location, sittingLocation;
 public boolean effectsEnabled=true, autoAttackEnabled=true, manualAttackEnabled=true;
 public long autoAttackCooldownUntil, manualAttackCooldownUntil;
 public record Position(UUID world,double x,double y,double z,float yaw) {
  public Position {
   if(world==null || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
     || !Float.isFinite(yaw) || Math.abs(x)>30_000_000 || Math.abs(z)>30_000_000)
    throw new IllegalArgumentException("Invalid position");
  }
 }
 public void validate() {
  if(dragonId==null || ownerUuid==null || location==null || state==null || baseModelId==null
    || baseModelId.isBlank() || autoAttackCooldownUntil<0 || manualAttackCooldownUntil<0)
   throw new IllegalArgumentException("Incomplete dragon data");
  NameRules.name(dragonName); NameRules.color(nameColor);
  if(state==DragonState.SITTING && sittingLocation==null)
   throw new IllegalArgumentException("Sitting dragon has no saved seat");
 }
}
