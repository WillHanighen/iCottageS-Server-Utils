package com.icottage.serverutils.iCottageSServerUtils.listeners

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

/**
 * Listener for customizing player death messages with wittier alternatives
 */
class DeathMessageListener(private val plugin: JavaPlugin) : Listener {
    
    // Random instance for selecting random messages
    private val random = Random()
    
    // Map of death types to custom messages
    private val customDeathMessages = mapOf(
        // Arrow deaths
        "arrow" to listOf(
            "{player} was turned into a pincushion by {killer}",
            "{player} found out {killer} never misses leg day... or head day",
            "{player} learned that {killer}'s aim is deadly accurate",
            "{player} should have zigzagged to avoid {killer}'s arrows",
            "{player} tried to catch {killer}'s arrow with their face"
        ),
        
        // Cactus deaths
        "cactus" to listOf(
            "{player} hugged a cactus. It wasn't friendly",
            "{player} found out cacti aren't cuddly",
            "{player} got the point... many points actually",
            "{player} mistook a cactus for a friendly NPC",
            "{player} tried to high-five a cactus while running from {killer}"
        ),
        
        // Campfire/fire deaths
        "campfire" to listOf(
            "{player} became a human marshmallow",
            "{player} found out they're flammable",
            "{player} forgot to stop, drop, and roll",
            "{player} got too close to the campfire stories",
            "{player} tried to become the Human Torch while escaping {killer}"
        ),
        
        // Cramming deaths
        "cramming" to listOf(
            "{player} found out personal space is important",
            "{player} was squished like a bug",
            "{player} should have taken the stairs instead",
            "{player} was in the world's worst group hug",
            "{player} was compressed into a diamond by {killer}"
        ),
        
        // Dragon breath deaths
        "dragon_breath" to listOf(
            "{player} found out dragons don't use mouthwash",
            "{player} got a whiff of the ender dragon's morning breath",
            "{player} learned dragons don't believe in personal space",
            "{player} discovered dragon breath isn't just a cocktail",
            "{player} was mouthwashed away by the ender dragon's halitosis"
        ),
        
        // Drowning deaths
        "drown" to listOf(
            "{player} forgot they weren't a fish",
            "{player} forgot to drink a water breathing potion",
            "{player} should have packed scuba gear",
            "{player} discovered breathing underwater isn't a feature",
            "{player} went swimming with concrete shoes thanks to {killer}"
        ),
        
        // Dehydration deaths
        "dry_out" to listOf(
            "{player} forgot to stay hydrated",
            "{player} turned into human jerky",
            "{player} should have packed more water bottles",
            "{player} needs to drink more water",
            "{player} became a raisin while running from {killer}"
        ),
        
        // Ender pearl deaths
        "ender_pearl" to listOf(
            "{player} teleported into the ground",
            "{player}'s ender pearl had a slight calibration issue",
            "{player} should have read the ender pearl safety manual",
            "{player} teleported with style... and fatal velocity",
            "{player} tried to escape {killer} with an ender pearl. It didn't end well"
        ),
        
        // Explosion deaths
        "explosion" to listOf(
            "{player} went boom",
            "{player} was scattered across several chunks",
            "{player} experienced rapid unplanned disassembly",
            "{player} found out TNT is not a toy",
            "{player} was blown to smithereens by {killer}"
        ),
        
        // Fall deaths
        "fall" to listOf(
            "{player} believed they could fly",
            "{player} forgot about gravity",
            "{player} didn't stick the landing",
            "{player} tried parkour without a license",
            "{player} jumped from too high while escaping {killer}"
        ),
        
        // Falling anvil deaths
        "falling_anvil" to listOf(
            "{player} was flattened like a cartoon character",
            "{player} looked up at the wrong moment",
            "{player} learned about the dangers of physics",
            "{player} was anvil-ated",
            "{player} got a splitting headache from {killer}'s anvil"
        ),
        
        // Falling block deaths
        "falling_block" to listOf(
            "{player} should have worn a hard hat",
            "{player} was in the wrong place at the wrong time",
            "{player} was crushed by the weight of their decisions... and a block",
            "{player} discovered gravity works on blocks too",
            "{player} was squashed flat while fighting {killer}"
        ),
        
        // Falling stalactite deaths
        "falling_stalactite" to listOf(
            "{player} looked up in a cave at the wrong time",
            "{player} was impaled by nature's icicles",
            "{player} discovered stalactites are pointy... the hard way",
            "{player} should have brought an umbrella to the cave",
            "{player} was skewered like a kebab while fighting {killer}"
        ),
        
        // Fireball deaths
        "fireball" to listOf(
            "{player} caught a fireball with their face",
            "{player} was roasted like a marshmallow",
            "{player} couldn't dodge {killer}'s spicy meatball",
            "{player} found out fireballs aren't friendly",
            "{player} was turned into charcoal by {killer}"
        ),
        
        // Firework deaths
        "fireworks" to listOf(
            "{player} took a firework to the face",
            "{player} celebrated too hard",
            "{player} had an explosive personality",
            "{player} was the grand finale",
            "{player} was turned into a firework show by {killer}"
        ),
        
        // Elytra crash deaths
        "fly_into_wall" to listOf(
            "{player} forgot walls exist",
            "{player} needed flying lessons",
            "{player} should have installed brakes",
            "{player} wasn't cleared for landing",
            "{player} crashed and burned while trying to escape {killer}"
        ),
        
        // Freeze deaths
        "freeze" to listOf(
            "{player} became a popsicle",
            "{player} should have packed a sweater",
            "{player} learned the hard way that hypothermia is no joke",
            "{player} turned into an ice sculpture",
            "{player} was frozen solid by {killer}"
        ),
        
        // Generic deaths
        "generic" to listOf(
            "{player} died in mysterious circumstances",
            "{player} shuffled off this mortal coil",
            "{player} kicked the bucket",
            "{player} bit the dust",
            "{player} met their maker thanks to {killer}"
        ),
        
        // Generic kill deaths
        "generic_kill" to listOf(
            "{player} was eliminated",
            "{player} was taken out",
            "{player} was removed from the equation",
            "{player} was defeated soundly",
            "{player} was utterly destroyed by {killer}"
        ),
        
        // Hot floor deaths
        "hot_floor" to listOf(
            "{player} did the hot foot dance on magma blocks",
            "{player} discovered that magma blocks aren't good dance floors",
            "{player} needed asbestos boots for walking on magma",
            "{player} played 'the floor is lava' for real and lost",
            "{player} forgot to wear fire protection boots while escaping {killer}"
        ),
        
        // In fire deaths (standing in fire)
        "in_fire" to listOf(
            "{player} stood in an open flame for too long",
            "{player} tried to become a human torch",
            "{player} thought they were fireproof",
            "{player} couldn't resist the warmth of an open flame",
            "{player} was roasted like a marshmallow while fighting {killer}"
        ),
        
        // Suffocation deaths
        "in_wall" to listOf(
            "{player} tried to become one with the wall",
            "{player} forgot they need air to breathe",
            "{player} got stuck between a rock and a hard place",
            "{player} should have brought a pickaxe",
            "{player} was entombed alive while fighting {killer}"
        ),
        
        // Indirect magic deaths
        "indirect_magic" to listOf(
            "{player} was hexed into oblivion by {killer}",
            "{player} was cursed by {killer}'s magical prowess",
            "{player} found out {killer}'s magic is the real deal",
            "{player} was transformed into a corpse by {killer}'s spell",
            "{player} failed their saving throw against {killer}'s magic"
        ),
        
        // Lava deaths (swimming in lava)
        "lava" to listOf(
            "{player} took a bath in spicy orange juice",
            "{player} tried swimming in molten rock",
            "{player} thought lava was just spicy water",
            "{player} discovered that lava isn't a good hot tub replacement",
            "{player} dove into a lava pool while escaping from {killer}"
        ),
        
        // Lightning deaths
        "lightning_bolt" to listOf(
            "{player} was thunderstruck",
            "{player} was in the wrong place at the wrong storm",
            "{player} was shockingly unlucky",
            "{player} didn't know they were a lightning rod",
            "{player} was electrified while fighting {killer}"
        ),
        
        // Mace smash deaths
        "mace_smash" to listOf(
            "{player} was turned into a pancake by {killer}'s mace",
            "{player} felt the full weight of {killer}'s argument",
            "{player} was on the receiving end of {killer}'s home run",
            "{player} was hammered down by {killer}",
            "{player} was flattened by {killer}'s mace"
        ),
        
        // Magic deaths
        "magic" to listOf(
            "{player} was magicked out of existence",
            "{player} didn't believe in magic... until now",
            "{player} was abracadabra'd to death",
            "{player} vanished in a puff of smoke",
            "{player} was enchanted to death while running from {killer}"
        ),
        
        // Mob attack deaths
        "mob_attack" to listOf(
            "{player} was torn apart by {killer}",
            "{player} was mauled by {killer}",
            "{player} was no match for {killer}",
            "{player} underestimated {killer}'s power",
            "{player} was sliced and diced by {killer}"
        ),
        
        // Mob attack no aggro deaths
        "mob_attack_no_aggro" to listOf(
            "{player} was ambushed by {killer}",
            "{player} didn't see {killer} coming",
            "{player} was caught off guard by {killer}",
            "{player} was surprised by {killer}'s sudden attack",
            "{player} was blindsided by {killer}"
        ),
        
        // Mob projectile deaths
        "mob_projectile" to listOf(
            "{player} couldn't dodge {killer}'s projectile",
            "{player} was sniped by {killer}",
            "{player} was hit by {killer}'s long-range attack",
            "{player} was struck down from afar by {killer}",
            "{player} was shot by {killer}'s ranged attack"
        ),
        
        // On fire deaths (burning to death after being set on fire)
        "on_fire" to listOf(
            "{player} burned to a crisp after being set ablaze",
            "{player} couldn't put themselves out in time",
            "{player} ran around screaming 'I'M ON FIRE!' until they weren't",
            "{player} should have jumped in water after catching fire",
            "{player} was turned into charcoal after {killer} set them ablaze"
        ),
        
        // Void deaths
        "out_of_world" to listOf(
            "{player} fell into the endless abyss",
            "{player} discovered the world has an edge",
            "{player} found out what's below bedrock",
            "{player} took a one-way trip to the void",
            "{player} was pushed into nothingness by {killer}"
        ),
        
        // Border deaths
        "outside_border" to listOf(
            "{player} tried to explore beyond the world border",
            "{player} went where no player should go",
            "{player} found out the hard way that boundaries exist",
            "{player} was erased by the edge of reality",
            "{player} was forced out of bounds by {killer}"
        ),
        
        // Player attack deaths
        "player_attack" to listOf(
            "{player} was demolished by {killer}",
            "{player} was no match for {killer}'s combat skills",
            "{player} was schooled in PvP by {killer}",
            "{player} was outplayed by {killer}",
            "{player} was sent to respawn by {killer}"
        ),
        
        // Player explosion deaths
        "player_explosion" to listOf(
            "{player} was blown to smithereens by {killer}",
            "{player} was turned into confetti by {killer}",
            "{player} was scattered across the landscape by {killer}",
            "{player} was exploded into tiny bits by {killer}",
            "{player} was detonated by {killer}"
        ),
        
        // Sonic boom deaths
        "sonic_boom" to listOf(
            "{player} was blasted by sound waves",
            "{player} experienced the loudest concert ever",
            "{player} should have worn ear protection",
            "{player} was vibrated into pieces",
            "{player} was sonically obliterated while running from {killer}"
        ),
        
        // Spit deaths
        "spit" to listOf(
            "{player} was dissolved by {killer}'s acidic spit",
            "{player} was corroded by {killer}'s saliva",
            "{player} was melted by {killer}'s disgusting projectile",
            "{player} was liquefied by {killer}'s spit",
            "{player} was spat on with extreme prejudice by {killer}"
        ),
        
        // Stalagmite deaths
        "stalagmite" to listOf(
            "{player} was impaled on nature's spikes",
            "{player} fell onto a very pointy rock",
            "{player} discovered stalagmites are sharp... the hard way",
            "{player} was kebab'd by a stalagmite",
            "{player} was skewered like a shish kebab while fighting {killer}"
        ),
        
        // Starvation deaths
        "starve" to listOf(
            "{player} forgot to eat",
            "{player} should have packed a lunch",
            "{player} wasted away to nothing",
            "{player} needed a Snickers",
            "{player} was too busy fighting {killer} to eat"
        ),
        
        // Sting deaths
        "sting" to listOf(
            "{player} was stung into oblivion",
            "{player} found out they're allergic to stingers",
            "{player} was pricked one too many times",
            "{player} was stung like a voodoo doll",
            "{player} was turned into a pincushion by {killer}'s stinger"
        ),
        
        // Sweet berry bush deaths
        "sweet_berry_bush" to listOf(
            "{player} was poked to death by deceptively sweet berries",
            "{player} found out berry bushes fight back",
            "{player} should have worn gardening gloves",
            "{player} was hugged too tightly by a berry bush",
            "{player} was turned into a berry smoothie while escaping {killer}"
        ),
        
        // Thorns deaths
        "thorns" to listOf(
            "{player} was killed by their own aggression",
            "{player} learned that karma is spiky",
            "{player} hurt themselves in confusion",
            "{player} was defeated by {killer}'s prickly defense",
            "{player} was hoisted by their own petard"
        ),
        
        // Thrown deaths
        "thrown" to listOf(
            "{player} was yeeted into the afterlife by {killer}",
            "{player} was tossed aside like yesterday's trash by {killer}",
            "{player} was thrown for a loop by {killer}",
            "{player} was discarded by {killer}",
            "{player} was launched into orbit by {killer}"
        ),
        
        // Trident deaths
        "trident" to listOf(
            "{player} was skewered by {killer}'s trident",
            "{player} was harpooned like a fish by {killer}",
            "{player} was forked over by {killer}",
            "{player} was turned into a shish kebab by {killer}",
            "{player} was impaled by {killer}'s three-pronged fury"
        ),
        
        // Unattributed fireball deaths
        "unattributed_fireball" to listOf(
            "{player} was toasted by a mystery fireball",
            "{player} was hit by a fireball from nowhere",
            "{player} was roasted by an anonymous flame",
            "{player} was cooked by a stray fireball",
            "{player} was flame-broiled while fighting {killer}"
        ),
        
        // Wind charge deaths
        "wind_charge" to listOf(
            "{player} was blown away by {killer}",
            "{player} was swept off their feet by {killer}",
            "{player} couldn't stand against {killer}'s wind",
            "{player} was launched into the air by {killer}",
            "{player} was carried away by {killer}'s gust"
        ),
        
        // Wither deaths
        "wither" to listOf(
            "{player} withered into dust",
            "{player} decayed into nothingness",
            "{player} rotted away",
            "{player} disintegrated slowly and painfully",
            "{player} withered away while fighting {killer}"
        ),
        
        // Wither skull deaths
        "wither_skull" to listOf(
            "{player} took a wither skull to the face from {killer}",
            "{player} was blasted by {killer}'s skull projectile",
            "{player} couldn't dodge {killer}'s skull",
            "{player} was withered away by {killer}'s skull",
            "{player} was decimated by {killer}'s wither skull"
        )
    )
    
    /**
     * Handle player death events to customize death messages
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val deathMessage = event.deathMessage() ?: return
        val player = event.entity
        val killer = player.killer
        val deathType = determineDeathType(event)
        
        // Get custom messages for this death type
        val customMessages = customDeathMessages[deathType] ?: return
        
        // Filter messages based on whether there's a killer or not
        val appropriateMessages = customMessages.filter { message ->
            // If there's a killer, we can use any message
            // If there's no killer, we should only use messages without {killer}
            killer != null || !message.contains("{killer}")
        }
        
        if (appropriateMessages.isEmpty()) {
            // Fallback to generic if no appropriate messages
            return
        }
        
        // Select a random message from the filtered list
        val randomMessage = appropriateMessages[random.nextInt(appropriateMessages.size)]
        
        // Replace placeholders
        val formattedMessage = formatDeathMessage(randomMessage, player, killer, player.inventory.itemInMainHand)
        
        // Set the custom death message
        event.deathMessage(formattedMessage)
        
        // Log the death type and message for debugging
        plugin.logger.info("Death type: $deathType, Original message: ${deathMessage.toString()}, Custom message: $randomMessage")
    }
    
    /**
     * Determine the type of death from the death event
     */
    private fun determineDeathType(event: PlayerDeathEvent): String {
        val deathMessage = event.deathMessage().toString().lowercase()
        val lastDamageCause = event.entity.lastDamageCause?.cause?.name?.lowercase() ?: ""
        
        // Log the damage information for debugging
        plugin.logger.info("Death cause: $lastDamageCause, Message: $deathMessage")
        
        // First check the damage cause, which is more reliable
        val typeFromCause = when (lastDamageCause) {
            "fire" -> "in_fire"
            "fire_tick" -> "on_fire"
            "lava" -> "lava"
            "hot_floor" -> "hot_floor"
            "magma_block" -> "hot_floor"
            "campfire" -> "campfire"
            "fall" -> if (deathMessage.contains("ender pearl")) "ender_pearl" else "fall"
            "drowning" -> "drown"
            "dryout" -> "dry_out"
            "entity_attack" -> {
                when {
                    deathMessage.contains("mace") || deathMessage.contains("smashed") -> "mace_smash"
                    deathMessage.contains("sting") -> "sting"
                    deathMessage.contains("trident") || deathMessage.contains("impaled") -> "trident"
                    deathMessage.contains("thrown") || deathMessage.contains("pummeled") -> "thrown"
                    deathMessage.contains("slain") && event.entity.killer != null -> "player_attack"
                    deathMessage.contains("slain") -> "mob_attack"
                    else -> "mob_attack"
                }
            }
            "projectile" -> {
                when {
                    deathMessage.contains("shot") && deathMessage.contains("skull") -> "wither_skull"
                    deathMessage.contains("shot") -> "arrow"
                    deathMessage.contains("spit") -> "spit"
                    else -> "mob_projectile"
                }
            }
            "entity_explosion" -> {
                if (deathMessage.contains("firework") || deathMessage.contains("bang")) {
                    "fireworks"
                } else {
                    "explosion"
                }
            }
            "block_explosion" -> "explosion"
            "suffocation" -> "in_wall"
            "void" -> "out_of_world"
            "lightning" -> "lightning_bolt"
            "starvation" -> "starve"
            "thorns" -> "thorns"
            "dragon_breath" -> "dragon_breath"
            "fly_into_wall" -> "fly_into_wall"
            "freeze" -> "freeze"
            "falling_block" -> "falling_block"
            "falling_stalactite" -> "falling_stalactite"
            "wither" -> "wither"
            "magic" -> "magic"
            "contact" -> if (deathMessage.contains("berry")) "sweet_berry_bush" else "cactus"
            else -> ""
        }
        
        if (typeFromCause.isNotEmpty()) {
            return typeFromCause
        }
        
        // Fallback to message-based detection if damage cause doesn't give us enough info
        return when {
            deathMessage.contains("shot") && deathMessage.contains("skull") -> "wither_skull"
            deathMessage.contains("shot") && !deathMessage.contains("skull") -> "arrow"
            deathMessage.contains("pricked") || deathMessage.contains("cactus") -> "cactus"
            deathMessage.contains("flames") && !deathMessage.contains("went off") -> "in_fire"
            deathMessage.contains("campfire") || deathMessage.contains("went up in flames") -> "campfire"
            deathMessage.contains("squished") -> "cramming"
            deathMessage.contains("dragon") -> "dragon_breath"
            deathMessage.contains("drowned") -> "drown"
            deathMessage.contains("dehydration") -> "dry_out"
            deathMessage.contains("ender pearl") -> "ender_pearl"
            deathMessage.contains("ground") && deathMessage.contains("hard") -> "fall"
            deathMessage.contains("blew up") || deathMessage.contains("blown up") -> "explosion"
            deathMessage.contains("anvil") -> "falling_anvil"
            deathMessage.contains("falling block") -> "falling_block"
            deathMessage.contains("stalactite") -> "falling_stalactite"
            deathMessage.contains("fireball") -> "fireball"
            deathMessage.contains("firework") || deathMessage.contains("bang") -> "fireworks"
            deathMessage.contains("kinetic") -> "fly_into_wall"
            deathMessage.contains("froze") -> "freeze"
            deathMessage.contains("lava") -> "lava"
            deathMessage.contains("lightning") -> "lightning_bolt"
            deathMessage.contains("smashed") || deathMessage.contains("mace") -> "mace_smash"
            deathMessage.contains("magic") -> "magic"
            deathMessage.contains("slain") && event.entity.killer is Player -> "player_attack"
            deathMessage.contains("slain") -> "mob_attack"
            deathMessage.contains("sonic") -> "sonic_boom"
            deathMessage.contains("stalagmite") -> "stalagmite"
            deathMessage.contains("starved") -> "starve"
            deathMessage.contains("stung") -> "sting"
            deathMessage.contains("spit") -> "spit"
            deathMessage.contains("berry") -> "sweet_berry_bush"
            deathMessage.contains("trying to hurt") -> "thorns"
            deathMessage.contains("pummeled") -> "thrown"
            deathMessage.contains("impaled") && !deathMessage.contains("stalagmite") -> "trident"
            deathMessage.contains("out of the world") -> "out_of_world"
            deathMessage.contains("border") -> "outside_border"
            deathMessage.contains("withered") -> "wither"
            deathMessage.contains("suffocated") -> "in_wall"
            deathMessage.contains("floor") && deathMessage.contains("lava") -> "hot_floor"
            deathMessage.contains("discovered the floor") -> "hot_floor"
            deathMessage.contains("burned") -> "on_fire"
            else -> "generic"
        }
    }
    
    /**
     * Format a death message with player, killer, and item information
     */
    private fun formatDeathMessage(message: String, player: Player, killer: Player?, item: ItemStack?): Component {
        val playerName = player.name
        
        // Only use killer name if there is actually a killer
        val killerName = if (killer != null) {
            killer.name
        } else {
            // If no killer but message has {killer}, use a generic term
            // This should rarely happen due to our filtering, but just in case
            "something"
        }
        
        // Get item name if relevant
        val itemName = if (killer != null && item != null && item.type.isItem && item.type.name != "AIR") {
            item.itemMeta?.displayName()?.toString() ?: item.type.name.lowercase().replace("_", " ")
        } else {
            "fists"
        }
        
        val formattedMessage = message
            .replace("{player}", playerName)
            .replace("{killer}", killerName)
            .replace("{item}", itemName)
        
        return Component.text(formattedMessage).color(TextColor.color(255, 85, 85))
    }
}
