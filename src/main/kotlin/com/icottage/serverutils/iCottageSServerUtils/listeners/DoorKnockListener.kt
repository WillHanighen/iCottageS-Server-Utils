package com.icottage.serverutils.iCottageSServerUtils.listeners

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listener for door knocking interactions
 */
class DoorKnockListener(private val plugin: JavaPlugin) : Listener {
    
    // No cooldown for door knocking
    
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only handle left-clicks on blocks
        if (event.action != Action.LEFT_CLICK_BLOCK || event.clickedBlock == null) {
            return
        }
        
        // Ignore off-hand interactions
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        
        val player = event.player
        val block = event.clickedBlock!!
        
        // Check if the block is a door or trapdoor
        if (isDoorOrTrapdoor(block.type)) {
            // Check if it's an iron door/trapdoor to play a different sound
            val isIron = isIronDoorOrTrapdoor(block.type)
            
            // Play door knock sound immediately with no cooldown
            playDoorKnockSound(player, block.location.x, block.location.y, block.location.z, isIron)
        }
    }
    
    /**
     * Check if a block is a door or trapdoor
     */
    private fun isDoorOrTrapdoor(material: Material): Boolean {
        return material.name.contains("DOOR") || material.name.contains("TRAPDOOR")
    }
    
    /**
     * Check if a block is an iron door or trapdoor
     */
    private fun isIronDoorOrTrapdoor(material: Material): Boolean {
        return material == Material.IRON_DOOR || 
               material == Material.IRON_TRAPDOOR
    }
    
    /**
     * Play the door knock sound
     * @param isIronDoor Whether the door is made of iron (for different sound)
     */
    private fun playDoorKnockSound(player: Player, x: Double, y: Double, z: Double, isIronDoor: Boolean = false) {
        // Use the zombie wooden door attack sound as it sounds like knocking
        val location = player.location.clone()
        location.x = x
        location.y = y
        location.z = z
        
        // Choose the appropriate sound based on door material
        val knockSound = if (isIronDoor) {
            Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR // Metallic sound for iron doors
        } else {
            Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR // Wood sound for wooden doors
        }
        
        // Play first knock sound
        player.world.playSound(
            location,
            knockSound,
            SoundCategory.BLOCKS,
            1.0f,  // Volume
            if (isIronDoor) 0.8f else 0.75f // Pitch adjustment based on door type
        )
        
        // Play a second sound for more depth to the knock
        player.world.playSound(
            location,
            knockSound,
            SoundCategory.BLOCKS,
            1.0f,
            if (isIronDoor) 0.6f else 0.5f
        )
        
        // Also play for nearby players (within 25 blocks)
        for (nearbyPlayer in player.world.players) {
            if (nearbyPlayer != player && nearbyPlayer.location.distance(location) <= 25) {
                nearbyPlayer.playSound(
                    location,
                    knockSound,
                    SoundCategory.BLOCKS,
                    1.0f,
                    if (isIronDoor) 0.8f else 0.75f
                )
                nearbyPlayer.playSound(
                    location,
                    knockSound,
                    SoundCategory.BLOCKS,
                    1.0f,
                    if (isIronDoor) 0.6f else 0.5f
                )
            }
        }
    }
}
