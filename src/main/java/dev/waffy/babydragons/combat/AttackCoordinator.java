package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.*;
import dev.waffy.babydragons.movement.DragonMovementController;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

public final class AttackCoordinator {
 private enum Phase { APPROACH, EXECUTE, RETURN }
 private static final class Sequence {
  UUID token,owner;AttackGate.Kind kind;DragonState previous;DragonData.Position seat;
  LivingEntity enemy;Location target,origin,lastProgress;
  Phase phase=Phase.APPROACH;long phaseStart,executeStart,progressTick;
  int animationTicks;boolean startHitDone,midHitDone,endHitDone,completed;ChainStrikes chain;
 }
 private final BaByDragonsPlugin plugin;
 private final AttackGate gate=new AttackGate();
 private Sequence sequence;
 private EnderCrystal crystal;
 public AttackCoordinator(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public boolean busy() { return sequence!=null || gate.busy(); }
 public AttackGate.Kind kind() { return sequence==null?null:sequence.kind; }
 public String status() {
  Sequence s=sequence;
  return s==null?"idle":"kind="+s.kind+" | phase="+s.phase+" | target="+(s.enemy==null?s.target:s.enemy.getUniqueId())
   +" | strike="+s.chain.index()+" | chain targets="+s.chain.targetCount()+" | hit="+s.chain.hit();
 }
 public boolean isCrystal(Entity entity) { return crystal!=null && crystal.getUniqueId().equals(entity.getUniqueId()); }
 public boolean startAuto(LivingEntity attacker) { return start(AttackGate.Kind.AUTO,attacker); }
 public boolean startManual(LivingEntity target) {
  if(!plugin.models().fireReady()) throw new IllegalStateException("Fire blueprint/execute unavailable; check console.");
  return start(AttackGate.Kind.MANUAL,target);
 }
 private boolean start(AttackGate.Kind kind,LivingEntity enemy) {
  if(!plugin.dragons().valid()) return false;
  Player owner=plugin.dragons().owner();Vex base=plugin.dragons().controller();
  if(owner==null || !owner.isOnline() || owner.isDead() || !owner.hasPermission("babydragons.attack")
   || !plugin.damage().allowed(enemy,owner,kind==AttackGate.Kind.MANUAL) || !base.getWorld().equals(owner.getWorld())) return false;
  double range=plugin.settings().d(kind==AttackGate.Kind.AUTO?"galaxy-attack.max-chase-distance":"fire-attack.target-range");
  if(owner.getLocation().distanceSquared(enemy.getLocation())>range*range) return false;
  int animationTicks=plugin.models().attackTicks(kind==AttackGate.Kind.MANUAL);
  List<Integer> strikeTicks=plugin.settings().strikes();
  int clipTicks=Math.max(4,animationTicks-4);
  if(strikeTicks.getLast()>clipTicks) strikeTicks=java.util.stream.IntStream.rangeClosed(1,4).map(i->i*clipTicks/4).boxed().toList();
  UUID token=gate.acquire(plugin.dragons().data(),kind,System.currentTimeMillis());if(token==null) return false;
  Sequence s=new Sequence();s.token=token;s.kind=kind;s.owner=owner.getUniqueId();s.animationTicks=animationTicks;
  s.previous=plugin.dragons().data().state;s.seat=plugin.dragons().data().sittingLocation;
  s.enemy=enemy;s.target=enemy.getLocation();s.origin=s.target.clone();s.phaseStart=plugin.tick();
  s.progressTick=plugin.tick();s.lastProgress=base.getLocation();s.chain=new ChainStrikes(strikeTicks);
  sequence=s;
  return true;
 }
 public void tick(long tick) {
  Sequence s=sequence;if(s==null) return;
  try {
   Player owner=plugin.dragons().owner();Vex base=plugin.dragons().controller();
   if(owner==null || owner.isDead() || !owner.isOnline() || !owner.getUniqueId().equals(s.owner)
    || base==null || !base.isValid() || !owner.getWorld().equals(s.origin.getWorld()) || !base.getWorld().equals(s.origin.getWorld())) { cancel();return; }
   if(s.phase!=Phase.EXECUTE && tick-s.phaseStart>plugin.settings().d("movement.attack-timeout-seconds")*20) {
    emergencyReturn(s,base,owner);return;
   }
   switch(s.phase) {
    case APPROACH -> {
     refreshTarget(s,owner);
     Location destination=s.kind==AttackGate.Kind.MANUAL?s.target.clone().add(0,1.7,0):s.target;
     boolean arrived=plugin.movement().toward(base,destination,plugin.settings().d("movement.attack.max-speed"),s.kind==AttackGate.Kind.MANUAL?.15:plugin.settings().d("movement.attack-stop-distance"));
     plugin.movement().face(base,s.target);
     plugin.animations().normal(!arrived,tick);
     if(arrived) beginExecute(s,base,tick);
     else if(stuck(s,base,tick)) emergencyReturn(s,base,owner);
    }
    case EXECUTE -> {
     long elapsed=tick-s.executeStart;
     if(s.kind==AttackGate.Kind.AUTO) galaxy(s,owner,base,elapsed);
     else fire(s,owner,base,elapsed);
     boolean moments=s.kind==AttackGate.Kind.AUTO?s.chain.complete():s.endHitDone && crystal==null;
     if(moments && elapsed>=s.animationTicks && plugin.models().attackFinished(s.kind==AttackGate.Kind.MANUAL)) {
      if(s.kind==AttackGate.Kind.AUTO) levitate(s,owner);
      s.completed=true;startReturn(s,tick);
     } else if(elapsed>Math.max(s.animationTicks,plugin.settings().strikes().getLast())+200) {
      plugin.getLogger().warning("Execute completion watchdog expired; recovering dragon safely");
      startReturn(s,tick);
     }
    }
    case RETURN -> {
     Location destination=s.previous==DragonState.SITTING?DragonMovementController.location(s.seat):owner.getLocation().add(0,.2,0);
     boolean arrived=plugin.movement().toward(base,destination,plugin.settings().d("movement.return.max-speed"),s.previous==DragonState.SITTING?0:plugin.settings().d("movement.follow.distance"));
     plugin.movement().face(base,destination);
     plugin.animations().normal(!arrived,tick);
     if(arrived) finish(false);else if(stuck(s,base,tick)) emergencyReturn(s,base,owner);
    }
   }
  } catch(RuntimeException error) {
   plugin.getLogger().log(java.util.logging.Level.WARNING,"Attack cancelled after an error",error);cancel();
  }
 }
 private void beginExecute(Sequence s,Vex base,long tick) {
  plugin.movement().stop(base);plugin.movement().face(base,s.target);
  boolean manual=s.kind==AttackGate.Kind.MANUAL;
  boolean played=plugin.animations().attack(manual?"":plugin.settings().s("galaxy-attack.animation.id"));
  if(manual) played=plugin.models().beginFire();
  if(!played) { cancel();return; }
  s.phase=Phase.EXECUTE;s.executeStart=tick;
  if(!manual) plugin.immunity().begin(plugin.dragons().owner(),s.token);
  plugin.sounds().play(manual?"fire-execute":"galaxy-execute-start",base.getLocation(),null);
  if(manual) fire(s,plugin.dragons().owner(),base,0);
 }
 private boolean eligible(Sequence s,LivingEntity target,Player owner) {
  if(target==null || !plugin.damage().allowed(target,owner,s.kind==AttackGate.Kind.MANUAL)) return false;
  double range=plugin.settings().d(s.kind==AttackGate.Kind.AUTO?"galaxy-attack.max-chase-distance":"fire-attack.target-range");
  return target.getLocation().distanceSquared(s.origin)<=range*range && target.getLocation().distanceSquared(owner.getLocation())<=range*range;
 }
 private List<LivingEntity> candidates(Sequence s,Player owner) {
  double radius=plugin.settings().d("galaxy-attack.chain-radius");
  List<LivingEntity> found=new ArrayList<>();
  if(eligible(s,s.enemy,owner)) found.add(s.enemy);
  for(Entity e:s.target.getWorld().getNearbyEntities(s.target,radius,radius,radius))
   if(e instanceof LivingEntity living && eligible(s,living,owner) && !found.contains(living)
    && living.getLocation().distanceSquared(s.target)<=radius*radius) found.add(living);
  return found;
 }
 private void refreshTarget(Sequence s,Player owner) {
  if(!eligible(s,s.enemy,owner)) {
   if(s.kind==AttackGate.Kind.AUTO) chooseNext(s,owner);else s.enemy=null;
  }
  if(s.enemy!=null) s.target=s.enemy.getLocation();
 }
 private void chooseNext(Sequence s,Player owner) {
  List<LivingEntity> options=candidates(s,owner);
  UUID next=s.chain.select(options.stream().map(Entity::getUniqueId).toList());
  s.enemy=options.stream().filter(e->e.getUniqueId().equals(next)).findFirst().orElse(null);
  if(s.enemy!=null) s.target=s.enemy.getLocation();
 }
 private void galaxy(Sequence s,Player owner,Vex base,long elapsed) {
  refreshTarget(s,owner);
  if(!s.chain.complete()) {
   plugin.movement().toward(base,s.target,plugin.settings().d("movement.attack.max-speed"),plugin.settings().d("movement.attack-stop-distance"));
   plugin.movement().face(base,s.target);
   if(elapsed%4==0) indicator(s.target);
   while(s.chain.due(elapsed)) {
    UUID selected=s.enemy==null?null:s.enemy.getUniqueId();
    s.target.getWorld().spawnParticle(Particle.DRAGON_BREATH,s.target.clone().add(0,.8,0),16,.6,.3,.6,.01);
    plugin.sounds().play("galaxy-strike",s.target,null);
    if(eligible(s,s.enemy,owner) && base.getLocation().distanceSquared(s.target)<=3.5*3.5) {
     double amount=plugin.settings().d(s.enemy instanceof Player?"galaxy-attack.player-damage-per-strike":"galaxy-attack.mob-damage-per-strike");
     if(plugin.damage().damage(s.enemy,owner,amount,false)) s.chain.hit(selected);
    }
    s.chain.strike(selected);chooseNext(s,owner);
   }
  } else { plugin.movement().stop(base);plugin.movement().face(base,s.target); }
 }
 private void fire(Sequence s,Player owner,Vex base,long elapsed) {
  refreshTarget(s,owner);
  plugin.movement().toward(base,s.target.clone().add(0,1.7,0),plugin.settings().d("movement.attack.max-speed"),.15);
  plugin.movement().face(base,s.target);beam(base,s.target);
  int clipTicks=Math.max(1,s.animationTicks-4);
  if(!s.startHitDone) { s.startHitDone=true;fireHit(s,owner,base,0); }
  if(!s.midHitDone && elapsed>=clipTicks/2) { s.midHitDone=true;fireHit(s,owner,base,1); }
  if(!s.endHitDone && elapsed>=clipTicks) { s.endHitDone=true;fireHit(s,owner,base,2); }
 }
 private void fireHit(Sequence s,Player owner,Vex base,int moment) {
  World world=s.target.getWorld();
  try {
   if(moment==2) { spawnCrystal(s.target.clone().add(0,.5,0));plugin.sounds().play("crystal-pop",s.target,null);world.spawnParticle(Particle.EXPLOSION,s.target,1); }
   world.spawnParticle(Particle.FLAME,s.target,8+moment*12,.5+moment*.25,.4,.5+moment*.25,.02);
   world.spawnParticle(Particle.END_ROD,s.target,2+moment*2,.6,.4,.6,.01);
   double x=plugin.settings().d("fire-attack.blast-radius.x"),y=plugin.settings().d("fire-attack.blast-radius.y"),z=plugin.settings().d("fire-attack.blast-radius.z");
   if(base.getLocation().distanceSquared(s.target)<=4*4)
    for(Entity e:world.getNearbyEntities(s.target,x,y,z)) if(e instanceof LivingEntity living) {
     boolean hit=plugin.damage().damage(living,owner,plugin.settings().d(living instanceof Player?"fire-attack.player-damage":"fire-attack.mob-damage"),true);
     if(hit && moment==2 && living instanceof Player player && !player.isDead()) plugin.launches().launch(player,s.token);
    }
  } finally { if(moment==2) removeCrystal(); }
 }
 private void levitate(Sequence s,Player owner) {
  for(UUID id:s.chain.hit()) {
   Entity e=plugin.getServer().getEntity(id);
   if(e instanceof LivingEntity living && plugin.damage().allowed(living,owner,false))
    living.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,(int)(plugin.settings().d("galaxy-attack.levitation-seconds")*20),plugin.settings().i("galaxy-attack.levitation-amplifier")));
  }
 }
 private void startReturn(Sequence s,long tick) {
  plugin.immunity().end(s.token);
  removeCrystal();plugin.models().endFire();plugin.animations().reset();s.phase=Phase.RETURN;s.phaseStart=tick;
  s.lastProgress=plugin.dragons().controller().getLocation();s.progressTick=tick;
 }
 private boolean stuck(Sequence s,Vex base,long tick) {
  if(base.getLocation().distanceSquared(s.lastProgress)>.0025) { s.lastProgress=base.getLocation();s.progressTick=tick; }
  return tick-s.progressTick>=plugin.settings().d("movement.unstuck-seconds")*20;
 }
 private void emergencyReturn(Sequence s,Vex base,Player owner) {
  plugin.movement().teleport(base,s.previous==DragonState.SITTING?DragonMovementController.location(s.seat):plugin.movement().safeNear(owner));
  finish(false);
 }
 private void indicator(Location target) {
  for(int i=0;i<8;i++) {
   double angle=i*Math.PI/4;
   target.getWorld().spawnParticle(Particle.REVERSE_PORTAL,target.clone().add(Math.cos(angle)*.7,.2,Math.sin(angle)*.7),1,0,0,0,0);
  }
 }
 private void beam(Vex base,Location target) {
  Location start=base.getLocation().add(0,.6,0);Vector delta=target.toVector().subtract(start.toVector());
  int samples=Math.max(2,Math.min(24,(int)Math.ceil(delta.length()*3)));
  for(int i=0;i<=samples;i++) {
   Location point=start.clone().add(delta.clone().multiply((double)i/samples));
   target.getWorld().spawnParticle(Particle.FLAME,point,1,.03,.03,.03,0);
   if(i%5==0) target.getWorld().spawnParticle(Particle.END_ROD,point,1,0,0,0,0);
  }
 }
 private void spawnCrystal(Location where) {
  removeCrystal();crystal=where.getWorld().spawn(where,EnderCrystal.class,e->{
   plugin.dragons().mark(e,"crystal");e.setInvulnerable(true);e.setSilent(true);e.setShowingBottom(false);e.setGravity(false);e.setPersistent(false);
  });
 }
 private void removeCrystal() { if(crystal!=null) { crystal.remove();crystal=null; } }
 public void cancel() { if(sequence!=null) finish(true);else { removeCrystal();plugin.immunity().end(); } }
 private void finish(boolean cancelled) {
  Sequence s=sequence;if(s==null) return;
  try { plugin.models().endFire(); }
  finally {
   try {
    removeCrystal();Vex base=plugin.dragons().controller();
    if(base!=null && base.isValid()) {
     plugin.movement().stop(base);
     if(s.previous==DragonState.SITTING && s.seat!=null) {
      Location seat=DragonMovementController.location(s.seat);
      if(cancelled) plugin.movement().teleport(base,seat);
      else base.setRotation(seat.getYaw(),0);
     }
    }
    plugin.animations().reset();
    if(s.completed && !cancelled) gate.complete(plugin.dragons().data(),s.kind,s.token,true,System.currentTimeMillis(),(long)(plugin.settings().d("galaxy-attack.cooldown-seconds")*1000));
   } finally {
    sequence=null;gate.release(s.token);
    try { if(s.phase!=Phase.RETURN) plugin.immunity().end(s.token); } finally { plugin.dragons().save(); }
   }
  }
 }
}
