package dev.waffy.babydragons.effects;
public record EffectLease(int amplifier,long expiresAtTick,boolean ambient,boolean particles,boolean icon) {
 public boolean matches(int level,int remaining,long tick,boolean isAmbient,boolean hasParticles,boolean hasIcon) {
  return level==amplifier && Math.abs(remaining-Math.max(0,expiresAtTick-tick))<=3
   && ambient==isAmbient && particles==hasParticles && icon==hasIcon;
 }
}
