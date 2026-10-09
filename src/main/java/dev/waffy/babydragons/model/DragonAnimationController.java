package dev.waffy.babydragons.model;
import dev.waffy.babydragons.BaByDragonsPlugin;
public final class DragonAnimationController {
 private final BaByDragonsPlugin plugin;
 private String current;
 private long overrideUntil;
 private boolean attackOverride;
 public DragonAnimationController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void normal(boolean moving,long tick) {
  if(attackOverride || tick<overrideUntil) return;
  String clip=plugin.settings().s(moving?"animations.follow":"animations.idle");
  if(!clip.equals(current) || !plugin.models().isGalaxyPlaying(clip))
   if(plugin.models().playGalaxy(clip,false)) current=clip;
 }
 public boolean temporary(String clip,long tick,long duration) {
  if(attackOverride) return false;
  plugin.models().stopGalaxy();
  if(!plugin.models().playGalaxy(clip,true)) return false;
  current=clip;overrideUntil=tick+duration;return true;
 }
 public boolean attack(String clip) {
  reset();attackOverride=true;
  return clip.isEmpty() || plugin.models().beginGalaxyAttack();
 }
 public void sitting(long tick) {
  if(attackOverride || tick<overrideUntil) return;
  String clip=plugin.settings().s("animations.sitting");
  if(clip.isEmpty()) { plugin.models().stopGalaxy();current=null; }
  else if(!clip.equals(current) || !plugin.models().isGalaxyPlaying(clip)) {
   plugin.models().stopGalaxy();if(plugin.models().playGalaxy(clip,true)) current=clip;
  }
 }
 public void reset() { current=null;overrideUntil=0;attackOverride=false;plugin.models().stopGalaxy(); }
}
