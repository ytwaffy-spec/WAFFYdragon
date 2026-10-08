package dev.waffy.babydragons.combat;
import dev.waffy.babydragons.*;
import dev.waffy.babydragons.movement.DragonMovementController;
import java.util.UUID;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

public final class AttackCoordinator {
 private enum Phase { APPROACH, WINDUP, CRYSTAL, RECOVER, RETURN }
 private static final class Sequence {
  UUID token,owner;AttackGate.Kind kind;DragonState previous;DragonData.Position seat;
  LivingEntity enemy;Location target;Phase phase=Phase.APPROACH;long started,deadline;boolean executed;
 }
 private final BaByDragonsPlugin plugin;
 private final AttackGate gate=new AttackGate();
 private Sequence sequence;
 private EnderCrystal crystal;
 public AttackCoordinator(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public boolean busy() { return sequence!=null || gate.busy(); }
 public AttackGate.Kind kind() { return sequence==null?null:sequence.kind; }
 public boolean isCrystal(Entity entity) { return crystal!=null && crystal.getUniqueId().equals(entity.getUniqueId()); }
 public boolean startAuto(LivingEntity attacker) {
  Player owner=plugin.dragons().owner();
  if(owner==null || !plugin.damage().allowed(attacker,owner,false)) return false;
  return start(AttackGate.Kind.AUTO,attacker,attacker.getLocation());
 }
 public boolean startManual(Location target) {
  if(!plugin.models().fireReady()) throw new IllegalStateException("Fire blueprint/execute is unavailable; check console.");
  return start(AttackGate.Kind.MANUAL,null,target);
 }
 private boolean start(AttackGate.Kind kind,LivingEntity enemy,Location target) {
  if(!plugin.dragons().valid()) return false;
  Player owner=plugin.dragons().owner();
  if(owner==null || !owner.isOnline() || owner.isDead() || !owner.hasPermission("babydragons.attack")
   || !target.getWorld().equals(owner.getWorld()) || !target.getWorld().equals(plugin.dragons().controller().getWorld())) return false;
  UUID token=gate.acquire(plugin.dragons().data(),kind,System.currentTimeMillis());if(token==null) return false;
  Sequence s=new Sequence();s.token=token;s.kind=kind;s.owner=owner.getUniqueId();
  s.previous=plugin.dragons().data().state;s.seat=plugin.dragons().data().sittingLocation;
  s.enemy=enemy;s.target=target.clone();s.started=plugin.tick();sequence=s;return true;
 }
 public void tick(long tick) {
  Sequence s=sequence;if(s==null) return;
  try {
   Player owner=plugin.dragons().owner();Vex base=plugin.dragons().controller();
   if(owner==null || owner.isDead() || !owner.isOnline() || !owner.getUniqueId().equals(s.owner)
    || base==null || !base.isValid() || !owner.getWorld().equals(s.target.getWorld())
    || !base.getWorld().equals(s.target.getWorld())) { cancel();return; }
   if(tick-s.started>plugin.settings().d("movement.attack-timeout-seconds")*20) { cancel();return; }
   if(s.kind==AttackGate.Kind.AUTO && s.phase!=Phase.RETURN && s.phase!=Phase.RECOVER) {
    if(s.enemy==null || !plugin.damage().allowed(s.enemy,owner,false)) { cancel();return; }
    s.target=s.enemy.getLocation().add(0,.5,0);
   }
   switch(s.phase) {
    case APPROACH -> {
     Location destination=s.kind==AttackGate.Kind.MANUAL?s.target.clone().add(0,2,0):s.target;
     double stop=s.kind==AttackGate.Kind.MANUAL?.7:plugin.settings().d("movement.attack-stop-distance");
     boolean arrived=plugin.movement().toward(base,destination,plugin.settings().d("movement.attack-speed-per-tick"),stop);
     plugin.animations().normal(!arrived,tick);
     if(arrived) {
      plugin.movement().stop(base);plugin.movement().face(base,s.target);
      int duration=plugin.settings().i("animations.execute-windup-ticks")+plugin.settings().i("animations.execute-recovery-ticks")
       +plugin.settings().i("manual-attack.crystal-ticks");
      boolean played;
      if(s.kind==AttackGate.Kind.AUTO) played=plugin.animations().temporary(plugin.settings().s("animations.auto-attack"),tick,duration);
      else {
       plugin.animations().temporary(plugin.settings().s("animations.idle"),tick,duration);
       played=plugin.models().beginFire();
      }
      if(!played) { cancel();return; }
      s.phase=Phase.WINDUP;s.deadline=tick+plugin.settings().i("animations.execute-windup-ticks");
     }
    }
    case WINDUP -> {
     plugin.movement().stop(base);plugin.movement().face(base,s.target);
     if(tick>=s.deadline) {
      if(s.kind==AttackGate.Kind.AUTO) {
       double reach=plugin.settings().d("movement.attack-stop-distance")+2;
       if(base.getLocation().distanceSquared(s.target)<=reach*reach) executeAuto(s,owner);
       s.phase=Phase.RECOVER;s.deadline=tick+plugin.settings().i("animations.execute-recovery-ticks");
      } else {
       spawnCrystal(s.target);s.phase=Phase.CRYSTAL;s.deadline=tick+plugin.settings().i("manual-attack.crystal-ticks");
      }
     }
    }
    case CRYSTAL -> {
     plugin.movement().stop(base);
     if(tick>=s.deadline) {
      removeCrystal();executeManual(s,owner);
      s.phase=Phase.RECOVER;s.deadline=tick+plugin.settings().i("animations.execute-recovery-ticks");
     }
    }
    case RECOVER -> {
     plugin.movement().stop(base);
     if(tick>=s.deadline) { plugin.models().endFire();plugin.animations().reset();s.phase=Phase.RETURN; }
    }
    case RETURN -> {
     Location destination=s.previous==DragonState.SITTING?DragonMovementController.location(s.seat):owner.getLocation().add(0,.2,0);
     boolean arrived=plugin.movement().toward(base,destination,plugin.settings().d("movement.attack-speed-per-tick"),
      s.previous==DragonState.SITTING?.15:plugin.settings().d("movement.follow-distance"));
     plugin.animations().normal(!arrived,tick);if(arrived) finish();
    }
   }
  } catch(RuntimeException error) {
   plugin.getLogger().log(java.util.logging.Level.WARNING,"Attack cancelled after an error",error);cancel();
  }
 }
 private void executeAuto(Sequence s,Player owner) {
  if(plugin.damage().damage(s.enemy,owner,plugin.settings().d("auto-attack.damage"),false)) {
   executed(s);
   s.enemy.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,(int)(plugin.settings().d("auto-attack.levitation-seconds")*20),
    plugin.settings().i("auto-attack.levitation-amplifier")));
   double r=plugin.settings().d("auto-attack.breath-radius");
   s.target.getWorld().spawnParticle(Particle.DRAGON_BREATH,s.target,90,r,.5,r,.025);
  }
 }
 private void executeManual(Sequence s,Player owner) {
  World world=s.target.getWorld();
  world.playSound(s.target,Sound.ENTITY_GENERIC_EXPLODE,.7f,1.4f);
  world.spawnParticle(Particle.EXPLOSION,s.target,1);
  world.spawnParticle(Particle.FLAME,s.target,70,1.2,.6,1.2,.025);
  world.spawnParticle(Particle.END_ROD,s.target,40,1,.8,1,.02);
  executed(s); // A completed visual strike consumes only manual cooldown, even with no enemy.
  double r=plugin.settings().d("manual-attack.radius");
  for(Entity e:world.getNearbyEntities(s.target,r,r,r))
   if(e instanceof LivingEntity living && living.getLocation().distanceSquared(s.target)<=r*r)
    plugin.damage().damage(living,owner,plugin.settings().d("manual-attack.damage"),true);
 }
 private void executed(Sequence s) {
  if(s.executed) return;s.executed=true;
  String path=s.kind==AttackGate.Kind.AUTO?"auto-attack.cooldown-seconds":"manual-attack.cooldown-seconds";
  gate.complete(plugin.dragons().data(),s.kind,s.token,true,System.currentTimeMillis(),(long)(plugin.settings().d(path)*1000));
  plugin.dragons().save();
 }
 private void spawnCrystal(Location where) {
  removeCrystal();
  crystal=where.getWorld().spawn(where,EnderCrystal.class,e->{
   plugin.dragons().mark(e,"crystal");e.setInvulnerable(true);e.setSilent(true);e.setShowingBottom(false);e.setGravity(false);e.setPersistent(false);
  });
 }
 private void removeCrystal() { if(crystal!=null) { crystal.remove();crystal=null; } }
 public void cancel() { if(sequence!=null) finish();else removeCrystal(); }
 private void finish() {
  Sequence s=sequence;if(s==null) return;
  try { plugin.models().endFire(); }
  finally {
   try {
    removeCrystal();Vex base=plugin.dragons().controller();
    if(plugin.dragons().data()!=null) { plugin.dragons().data().state=s.previous;plugin.dragons().data().sittingLocation=s.seat; }
    if(base!=null && base.isValid()) {
     base.setVelocity(new Vector());
     if(s.previous==DragonState.SITTING && s.seat!=null) plugin.movement().teleport(base,DragonMovementController.location(s.seat));
    }
    plugin.animations().reset();
   } finally { gate.release(s.token);sequence=null;plugin.dragons().save(); }
  }
 }
}
