package com.icottage.serverutils.iCottageSServerUtils.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.*
import kotlin.math.sin

/**
 * General utility commands available to all players
 */
class GeneralCommands(private val plugin: JavaPlugin) : CommandExecutor, TabCompleter, Listener {

    // Map to track players with RGB hats
    private val rgbHatPlayers = HashSet<UUID>()
    
    // Map of task IDs for RGB hat animation
    private val rgbHatTasks = HashMap<UUID, Int>()
    
    // Map to store original helmets
    private val originalHelmets = HashMap<UUID, ItemStack?>()
    
    // List of colored glass materials
    private val glassMaterials = listOf(
        Material.ORANGE_STAINED_GLASS,
        Material.MAGENTA_STAINED_GLASS,
        Material.LIGHT_BLUE_STAINED_GLASS,
        Material.YELLOW_STAINED_GLASS,
        Material.LIME_STAINED_GLASS,
        Material.PINK_STAINED_GLASS,
        Material.GRAY_STAINED_GLASS,
        Material.LIGHT_GRAY_STAINED_GLASS,
        Material.CYAN_STAINED_GLASS,
        Material.PURPLE_STAINED_GLASS,
        Material.BLUE_STAINED_GLASS,
        Material.BROWN_STAINED_GLASS,
        Material.GREEN_STAINED_GLASS,
        Material.RED_STAINED_GLASS,
        Material.BLACK_STAINED_GLASS
    )
    
    /**
     * Register all general commands and events
     */
    fun registerCommands() {
        val commands = listOf("hat", "rgbhat")
        
        for (commandName in commands) {
            val command = plugin.getCommand(commandName)
            if (command != null) {
                command.setExecutor(this)
                command.tabCompleter = this
                plugin.logger.info("Registered general command: $commandName")
            } else {
                plugin.logger.warning("Failed to register general command: $commandName - not found in plugin.yml")
            }
        }
        
        // Register events for RGB hat functionality
        plugin.server.pluginManager.registerEvents(this, plugin)
    }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players").color(NamedTextColor.RED))
            return true
        }
        
        when (label.lowercase()) {
            "hat" -> return handleHatCommand(sender, args)
            "rgbhat" -> return handleRgbHatCommand(sender, args)
        }
        
        return false
    }
    
    /**
     * Handle the hat command
     */
    private fun handleHatCommand(player: Player, args: Array<out String>): Boolean {
        if (!player.hasPermission("serverutils.hat")) {
            player.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        // Get the item in the player's main hand
        val handItem = player.inventory.itemInMainHand
        
        if (handItem.type == Material.AIR) {
            player.sendMessage(Component.text("You must be holding an item to use this command").color(NamedTextColor.RED))
            return true
        }
        
        // Get the current helmet
        val currentHelmet = player.inventory.helmet
        
        // Set the hand item as the helmet
        player.inventory.helmet = handItem.clone()
        
        // Replace the hand item with the current helmet
        player.inventory.setItemInMainHand(currentHelmet)
        
        player.sendMessage(Component.text("You are now wearing the item as a hat!").color(NamedTextColor.GREEN))
        return true
    }
    
    /**
     * Handle the RGB hat command
     */
    private fun handleRgbHatCommand(player: Player, args: Array<out String>): Boolean {
        if (!player.hasPermission("serverutils.rgbhat")) {
            player.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        val playerUuid = player.uniqueId
        
        // Check if the player already has an RGB hat
        if (rgbHatPlayers.contains(playerUuid)) {
            // Remove the RGB hat
            stopRgbHat(player)
            player.sendMessage(Component.text("RGB hat disabled").color(NamedTextColor.RED))
            return true
        }
        
        // Start the RGB hat
        startRgbHat(player)
        player.sendMessage(Component.text("RGB hat enabled! Your glass hat will now cycle through colors and your old helmet will be restored when disabled").color(NamedTextColor.GREEN))
        return true
    }
    
    /**
     * Start the RGB hat animation for a player
     */
    private fun startRgbHat(player: Player) {
        val playerUuid = player.uniqueId
        
        // Store the player's current helmet before replacing it
        originalHelmets[playerUuid] = player.inventory.helmet?.clone()
        
        // Add the player to the RGB hat players set
        rgbHatPlayers.add(playerUuid)
        
        // Create a task to update the hat color
        val taskId = object : BukkitRunnable() {
            private var tick = 0
            
            override fun run() {
                // Check if the player is still online
                val onlinePlayer = Bukkit.getPlayer(playerUuid)
                if (onlinePlayer == null || !onlinePlayer.isOnline) {
                    // If player went offline, make sure we clean up
                    rgbHatPlayers.remove(playerUuid)
                    rgbHatTasks.remove(playerUuid)
                    originalHelmets.remove(playerUuid)
                    this.cancel()
                    return
                }
                
                // Update the hat color
                updateRgbHat(onlinePlayer, tick)
                tick += 5
            }
        }.runTaskTimer(plugin, 0L, 5L).taskId
        
        // Store the task ID
        rgbHatTasks[playerUuid] = taskId
    }
    
    /**
     * Stop the RGB hat animation for a player
     */
    private fun stopRgbHat(player: Player) {
        val playerUuid = player.uniqueId
        
        // Remove the player from the RGB hat players set
        rgbHatPlayers.remove(playerUuid)
        
        // Cancel the task
        val taskId = rgbHatTasks.remove(playerUuid)
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId)
        }
        
        // Always restore the original helmet regardless of current helmet
        player.inventory.helmet = originalHelmets[playerUuid]
        originalHelmets.remove(playerUuid)
    }
    
    /**
     * Update the RGB hat color for a player
     */
    private fun updateRgbHat(player: Player, tick: Int) {
        // Calculate which glass type to use based on the tick
        val glassIndex = (tick / 10) % glassMaterials.size
        val glassType = glassMaterials[glassIndex]
        
        // Create a new glass block
        val glassItem = ItemStack(glassType)
        val meta = glassItem.itemMeta
        
        // Calculate a smooth RGB color for the name using sine waves
        val red = (sin(tick * 0.05) * 127 + 128).toInt()
        val green = (sin(tick * 0.05 + 2) * 127 + 128).toInt()
        val blue = (sin(tick * 0.05 + 4) * 127 + 128).toInt()
        val color = TextColor.color(red, green, blue)
        
        // Set custom name using Adventure API
        meta.displayName(Component.text("RGB Hat").color(color))
        
        // Add lore to indicate this is an RGB hat
        val lore = ArrayList<Component>()
        lore.add(Component.text("This hat cycles through colors").color(NamedTextColor.GRAY))
        lore.add(Component.text("Use /rgbhat to toggle").color(NamedTextColor.GRAY))
        meta.lore(lore)
        
        // Apply the metadata
        glassItem.itemMeta = meta
        
        // Set the player's helmet to the new glass block
        player.inventory.helmet = glassItem
    }
    
    /**
     * Check if a material is a glass block
     */
    private fun isGlassBlock(material: Material): Boolean {
        return material.name.contains("GLASS")
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        return emptyList()
    }
    
    /**
     * Prevent players from removing RGB hats through inventory clicks
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val playerUuid = player.uniqueId
        
        // If player has RGB hat and they're trying to modify helmet slot
        if (rgbHatPlayers.contains(playerUuid)) {
            if ((event.slotType == InventoryType.SlotType.ARMOR && event.rawSlot == 5) || 
                (event.clickedInventory == player.inventory && event.slot == 39)) {
                // Cancel the event to prevent removing the hat
                event.isCancelled = true
                player.sendMessage(Component.text("You cannot remove your RGB hat while it's active. Use /rgbhat to disable it first.").color(NamedTextColor.RED))
            }
        }
    }
    
    /**
     * Clean up RGB hat when player dies
     */
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val playerUuid = player.uniqueId
        
        if (rgbHatPlayers.contains(playerUuid)) {
            // Stop the RGB hat effect
            stopRgbHat(player)
            
            // Prevent the RGB hat from dropping
            val drops = event.drops
            drops.removeIf { item -> item.type.name.contains("GLASS") && item.itemMeta?.hasDisplayName() == true && item.itemMeta?.displayName().toString().contains("RGB Hat") }
        }
    }
    
    /**
     * Clean up RGB hat when player quits
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerUuid = player.uniqueId
        
        if (rgbHatPlayers.contains(playerUuid)) {
            // Stop the RGB hat effect and restore original helmet
            stopRgbHat(player)
        }
    }
}
