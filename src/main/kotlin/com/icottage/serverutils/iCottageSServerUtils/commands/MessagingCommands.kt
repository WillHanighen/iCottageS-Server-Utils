package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.messaging.MessagingManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Commands for private messaging between players
 */
class MessagingCommands(private val plugin: JavaPlugin, private val messagingManager: MessagingManager) : CommandExecutor, TabCompleter {
    
    /**
     * Register commands
     */
    fun registerCommands() {
        val commands = listOf("msg", "r", "reply", "ignore", "unignore")
        
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
            "msg" -> handleMsgCommand(sender, args)
            "r", "reply" -> handleReplyCommand(sender, args)
            "ignore" -> handleIgnoreCommand(sender, args)
            "unignore" -> handleUnignoreCommand(sender, args)
            else -> return false
        }
        
        return true
    }
    
    /**
     * Handle /msg command
     */
    private fun handleMsgCommand(player: Player, args: Array<out String>): Boolean {
        if (args.size < 2) {
            player.sendMessage(Component.text("Usage: /msg <player> <message>").color(NamedTextColor.RED))
            return false
        }
        
        val targetName = args[0]
        val message = args.drop(1).joinToString(" ")
        
        messagingManager.sendPrivateMessage(player, targetName, message)
        return true
    }
    
    /**
     * Handle /r or /reply command
     */
    private fun handleReplyCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /r <message>").color(NamedTextColor.RED))
            return false
        }
        
        val message = args.joinToString(" ")
        
        messagingManager.replyToLastMessage(player, message)
        return true
    }
    
    /**
     * Handle /ignore command
     */
    private fun handleIgnoreCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /ignore <player>").color(NamedTextColor.RED))
            return false
        }
        
        val targetName = args[0]
        
        messagingManager.ignorePlayer(player, targetName)
        return true
    }
    
    /**
     * Handle /unignore command
     */
    private fun handleUnignoreCommand(player: Player, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage(Component.text("Usage: /unignore <player>").color(NamedTextColor.RED))
            return false
        }
        
        val targetName = args[0]
        
        messagingManager.unignorePlayer(player, targetName)
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
        
        when (command.name.lowercase()) {
            "msg" -> {
                if (args.size == 1) {
                    // Complete player names
                    val prefix = args[0].lowercase()
                    return plugin.server.onlinePlayers
                        .filter { it.name.lowercase().startsWith(prefix) && it.uniqueId != sender.uniqueId }
                        .map { it.name }
                }
            }
            "ignore" -> {
                if (args.size == 1) {
                    // Complete player names
                    val prefix = args[0].lowercase()
                    return plugin.server.onlinePlayers
                        .filter { it.name.lowercase().startsWith(prefix) && it.uniqueId != sender.uniqueId }
                        .map { it.name }
                }
            }
            "unignore" -> {
                if (args.size == 1) {
                    // Complete ignored player names
                    val prefix = args[0].lowercase()
                    val ignoredUUIDs = messagingManager.getIgnoredPlayers(sender.uniqueId)
                    
                    return plugin.server.onlinePlayers
                        .filter { it.uniqueId in ignoredUUIDs && it.name.lowercase().startsWith(prefix) }
                        .map { it.name }
                }
            }
        }
        
        return emptyList()
    }
}
