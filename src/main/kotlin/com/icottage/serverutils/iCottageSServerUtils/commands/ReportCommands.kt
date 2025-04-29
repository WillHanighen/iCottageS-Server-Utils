package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.report.ReportManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

/**
 * Commands for the player reporting system
 */
class ReportCommands(private val plugin: JavaPlugin, private val reportManager: ReportManager) : CommandExecutor, TabCompleter {
    
    /**
     * Register commands
     */
    fun registerCommands() {
        val commands = listOf("report")
        
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
            "report" -> handleReportCommand(sender, args)
            else -> return false
        }
        
        return true
    }
    
    /**
     * Handle /report command
     */
    private fun handleReportCommand(player: Player, args: Array<out String>): Boolean {
        if (args.size < 3) {
            player.sendMessage(
                Component.text("Usage: /report <player> <reason> <details>").color(NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("Valid reasons: ").color(NamedTextColor.GOLD))
                    .append(Component.text(reportManager.getValidReasons().joinToString(", ")).color(NamedTextColor.WHITE))
            )
            return false
        }
        
        val targetName = args[0]
        val reason = args[1]
        val details = args.drop(2).joinToString(" ")
        
        reportManager.submitReport(player, targetName, reason, details)
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
        
        val completions = ArrayList<String>()
        
        when (command.name.lowercase()) {
            "report" -> {
                when (args.size) {
                    1 -> {
                        // Complete player names
                        val prefix = args[0].lowercase()
                        val playerNames = plugin.server.onlinePlayers
                            .filter { it.name.lowercase().startsWith(prefix) && it.uniqueId != sender.uniqueId }
                            .map { it.name }
                        completions.addAll(playerNames)
                    }
                    2 -> {
                        // Complete reasons
                        val prefix = args[1].lowercase()
                        val reasons = reportManager.getValidReasons()
                            .filter { it.lowercase().startsWith(prefix) }
                        completions.addAll(reasons)
                    }
                }
            }
        }
        
        return completions
    }
}
