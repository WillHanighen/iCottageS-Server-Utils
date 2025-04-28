package com.icottage.serverutils.iCottageSServerUtils.listeners

import com.icottage.serverutils.iCottageSServerUtils.combat.CombatManager
import org.bukkit.entity.Player
import org.bukkit.entity.Zombie
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

/**
 * Listener for combat-related events
 */
class CombatListener(
    private val plugin: JavaPlugin,
    private val combatManager: CombatManager
) : Listener {
    
    /**
     * Handle player-to-player damage to tag players in combat
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        // Check if this is PvP combat
        val damaged = event.entity
        val damager = event.damager
        
        // Always tag the damaged player if they're a player
        if (damaged is Player) {
            // Handle direct player damage
            if (damager is Player) {
                combatManager.tagPlayerCombat(damaged) // Tag victim
                combatManager.tagPlayerCombat(damager) // Tag attacker
                return
            }
            
            // Handle projectile damage (arrows, etc.)
            if (damager is org.bukkit.entity.Projectile) {
                val shooter = damager.shooter
                if (shooter is Player) {
                    combatManager.tagPlayerCombat(damaged) // Tag victim
                    combatManager.tagPlayerCombat(shooter) // Tag shooter
                    return
                }
            }
            
            // Handle indirect damage (TNT, etc.)
            if (damager is org.bukkit.entity.TNTPrimed || damager is org.bukkit.entity.minecart.ExplosiveMinecart) {
                val source = damager.getMetadata("source").firstOrNull()?.value()
                if (source is Player) {
                    combatManager.tagPlayerCombat(damaged) // Tag victim
                    combatManager.tagPlayerCombat(source) // Tag source player
                    return
                }
            }
            
            // Handle damage from entities owned by players (wolves, etc.)
            if (damager is org.bukkit.entity.Tameable && damager.isTamed) {
                val owner = damager.owner
                if (owner is Player) {
                    combatManager.tagPlayerCombat(damaged) // Tag victim
                    combatManager.tagPlayerCombat(owner) // Tag pet owner
                    return
                }
            }
        }
        
        // Check if a player is attacking an entity that belongs to another player
        if (damager is Player && damaged is org.bukkit.entity.Tameable && damaged.isTamed) {
            val owner = damaged.owner
            if (owner is Player && owner != damager) {
                combatManager.tagPlayerCombat(damager) // Tag attacker
                combatManager.tagPlayerCombat(owner) // Tag pet owner
                return
            }
        }
    }
    
    /**
     * Handle player quit during combat
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val playerUuid = player.uniqueId
        val playerName = player.name
        
        plugin.logger.info("Player $playerName (UUID: $playerUuid) is quitting, checking combat status")
        
        // Double check both combat tracking methods
        val inCombatMap = combatManager.isInCombat(player)
        val inCombatMeta = player.hasMetadata("in_combat")
        val inCombat = inCombatMap || inCombatMeta
        
        plugin.logger.info("Combat status for $playerName: inCombatMap=$inCombatMap, inCombatMeta=$inCombatMeta")
        
        // Force check if player is in combat and handle logout
        if (inCombat) {
            plugin.logger.info("Player $playerName is in combat and quitting, handling combat logout")
            
            // Call handleCombatLogout directly - we've fixed the implementation to work correctly
            combatManager.handleCombatLogout(player)
        } else {
            plugin.logger.info("Player $playerName is not in combat, allowing normal quit")
        }
    }
    
    /**
     * Handle player join to check for combat logger
     */
    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        plugin.logger.info("Player ${player.name} (UUID: ${player.uniqueId}) is joining, checking for combat logger")
        
        try {
            // Delay the reconnect handling to ensure the player is fully loaded
            plugin.server.scheduler.scheduleSyncDelayedTask(plugin, {
                combatManager.handlePlayerReconnect(player)
            }, 5L) // Wait 5 ticks (1/4 second) to ensure player is fully loaded
        } catch (e: Exception) {
            plugin.logger.severe("Error handling player reconnect for ${player.name}: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handle combat logger death
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        
        // Check if the entity is a combat logger
        if (entity is Zombie && entity.hasMetadata("combat_logger")) {
            plugin.logger.info("Combat logger zombie died: ${entity.uniqueId}")
            
            // Clear vanilla drops (rotten flesh, etc.)
            event.drops.clear()
            
            // Set experience to 0
            event.droppedExp = 0
            
            try {
                // Handle the combat logger death (this will drop the player's items)
                combatManager.handleCombatLoggerKilled(entity)
            } catch (e: Exception) {
                plugin.logger.severe("Error handling combat logger death: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
