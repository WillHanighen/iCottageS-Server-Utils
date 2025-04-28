package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Team

/**
 * Listener for player display name and tab list formatting
 */
class RankDisplayListener(private val rankManager: RankManager) : Listener {
    
    /**
     * Updates player display name and tab list on join
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        updatePlayerDisplayName(event.player)
        updateTabList()
    }
    
    /**
     * Updates a player's display name with their rank prefix
     */
    fun updatePlayerDisplayName(player: Player) {
        val rank = rankManager.getPlayerRank(player)
        
        // Create the display name with rank prefix: [Rank] Username
        val displayName = Component.empty()
            .append(rank.getPrefixComponent())
            .append(Component.text(" "))
            .append(Component.text(player.name))
        
        // Set the player's display name
        player.displayName(displayName)
        player.playerListName(displayName)
        
        // Update the player's team for tab list sorting
        updatePlayerTeam(player, rank.weight)
    }
    
    /**
     * Updates the tab list for all players
     */
    fun updateTabList() {
        // Sort all online players by rank weight
        // Get the server instance from any online player, or return if no players are online
        val onlinePlayers = org.bukkit.Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return
        
        val server = org.bukkit.Bukkit.getServer()
        val players = rankManager.getAllRanksSorted().flatMap { rank ->
            server.onlinePlayers.filter { 
                rankManager.getPlayerRank(it).name.equals(rank.name, ignoreCase = true) 
            }.sortedBy { it.name }
        }
        
        // Update each player's team for tab list sorting
        players.forEach { player ->
            val rank = rankManager.getPlayerRank(player)
            updatePlayerTeam(player, rank.weight)
        }
    }
    
    /**
     * Updates a player's team for tab list sorting
     */
    private fun updatePlayerTeam(player: Player, weight: Int) {
        val scoreboard = player.server.scoreboardManager.mainScoreboard
        
        // Create a team name with weight for sorting (higher weights first)
        // Format: w{weightPadded}_{playerName}
        // Padding ensures correct alphabetical sorting
        val weightPadded = String.format("%05d", 99999 - weight) // Invert for descending order
        val teamName = "w${weightPadded}_${player.name}"
        
        // Remove player from any existing teams
        for (team in scoreboard.teams) {
            if (team.hasEntry(player.name)) {
                team.removeEntry(player.name)
            }
        }
        
        // Get or create the team
        var team = scoreboard.getTeam(teamName)
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName)
        }
        
        // Add player to the team
        team.addEntry(player.name)
        
        // Set team prefix to rank prefix
        val rank = rankManager.getPlayerRank(player)
        // Use the Component-based approach for the prefix
        val prefixComponent = rank.getPrefixComponent().append(Component.text(" "))
        team.prefix(prefixComponent)
    }
}
