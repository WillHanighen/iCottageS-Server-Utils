package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.teleport.TeleportManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Commands for the teleport request system
 */
class TeleportCommands(private val plugin: JavaPlugin, private val teleportManager: TeleportManager) : CommandExecutor, TabCompleter {
    
    /**
     * Register commands
     */
    fun registerCommands() {
        val commands = listOf("tpa", "tpaccept", "tpdeny", "tpcancel")
        
        commands.forEach { command ->
            plugin.getCommand(command)?.setExecutor(this)
            plugin.getCommand(command)?.tabCompleter = this
        }
    }
    
    /**
     * Execute commands
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("This command can only be used by players").color(NamedTextColor.RED))
            return true
        }
        
        when (command.name.lowercase()) {
            "tpa" -> handleTpaCommand(sender, args)
            "tpaccept" -> handleTpAcceptCommand(sender, args)
            "tpdeny" -> handleTpDenyCommand(sender, args)
            "tpcancel" -> handleTpCancelCommand(sender, args)
            else -> return false
        }
        
        return true
    }
    
    /**
     * Handle /tpa command
     */
    private fun handleTpaCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /tpa <player>").color(NamedTextColor.RED))
            return false
        }
        
        val targetName = args[0]
        
        teleportManager.sendTeleportRequest(player, targetName)
        return true
    }
    
    /**
     * Handle /tpaccept command
     */
    private fun handleTpAcceptCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /tpaccept <player>").color(NamedTextColor.RED))
            return false
        }
        
        val senderName = args[0]
        
        teleportManager.acceptTeleportRequest(player, senderName)
        return true
    }
    
    /**
     * Handle /tpdeny command
     */
    private fun handleTpDenyCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /tpdeny <player>").color(NamedTextColor.RED))
            return false
        }
        
        val senderName = args[0]
        
        teleportManager.denyTeleportRequest(player, senderName)
        return true
    }
    
    /**
     * Tab completion
     */
    /**
     * Handle /tpcancel command
     */
    private fun handleTpCancelCommand(player: Player, args: Array<out String>): Boolean {
        if (!teleportManager.hasPendingTeleport(player.uniqueId)) {
            player.sendMessage(Component.text("You don't have a pending teleport to cancel").color(NamedTextColor.RED))
            return false
        }
        
        teleportManager.cancelPendingTeleport(player.uniqueId, "Teleport cancelled by you")
        return true
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) {
            return emptyList()
        }
        
        val completions = ArrayList<String>()
        
        when (command.name.lowercase()) {
            "tpa" -> {
                if (args.size == 1) {
                    // Complete player names
                    val prefix = args[0].lowercase()
                    val playerNames = plugin.server.onlinePlayers
                        .filter { it.name.lowercase().startsWith(prefix) && it.uniqueId != sender.uniqueId }
                        .map { it.name }
                    completions.addAll(playerNames)
                }
            }
            "tpaccept", "tpdeny" -> {
                if (args.size == 1) {
                    // Complete with names of players who have sent teleport requests to this player
                    val prefix = args[0].lowercase()
                    val requestSenders = teleportManager.getPendingRequestSenders(sender.uniqueId)
                    
                    if (requestSenders.isNotEmpty()) {
                        val playerNames = plugin.server.onlinePlayers
                            .filter { it.uniqueId in requestSenders && it.name.lowercase().startsWith(prefix) }
                            .map { it.name }
                        completions.addAll(playerNames)
                    }
                }
            }
            // No tab completion for tpcancel
        }
        
        return completions
    }
}
