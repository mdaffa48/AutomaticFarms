package me.aglerr.automaticfarms.listeners;

import com.cryptomorin.xseries.XMaterial;
import me.aglerr.automaticfarms.AutomaticFarms;
import me.aglerr.automaticfarms.enums.DataType;
import me.aglerr.automaticfarms.managers.CropsManager;
import me.aglerr.automaticfarms.managers.DataManager;
import me.aglerr.automaticfarms.managers.GrowingManager;
import me.aglerr.automaticfarms.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class BreakNewVersion implements Listener {

    private final AutomaticFarms plugin;
    public BreakNewVersion(final AutomaticFarms plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBreak(BlockBreakEvent event){

        FileConfiguration config = plugin.getConfig();

        Player player = event.getPlayer();
        Block block = event.getBlock();

        CropsManager cropsManager = plugin.getCropsManager();
        GrowingManager growingManager = plugin.getGrowingManager();

        if(growingManager.isBlockGrowing(block)){
            event.setCancelled(true);
            return;
        }

        if(!cropsManager.isMaterialExist(block.getType())) return;
        if(!(block.getBlockData() instanceof Ageable)) return;

        if(config.getBoolean("checks.onlyFullyGrown")){
            if(!growingManager.isFullyGrown(block)) return;
        }

        event.setCancelled(true);

        for(ItemStack stack : event.getBlock().getDrops(player.getItemInHand())){
            block.getWorld().dropItemNaturally(block.getLocation(), stack);
        }

        Ageable ageable = (Ageable) block.getBlockData();
        Location location = block.getLocation().clone().add(0.5, 1, 0.5);

        this.handleCropGrowing(player, ageable, block, location);

    }

    @EventHandler
    public void playerBreakCactus(BlockBreakEvent event){

        Block block = event.getBlock();
        Location location = block.getLocation().clone().add(0.5, 1, 0.5);

        if(block.getType() == XMaterial.CACTUS.parseMaterial()){
            if(block.getRelative(BlockFace.DOWN).getType() == XMaterial.CACTUS.parseMaterial()) return;

            this.handleCactusGrowing(event.getPlayer(), block.getLocation(), location);

        }

    }

    private void handleCactusGrowing(Player player, Location blockLocation, Location particleLocation){

        CropsManager cropsManager = plugin.getCropsManager();
        DataManager dataManager = plugin.getDataManager();

        dataManager.incrementOrAssignData(player.getUniqueId(), DataType.CACTUS);

        int waitTime = cropsManager.getGrowingWaitTime(XMaterial.CACTUS.parseMaterial());

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> new BukkitRunnable(){
            @Override
            public void run(){

                blockLocation.getBlock().setType(XMaterial.CACTUS.parseMaterial());
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        Utils.summonParticle(particleLocation));

                this.cancel();

            }
        }.runTaskTimer(plugin, 0L, 20L), waitTime * 20);

    }

    private void handleCropGrowing(Player player, Ageable ageable, Block block, Location location){

        ageable.setAge(0);
        block.setBlockData(ageable);

        CropsManager cropsManager = plugin.getCropsManager();
        GrowingManager growingManager = plugin.getGrowingManager();
        DataManager dataManager = plugin.getDataManager();

        DataType type = null;
        if(block.getType() == XMaterial.WHEAT.parseMaterial()) type = DataType.WHEAT;
        if(block.getType() == XMaterial.POTATOES.parseMaterial()) type = DataType.POTATO;
        if(block.getType() == XMaterial.CARROTS.parseMaterial()) type = DataType.CARROT;
        if(block.getType() == XMaterial.NETHER_WART.parseMaterial()) type = DataType.NETHER_WART;

        dataManager.incrementOrAssignData(player.getUniqueId(), type);

        int waitTime = cropsManager.getGrowingWaitTime(block.getType());

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> new BukkitRunnable(){
            @Override
            public void run(){

                if(!(block.getBlockData() instanceof Ageable)){
                    this.cancel();
                    return;
                }

                Ageable finalAgeable = (Ageable) block.getBlockData();
                int age = finalAgeable.getAge();

                if(age >= finalAgeable.getMaximumAge()){
                    growingManager.removeGrowingBlock(block);
                    this.cancel();
                    return;
                }

                finalAgeable.setAge(age + 1);
                block.setBlockData(finalAgeable);

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        Utils.summonParticle(location));

            }
        }.runTaskTimer(plugin, 0L, 20L), waitTime * 20);

    }

}
