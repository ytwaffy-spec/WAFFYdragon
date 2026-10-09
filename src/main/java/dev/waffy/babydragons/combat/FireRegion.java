package dev.waffy.babydragons.combat;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class FireRegion {
 private final Location center;
 private final Vector right;
 public FireRegion(Location center,Vector forward) {
  this.center=center.clone();forward=forward.clone().setY(0);
  if(forward.lengthSquared()<.001) forward=new Vector(0,0,1);
  forward.normalize();right=new Vector(-forward.getZ(),0,forward.getX());
 }
 public Location center() { return center.clone(); }
 public Vector blast(int moment) { return right.clone().multiply(moment==0?.65:-.65).setY(.25); }
 public Location clamp(Location location,boolean vertical) {
  Location result=location.clone();
  result.setX(Math.clamp(result.getX(),center.getX()-3.3,center.getX()+3.3));
  result.setZ(Math.clamp(result.getZ(),center.getZ()-3.3,center.getZ()+3.3));
  if(vertical) result.setY(Math.clamp(result.getY(),center.getY()-3.3,center.getY()+3.3));
  return result;
 }
 public Vector velocity(Location location,Vector velocity,boolean vertical) {
  Location predicted=location.clone().add(velocity);Location bounded=clamp(predicted,vertical);
  return bounded.toVector().subtract(location.toVector());
 }
 public static int[] pulseTicks(int length) {
  if(length<8) throw new IllegalArgumentException("Fire execute must last at least 8 ticks for three blasts and recovery");
  int end=Math.min((int)(length*.78),length-4);
  int middle=Math.max(2,Math.min((int)(length*.53),end-1));
  return new int[]{Math.max(1,Math.min((int)(length*.23),middle-1)),middle,end};
 }
}
