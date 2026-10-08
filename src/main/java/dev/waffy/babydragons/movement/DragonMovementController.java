package dev.waffy.babydragons.movement;
import dev.waffy.babydragons.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
public final class DragonMovementController {
 private final BaByDragonsPlugin plugin;
 public DragonMovementController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public static Location location(DragonData.Position p) {
  World w=Bukkit.getWorld(p.world());if(w==null) throw new IllegalStateException("Saved world unavailable: "+p.world());
  return new Location(w,p.x(),p.y(),p.z(),p.yaw(),0);
 }
 public static DragonData.Position position(Location l) {
  return new DragonData.Position(l.getWorld().getUID(),l.getX(),l.getY(),l.getZ(),l.getYaw());
 }
 public Location safeNear(Player player) {
  Location base=player.getLocation();
  for(double radius:new double[]{3,2,4,1}) for(int n=0;n<8;n++) {
   double angle=Math.toRadians(base.getYaw()+n*45);
   Location probe=base.clone().add(Math.sin(angle)*radius,2,-Math.cos(angle)*radius);
   var hit=probe.getWorld().rayTraceBlocks(probe,new Vector(0,-1,0),8,FluidCollisionMode.NEVER,true);
   if(hit==null) continue;
   Location result=hit.getHitPosition().toLocation(base.getWorld()).add(0,.15,0);
   if(clear(result)) { result.setYaw(base.getYaw());result.setPitch(0);return result; }
  }
  Location fallback=base.clone().add(0,1,0);
  if(clear(fallback)) return fallback;
  throw new IllegalStateException("No clear position near owner. Move into open space.");
 }
 private boolean clear(Location l) {
  return l.getY()>l.getWorld().getMinHeight() && l.getY()+2<l.getWorld().getMaxHeight()
    && l.getWorld().getWorldBorder().isInside(l) && l.getBlock().isPassable() && l.clone().add(0,1,0).getBlock().isPassable();
 }
 public void stop(Vex entity) { entity.setVelocity(new Vector()); }
 public boolean toward(Vex entity,Location destination,double speed,double stopDistance) {
  if(!entity.getWorld().equals(destination.getWorld())) return false;
  Vector delta=destination.toVector().subtract(entity.getLocation().toVector());double distance=delta.length();
  if(distance<=stopDistance) { stop(entity);return true; }
  double capped=Math.min(speed,Math.max(0,distance-stopDistance)/plugin.settings().i("movement.update-ticks"));
  entity.setVelocity(delta.normalize().multiply(capped));face(entity,destination);return false;
 }
 public void face(Vex entity,Location target) {
  Vector delta=target.toVector().subtract(entity.getLocation().toVector());
  if(delta.getX()*delta.getX()+delta.getZ()*delta.getZ()<.001) return;
  float yaw=(float)Math.toDegrees(Math.atan2(-delta.getX(),delta.getZ()));
  float current=entity.getLocation().getYaw();
  float gap=(float)(((yaw-current+540)%360+360)%360-180);
  entity.setRotation(current+gap*.45f,0);
 }
 public boolean follow(Vex entity,Player owner) {
  double far=plugin.settings().d("movement.teleport-distance");
  if(!entity.getWorld().equals(owner.getWorld()) || entity.getLocation().distanceSquared(owner.getLocation())>far*far) {
   teleport(entity,safeNear(owner));return true;
  }
  return !toward(entity,owner.getLocation().add(0,.2,0),plugin.settings().d("movement.speed-per-tick"),plugin.settings().d("movement.follow-distance"));
 }
 public void teleport(Vex entity,Location target) {
  plugin.dragons().holdChunk(target);stop(entity);
  if(!entity.teleport(target)) throw new IllegalStateException("Controller teleport was denied");
 }
}
