package dev.waffy.babydragons.combat;
import java.util.*;

/** One timeline, including empty visual strikes, with least-used eligible targets first. */
public final class ChainStrikes {
 private final List<Integer> times;
 private final Map<UUID,Integer> uses=new LinkedHashMap<>();
 private final Set<UUID> hit=new LinkedHashSet<>();
 private int index;
 public ChainStrikes(List<Integer> times) { this.times=List.copyOf(times); }
 public int index() { return index; }
 public int targetCount() { return uses.size(); }
 public Set<UUID> hit() { return Set.copyOf(hit); }
 public void hit(UUID target) { hit.add(target); }
 public boolean due(long elapsed) { return index<times.size() && elapsed>=times.get(index); }
 public boolean complete() { return index==times.size(); }
 public UUID select(List<UUID> candidates) {
  return candidates.stream().min(Comparator.comparingInt(id->uses.getOrDefault(id,0))).orElse(null);
 }
 public void strike(UUID target) {
  if(complete()) throw new IllegalStateException("All strikes already consumed");
  if(target!=null) uses.merge(target,1,Integer::sum);
  index++;
 }
}
