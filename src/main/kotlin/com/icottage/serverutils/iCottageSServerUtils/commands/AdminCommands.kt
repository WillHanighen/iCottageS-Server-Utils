package com.icottage.serverutils.iCottageSServerUtils.commands

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.StringUtil
import java.util.*

/**
 * Admin utility commands like heal, feed, invsee, echest, etc.
 */
class AdminCommands(private val plugin: JavaPlugin) : CommandExecutor, TabCompleter {

    // Map to track open inventory views
    private val openInventories = HashMap<UUID, String>()
    
    // Filler item for empty slots in inventory view
    private val FILLER_ITEM = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
        val meta = itemMeta
        meta.displayName(Component.text(" ").color(NamedTextColor.GRAY))
        itemMeta = meta
    }
    
    /**
     * Register all admin commands
     */
    fun registerCommands() {
        val commands = listOf("heal", "feed", "invsee", "echest")
        
        for (commandName in commands) {
            val command = plugin.getCommand(commandName)
            if (command != null) {
                command.setExecutor(this)
                command.tabCompleter = this
                plugin.logger.info("Registered admin command: $commandName")
            } else {
                plugin.logger.warning("Failed to register admin command: $commandName - not found in plugin.yml")
            }
        }
        
        // Register inventory events
        plugin.server.pluginManager.registerEvents(AdminInventoryListener(plugin, this), plugin)
    }
    
    /**
     * Check if a player has an open inventory view
     */
    fun hasOpenInventory(playerUuid: UUID): Boolean {
        return openInventories.containsKey(playerUuid)
    }
    
    /**
     * Get the type of open inventory view for a player
     */
    fun getOpenInventoryType(playerUuid: UUID): String? {
        return openInventories[playerUuid]
    }
    
    /**
     * Set the open inventory view for a player
     */
    fun setOpenInventory(playerUuid: UUID, type: String) {
        openInventories[playerUuid] = type
    }
    
    /**
     * Remove the open inventory view for a player
     */
    fun removeOpenInventory(playerUuid: UUID) {
        openInventories.remove(playerUuid)
    }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player && label != "heal" && label != "feed") {
            sender.sendMessage(Component.text("This command can only be used by players").color(NamedTextColor.RED))
            return true
        }
        
        when (label.lowercase()) {
            "heal" -> return handleHealCommand(sender, args)
            "feed" -> return handleFeedCommand(sender, args)
            "invsee" -> return handleInvseeCommand(sender as Player, args)
            "echest" -> return handleEchestCommand(sender as Player, args)
        }
        
        return false
    }
    
    /**
     * Handle the heal command
     */
    private fun handleHealCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.admin.heal")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        val target: Player
        
        if (args.isEmpty()) {
            // Self-heal
            if (sender !is Player) {
                sender.sendMessage(Component.text("Please specify a player to heal").color(NamedTextColor.RED))
                return true
            }
            target = sender
        } else {
            // Heal another player
            if (!sender.hasPermission("serverutils.admin.heal.others")) {
                sender.sendMessage(Component.text("You don't have permission to heal other players").color(NamedTextColor.RED))
                return true
            }
            
            val playerName = args[0]
            target = Bukkit.getPlayer(playerName) ?: run {
                sender.sendMessage(Component.text("Player $playerName is not online").color(NamedTextColor.RED))
                return true
            }
        }
        
        // Heal the player
        target.health = target.maxHealth
        target.fireTicks = 0
        
        // Remove negative potion effects
        for (effect in target.activePotionEffects) {
            if (isNegativePotionEffect(effect.type)) {
                target.removePotionEffect(effect.type)
            }
        }
        
        target.sendMessage(Component.text("You have been healed").color(NamedTextColor.GREEN))
        
        if (sender != target) {
            sender.sendMessage(Component.text("You have healed ${target.name}").color(NamedTextColor.GREEN))
        }
        
        return true
    }
    
    /**
     * Handle the feed command
     */
    private fun handleFeedCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.admin.feed")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        val target: Player
        
        if (args.isEmpty()) {
            // Self-feed
            if (sender !is Player) {
                sender.sendMessage(Component.text("Please specify a player to feed").color(NamedTextColor.RED))
                return true
            }
            target = sender
        } else {
            // Feed another player
            if (!sender.hasPermission("serverutils.admin.feed.others")) {
                sender.sendMessage(Component.text("You don't have permission to feed other players").color(NamedTextColor.RED))
                return true
            }
            
            val playerName = args[0]
            target = Bukkit.getPlayer(playerName) ?: run {
                sender.sendMessage(Component.text("Player $playerName is not online").color(NamedTextColor.RED))
                return true
            }
        }
        
        // Feed the player
        target.foodLevel = 20
        // Use reflection to set saturation since the setter might be protected in some API versions
        try {
            val setSaturationMethod = target.javaClass.getDeclaredMethod("setSaturation", Float::class.java)
            setSaturationMethod.isAccessible = true
            setSaturationMethod.invoke(target, 20.0f)
            plugin.logger.info("Set saturation for ${target.name} to 20.0")
        } catch (e: Exception) {
            // Fallback method
            try {
                val saturationField = target.javaClass.getDeclaredField("saturation")
                saturationField.isAccessible = true
                saturationField.set(target, 20.0f)
                plugin.logger.info("Set saturation field for ${target.name} to 20.0")
            } catch (e2: Exception) {
                plugin.logger.warning("Could not set saturation for ${target.name}: ${e2.message}")
            }
        }
        target.exhaustion = 0f
        
        target.sendMessage(Component.text("You have been fed").color(NamedTextColor.GREEN))
        
        if (sender != target) {
            sender.sendMessage(Component.text("You have fed ${target.name}").color(NamedTextColor.GREEN))
        }
        
        return true
    }
    
    /**
     * Handle the invsee command
     */
    private fun handleInvseeCommand(player: Player, args: Array<out String>): Boolean {
        if (!player.hasPermission("serverutils.admin.invsee")) {
            player.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Please specify a player to view their inventory").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getPlayer(targetName) ?: run {
            player.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
            return true
        }
        
        // Create a custom inventory with the target's items
        val inventory = Bukkit.createInventory(null, 54, Component.text("${target.name}'s Inventory").color(NamedTextColor.DARK_PURPLE))
        
        // Fill with filler items first
        for (i in 0 until inventory.size) {
            inventory.setItem(i, FILLER_ITEM)
        }
        
        // Copy the target's inventory
        // Main inventory (9-35)
        for (i in 0 until 27) {
            inventory.setItem(i + 9, target.inventory.getItem(i + 9))
        }
        
        // Hotbar (0-8) - Place at the bottom
        for (i in 0 until 9) {
            inventory.setItem(i + 36, target.inventory.getItem(i))
        }
        
        // Armor slots (36-39) - Place at the top right
        inventory.setItem(0, target.inventory.helmet)
        inventory.setItem(1, target.inventory.chestplate)
        inventory.setItem(2, target.inventory.leggings)
        inventory.setItem(3, target.inventory.boots)
        
        // Offhand (40) - Place at the top right
        inventory.setItem(8, target.inventory.itemInOffHand)
        
        // Open the inventory for the player
        player.openInventory(inventory)
        
        // Track this open inventory
        setOpenInventory(player.uniqueId, "invsee:${target.uniqueId}")
        
        return true
    }
    
    /**
     * Handle the echest command
     */
    private fun handleEchestCommand(player: Player, args: Array<out String>): Boolean {
        if (!player.hasPermission("serverutils.admin.echest.use")) {
            player.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        val target: Player
        
        if (args.isEmpty()) {
            // View own enderchest
            target = player
        } else {
            // View another player's enderchest
            if (!player.hasPermission("serverutils.admin.echest.others")) {
                player.sendMessage(Component.text("You don't have permission to view other players' enderchests").color(NamedTextColor.RED))
                return true
            }
            
            val targetName = args[0]
            target = Bukkit.getPlayer(targetName) ?: run {
                player.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
                return true
            }
        }
        
        // Create a custom inventory with the target's enderchest items
        val inventory = Bukkit.createInventory(null, 27, Component.text("${target.name}'s Enderchest").color(NamedTextColor.DARK_PURPLE))
        
        // Copy the target's enderchest
        for (i in 0 until target.enderChest.size) {
            inventory.setItem(i, target.enderChest.getItem(i))
        }
        
        // Open the inventory for the player
        player.openInventory(inventory)
        
        // Track this open inventory
        setOpenInventory(player.uniqueId, "echest:${target.uniqueId}")
        
        return true
    }
    
    /**
     * Check if a potion effect is negative
     */
    private fun isNegativePotionEffect(type: org.bukkit.potion.PotionEffectType): Boolean {
        // Use string comparison to avoid API version issues
        val negativeEffects = setOf(
            "POISON",
            "WITHER",
            "BLINDNESS",
            "NAUSEA", // Previously known as CONFUSION in older versions
            "HARM",
            "HUNGER",
            "SLOW",
            "SLOW_DIGGING",
            "WEAKNESS"
        )
        
        return type.name in negativeEffects
    }
    

    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val completions = ArrayList<String>()
        
        if (args.size == 1) {
            // Player name completions
            val playerNames = Bukkit.getOnlinePlayers().map { it.name }
            StringUtil.copyPartialMatches(args[0], playerNames, completions)
        }
        
        return completions
    }
}
