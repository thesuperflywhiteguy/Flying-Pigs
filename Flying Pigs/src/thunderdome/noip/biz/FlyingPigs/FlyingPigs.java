package thunderdome.noip.biz.FlyingPigs;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlyingPigs extends JavaPlugin {

    private final HashMap<Player, PigData> pigMap = new HashMap<Player, PigData>();

    @Override
    public void onEnable() {
        Server s = getServer();
        //TODO may need to check a file to load state
        s.getScheduler().scheduleSyncRepeatingTask(this, new Controller(), 1, 1);
        s.getPluginManager().registerEvents(new EventListeners(), this);
        s.getLogger().info("Daves Custom plugin enabled!");
    }

    @Override
    public void onDisable() {
        Server s = getServer();
        s.getLogger().info("Daves Custom plugin disabled!");
        //TODO may want to create a file to save state
    }

    private class Controller implements Runnable {
        public void run() {
            for (Entry<Player, PigData> e : pigMap.entrySet()) {
                Player player = e.getKey();
                PigData pigData = e.getValue();
//                if (pigData.pig == null){
//                    if (player.isOnline()){
//                        if (player.isInsideVehicle()){
//                            pigData.pig = (Pig)player.getVehicle();
//                            pigData.speed = 0;
//                            getServer().getLogger().info("pig is back");
//                        }
//                        //pigData.pig = (Pig) player.getWorld().spawnEntity(player.getLocation().subtract(new Vector(0,0,-1)), EntityType.PIG);
//                        //pigData.pig.setSaddle(true);
//                       // pigData.pig.setPassenger(player);
//                        //pigData.speed = 0;
//                        //e.setValue(pigData);
//                        getServer().getLogger().info("re-adding pig");
//                        continue;
//                    }
//                    else{
                //        continue;
                //    }
                //}
                if (pigData.pig == null && player.isOnline()){
                  pigData.pig = (Pig) player.getWorld().spawnEntity(player.getLocation(), EntityType.PIG);
                  pigData.pig.setSaddle(true);
                  pigData.pig.setPassenger(player);
                  pigData.speed = 0;
                  getServer().getLogger().info("re-adding pig");
                }
                else if (!player.isOnline()) {
                    //remove the pig because player is disco
                    //pigData.pig.remove();
                    //pigData.pig = null;
                    pigData.speed = 0;
                    pigData.pig = null;
                    //pigMap.put(player, pigData);
                    continue;
                } 
//                else if (pigData.pig.isEmpty() || pigData.pig.isDead()) {
//                    getServer().getLogger().info("Removing abandond pig!");
//                    //remove the entity from the pigMap because the rider hopped off
//                    pigMap.remove(player);
//                    return;
//                }

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
        }
        public Pig pig;
        public double speed;
    }
    public class EventListeners implements Listener {

        @EventHandler
        public void hoppedOnAPig(PlayerInteractEntityEvent e) {
            Entity entity = e.getRightClicked();
            if (entity.getType().equals(EntityType.PIG)) {
                Pig pig = (Pig) entity;
                if (pig.isEmpty() && pig.hasSaddle()) {
                    pigMap.put(e.getPlayer(), new PigData(pig, 0));
                }
            }
        }


        @EventHandler (priority = EventPriority.HIGH)
        public void onPlayerInteract(PlayerInteractEvent event) {
            final Action action = event.getAction();
            if (pigMap.containsKey(event.getPlayer())){
                if (action == Action.LEFT_CLICK_BLOCK || action == Action.LEFT_CLICK_AIR){
                    Player p = event.getPlayer();
                    Location eyeLocation = p.getEyeLocation();
                    Snowball snowball = p.getWorld().spawn(eyeLocation.add(eyeLocation.getDirection().multiply(3)), Snowball.class);
                    snowball.setShooter(p);
                    snowball.setVelocity(p.getLocation().getDirection().multiply(2)); 
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
                        getServer().getLogger().info("Removing abandond pig!");
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
