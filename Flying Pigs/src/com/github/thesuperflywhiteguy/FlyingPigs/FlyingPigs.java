package com.github.thesuperflywhiteguy.FlyingPigs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlyingPigs extends JavaPlugin {

    private final HashMap<Player, PigData> pigMap = new HashMap<Player, PigData>();
    private ArrayList<String> offlinePlayers = new ArrayList<String>();
    @Override
    public void onEnable() {
        Server s = getServer();
        //TODO may need to check a file to load state
        s.getScheduler().scheduleSyncRepeatingTask(this, new Controller(), 1, 1);
        s.getPluginManager().registerEvents(new EventListeners(), this);
    }

    @Override
    public void onDisable() {
        //TODO may want to create a file to save state
    }

    private class Controller implements Runnable {
        public void run() {
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
                    player.sendMessage(ChatColor.BLUE   + "------------Wee Haw!!-----------");
                    player.sendMessage(ChatColor.GREEN  + "--Scroll forward to fly faster--");
                    player.sendMessage(ChatColor.RED    + "----Scroll back to slow down----");
                    player.sendMessage(ChatColor.YELLOW + "------left click to fire!!------"); 
                    pigData.speed = 0;
                    getServer().getLogger().info("Setting Passanger!");
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
                if (pig.isEmpty() && pig.hasSaddle() && player.getItemInHand().getType() != Material.LEASH) {
                    pigMap.put(player, new PigData(pig, 0));
                    player.sendMessage(ChatColor.BLUE   + "------------Wee Haw!!-----------");
                    player.sendMessage(ChatColor.GREEN  + "--Scroll forward to fly faster--");
                    player.sendMessage(ChatColor.RED    + "----Scroll back to slow down----");
                    player.sendMessage(ChatColor.YELLOW + "------left click to fire!!------"); 
                }
            }
        }


        @EventHandler (priority = EventPriority.HIGH)
        public void onPlayerInteract(PlayerInteractEvent event) {
            final Action action = event.getAction();
            if (pigMap.containsKey(event.getPlayer())){
                if (action == Action.LEFT_CLICK_AIR){
                    Player p = event.getPlayer();
                    Location eyeLocation = p.getEyeLocation();
                    Arrow arrow = p.getWorld().spawn(eyeLocation.add(eyeLocation.getDirection().multiply(3)), Arrow.class);
                    arrow.setShooter(p);
                    arrow.setVelocity(p.getLocation().getDirection().multiply(2)); 
                    event.setCancelled(true);
                }
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
        public void onPlayerLogin(PlayerLoginEvent event) {
            Player player = event.getPlayer();
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
            if (event.getCause() == DamageCause.FALL){
                Entity entity = event.getEntity();
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
    }
}
