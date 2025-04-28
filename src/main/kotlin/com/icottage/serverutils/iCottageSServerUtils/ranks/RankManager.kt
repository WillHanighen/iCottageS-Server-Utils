package com.icottage.serverutils.iCottageSServerUtils.ranks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiConsumer

/**
 * Manages player ranks and handles rank-related operations
 */
class RankManager(private val plugin: JavaPlugin) {
    private val ranks = mutableMapOf<String, Rank>()
    private val playerRanks = ConcurrentHashMap<UUID, String>()
    private val rankFile = File(plugin.dataFolder, "ranks.json")
    private val playerRankFile = File(plugin.dataFolder, "playerRanks.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    // Default rank for new players
    private var defaultRankName = "default"
    
    // Listeners for rank changes
    private var rankChangeListener: BiConsumer<Rank, Boolean>? = null
    private var playerRankChangeListener: BiConsumer<Player, String>? = null
    
    /**
     * Initializes the rank manager
     */
    fun initialize() {
        try {
            plugin.logger.info("Initializing rank manager...")
            plugin.dataFolder.mkdirs()
            loadDefaultRanks()
            loadRanks()
            loadPlayerRanks()
            plugin.logger.info("Rank manager initialized successfully")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize rank manager: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Creates default ranks if none exist
     */
    private fun loadDefaultRanks() {
        if (!rankFile.exists()) {
            val defaultRanks = listOf(
                Rank(
                    name = "default",
                    displayName = "&7Default",
                    prefix = "&7[Default]",
                    weight = 0
                ),
                Rank(
                    name = "vip",
                    displayName = "&aVIP",
                    prefix = "&a[VIP]",
                    weight = 10
                ),
                Rank(
                    name = "admin",
                    displayName = "&cAdmin",
                    prefix = "&c[Admin]",
                    weight = 100,
                    permissions = listOf("serverutils.admin")
                )
            )
            
            defaultRanks.forEach { ranks[it.name.lowercase()] = it }
            saveRanks()
        }
    }
    
    /**
     * Loads ranks from the JSON file
     */
    private fun loadRanks() {
        if (rankFile.exists()) {
            try {
                FileReader(rankFile).use { reader ->
                    val type = object : TypeToken<List<Rank>>() {}.type
                    val loadedRanks: List<Rank> = gson.fromJson(reader, type)
                    ranks.clear()
                    loadedRanks.forEach { ranks[it.name.lowercase()] = it }
                    plugin.logger.info("Loaded ${ranks.size} ranks")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load ranks: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Saves ranks to the JSON file
     */
    fun saveRanks() {
        try {
            FileWriter(rankFile).use { writer ->
                gson.toJson(ranks.values.toList(), writer)
            }
            plugin.logger.info("Saved ${ranks.size} ranks")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save ranks: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Loads player ranks from the JSON file
     */
    private fun loadPlayerRanks() {
        if (playerRankFile.exists()) {
            try {
                FileReader(playerRankFile).use { reader ->
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val loadedPlayerRanks: Map<String, String> = gson.fromJson(reader, type)
                    playerRanks.clear()
                    loadedPlayerRanks.forEach { (uuidStr, rankName) ->
                        try {
                            val uuid = UUID.fromString(uuidStr)
                            playerRanks[uuid] = rankName
                        } catch (e: IllegalArgumentException) {
                            plugin.logger.warning("Invalid UUID in player ranks: $uuidStr")
                        }
                    }
                    plugin.logger.info("Loaded ${playerRanks.size} player ranks")
                }
            } catch (e: Exception) {
                plugin.logger.severe("Failed to load player ranks: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Saves player ranks to the JSON file
     */
    fun savePlayerRanks() {
        try {
            val playerRanksMap = mutableMapOf<String, String>()
            playerRanks.forEach { (uuid, rankName) ->
                playerRanksMap[uuid.toString()] = rankName
            }
            
            FileWriter(playerRankFile).use { writer ->
                gson.toJson(playerRanksMap, writer)
            }
            plugin.logger.info("Saved ${playerRanks.size} player ranks")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to save player ranks: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Gets a rank by name
     */
    fun getRank(name: String): Rank? {
        return ranks[name.lowercase()]
    }
    
    /**
     * Gets all ranks
     */
    fun getAllRanks(): List<Rank> {
        return ranks.values.toList()
    }
    
    /**
     * Gets all ranks sorted by weight (highest first)
     */
    fun getAllRanksSorted(): List<Rank> {
        return ranks.values.sortedByDescending { it.weight }
    }
    
    /**
     * Adds or updates a rank
     */
    fun setRank(rank: Rank) {
        val isNew = !ranks.containsKey(rank.name.lowercase())
        ranks[rank.name.lowercase()] = rank
        saveRanks()
        
        // Notify listeners
        rankChangeListener?.accept(rank, isNew)
    }
    
    /**
     * Removes a rank
     */
    fun removeRank(name: String): Boolean {
        val removed = ranks.remove(name.lowercase()) != null
        if (removed) {
            saveRanks()
            // Update any players with this rank to default
            playerRanks.entries.removeIf { it.value.equals(name, ignoreCase = true) }
            savePlayerRanks()
        }
        return removed
    }
    
    /**
     * Sets the default rank name to assign to new players
     */
    fun setDefaultRankName(rankName: String) {
        if (rankName.isNotEmpty()) {
            defaultRankName = rankName
            plugin.logger.info("Set default rank to: $rankName")
        }
    }
    
    /**
     * Gets the default rank
     */
    private fun getDefaultRank(): Rank {
        // Try to get the configured default rank
        if (ranks.containsKey(defaultRankName)) {
            return ranks[defaultRankName]!!
        }
        
        // If the configured default rank doesn't exist, try to use the first rank
        if (ranks.isNotEmpty()) {
            return ranks.values.first()
        }
        
        // If no ranks exist, create a default rank
        val defaultRank = Rank("default", "&7Default", "&7", 0)
        ranks["default"] = defaultRank
        saveRanks()
        return defaultRank
    }
    
    /**
     * Gets a player's rank by UUID
     */
    fun getPlayerRankByUUID(uuid: UUID): Rank {
        val rankName = playerRanks[uuid]
        if (rankName != null) {
            return ranks[rankName] ?: getDefaultRank()
        }
        
        // If player doesn't have a rank, return the default rank
        return getDefaultRank()
    }
    
    /**
     * Gets a player's rank
     */
    fun getPlayerRank(player: Player): Rank {
        val rank = getPlayerRankByUUID(player.uniqueId)
        
        // If player doesn't have a rank, assign the default rank
        if (!playerRanks.containsKey(player.uniqueId)) {
            setPlayerRank(player, rank.name)
        }
        
        return rank
    }
    
    /**
     * Sets a player's rank
     */
    fun setPlayerRank(player: Player, rankName: String): Boolean {
        if (!ranks.containsKey(rankName)) {
            return false
        }
        
        playerRanks[player.uniqueId] = rankName
        savePlayerRanks()
        
        // Notify listener if set
        playerRankChangeListener?.accept(player, rankName)
        
        return true
    }
    
    /**
     * Removes a player's custom rank (sets to default)
     */
    fun removePlayerRank(player: Player) {
        playerRanks.remove(player.uniqueId)
        savePlayerRanks()
        
        // Notify listeners
        playerRankChangeListener?.accept(player, "default")
    }
    
    /**
     * Sets a listener for rank changes
     * @param listener BiConsumer that receives the rank and a boolean indicating if it's a new rank
     */
    fun setRankChangeListener(listener: BiConsumer<Rank, Boolean>) {
        this.rankChangeListener = listener
    }
    
    /**
     * Sets a listener for player rank changes
     * @param listener BiConsumer that receives the player and the rank name
     */
    fun setPlayerRankChangeListener(listener: BiConsumer<Player, String>) {
        this.playerRankChangeListener = listener
    }
}
