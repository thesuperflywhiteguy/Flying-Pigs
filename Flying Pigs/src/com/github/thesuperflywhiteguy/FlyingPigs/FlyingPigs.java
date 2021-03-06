package com.github.thesuperflywhiteguy.FlyingPigs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlyingPigs extends JavaPlugin {

    private final HashMap<Player, PigData> pigMap = new HashMap<Player, PigData>();
    private ArrayList<String> offlinePlayers = new ArrayList<String>();
    private Boolean skipNextFiredEvent = false;
    @Override
    public void onEnable() {
        Server s = getServer();
        //TODO may need to check a file to load state
        s.getScheduler().scheduleSyncRepeatingTask(this, new Controller(), 1, 1);
        s.getScheduler().scheduleSyncRepeatingTask(this, new pigHealthRegen(), 1, 20);
        s.getPluginManager().registerEvents(new EventListeners(), this);
    }

    @Override
    public void onDisable() {
        //TODO may want to create a file to save state
    }
    
    private class pigHealthRegen implements Runnable {
        public void run() {
            for (Entry<Player, PigData> e : pigMap.entrySet()) {
                PigData pigData = e.getValue();
                if (pigData.pig != null && pigData.pig.getHealth() != pigData.pig.getMaxHealth()){
                    double newHealth = pigData.pig.getHealth() + pigData.pig.getMaxHealth() / 10;
                    if (newHealth < pigData.pig.getMaxHealth()){
                        pigData.pig.setHealth(newHealth);
                    }
                    else{
                        pigData.pig.setHealth(pigData.pig.getMaxHealth());
                    }
                }
            }
        }
    }
    
    private class Controller implements Runnable {
        public void run() {
            skipNextFiredEvent = false;
            for (Entry<Player, PigData> e : pigMap.entrySet()) {
                Player player = e.getKey();
                PigData pigData = e.getValue();
                if (!player.isOnline()) {
                    pigData.speed = 0;
                    pigData.pig = null;
                    continue;
                } 
                else if (pigData.pig == null) {
                    Pig newpig = (Pig) player.getWorld().spawnEntity(player.getLocation(), EntityType.PIG);
                    newpig.setSaddle(true);
                    pigData.pig = newpig;
                    pigData.pig.setPassenger(player); 
                    player.sendMessage(ChatColor.BLUE   + "-------------------Wee Haw!!-------------------");
                    player.sendMessage(ChatColor.GREEN  + "----------Scroll forward to fly faster---------");
                    player.sendMessage(ChatColor.RED    + "------------Scroll back to slow down-----------");
                    player.sendMessage(ChatColor.BLUE   + "-----Dirrection goes where you are looking-----");
                    player.sendMessage(ChatColor.YELLOW + "----right click to shoot with item in hand!!---"); 
                    player.sendMessage(ChatColor.YELLOW + "-------Infinate Arrows | Snowballs | TNT!!-----"); 
                    pigData.speed = 0;
                    getServer().getLogger().info("Setting Passanger!");
                }
                else if (pigData.pig.isEmpty()){
                    pigMap.remove(player);
                    return;
                }

                // rework the controls of the pig here
                Location playerLocation = player.getEyeLocation();
                Location pigLocation = pigData.pig.getLocation();
                pigLocation.setPitch(playerLocation.getPitch());
                pigLocation.setYaw(playerLocation.getYaw());
                pigData.pig.setVelocity(pigLocation.getDirection().multiply(pigData.speed));
            }
        }
    }
    
    public class PigData {
        public PigData(Pig p, double s){
            pig = p;
            speed = s;
            tickTimer = 0;
        }
        public Pig pig;
        public double speed;
        public int tickTimer;
    }
    
    public class EventListeners implements Listener {

        @EventHandler
        public void hoppedOnAPig(PlayerInteractEntityEvent e) {
            Entity entity = e.getRightClicked();
            if (entity.getType().equals(EntityType.PIG)) {
                Pig pig = (Pig) entity;
                Player player = e.getPlayer();
                boolean holdingLeash = player.getItemInHand().getType() == Material.LEASH;
                if (pig.isEmpty() && pig.hasSaddle() && player.getItemInHand().getType() != Material.CARROT && (!holdingLeash || pig.isLeashed()) && (!pig.isLeashed() || pig.getLeashHolder() != player)) {
                    pigMap.put(player, new PigData(pig, 0));
                    player.sendMessage(ChatColor.BLUE   + "-------------------Wee Haw!!-------------------");
                    player.sendMessage(ChatColor.GREEN  + "----------Scroll forward to fly faster---------");
                    player.sendMessage(ChatColor.RED    + "------------Scroll back to slow down-----------");
                    player.sendMessage(ChatColor.BLUE   + "-----Dirrection goes where you are looking-----");
                    player.sendMessage(ChatColor.YELLOW + "----right click to shoot with item in hand!!---"); 
                    player.sendMessage(ChatColor.YELLOW + "-------Infinate Arrows | Snowballs | TNT!!-----"); 
                    skipNextFiredEvent = true;
                }
            }
        }

        @EventHandler 
        public void onPlayerInteract(PlayerInteractEvent event) {
            final Action action = event.getAction();
            //only fire for players riding on pigs
            Player p = event.getPlayer();
            if (!skipNextFiredEvent && pigMap.containsKey(p)){
                //only fire on left click air events
                if (action == Action.RIGHT_CLICK_AIR && (p.getItemInHand().getType() == Material.ARROW || 
                                                        p.getItemInHand().getType() == Material.SNOW_BALL || 
                                                        p.getItemInHand().getType() == Material.TNT) ){
                    Projectile projectile = null;
                    Location eyeLocation = p.getEyeLocation();
                    if (p.getItemInHand().getType() == Material.ARROW){
                        projectile = p.getWorld().spawn(eyeLocation.add(eyeLocation.getDirection().multiply(3)), Arrow.class);
                        p.playSound(eyeLocation, Sound.SHOOT_ARROW, 1, 0);
                    }
                    else if (p.getItemInHand().getType() == Material.SNOW_BALL){
                        projectile = p.getWorld().spawn(eyeLocation.add(eyeLocation.getDirection().multiply(3)), Snowball.class);
                        p.playSound(eyeLocation, Sound.SHOOT_ARROW, 1, 1);
                    }
                    else {
                        Pig pig = pigMap.get(p).pig;
                        if (pig.getHealth() == pig.getMaxHealth()){
                            projectile = p.getWorld().spawn(eyeLocation.add(eyeLocation.getDirection().multiply(3)), Fireball.class);
                            p.playSound(eyeLocation, Sound.GHAST_FIREBALL, 1, 0);
                            pig.setHealth(pig.getMaxHealth() / 5);
                        }
                        else{
                            return;
                        }
                    }
                    projectile.setShooter(p);
                    projectile.setVelocity(p.getLocation().getDirection().multiply(2)); 
                    event.setCancelled(true);
                }
            }
            else{
                skipNextFiredEvent = false;
            }
        }
        
        @EventHandler (priority = EventPriority.HIGH)
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if (pigMap.containsKey(player)){
                Pig hazardPig = pigMap.get(player).pig;
                hazardPig.eject();
                hazardPig.remove();
                offlinePlayers.add(player.getName());
            }
        }

        @EventHandler (priority = EventPriority.HIGH)
        public void onPlayerLogin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            player.sendMessage(ChatColor.AQUA + "----Flying Pigs V1.71 Loaded----"); 
            player.setSleepingIgnored(true);
            getServer().getLogger().info("playerLogin event");
            if (offlinePlayers.contains(player.getName())){
                PigData pigData = new PigData(null,0);
                pigMap.put(player, pigData);
                offlinePlayers.remove(player.getName());
            }
        }
        
        @EventHandler (priority = EventPriority.HIGH)
        public void onDamageEntity(EntityDamageEvent event) {
            Entity entity = event.getEntity();
            if (event.getCause() == DamageCause.FALL || event.getCause() == DamageCause.SUFFOCATION){
                if (!entity.isEmpty()){
                    Entity passenger = entity.getPassenger();
                    if (passenger instanceof Player){
                        Player player = (Player)passenger;
                        if (pigMap.containsKey(player)){
                            event.setCancelled(true);
                        }
                    }
                }
                else if (entity instanceof Player){
                    Player player = (Player)entity;
                    if (pigMap.containsKey(player)){
                        event.setCancelled(true);
                    }
                }
            }
            else if (entity instanceof Pig && !entity.isEmpty()){
                Entity passenger = entity.getPassenger();
                if (passenger instanceof Player && pigMap.containsKey(passenger)){
                    event.setCancelled(true);
                }
            }
        }
        

        @EventHandler (priority = EventPriority.HIGH)
        public void pigExit(VehicleExitEvent event) {
            if (event.getExited() instanceof Player){
                Player player = (Player)event.getExited();
                if(player.isOnline())
                {
                    if (pigMap.containsKey(player)){
                        pigMap.remove(player);
                    }
                }
            }
        }
        
        @EventHandler (priority = EventPriority.HIGH)
        public void onItemScroll(PlayerItemHeldEvent event) {
            if (pigMap.containsKey(event.getPlayer())){
                int currentSpot = event.getPreviousSlot();
                int nextSpot = event.getNewSlot();
                PigData pigData = pigMap.get(event.getPlayer());
                double speed = pigData.speed;
                if (currentSpot == 0){
                   if (nextSpot != 1){
                       speed += 0.1;
                   }
                   else{
                       //speed up
                       speed -= 0.1;
                   }
                }
                else if (currentSpot == 8){
                    if (nextSpot != 0){
                        //slow down
                        speed += 0.1;
                    }
                    else{
                        //speed up
                        speed -= 0.1;
                    }
                }
                else if (currentSpot < nextSpot){
                    //speed up
                    speed -= 0.1;
                }
                else{
                    //slow down
                    speed += 0.1;
                }
                if (speed < 0){
                    speed = 0;
                }
                else if (speed > 1){
                    speed = 1;
                }
                pigData.speed = speed;
            }
        }
        
        @EventHandler 
        public void pigBomb(ExplosionPrimeEvent event) {
            if (event.getEntity() instanceof Fireball){
                Entity player = ((Fireball)event.getEntity()).getShooter();
                if (player instanceof Player && pigMap.containsKey((Player)player)){
                    Entity entity = event.getEntity();
                    TNTPrimed tnt = player.getWorld().spawn(entity.getLocation(), TNTPrimed.class);
                    tnt.setFuseTicks(1);
                    tnt.setIsIncendiary(true);
                    tnt.setYield(5);
                    event.setCancelled(true);
                    entity.remove();
                }
            }
        }
        
    }
}
