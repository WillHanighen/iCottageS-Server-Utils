package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
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
    
    // Store a reference to the main scoreboard
    private val mainScoreboard = Bukkit.getScoreboardManager().mainScoreboard
    
    /**
     * Updates player display name and tab list on join
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        // Short delay to ensure everything is loaded properly
        val plugin = Bukkit.getPluginManager().getPlugin("iCottageServerUtils")
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                updatePlayerDisplayName(event.player)
                updateTabList()
            }, 5L) // 5 tick delay (1/4 second)
        } else {
            // Fallback if plugin reference can't be found
            updatePlayerDisplayName(event.player)
            updateTabList()
        }
    }
    
    /**
     * Updates a player's display name with their rank prefix
     */
    fun updatePlayerDisplayName(player: Player) {
        val rank = rankManager.getPlayerRank(player)
        
        // Create the display name with rank prefix: [Rank] Username
        val prefixComponent = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(rank.getPrefixComponent())
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
        
        val displayName = Component.empty()
            .append(prefixComponent)
            .append(Component.text(" "))
            .append(Component.text(player.name))
        
        // Set the player's display name
        player.displayName(displayName)
        player.playerListName(displayName)
        
        // Update the player's team for tab list sorting and nametag display
        setupPlayerTeam(player, rank.weight, prefixComponent)
    }
    
    /**
     * Updates the tab list for all players
     */
    fun updateTabList() {
        // Sort all online players by rank weight
        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.isEmpty()) return
        
        // Update each player's display name and team
        for (player in onlinePlayers) {
            updatePlayerDisplayName(player)
        }
    }
    
    /**
     * Sets up a player's team for tab list sorting and nametag display
     */
    private fun setupPlayerTeam(player: Player, weight: Int, prefixComponent: Component) {
        // Clean up any existing teams for this player first
        cleanupPlayerTeams(player)
        
        // Create a unique team name for this player based on their rank weight
        val weightPadded = String.format("%05d", 99999 - weight) // Invert for descending order
        val teamName = "w${weightPadded}_${player.name.take(10)}" // Limit name length to avoid issues
        
        // Create or get the team
        var team = mainScoreboard.getTeam(teamName)
        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName)
        }
        
        // Configure the team
        team.prefix(prefixComponent)
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.ALWAYS)
        
        // Add the player to the team
        team.addEntry(player.name)
    }
    
    /**
     * Removes a player from any existing teams
     */
    private fun cleanupPlayerTeams(player: Player) {
        // Remove player from any existing teams
        for (team in mainScoreboard.teams) {
            if (team.hasEntry(player.name)) {
                team.removeEntry(player.name)
            }
            
            // Clean up old teams that might be for this player
            if (team.name.endsWith("_${player.name.take(10)}")) {
                // Only remove the team if it has no entries
                if (team.entries.isEmpty()) {
                    try {
                        team.unregister()
                    } catch (e: Exception) {
                        // Ignore errors when trying to unregister teams
                    }
                }
            }
        }
    }
}
