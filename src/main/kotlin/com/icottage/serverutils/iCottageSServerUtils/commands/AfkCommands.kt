package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.afk.AfkManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Commands for the AFK system
 */
class AfkCommands(private val plugin: JavaPlugin, private val afkManager: AfkManager) : CommandExecutor, TabCompleter {
    
    /**
     * Register commands
     */
    fun registerCommands() {
        val commands = listOf("afk")
        
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
            "afk" -> handleAfkCommand(sender, args)
            else -> return false
        }
        
        return true
    }
    
    /**
     * Handle /afk command
     */
    private fun handleAfkCommand(player: Player, args: Array<out String>): Boolean {
        val isAfk = afkManager.isAfk(player.uniqueId)
        
        // Toggle AFK status
        afkManager.setAfk(player, !isAfk, true)
        
        return true
    }
    
    /**
     * Tab completion
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) {
            return emptyList()
        }
        
        if (command.name.lowercase() == "afk" && args.size == 1) {
            // Suggest player names for the AFK command
            val prefix = args[0].lowercase()
            return plugin.server.onlinePlayers
                .filter { it.name.lowercase().startsWith(prefix) }
                .map { it.name }
        }
        
        return emptyList()
    }
}
