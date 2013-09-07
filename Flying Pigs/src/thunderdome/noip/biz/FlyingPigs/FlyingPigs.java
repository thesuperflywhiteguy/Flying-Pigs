package thunderdome.noip.biz.FlyingPigs;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.Server;
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
                if (pigData.pig == null){
                    if (player.isOnline()){
                        pigData.pig = (Pig) player.getWorld().spawnEntity(player.getLocation(), EntityType.PIG);
                        pigData.pig.setSaddle(true);
                        pigData.pig.setPassenger(player);
                        pigData.speed = 0;
                        //e.setValue(pigData);
                        getServer().getLogger().info("re-adding pig");
                    }
                    else{
                        continue;
                    }
                }
                else if (!player.isOnline()) {
                    //remove the pig because player is disco
                    pigData.pig.remove();
                    pigData.pig = null;
                    //pigMap.put(player, pigData);
                    continue;
                } 
                else if (pigData.pig.isEmpty()) {
                    getServer().getLogger().info("Removing abandond pig!");
                    //remove the entity from the pigMap because the rider hopped off
                    pigMap.remove(player);
                    continue;
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
              if (action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR) {
            
                  Player player = event.getPlayer();
                  if (pigMap.containsKey(player)){
                      PigData pigData = pigMap.get(player);
                      double speed = pigData.speed;
                      if (action == Action.LEFT_CLICK_AIR){
                          speed += 0.25;
                      }
                      else{
                          speed -= 0.25;
                      }
                      if (speed > 1) speed = 1;
                      if (speed < 0 ) speed = 0;
                      pigData.speed = speed;
                  }
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
    }
}