package com.icottage.serverutils.iCottageSServerUtils.report

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Manages player reports for rule violations
 */
class ReportManager(private val plugin: JavaPlugin) {
    
    // Maps player UUIDs to their report cooldowns
    private val reportCooldowns = ConcurrentHashMap<UUID, Long>()
    
    // Default cooldown in seconds
    private val defaultCooldown = 60L
    
    // List of valid report reasons
    private val validReasons = listOf(
        "hacking", "cheating", "griefing", "spam", "harassment", 
        "inappropriate", "offensive", "threats", "advertising", "other"
    )
    
    // File to store reports
    private lateinit var reportsFile: File
    
    /**
     * Initialize the report manager
     */
    fun initialize() {
        // Create reports directory if it doesn't exist
        val reportsDir = File(plugin.dataFolder, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        
        // Create reports file
        reportsFile = File(reportsDir, "reports.txt")
        if (!reportsFile.exists()) {
            reportsFile.createNewFile()
        }
        
        plugin.logger.info("Report manager initialized")
    }
    
    /**
     * Submit a report
     */
    fun submitReport(reporter: Player, targetName: String, reason: String, details: String): Boolean {
        // Validate target player
        val target = Bukkit.getPlayer(targetName)
        
        if (target == null) {
            reporter.sendMessage(Component.text("Player $targetName is not online").color(NamedTextColor.RED))
            return false
        }
        
        if (target.uniqueId == reporter.uniqueId) {
            reporter.sendMessage(Component.text("You cannot report yourself").color(NamedTextColor.RED))
            return false
        }
        
        // Check if the reporter is on cooldown
        val currentTime = System.currentTimeMillis()
        val cooldownTime = reportCooldowns[reporter.uniqueId]
        
        if (cooldownTime != null && currentTime < cooldownTime) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(cooldownTime - currentTime)
            reporter.sendMessage(Component.text("You must wait $remainingSeconds seconds before submitting another report").color(NamedTextColor.RED))
            return false
        }
        
        // Validate reason
        val normalizedReason = reason.lowercase()
        if (!validReasons.contains(normalizedReason)) {
            reporter.sendMessage(
                Component.text("Invalid reason. Valid reasons are: ").color(NamedTextColor.RED)
                    .append(Component.text(validReasons.joinToString(", ")).color(NamedTextColor.GOLD))
            )
            return false
        }
        
        // Create the report
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
        val reportEntry = """
            |=== REPORT ===
            |Time: $timestamp
            |Reporter: ${reporter.name} (${reporter.uniqueId})
            |Target: ${target.name} (${target.uniqueId})
            |Reason: $normalizedReason
            |Details: $details
            |Location: World: ${target.world.name}, X: ${target.location.blockX}, Y: ${target.location.blockY}, Z: ${target.location.blockZ}
            |===============
            |
        """.trimMargin()
        
        // Save the report
        reportsFile.appendText(reportEntry)
        
        // Set cooldown
        reportCooldowns[reporter.uniqueId] = currentTime + TimeUnit.SECONDS.toMillis(defaultCooldown)
        
        // Notify the reporter
        reporter.sendMessage(Component.text("Your report against ${target.name} has been submitted").color(NamedTextColor.GREEN))
        
        // Notify online staff
        notifyStaff(reporter, target, normalizedReason)
        
        return true
    }
    
    /**
     * Notify online staff about a report
     */
    private fun notifyStaff(reporter: Player, target: Player, reason: String) {
        val staffMessage = Component.text("=== REPORT ===").color(NamedTextColor.RED)
            .append(Component.newline())
            .append(Component.text("Reporter: ").color(NamedTextColor.GOLD))
            .append(Component.text(reporter.name).color(NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("Target: ").color(NamedTextColor.GOLD))
            .append(Component.text(target.name).color(NamedTextColor.WHITE))
            .append(Component.newline())
            .append(Component.text("Reason: ").color(NamedTextColor.GOLD))
            .append(Component.text(reason).color(NamedTextColor.WHITE))
            .append(Component.newline())
            .append(
                Component.text("[Teleport to player]").color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/tp ${target.name}"))
            )
            .append(Component.newline())
            .append(Component.text("===============").color(NamedTextColor.RED))
        
        // Send to all players with the appropriate permission
        Bukkit.getOnlinePlayers().forEach { player ->
            if (player.hasPermission("icottage.report.receive")) {
                player.sendMessage(staffMessage)
            }
        }
    }
    
    /**
     * Get a list of valid report reasons
     */
    fun getValidReasons(): List<String> {
        return validReasons
    }
}
