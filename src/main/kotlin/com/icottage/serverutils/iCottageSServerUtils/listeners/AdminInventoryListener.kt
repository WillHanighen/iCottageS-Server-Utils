package com.icottage.serverutils.iCottageSServerUtils.commands

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Listener for admin inventory interactions
 */
class AdminInventoryListener(
    private val plugin: JavaPlugin,
    private val adminCommands: AdminCommands
) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if this player has an open admin inventory
        if (!adminCommands.hasOpenInventory(player.uniqueId)) {
            return
        }
        
        val inventoryType = adminCommands.getOpenInventoryType(player.uniqueId) ?: return
        
        if (inventoryType.startsWith("invsee:")) {
            // Handle invsee inventory click
            handleInvseeClick(event, player, inventoryType)
        } else if (inventoryType.startsWith("echest:")) {
            // Handle enderchest inventory click
            handleEchestClick(event, player, inventoryType)
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // Check if this player has an open admin inventory
        if (!adminCommands.hasOpenInventory(player.uniqueId)) {
            return
        }
        
        val inventoryType = adminCommands.getOpenInventoryType(player.uniqueId) ?: return
        
        // For simplicity, cancel all drags in admin inventories
        // This prevents complex edge cases with item duplication
        event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        // Check if this player has an open admin inventory
        if (!adminCommands.hasOpenInventory(player.uniqueId)) {
            return
        }
        
        val inventoryType = adminCommands.getOpenInventoryType(player.uniqueId) ?: return
        
        // Save changes if needed
        if (inventoryType.startsWith("invsee:")) {
            saveInvseeChanges(player, inventoryType, event.inventory)
        } else if (inventoryType.startsWith("echest:")) {
            saveEchestChanges(player, inventoryType, event.inventory)
        }
        
        // Remove tracking
        adminCommands.removeOpenInventory(player.uniqueId)
    }
    
    /**
     * Handle clicks in an invsee inventory
     */
    private fun handleInvseeClick(event: InventoryClickEvent, player: Player, inventoryType: String) {
        // Get the target player UUID
        val targetUuidString = inventoryType.substring("invsee:".length)
        val targetUuid = try {
            UUID.fromString(targetUuidString)
        } catch (e: Exception) {
            event.isCancelled = true
            return
        }
        
        // Get the target player
        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            event.isCancelled = true
            return
        }
        
        // Check if player has permission to modify inventory
        if (!player.hasPermission("serverutils.admin.invsee.modify")) {
            event.isCancelled = true
            return
        }
        
        // Handle clicks in the inventory
        val slot = event.rawSlot
        
        // If clicking in the top inventory (the target's inventory view)
        if (slot < event.view.topInventory.size) {
            // Map the slot in the custom inventory to the target's inventory
            val targetSlot = mapViewSlotToPlayerSlot(slot)
            
            if (targetSlot == -1) {
                // Clicking on a filler item or unmapped slot
                event.isCancelled = true
                return
            }
            
            // Allow the click and update the target's inventory directly
            // This is handled in the saveInvseeChanges method when the inventory is closed
        }
    }
    
    /**
     * Handle clicks in an enderchest inventory
     */
    private fun handleEchestClick(event: InventoryClickEvent, player: Player, inventoryType: String) {
        // Get the target player UUID
        val targetUuidString = inventoryType.substring("echest:".length)
        val targetUuid = try {
            UUID.fromString(targetUuidString)
        } catch (e: Exception) {
            event.isCancelled = true
            return
        }
        
        // Get the target player
        val target = Bukkit.getPlayer(targetUuid)
        if (target == null || !target.isOnline) {
            event.isCancelled = true
            return
        }
        
        // If viewing own enderchest, allow modifications
        if (player.uniqueId == targetUuid) {
            return
        }
        
        // Check if player has permission to modify other players' enderchests
        if (!player.hasPermission("serverutils.admin.echest.modify")) {
            event.isCancelled = true
            return
        }
        
        // Allow the click and update the target's enderchest directly
        // This is handled in the saveEchestChanges method when the inventory is closed
    }
    
    /**
     * Save changes to a player's inventory when closing invsee
     */
    private fun saveInvseeChanges(player: Player, inventoryType: String, inventory: org.bukkit.inventory.Inventory) {
        // Get the target player UUID
        val targetUuidString = inventoryType.substring("invsee:".length)
        val targetUuid = try {
            UUID.fromString(targetUuidString)
        } catch (e: Exception) {
            return
        }
        
        // Get the target player
        val target = Bukkit.getPlayer(targetUuid) ?: return
        
        // Check if player has permission to modify inventory
        if (!player.hasPermission("serverutils.admin.invsee.modify")) {
            return
        }
        
        // Update the target's inventory
        // Main inventory (9-35)
        for (i in 0 until 27) {
            target.inventory.setItem(i + 9, inventory.getItem(i + 9))
        }
        
        // Hotbar (0-8)
        for (i in 0 until 9) {
            target.inventory.setItem(i, inventory.getItem(i + 36))
        }
        
        // Armor slots (36-39)
        target.inventory.helmet = inventory.getItem(0)
        target.inventory.chestplate = inventory.getItem(1)
        target.inventory.leggings = inventory.getItem(2)
        target.inventory.boots = inventory.getItem(3)
        
        // Offhand (40)
        target.inventory.setItemInOffHand(inventory.getItem(8))
        
        // Update the target's inventory
        target.updateInventory()
    }
    
    /**
     * Save changes to a player's enderchest when closing echest
     */
    private fun saveEchestChanges(player: Player, inventoryType: String, inventory: org.bukkit.inventory.Inventory) {
        // Get the target player UUID
        val targetUuidString = inventoryType.substring("echest:".length)
        val targetUuid = try {
            UUID.fromString(targetUuidString)
        } catch (e: Exception) {
            return
        }
        
        // Get the target player
        val target = Bukkit.getPlayer(targetUuid) ?: return
        
        // If viewing own enderchest, allow modifications
        if (player.uniqueId == targetUuid) {
            // Update the player's enderchest
            for (i in 0 until target.enderChest.size) {
                target.enderChest.setItem(i, inventory.getItem(i))
            }
            return
        }
        
        // Check if player has permission to modify other players' enderchests
        if (!player.hasPermission("serverutils.admin.echest.modify")) {
            return
        }
        
        // Update the target's enderchest
        for (i in 0 until target.enderChest.size) {
            target.enderChest.setItem(i, inventory.getItem(i))
        }
    }
    
    /**
     * Map a slot in the custom inventory view to a slot in the player's inventory
     */
    private fun mapViewSlotToPlayerSlot(viewSlot: Int): Int {
        return when {
            // Armor slots
            viewSlot == 0 -> 39 // Helmet
            viewSlot == 1 -> 38 // Chestplate
            viewSlot == 2 -> 37 // Leggings
            viewSlot == 3 -> 36 // Boots
            
            // Offhand
            viewSlot == 8 -> 40
            
            // Main inventory (9-35)
            viewSlot in 9..35 -> viewSlot
            
            // Hotbar (0-8)
            viewSlot in 36..44 -> viewSlot - 36
            
            // Unmapped slot
            else -> -1
        }
    }
}
