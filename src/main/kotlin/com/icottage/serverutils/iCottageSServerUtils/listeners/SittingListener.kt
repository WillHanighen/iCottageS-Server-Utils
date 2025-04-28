package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.sitting.SittingManager
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.block.data.type.Slab

/**
 * Listener for player sitting interactions
 */
class SittingListener(
    private val plugin: JavaPlugin,
    private val sittingManager: SittingManager
) : Listener {
    
    /**
     * Handle player right-clicking on blocks to sit
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        // Only handle right-clicks on blocks
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.clickedBlock == null) {
            return
        }
        
        // Ignore off-hand interactions to prevent double-triggering
        if (event.hand != EquipmentSlot.HAND) {
            return
        }
        
        val player = event.player
        val block = event.clickedBlock!!
        
        // Check if the player is sneaking (shift-clicking)
        if (player.isSneaking) {
            return
        }
        
        // Check if the player is already sitting
        if (sittingManager.isPlayerSitting(player)) {
            return
        }
        
        // Check if the block is a valid sitting block
        if (isValidSittingBlock(block)) {
            // Check if the player is holding an item that could interact with the block
            if (player.inventory.itemInMainHand.type != Material.AIR) {
                // Many blocks have interactions when right-clicked with items
                // Only sit if the player's hand is empty to avoid interfering with normal gameplay
                return
            }
            
            // Make the player sit
            if (sittingManager.sitPlayer(player, block.location)) {
                // Cancel the event to prevent any other interactions
                event.setCancelled(true)
            }
        }
    }
    
    /**
     * Handle player movement to make them stand up
     */
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        // Only check for actual movement, not just looking around
        if (event.from.x == event.to.x && event.from.y == event.to.y && event.from.z == event.to.z) {
            return
        }
        
        // If the player is sitting and tries to move, make them stand up
        if (sittingManager.isPlayerSitting(event.player)) {
            sittingManager.standPlayer(event.player)
        }
    }
    
    /**
     * Handle player quitting to make them stand up
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (sittingManager.isPlayerSitting(event.player)) {
            sittingManager.standPlayer(event.player)
        }
    }
    
    /**
     * Handle player teleporting to make them stand up
     */
    @EventHandler
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (sittingManager.isPlayerSitting(event.player)) {
            sittingManager.standPlayer(event.player)
        }
    }
    
    /**
     * Handle player taking damage to make them stand up
     */
    @EventHandler
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            if (sittingManager.isPlayerSitting(player)) {
                sittingManager.standPlayer(player)
            }
        }
    }
    
    /**
     * Check if a block is valid for sitting
     */
    private fun isValidSittingBlock(block: Block): Boolean {
        val type = block.type
        
        // Check for stairs
        if (type.name.endsWith("_STAIRS")) {
            return true
        }
        
        // Check for carpets
        if (type.name.endsWith("_CARPET")) {
            return true
        }
        
        // Check for bottom slabs
        if (type.name.endsWith("_SLAB")) {
            val data = block.blockData
            if (data is Slab && data.type == Slab.Type.BOTTOM) {
                return true
            }
        }
        
        return false
    }
}
