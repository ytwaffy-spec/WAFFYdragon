package dev.waffy.babydragons;
import dev.waffy.babydragons.combat.*;
import dev.waffy.babydragons.command.*;
import dev.waffy.babydragons.effects.DragonEffectController;
import dev.waffy.babydragons.gui.*;
import dev.waffy.babydragons.listener.*;
import dev.waffy.babydragons.model.*;
import dev.waffy.babydragons.movement.DragonMovementController;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class BaByDragonsPlugin extends JavaPlugin {
 private Settings settings;
 private YamlConfiguration messages;
 private ModelEngineController models;
 private DragonAnimationController animations;
 private DragonMovementController movement;
 private DragonManager dragons;
 private AttackCoordinator attacks;
 private FireLaunchProtection launches;
 private CombatSounds sounds;
 private DamageService damage;
 private DragonEffectController effects;
 private ManualFireAttackController manual;
 private DragonInteractionListener interactions;
 private DragonControlGUI gui;
 private DragonStorageGUI storageGui;
 private final List<BukkitTask> tasks=new ArrayList<>();
 private long tick;
 private boolean ready;
 @Override public void onEnable() {
  saveDefaultConfig();
  if(!new File(getDataFolder(),"messages.yml").exists()) saveResource("messages.yml",false);
  try {
   settings=loadSettings();messages=loadMessages();
   var engine=getServer().getPluginManager().getPlugin("ModelEngine");
   if(engine==null || !engine.isEnabled()) throw new IllegalStateException("ModelEngine is required and must be enabled first.");
   models=new ModelEngineController(this);animations=new DragonAnimationController(this);movement=new DragonMovementController(this);
   dragons=new DragonManager(this);damage=new DamageService(this);effects=new DragonEffectController(this);
   launches=new FireLaunchProtection(this);sounds=new CombatSounds(this);
   attacks=new AttackCoordinator(this);manual=new ManualFireAttackController(this);
   interactions=new DragonInteractionListener(this);gui=new DragonControlGUI(this);storageGui=new DragonStorageGUI(this);
   for(Listener listener:List.of(damage,effects,manual,launches,interactions,new AutoDefenseController(this),new InventoryListener(this),new LifecycleListener(this)))
    getServer().getPluginManager().registerEvents(listener,this);
   models.registerInteractions(interactions,interactions::modelInteract);
   var normal=new DragonCommand(this);var admin=new DragonAdminCommand(this);
   Objects.requireNonNull(getCommand("dragon")).setExecutor(normal);getCommand("dragon").setTabCompleter(normal);
   Objects.requireNonNull(getCommand("dragonadmin")).setExecutor(admin);getCommand("dragonadmin").setTabCompleter(admin);
   ready=true;
   getServer().getScheduler().runTask(this,()->{
    try { models.validateModels();dragons.recover();dragons.cleanupLoaded();dragons.save();schedule(); }
    catch(RuntimeException error) { fail("Startup recovery failed; data retained",error); }
   });
  } catch(Exception error) { fail("BaByDragons could not enable",error); }
 }
 private YamlConfiguration loadMessages() throws Exception {
  var y=new YamlConfiguration();y.load(new File(getDataFolder(),"messages.yml"));return y;
 }
 private Settings loadSettings() throws Exception {
  File file=new File(getDataFolder(),"config.yml");var config=new YamlConfiguration();config.load(file);
  var defaults=new YamlConfiguration();
  try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(getResource("config.yml")),java.nio.charset.StandardCharsets.UTF_8)) { defaults.load(reader); }
  String before=config.saveToString();Settings.migrate(config,defaults);Settings result=new Settings(config);
  for(String key:List.of("galaxy-execute-start","galaxy-strike","fire-lock","fire-execute","fire-blast")) Sound.valueOf(result.s("sounds."+key+".sound"));
  if(!before.equals(config.saveToString())) {
   java.nio.file.Path backup=file.toPath().resolveSibling("config.before-1.67.A.yml");
   if(!java.nio.file.Files.exists(backup)) java.nio.file.Files.copy(file.toPath(),backup);
   config.save(file);
  }
  return result;
 }
 private void fail(String message,Exception e) {
  getLogger().log(Level.SEVERE,message,e);getServer().getPluginManager().disablePlugin(this);
 }
 private void schedule() {
  tasks.forEach(BukkitTask::cancel);tasks.clear();
  int period=settings.i("movement.update-ticks");
  tasks.add(getServer().getScheduler().runTaskTimer(this,()->{
   tick+=period;try { launches.tick();manual.tick();dragons.tick(tick); } catch(RuntimeException e) { fail("Dragon update failed; stopping safely",e); }
  },1,period));
  tasks.add(getServer().getScheduler().runTaskTimer(this,()->{
   try { effects.tick(); } catch(RuntimeException e) { fail("Dragon effects failed; stopping safely",e); }
  },20,settings.i("effects.refresh-ticks")));
  tasks.add(getServer().getScheduler().runTaskTimer(this,manual::feedback,20,20));
  tasks.add(getServer().getScheduler().runTaskTimer(this,gui::refreshOpen,20,20));
  tasks.add(getServer().getScheduler().runTaskTimer(this,dragons::save,settings.i("persistence.save-interval-ticks"),settings.i("persistence.save-interval-ticks")));
 }
 @Override public void onDisable() {
  tasks.forEach(BukkitTask::cancel);tasks.clear();
  if(manual!=null) manual.clear();
  if(launches!=null) launches.clear();
  if(ready && dragons!=null) {
   try { dragons.shutdown(); } catch(Exception e) { getLogger().log(Level.SEVERE,"Shutdown cleanup failed",e); }
  }
  for(Player p:getServer().getOnlinePlayers()) if(p.getOpenInventory().getTopInventory().getHolder() instanceof DragonMenuHolder) p.closeInventory();
  ready=false;
 }
 public void reloadSettings() throws Exception {
  // Validate new files before detaching an existing visual.
  Settings next=loadSettings();YamlConfiguration nextMessages=loadMessages();
  manual.clear();
  launches.clear();
  attacks.cancel();effects.clearAll();
  if(dragons.controller()!=null && dragons.controller().isValid()) movement.stop(dragons.controller());
  models.detach();settings=next;messages=nextMessages;
  try { models.validateModels();dragons.recover();animations.reset();dragons.save();schedule(); }
  catch(RuntimeException e) { fail("Reload recovery failed; data retained",e);throw e; }
 }
 public void toggleEffects() {
  if(dragons.data()==null) return;
  dragons.data().effectsEnabled=!dragons.data().effectsEnabled;
  if(!dragons.data().effectsEnabled) effects.clearAll();else effects.tick();dragons.save();
 }
 public void setAuto(boolean value) {
  setAssist(value);setDefense(value);
 }
 public void setAssist(boolean value) {
  if(dragons.data()==null) return;dragons.data().assistAttackEnabled=value;dragons.save();
 }
 public void setDefense(boolean value) {
  if(dragons.data()==null) return;dragons.data().defenseAttackEnabled=value;dragons.save();
 }
 public void setManual(boolean value) {
  if(dragons.data()==null) return;dragons.data().manualAttackEnabled=value;
  if(!value) { manual.clear();if(attacks.kind()==AttackGate.Kind.MANUAL) attacks.cancel(); }dragons.save();
 }
 public void busy(Player player) { player.sendActionBar(Component.text(dragons.data().dragonName+" is already attacking.")); }
 public String text(String key,String fallback) { return messages.getString(key,fallback); }
 public Component mm(String trusted) { return MiniMessage.miniMessage().deserialize(trusted); }
 public void message(CommandSender sender,String key) { sender.sendMessage(mm(text("prefix","")+text(key,key))); }
 public void action(Player player,String key) { player.sendActionBar(mm(text(key,key))); }
 public void renameHelp(Player player) { for(String line:messages.getStringList("rename-help")) player.sendMessage(mm(line)); }
 public void info(CommandSender sender) {
  var engine=getServer().getPluginManager().getPlugin("ModelEngine");
  sender.sendMessage("BaByDragons "+getPluginMeta().getVersion()+" | ModelEngine "+(engine==null?"missing":engine.getPluginMeta().getVersion()));
  sender.sendMessage("Galaxy: "+settings.s("models.galaxy.id")+" | validated: "+models.baseReady());
  sender.sendMessage("Fire: "+settings.s("models.fire.id")+" | validated: "+models.fireReady());
  DragonData d=dragons.data();if(d==null) { message(sender,"no-dragon");return; }
  sender.sendMessage("Dragon: "+d.dragonId+" | Owner: "+d.ownerUuid);
  sender.sendMessage("Controller: "+d.controllerEntityUuid+" | Name: "+d.dragonName+" "+d.nameColor);
  sender.sendMessage("State: "+d.state+(attacks.busy()?" (ATTACKING)":"")+" | Effects: "+d.effectsEnabled);
  sender.sendMessage("Assist: "+d.assistAttackEnabled+" | Defense: "+d.defenseAttackEnabled+" | Galaxy cooldown: "+AttackGate.remainingSeconds(d.autoAttackCooldownUntil,System.currentTimeMillis())+"s");
  sender.sendMessage("Fire: "+d.manualAttackEnabled+" | cooldown: None - Phase 1 | "+manual.status());
  sender.sendMessage("Location: "+d.location);
  var base=dragons.controller();
  if(base!=null) {
   sender.sendMessage("Controller type="+base.getType()+" | AI="+base.hasAI()+" | gravity="+base.hasGravity()+" | velocity="+base.getVelocity()+" | location="+base.getLocation());
   sender.sendMessage("Movement mode="+(attacks.busy()?attacks.status():d.state)+" | "+movement.status(base));
  }
  sender.sendMessage("Attack busy="+attacks.busy()+" | "+attacks.status());models.status(sender);
 }
 public NamespacedKey key(String key) { return new NamespacedKey(this,key); }
 public long tick() { return tick; }
 public Settings settings() { return settings; }
 public DragonManager dragons() { return dragons; }
 public ModelEngineController models() { return models; }
 public DragonAnimationController animations() { return animations; }
 public DragonMovementController movement() { return movement; }
 public AttackCoordinator attacks() { return attacks; }
 public FireLaunchProtection launches() { return launches; }
 public CombatSounds sounds() { return sounds; }
 public DamageService damage() { return damage; }
 public DragonEffectController effects() { return effects; }
 public ManualFireAttackController manual() { return manual; }
 public DragonInteractionListener interactions() { return interactions; }
 public DragonControlGUI gui() { return gui; }
 public DragonStorageGUI storageGui() { return storageGui; }
}
