package com.icottage.serverutils.iCottageSServerUtils.commands

import com.icottage.serverutils.iCottageSServerUtils.moderation.ModerationManager
import com.icottage.serverutils.iCottageSServerUtils.utils.TimeUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.UUID

/**
 * Handles moderation commands
 */
class ModerationCommands(private val moderationManager: ModerationManager) : CommandExecutor, TabCompleter {
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("mute", ignoreCase = true)) {
            return handleMuteCommand(sender, args)
        } else if (command.name.equals("unmute", ignoreCase = true)) {
            return handleUnmuteCommand(sender, args)
        } else if (command.name.equals("ban", ignoreCase = true)) {
            return handleBanCommand(sender, args)
        } else if (command.name.equals("unban", ignoreCase = true)) {
            return handleUnbanCommand(sender, args)
        } else if (command.name.equals("warn", ignoreCase = true)) {
            return handleWarnCommand(sender, args)
        } else if (command.name.equals("warnings", ignoreCase = true)) {
            return handleWarningsCommand(sender, args)
        } else if (command.name.equals("clearwarnings", ignoreCase = true)) {
            return handleClearWarningsCommand(sender, args)
        } else if (command.name.equals("kick", ignoreCase = true)) {
            return handleKickCommand(sender, args)
        } else if (command.name.equals("history", ignoreCase = true)) {
            return handleHistoryCommand(sender, args)
        }
        
        return false
    }
    
    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String> {
        val completions = mutableListOf<String>()
        
        if (command.name.equals("mute", ignoreCase = true) || 
            command.name.equals("ban", ignoreCase = true) || 
            command.name.equals("warn", ignoreCase = true) || 
            command.name.equals("kick", ignoreCase = true)) {
            
            if (args.size == 1) {
                // Complete player names
                return Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            } else if (args.size == 2) {
                // For the reason argument, suggest some common reasons
                val reasons = listOf(
                    "Spamming", "Harassment", "Inappropriate language", "Advertising",
                    "Griefing", "Hacking/Cheating", "Exploiting", "Disrespecting staff"
                )
                return reasons.filter { it.startsWith(args[1], ignoreCase = true) }
            } else if (args.size == 3 && (command.name.equals("mute", ignoreCase = true) || command.name.equals("ban", ignoreCase = true))) {
                // For the duration argument, suggest some common durations
                val durations = listOf("1h", "6h", "12h", "1d", "3d", "7d", "30d", "permanent")
                return durations.filter { it.startsWith(args[2], ignoreCase = true) }
            }
        } else if (command.name.equals("unmute", ignoreCase = true) || 
                   command.name.equals("unban", ignoreCase = true) || 
                   command.name.equals("warnings", ignoreCase = true) || 
                   command.name.equals("clearwarnings", ignoreCase = true) || 
                   command.name.equals("history", ignoreCase = true)) {
            
            if (args.size == 1) {
                // Complete player names
                return Bukkit.getOnlinePlayers()
                    .map { it.name }
                    .filter { it.startsWith(args[0], ignoreCase = true) }
            }
        }
        
        return completions
    }
    
    /**
     * Handle the /mute command
     */
    private fun handleMuteCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.mute")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage(Component.text("Usage: /mute <player> <reason> <duration>").color(NamedTextColor.RED))
            sender.sendMessage(Component.text("Duration examples: 1h, 2d, 1w, 30d, permanent").color(NamedTextColor.GRAY))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        // Check if the target has a higher rank than the sender
        if (sender is Player && target is Player && !sender.hasPermission("serverutils.moderation.bypass")) {
            if (target.hasPermission("serverutils.moderation.exempt")) {
                sender.sendMessage(Component.text("You cannot mute this player").color(NamedTextColor.RED))
                return true
            }
        }
        
        val reason = args[1]
        val durationStr = args[2]
        val duration = TimeUtils.parseTimeString(durationStr)
        
        if (duration < 0) {
            sender.sendMessage(Component.text("Invalid duration format: $durationStr").color(NamedTextColor.RED))
            sender.sendMessage(Component.text("Examples: 1h, 2d, 1w, 30d, permanent").color(NamedTextColor.GRAY))
            return true
        }
        
        val success = moderationManager.mutePlayer(target, reason, duration, sender)
        
        if (success) {
            val formattedTime = if (duration == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(duration)}"
            sender.sendMessage(Component.text("Muted ${target.name} $formattedTime").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} muted ${target.name} $formattedTime").color(NamedTextColor.YELLOW)
                    .append(Component.text("\nReason: $reason").color(NamedTextColor.YELLOW)),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("Failed to mute ${target.name}").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /unmute command
     */
    private fun handleUnmuteCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.unmute")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /unmute <player>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        val success = moderationManager.unmutePlayer(target, sender)
        
        if (success) {
            sender.sendMessage(Component.text("Unmuted ${target.name}").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} unmuted ${target.name}").color(NamedTextColor.YELLOW),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("${target.name} is not muted").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /ban command
     */
    private fun handleBanCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.ban")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.size < 3) {
            sender.sendMessage(Component.text("Usage: /ban <player> <reason> <duration>").color(NamedTextColor.RED))
            sender.sendMessage(Component.text("Duration examples: 1h, 2d, 1w, 30d, permanent").color(NamedTextColor.GRAY))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        // Check if the target has a higher rank than the sender
        if (sender is Player && target is Player && !sender.hasPermission("serverutils.moderation.bypass")) {
            if (target.hasPermission("serverutils.moderation.exempt")) {
                sender.sendMessage(Component.text("You cannot ban this player").color(NamedTextColor.RED))
                return true
            }
        }
        
        val reason = args[1]
        val durationStr = args[2]
        val duration = TimeUtils.parseTimeString(durationStr)
        
        if (duration < 0) {
            sender.sendMessage(Component.text("Invalid duration format: $durationStr").color(NamedTextColor.RED))
            sender.sendMessage(Component.text("Examples: 1h, 2d, 1w, 30d, permanent").color(NamedTextColor.GRAY))
            return true
        }
        
        val success = moderationManager.banPlayer(target, reason, duration, sender)
        
        if (success) {
            val formattedTime = if (duration == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(duration)}"
            sender.sendMessage(Component.text("Banned ${target.name} $formattedTime").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} banned ${target.name} $formattedTime").color(NamedTextColor.YELLOW)
                    .append(Component.text("\nReason: $reason").color(NamedTextColor.YELLOW)),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("Failed to ban ${target.name}").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /unban command
     */
    private fun handleUnbanCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.unban")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /unban <player>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        val success = moderationManager.unbanPlayer(target, sender)
        
        if (success) {
            sender.sendMessage(Component.text("Unbanned ${target.name}").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} unbanned ${target.name}").color(NamedTextColor.YELLOW),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("${target.name} is not banned").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /warn command
     */
    private fun handleWarnCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.warn")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /warn <player> <reason>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        // Check if the target has a higher rank than the sender
        if (sender is Player && target is Player && !sender.hasPermission("serverutils.moderation.bypass")) {
            if (target.hasPermission("serverutils.moderation.exempt")) {
                sender.sendMessage(Component.text("You cannot warn this player").color(NamedTextColor.RED))
                return true
            }
        }
        
        val reason = args[1]
        
        val success = moderationManager.warnPlayer(target, reason, sender)
        
        if (success) {
            sender.sendMessage(Component.text("Warned ${target.name}").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} warned ${target.name}").color(NamedTextColor.YELLOW)
                    .append(Component.text("\nReason: $reason").color(NamedTextColor.YELLOW)),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("Failed to warn ${target.name}").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /warnings command
     */
    private fun handleWarningsCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.warnings")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /warnings <player>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        val warnings = moderationManager.getWarnings(target)
        
        if (warnings.isEmpty()) {
            sender.sendMessage(Component.text("${target.name} has no warnings").color(NamedTextColor.YELLOW))
            return true
        }
        
        sender.sendMessage(Component.text("Warnings for ${target.name} (${warnings.size}):").color(NamedTextColor.YELLOW))
        
        for ((index, warning) in warnings.withIndex()) {
            val date = java.util.Date(warning.timestamp)
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val formattedDate = dateFormat.format(date)
            
            sender.sendMessage(
                Component.text("${index + 1}. ").color(NamedTextColor.GOLD)
                    .append(Component.text("Reason: ${warning.reason}").color(NamedTextColor.YELLOW))
                    .append(Component.text(" | By: ${warning.issuerName}").color(NamedTextColor.YELLOW))
                    .append(Component.text(" | Date: $formattedDate").color(NamedTextColor.YELLOW))
            )
        }
        
        return true
    }
    
    /**
     * Handle the /clearwarnings command
     */
    private fun handleClearWarningsCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.clearwarnings")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /clearwarnings <player>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        val success = moderationManager.clearWarnings(target, sender)
        
        if (success) {
            sender.sendMessage(Component.text("Cleared all warnings for ${target.name}").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} cleared all warnings for ${target.name}").color(NamedTextColor.YELLOW),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("${target.name} has no warnings").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /kick command
     */
    private fun handleKickCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.kick")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.size < 2) {
            sender.sendMessage(Component.text("Usage: /kick <player> <reason>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null || !target.isOnline) {
            sender.sendMessage(Component.text("Player not found or not online: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        // Check if the target has a higher rank than the sender
        if (sender is Player && !sender.hasPermission("serverutils.moderation.bypass")) {
            if (target.hasPermission("serverutils.moderation.exempt")) {
                sender.sendMessage(Component.text("You cannot kick this player").color(NamedTextColor.RED))
                return true
            }
        }
        
        val reason = args[1]
        
        val success = moderationManager.kickPlayer(target, reason, sender)
        
        if (success) {
            sender.sendMessage(Component.text("Kicked ${target.name}").color(NamedTextColor.GREEN))
            
            // Broadcast to staff
            Bukkit.broadcast(
                Component.text("${sender.name} kicked ${target.name}").color(NamedTextColor.YELLOW)
                    .append(Component.text("\nReason: $reason").color(NamedTextColor.YELLOW)),
                "serverutils.moderation.notify"
            )
        } else {
            sender.sendMessage(Component.text("Failed to kick ${target.name}").color(NamedTextColor.RED))
        }
        
        return true
    }
    
    /**
     * Handle the /history command
     */
    private fun handleHistoryCommand(sender: CommandSender, args: Array<out String>): Boolean {
        if (!sender.hasPermission("serverutils.moderation.history")) {
            sender.sendMessage(Component.text("You don't have permission to use this command").color(NamedTextColor.RED))
            return true
        }
        
        if (args.isEmpty()) {
            sender.sendMessage(Component.text("Usage: /history <player>").color(NamedTextColor.RED))
            return true
        }
        
        val targetName = args[0]
        val target = Bukkit.getOfflinePlayerIfCached(targetName)
        
        if (target == null || !target.hasPlayedBefore()) {
            sender.sendMessage(Component.text("Player not found: $targetName").color(NamedTextColor.RED))
            return true
        }
        
        sender.sendMessage(Component.text("Moderation history for ${target.name}:").color(NamedTextColor.YELLOW))
        
        // Check if player is currently muted
        val muteEntry = moderationManager.getMuteEntry(target)
        if (muteEntry != null) {
            val remainingTime = TimeUtils.getRemainingTime(muteEntry.expiration)
            val formattedTime = if (remainingTime == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(remainingTime)}"
            
            sender.sendMessage(
                Component.text("Currently muted $formattedTime").color(NamedTextColor.RED)
                    .append(Component.text("\nReason: ${muteEntry.reason}").color(NamedTextColor.RED))
                    .append(Component.text("\nIssued by: ${muteEntry.issuerName}").color(NamedTextColor.RED))
            )
        }
        
        // Check if player is currently banned
        val banEntry = moderationManager.getBanEntry(target)
        if (banEntry != null) {
            val remainingTime = TimeUtils.getRemainingTime(banEntry.expiration)
            val formattedTime = if (remainingTime == Long.MAX_VALUE) "permanently" else "for ${TimeUtils.formatTime(remainingTime)}"
            
            sender.sendMessage(
                Component.text("Currently banned $formattedTime").color(NamedTextColor.RED)
                    .append(Component.text("\nReason: ${banEntry.reason}").color(NamedTextColor.RED))
                    .append(Component.text("\nIssued by: ${banEntry.issuerName}").color(NamedTextColor.RED))
            )
        }
        
        // Get warnings
        val warnings = moderationManager.getWarnings(target)
        if (warnings.isNotEmpty()) {
            sender.sendMessage(Component.text("Warnings (${warnings.size}):").color(NamedTextColor.GOLD))
            
            for ((index, warning) in warnings.withIndex()) {
                val date = java.util.Date(warning.timestamp)
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val formattedDate = dateFormat.format(date)
                
                sender.sendMessage(
                    Component.text("${index + 1}. ").color(NamedTextColor.GOLD)
                        .append(Component.text("Reason: ${warning.reason}").color(NamedTextColor.YELLOW))
                        .append(Component.text(" | By: ${warning.issuerName}").color(NamedTextColor.YELLOW))
                        .append(Component.text(" | Date: $formattedDate").color(NamedTextColor.YELLOW))
                )
            }
        }
        
        // Get kick history
        val kicks = moderationManager.getKickHistory(target)
        if (kicks.isNotEmpty()) {
            sender.sendMessage(Component.text("Kicks (${kicks.size}):").color(NamedTextColor.GOLD))
            
            for ((index, kick) in kicks.withIndex()) {
                val date = java.util.Date(kick.timestamp)
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                val formattedDate = dateFormat.format(date)
                
                sender.sendMessage(
                    Component.text("${index + 1}. ").color(NamedTextColor.GOLD)
                        .append(Component.text("Reason: ${kick.reason}").color(NamedTextColor.YELLOW))
                        .append(Component.text(" | By: ${kick.issuerName}").color(NamedTextColor.YELLOW))
                        .append(Component.text(" | Date: $formattedDate").color(NamedTextColor.YELLOW))
                )
            }
        }
        
        if (muteEntry == null && banEntry == null && warnings.isEmpty() && kicks.isEmpty()) {
            sender.sendMessage(Component.text("No moderation history found").color(NamedTextColor.GREEN))
        }
        
        return true
    }
}
