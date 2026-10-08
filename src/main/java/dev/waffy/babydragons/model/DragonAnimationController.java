package dev.waffy.babydragons.model;
import dev.waffy.babydragons.BaByDragonsPlugin;
public final class DragonAnimationController {
 private final BaByDragonsPlugin plugin;
 private String current;
 private long overrideUntil;
 public DragonAnimationController(BaByDragonsPlugin plugin) { this.plugin=plugin; }
 public void normal(boolean moving,long tick) {
  if(tick<overrideUntil) return;
  String clip=plugin.settings().s(moving?"animations.follow":"animations.idle");
  if(!clip.equals(current) || !plugin.models().isGalaxyPlaying(clip))
   if(plugin.models().playGalaxy(clip,false)) current=clip;
 }
 public boolean temporary(String clip,long tick,long duration) {
  if(!plugin.models().playGalaxy(clip,true)) return false;
  current=clip;overrideUntil=tick+duration;return true;
 }
 public void reset() { current=null;overrideUntil=0;plugin.models().stopGalaxy(); }
}
