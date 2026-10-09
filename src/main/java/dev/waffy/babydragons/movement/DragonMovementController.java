package dev.waffy.babydragons.movement;
import dev.waffy.babydragons.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
public final class DragonMovementController {
 private final BaByDragonsPlugin plugin;
 private Location target;
 private long lastMovement;
 private double currentSpeed;
 private float desiredYaw;
 private boolean followingMoving;
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
 public void stop(Vex entity) { currentSpeed=0;followingMoving=false;entity.setVelocity(new Vector()); }
 public static float smoothYaw(float current,float desired,double factor) {
  double gap=((desired-current+540)%360+360)%360-180;
  return (float)(current+gap*factor);
 }
 public static double smoothSpeed(double previous,double distance,double stop,double maximum,double factor) {
  double remaining=Math.max(0,distance-stop);
  if(remaining<.001) return 0;
  double desired=Math.min(maximum,remaining*factor);
  return Math.min(remaining,previous+(desired-previous)*factor);
 }
 public static Vector step(Vector delta,double maxStep,double stopDistance) {
  double distance=delta.length();
  if(distance<=stopDistance || distance==0) return new Vector();
  return delta.clone().multiply(Math.min(maxStep,distance-stopDistance)/distance);
 }
 public String status(Vex entity) {
  return "yaw="+entity.getLocation().getYaw()+" | desiredYaw="+desiredYaw+" | target="+target+" | distance="+(target!=null && target.getWorld().equals(entity.getWorld())?entity.getLocation().distance(target):"n/a")
   +" | last successful movement="+(lastMovement==0?"never":java.time.Instant.ofEpochMilli(lastMovement));
 }
 public Location ground(Location current) {
  for(int radius=0;radius<=2;radius++) for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
   Location probe=current.clone().add(x,1,z);
   var hit=probe.getWorld().rayTraceBlocks(probe,new Vector(0,-1,0),Math.min(128,probe.getY()-probe.getWorld().getMinHeight()),FluidCollisionMode.NEVER,true);
   if(hit==null) continue;
   Location seat=hit.getHitPosition().toLocation(current.getWorld()).add(0,.05,0);seat.setYaw(current.getYaw());
   if(clear(seat)) return seat;
  }
  throw new IllegalStateException("No safe ground below dragon; summon it near solid ground first.");
 }
 public boolean toward(Vex entity,Location destination,double speed,double stopDistance) {
  target=destination.clone();
  if(!entity.getWorld().equals(destination.getWorld())) return false;
  Vector delta=destination.toVector().subtract(entity.getLocation().toVector());double distance=delta.length();
  if(distance<=stopDistance+.001) { stop(entity);return true; }
  currentSpeed=smoothSpeed(currentSpeed,distance,stopDistance,speed,plugin.settings().d("movement.follow.smooth-factor"));
  Vector step=step(delta,currentSpeed*plugin.settings().i("movement.update-ticks"),stopDistance);
  Location next=entity.getLocation().add(step);
  if(delta.getX()*delta.getX()+delta.getZ()*delta.getZ()>.001)
   desiredYaw=(float)Math.toDegrees(Math.atan2(-delta.getX(),delta.getZ()));
  next.setYaw(smoothYaw(next.getYaw(),desiredYaw,plugin.settings().d("movement.follow.rotation-smooth-factor")));
  next.setPitch(0);plugin.dragons().holdChunk(next);entity.setVelocity(new Vector());
  if(!entity.teleport(next)) return false;
  lastMovement=System.currentTimeMillis();
  return entity.getLocation().distance(destination)<=stopDistance+1e-7;
 }
 public void face(Vex entity,Location target) {
  Vector delta=target.toVector().subtract(entity.getLocation().toVector());
  if(delta.getX()*delta.getX()+delta.getZ()*delta.getZ()<.001) return;
  desiredYaw=(float)Math.toDegrees(Math.atan2(-delta.getX(),delta.getZ()));
  entity.setRotation(smoothYaw(entity.getLocation().getYaw(),desiredYaw,plugin.settings().d("movement.follow.rotation-smooth-factor")),0);
 }
 public boolean follow(Vex entity,Player owner) {
  double far=plugin.settings().d("movement.teleport-distance");
  if(!entity.getWorld().equals(owner.getWorld()) || entity.getLocation().distanceSquared(owner.getLocation())>far*far) {
   teleport(entity,safeNear(owner));return true;
  }
  Location destination=owner.getLocation().add(0,.2,0);
  double distance=plugin.settings().d("movement.follow.distance");
  if(!followingMoving && entity.getLocation().distanceSquared(destination)<=(distance+.15)*(distance+.15)) {
   target=destination;stop(entity);face(entity,owner.getEyeLocation());return false;
  }
  boolean moving=!toward(entity,destination,plugin.settings().d("movement.follow.max-speed"),distance);
  followingMoving=moving;
  if(!moving) face(entity,owner.getEyeLocation());
  return moving;
 }
 public void teleport(Vex entity,Location target) {
  plugin.dragons().holdChunk(target);stop(entity);
  if(!entity.teleport(target)) throw new IllegalStateException("Controller teleport was denied");
  lastMovement=System.currentTimeMillis();
 }
}
