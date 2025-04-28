package com.icottage.serverutils.iCottageSServerUtils.sitting

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages player sitting functionality
 */
class SittingManager(private val plugin: JavaPlugin) {
    // Configuration options
    private val enabled: Boolean = plugin.config.getBoolean("sitting.enabled", true)
    private val allowStairs: Boolean = plugin.config.getBoolean("sitting.allow-stairs", true)
    private val allowCarpets: Boolean = plugin.config.getBoolean("sitting.allow-carpets", true)
    private val allowSlabs: Boolean = plugin.config.getBoolean("sitting.allow-slabs", true)
    private val allowInCombat: Boolean = plugin.config.getBoolean("sitting.allow-in-combat", false)
    // Map of player UUIDs to their seat (armor stand)
    private val sittingPlayers = ConcurrentHashMap<UUID, ArmorStand>()
    
    // Map of armor stand UUIDs to player UUIDs
    private val seatToPlayer = ConcurrentHashMap<UUID, UUID>()
    
    // Task ID for the look direction update task
    private var lookUpdateTaskId: Int = -1
    
    /**
     * Check if a player is currently sitting
     */
    fun isPlayerSitting(player: Player): Boolean {
        return sittingPlayers.containsKey(player.uniqueId)
    }
    
    /**
     * Make a player sit at the specified location
     */
    fun sitPlayer(player: Player, location: Location): Boolean {
        // Check if sitting is enabled
        if (!enabled) {
            return false
        }
        
        // Don't allow sitting if the player is already sitting
        if (isPlayerSitting(player)) {
            return false
        }
        
        // Check if player is in combat and it's not allowed
        if (!allowInCombat && player.hasMetadata("in_combat")) {
            return false
        }
        
        // Create a seat (invisible armor stand)
        val seat = createSeat(location)
        if (seat != null) {
            // Add player to sitting players map
            sittingPlayers[player.uniqueId] = seat
            seatToPlayer[seat.uniqueId] = player.uniqueId
            
            // Make player sit on the armor stand
            seat.addPassenger(player)
            return true
        }
        
        return false
    }
    
    /**
     * Make a player stand up
     */
    fun standPlayer(player: Player) {
        val seat = sittingPlayers[player.uniqueId]
        if (seat != null) {
            // Remove player from seat
            seat.removePassenger(player)
            
            // Remove from maps
            sittingPlayers.remove(player.uniqueId)
            seatToPlayer.remove(seat.uniqueId)
            
            // Remove the seat
            seat.remove()
        }
    }
    
    /**
     * Stand up all sitting players
     */
    fun standAllPlayers() {
        val players = sittingPlayers.keys.toList()
        for (playerId in players) {
            val player = plugin.server.getPlayer(playerId)
            if (player != null) {
                standPlayer(player)
            }
        }
        
        // Cancel the look update task when plugin is disabled
        if (lookUpdateTaskId != -1) {
            plugin.server.scheduler.cancelTask(lookUpdateTaskId)
            lookUpdateTaskId = -1
        }
    }
    
    /**
     * Get the player associated with a seat
     */
    fun getPlayerBySeat(seat: ArmorStand): Player? {
        val playerId = seatToPlayer[seat.uniqueId] ?: return null
        return plugin.server.getPlayer(playerId)
    }
    
    /**
     * Initialize the sitting manager
     */
    fun initialize() {
        // Start a task to update player look directions while sitting
        lookUpdateTaskId = plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, Runnable {
            updatePlayerLookDirections()
        }, 1L, 1L) // Update every tick for smooth head movement
        
        plugin.logger.info("Sitting manager initialized")
    }
    
    /**
     * Update the look direction of all sitting players
     */
    private fun updatePlayerLookDirections() {
        for ((playerUuid, seat) in sittingPlayers) {
            val player = plugin.server.getPlayer(playerUuid) ?: continue
            
            // Get the block the player is sitting on
            val block = seat.location.block
            val blockType = block.type.name
            
            if (blockType.endsWith("_STAIRS")) {
                // For stairs: Keep the fixed forward direction (don't update rotation)
                // The yaw was set when the player first sat down based on stair orientation
                // Do nothing here - keep the original orientation
            } else {
                // For carpets and slabs: Update both yaw and pitch to match player's head
                seat.setRotation(player.location.yaw, player.location.pitch)
            }
        }
    }
    
    /**
     * Create a seat (invisible armor stand) at the specified location
     */
    private fun createSeat(location: Location): ArmorStand? {
        // Create a new location with adjusted Y position for sitting
        val seatLocation = location.clone()
        
        // Default yaw (player's current direction)
        var yaw = location.yaw
        
        // Adjust position based on block type
        when (location.block.type) {
            // Stairs - position in the middle of the block, slightly lower
            Material.ACACIA_STAIRS, Material.BIRCH_STAIRS, Material.DARK_OAK_STAIRS,
            Material.JUNGLE_STAIRS, Material.OAK_STAIRS, Material.SPRUCE_STAIRS,
            Material.STONE_STAIRS, Material.COBBLESTONE_STAIRS, Material.BRICK_STAIRS,
            Material.SANDSTONE_STAIRS, Material.RED_SANDSTONE_STAIRS, Material.NETHER_BRICK_STAIRS,
            Material.QUARTZ_STAIRS, Material.PURPUR_STAIRS, Material.PRISMARINE_STAIRS,
            Material.PRISMARINE_BRICK_STAIRS, Material.DARK_PRISMARINE_STAIRS,
            Material.CRIMSON_STAIRS, Material.WARPED_STAIRS, Material.BLACKSTONE_STAIRS,
            Material.POLISHED_BLACKSTONE_STAIRS, Material.POLISHED_BLACKSTONE_BRICK_STAIRS,
            Material.GRANITE_STAIRS, Material.DIORITE_STAIRS, Material.ANDESITE_STAIRS,
            Material.RED_NETHER_BRICK_STAIRS, Material.MOSSY_STONE_BRICK_STAIRS,
            Material.MOSSY_COBBLESTONE_STAIRS, Material.END_STONE_BRICK_STAIRS,
            Material.STONE_BRICK_STAIRS, Material.SMOOTH_SANDSTONE_STAIRS,
            Material.SMOOTH_RED_SANDSTONE_STAIRS, Material.SMOOTH_QUARTZ_STAIRS,
            Material.POLISHED_GRANITE_STAIRS, Material.POLISHED_DIORITE_STAIRS,
            Material.POLISHED_ANDESITE_STAIRS, Material.CUT_COPPER_STAIRS,
            Material.EXPOSED_CUT_COPPER_STAIRS, Material.WEATHERED_CUT_COPPER_STAIRS,
            Material.OXIDIZED_CUT_COPPER_STAIRS, Material.WAXED_CUT_COPPER_STAIRS,
            Material.WAXED_EXPOSED_CUT_COPPER_STAIRS, Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,
            Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Material.COBBLED_DEEPSLATE_STAIRS,
            Material.POLISHED_DEEPSLATE_STAIRS, Material.DEEPSLATE_BRICK_STAIRS,
            Material.DEEPSLATE_TILE_STAIRS, Material.MANGROVE_STAIRS,
            Material.MUD_BRICK_STAIRS, Material.BAMBOO_STAIRS, Material.BAMBOO_MOSAIC_STAIRS,
            Material.CHERRY_STAIRS -> {
                // Check if stairs are allowed
                if (!allowStairs) return null
                
                // Get stair data to determine orientation
                val blockData = location.block.blockData
                if (blockData is org.bukkit.block.data.Directional) {
                    // Set the yaw based on the stair direction
                    when (blockData.facing) {
                        org.bukkit.block.BlockFace.NORTH -> yaw = 0f
                        org.bukkit.block.BlockFace.EAST -> yaw = 90f
                        org.bukkit.block.BlockFace.SOUTH -> yaw = 180f
                        org.bukkit.block.BlockFace.WEST -> yaw = 270f
                        else -> {} // Keep default yaw
                    }
                }
                
                seatLocation.x += 0.5
                seatLocation.y += 0.5
                seatLocation.z += 0.5
            }
            
            // Carpets - position in the middle of the block, lower
            Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET,
            Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET,
            Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET,
            Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
            Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET,
            Material.BLACK_CARPET -> {
                // Check if carpets are allowed
                if (!allowCarpets) return null
                seatLocation.x += 0.5
                seatLocation.y += 0.0
                seatLocation.z += 0.5
            }
            
            // Slabs - position in the middle of the block, slightly lower
            Material.STONE_SLAB, Material.SMOOTH_STONE_SLAB, Material.SANDSTONE_SLAB,
            Material.CUT_SANDSTONE_SLAB, Material.COBBLESTONE_SLAB, Material.BRICK_SLAB,
            Material.STONE_BRICK_SLAB, Material.NETHER_BRICK_SLAB, Material.QUARTZ_SLAB,
            Material.RED_SANDSTONE_SLAB, Material.CUT_RED_SANDSTONE_SLAB, Material.PURPUR_SLAB,
            Material.PRISMARINE_SLAB, Material.PRISMARINE_BRICK_SLAB, Material.DARK_PRISMARINE_SLAB,
            Material.OAK_SLAB, Material.SPRUCE_SLAB, Material.BIRCH_SLAB, Material.JUNGLE_SLAB,
            Material.ACACIA_SLAB, Material.DARK_OAK_SLAB, Material.CRIMSON_SLAB, Material.WARPED_SLAB,
            Material.BLACKSTONE_SLAB, Material.POLISHED_BLACKSTONE_SLAB, Material.POLISHED_BLACKSTONE_BRICK_SLAB,
            Material.CUT_COPPER_SLAB, Material.EXPOSED_CUT_COPPER_SLAB, Material.WEATHERED_CUT_COPPER_SLAB,
            Material.OXIDIZED_CUT_COPPER_SLAB, Material.WAXED_CUT_COPPER_SLAB,
            Material.WAXED_EXPOSED_CUT_COPPER_SLAB, Material.WAXED_WEATHERED_CUT_COPPER_SLAB,
            Material.WAXED_OXIDIZED_CUT_COPPER_SLAB, Material.COBBLED_DEEPSLATE_SLAB,
            Material.POLISHED_DEEPSLATE_SLAB, Material.DEEPSLATE_BRICK_SLAB, Material.DEEPSLATE_TILE_SLAB,
            Material.MANGROVE_SLAB, Material.MUD_BRICK_SLAB, Material.BAMBOO_SLAB, Material.BAMBOO_MOSAIC_SLAB,
            Material.CHERRY_SLAB -> {
                // Check if slabs are allowed
                if (!allowSlabs) return null
                // Check if it's a bottom slab
                val data = location.block.blockData
                if (data is org.bukkit.block.data.type.Slab && 
                    data.type == org.bukkit.block.data.type.Slab.Type.BOTTOM) {
                    seatLocation.x += 0.5
                    seatLocation.y += 0.5
                    seatLocation.z += 0.5
                } else {
                    // Not a bottom slab, don't allow sitting
                    return null
                }
            }
            
            // Not a sittable block
            else -> return null
        }
        
        // Create the armor stand with the correct yaw
        seatLocation.yaw = yaw
        val seat = location.world.spawnEntity(seatLocation, EntityType.ARMOR_STAND) as ArmorStand
        
        // Configure the armor stand to be invisible and not interact with the world
        seat.isVisible = false
        seat.setGravity(false)
        seat.isMarker = true
        seat.isInvulnerable = true
        seat.canPickupItems = false
        seat.isPersistent = false
        seat.setBasePlate(false)
        seat.isCustomNameVisible = false
        
        // Try to minimize the steed hearts display
        try {
            // Set the armor stand's health to minimum
            seat.setHealth(20.0)
            
            // Make the armor stand invisible to prevent UI elements
            seat.isInvisible = true
        } catch (e: Exception) {
            plugin.logger.warning("Failed to hide steed hearts: ${e.message}")
        }
        
        return seat
    }
}
