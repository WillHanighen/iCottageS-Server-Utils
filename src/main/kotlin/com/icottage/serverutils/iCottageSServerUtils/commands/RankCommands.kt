package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.listeners.RankDisplayListener
import com.icottage.serverutils.iCottageSServerUtils.ranks.Rank
import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * Commands for managing ranks
 */
class RankCommands(
    private val rankManager: RankManager,
    private val rankDisplayListener: RankDisplayListener
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sendHelpMessage(sender)
            return true
        }

        when (args[0].lowercase()) {
            "list" -> listRanks(sender)
            "info" -> {
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Usage: /rank info <rank>").color(NamedTextColor.RED))
                    return true
                }
                showRankInfo(sender, args[1])
            }
            "create" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Usage: /rank create <name> <displayName> <prefix> [weight]").color(NamedTextColor.RED))
                    return true
                }
                val name = args[1]
                val displayName = args[2]
                val prefix = args[3]
                val weight = if (args.size > 4) args[4].toIntOrNull() ?: 0 else 0
                createRank(sender, name, displayName, prefix, weight)
            }
            "edit" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Usage: /rank edit <rank> <property> <value>").color(NamedTextColor.RED))
                    sender.sendMessage(Component.text("Properties: displayname, prefix, weight").color(NamedTextColor.YELLOW))
                    return true
                }
                val rankName = args[1]
                val property = args[2].lowercase()
                val value = args.copyOfRange(3, args.size).joinToString(" ")
                editRank(sender, rankName, property, value)
            }
            "permission" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                if (args.size < 4) {
                    sender.sendMessage(Component.text("Usage: /rank permission <rank> <add|remove> <permission>").color(NamedTextColor.RED))
                    return true
                }
                val rankName = args[1]
                val action = args[2].lowercase()
                val permission = args[3]
                editRankPermission(sender, rankName, action, permission)
            }
            "delete" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                if (args.size < 2) {
                    sender.sendMessage(Component.text("Usage: /rank delete <rank>").color(NamedTextColor.RED))
                    return true
                }
                deleteRank(sender, args[1])
            }
            "set" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                if (args.size < 3) {
                    sender.sendMessage(Component.text("Usage: /rank set <player> <rank>").color(NamedTextColor.RED))
                    return true
                }
                val targetPlayer = Bukkit.getPlayer(args[1])
                if (targetPlayer == null) {
                    sender.sendMessage(Component.text("Player not found: ${args[1]}").color(NamedTextColor.RED))
                    return true
                }
                setPlayerRank(sender, targetPlayer, args[2])
            }
            "reload" -> {
                if (!sender.hasPermission("serverutils.rank.admin")) {
                    sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
                    return true
                }
                reloadRanks(sender)
            }
            "colors" -> {
                showColorHelp(sender)
            }
            else -> sendHelpMessage(sender)
        }

        return true
    }

    private fun sendHelpMessage(sender: CommandSender) {
        sender.sendMessage(Component.text("=== Rank Commands ===").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("/rank list - List all ranks").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/rank info <rank> - Show information about a rank").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("/rank colors - Show color code help").color(NamedTextColor.YELLOW))
        
        if (sender.hasPermission("serverutils.rank.admin")) {
            sender.sendMessage(Component.text("/rank create <name> <displayName> <prefix> [weight] - Create a new rank").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/rank edit <rank> <property> <value> - Edit a rank's properties").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/rank permission <rank> <add|remove> <permission> - Edit rank permissions").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/rank delete <rank> - Delete a rank").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/rank set <player> <rank> - Set a player's rank").color(NamedTextColor.YELLOW))
            sender.sendMessage(Component.text("/rank reload - Reload ranks from file").color(NamedTextColor.YELLOW))
        }
    }

    private fun listRanks(sender: CommandSender) {
        val ranks = rankManager.getAllRanksSorted()
        
        if (ranks.isEmpty()) {
            sender.sendMessage(Component.text("No ranks found").color(NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.text("=== Available Ranks ===").color(NamedTextColor.GOLD))
        ranks.forEach { rank ->
            sender.sendMessage(
                Component.text("- ${rank.name} (${rank.getFormattedDisplayName()}) - Weight: ${rank.weight}")
                    .color(NamedTextColor.YELLOW)
            )
        }
    }

    private fun showRankInfo(sender: CommandSender, rankName: String) {
        val rank = rankManager.getRank(rankName)
        
        if (rank == null) {
            sender.sendMessage(Component.text("Rank not found: $rankName").color(NamedTextColor.RED))
            return
        }
        
        sender.sendMessage(Component.text("=== Rank: ${rank.name} ===").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Display Name: ${rank.getFormattedDisplayName()}").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Prefix: ${rank.getFormattedPrefix()}").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("Weight: ${rank.weight}").color(NamedTextColor.YELLOW))
        
        if (rank.permissions.isNotEmpty()) {
            sender.sendMessage(Component.text("Permissions:").color(NamedTextColor.YELLOW))
            rank.permissions.forEach { perm ->
                sender.sendMessage(Component.text("- $perm").color(NamedTextColor.GRAY))
            }
        }
    }

    private fun createRank(sender: CommandSender, name: String, displayName: String, prefix: String, weight: Int) {
        if (rankManager.getRank(name) != null) {
            sender.sendMessage(Component.text("A rank with that name already exists").color(NamedTextColor.RED))
            return
        }
        
        val rank = Rank(
            name = name,
            displayName = displayName,
            prefix = prefix,
            weight = weight
        )
        
        rankManager.setRank(rank)
        sender.sendMessage(Component.text("Rank created: $name").color(NamedTextColor.GREEN))
        
        // Update tab list for all players
        rankDisplayListener.updateTabList()
    }

    private fun deleteRank(sender: CommandSender, rankName: String) {
        if (rankName.equals("default", ignoreCase = true)) {
            sender.sendMessage(Component.text("Cannot delete the default rank").color(NamedTextColor.RED))
            return
        }
        
        val success = rankManager.removeRank(rankName)
        
        if (success) {
            sender.sendMessage(Component.text("Rank deleted: $rankName").color(NamedTextColor.GREEN))
            
            // Update tab list for all players
            rankDisplayListener.updateTabList()
        } else {
            sender.sendMessage(Component.text("Rank not found: $rankName").color(NamedTextColor.RED))
        }
    }

    private fun setPlayerRank(sender: CommandSender, player: Player, rankName: String) {
        val success = rankManager.setPlayerRank(player, rankName)
        
        if (success) {
            sender.sendMessage(Component.text("Set ${player.name}'s rank to $rankName").color(NamedTextColor.GREEN))
            
            // Update player display name and tab list
            rankDisplayListener.updatePlayerDisplayName(player)
            rankDisplayListener.updateTabList()
        } else {
            sender.sendMessage(Component.text("Rank not found: $rankName").color(NamedTextColor.RED))
        }
    }

    private fun reloadRanks(sender: CommandSender) {
        rankManager.initialize()
        sender.sendMessage(Component.text("Ranks reloaded").color(NamedTextColor.GREEN))
        
        // Update tab list for all players
        rankDisplayListener.updateTabList()
    }

    /**
     * Edits a rank's properties
     */
    private fun editRank(sender: CommandSender, rankName: String, property: String, value: String) {
        val rank = rankManager.getRank(rankName)
        
        if (rank == null) {
            sender.sendMessage(Component.text("Rank not found: $rankName").color(NamedTextColor.RED))
            return
        }
        
        val updatedRank = when (property) {
            "displayname" -> rank.copy(displayName = value)
            "prefix" -> rank.copy(prefix = value)
            "weight" -> {
                val weightValue = value.toIntOrNull()
                if (weightValue == null) {
                    sender.sendMessage(Component.text("Weight must be a number").color(NamedTextColor.RED))
                    return
                }
                rank.copy(weight = weightValue)
            }
            else -> {
                sender.sendMessage(Component.text("Unknown property: $property").color(NamedTextColor.RED))
                sender.sendMessage(Component.text("Valid properties: displayname, prefix, weight").color(NamedTextColor.YELLOW))
                return
            }
        }
        
        rankManager.setRank(updatedRank)
        sender.sendMessage(Component.text("Updated rank $rankName: $property = $value").color(NamedTextColor.GREEN))
        
        // Update tab list for all players
        rankDisplayListener.updateTabList()
    }
    
    /**
     * Edits a rank's permissions
     */
    private fun editRankPermission(sender: CommandSender, rankName: String, action: String, permission: String) {
        val rank = rankManager.getRank(rankName)
        
        if (rank == null) {
            sender.sendMessage(Component.text("Rank not found: $rankName").color(NamedTextColor.RED))
            return
        }
        
        val updatedPermissions = when (action) {
            "add" -> {
                if (rank.permissions.contains(permission)) {
                    sender.sendMessage(Component.text("Rank already has permission: $permission").color(NamedTextColor.RED))
                    return
                }
                rank.permissions + permission
            }
            "remove" -> {
                if (!rank.permissions.contains(permission)) {
                    sender.sendMessage(Component.text("Rank doesn't have permission: $permission").color(NamedTextColor.RED))
                    return
                }
                rank.permissions - permission
            }
            else -> {
                sender.sendMessage(Component.text("Unknown action: $action").color(NamedTextColor.RED))
                sender.sendMessage(Component.text("Valid actions: add, remove").color(NamedTextColor.YELLOW))
                return
            }
        }
        
        val updatedRank = rank.copy(permissions = updatedPermissions)
        rankManager.setRank(updatedRank)
        
        if (action == "add") {
            sender.sendMessage(Component.text("Added permission $permission to rank $rankName").color(NamedTextColor.GREEN))
        } else {
            sender.sendMessage(Component.text("Removed permission $permission from rank $rankName").color(NamedTextColor.GREEN))
        }
    }
    
    /**
     * Shows color code help
     */
    private fun showColorHelp(sender: CommandSender) {
        sender.sendMessage(Component.text("=== Color Code Help ===").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("Traditional color codes:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("&0 - Black").color(NamedTextColor.DARK_GRAY))
        sender.sendMessage(Component.text("&1 - Dark Blue").color(NamedTextColor.DARK_BLUE))
        sender.sendMessage(Component.text("&2 - Dark Green").color(NamedTextColor.DARK_GREEN))
        sender.sendMessage(Component.text("&3 - Dark Aqua").color(NamedTextColor.DARK_AQUA))
        sender.sendMessage(Component.text("&4 - Dark Red").color(NamedTextColor.DARK_RED))
        sender.sendMessage(Component.text("&5 - Dark Purple").color(NamedTextColor.DARK_PURPLE))
        sender.sendMessage(Component.text("&6 - Gold").color(NamedTextColor.GOLD))
        sender.sendMessage(Component.text("&7 - Gray").color(NamedTextColor.GRAY))
        sender.sendMessage(Component.text("&8 - Dark Gray").color(NamedTextColor.DARK_GRAY))
        sender.sendMessage(Component.text("&9 - Blue").color(NamedTextColor.BLUE))
        sender.sendMessage(Component.text("&a - Green").color(NamedTextColor.GREEN))
        sender.sendMessage(Component.text("&b - Aqua").color(NamedTextColor.AQUA))
        sender.sendMessage(Component.text("&c - Red").color(NamedTextColor.RED))
        sender.sendMessage(Component.text("&d - Light Purple").color(NamedTextColor.LIGHT_PURPLE))
        sender.sendMessage(Component.text("&e - Yellow").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("&f - White").color(NamedTextColor.WHITE))
        
        sender.sendMessage(Component.text("\nFormat codes:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("&l - Bold").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&o - Italic").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&n - Underline").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&m - Strikethrough").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&k - Obfuscated").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&r - Reset").color(NamedTextColor.WHITE))
        
        sender.sendMessage(Component.text("\nHex color codes:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("&#RRGGBB - Hex color (e.g., &#ff0000 for red)").color(NamedTextColor.WHITE))
        
        sender.sendMessage(Component.text("\nExamples:").color(NamedTextColor.YELLOW))
        sender.sendMessage(Component.text("&cRed text").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&l&eYellow bold text").color(NamedTextColor.WHITE))
        sender.sendMessage(Component.text("&#ff5500Orange text with hex color").color(NamedTextColor.WHITE))
    }
    
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        if (args.size == 1) {
            val subCommands = mutableListOf("list", "info", "colors")
            
            if (sender.hasPermission("serverutils.rank.admin")) {
                subCommands.addAll(listOf("create", "edit", "permission", "delete", "set", "reload"))
            }
            
            return subCommands.filter { it.startsWith(args[0].lowercase()) }
        }

        when (args[0].lowercase()) {
            "info", "delete", "edit", "permission" -> {
                if (args.size == 2) {
                    return rankManager.getAllRanks()
                        .map { it.name }
                        .filter { it.startsWith(args[1].lowercase()) }
                }
            }
            "edit" -> {
                if (args.size == 3) {
                    return listOf("displayname", "prefix", "weight")
                        .filter { it.startsWith(args[2].lowercase()) }
                }
            }
            "permission" -> {
                if (args.size == 3) {
                    return listOf("add", "remove")
                        .filter { it.startsWith(args[2].lowercase()) }
                }
            }
            "set" -> {
                if (args.size == 2) {
                    return Bukkit.getOnlinePlayers()
                        .map { it.name }
                        .filter { it.startsWith(args[1], ignoreCase = true) }
                } else if (args.size == 3) {
                    return rankManager.getAllRanks()
                        .map { it.name }
                        .filter { it.startsWith(args[2].lowercase()) }
                }
            }
        }

        return emptyList()
    }
}
