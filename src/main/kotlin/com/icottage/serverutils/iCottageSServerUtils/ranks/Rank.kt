package com.icottage.serverutils.iCottageSServerUtils.ranks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.util.regex.Pattern

/**
 * Represents a player rank in the server
 * @property name The name of the rank
 * @property displayName The display name of the rank (with color codes)
 * @property prefix The prefix to show before the player's name
 * @property weight The weight of the rank (higher weight ranks appear higher in tab)
 * @property permissions List of permissions granted by this rank
 */
data class Rank(
    val name: String,
    val displayName: String,
    val prefix: String,
    val weight: Int,
    val permissions: List<String> = emptyList()
) {
    companion object {
        // Pattern for hex color codes like &#RRGGBB
        private val HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})")
    }
    
    /**
     * Gets the formatted prefix as a Component
     */
    fun getPrefixComponent(): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix)
    }
    
    /**
     * Gets the formatted display name as a Component
     */
    fun getDisplayNameComponent(): Component {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
    }
    
    /**
     * Gets the formatted prefix for legacy systems
     */
    fun getFormattedPrefix(): String {
        return prefix
    }
    
    /**
     * Gets the formatted display name for legacy systems
     */
    fun getFormattedDisplayName(): String {
        return displayName
    }
    
    /**
     * Extracts the TextColor from the rank's prefix or display name
     * @return The TextColor of the rank, or null if no color is found
     */
    fun getColor(): TextColor? {
        // Try to extract color from the prefix component
        val prefixComponent = getPrefixComponent()
        val prefixStyle = prefixComponent.style()
        if (prefixStyle.color() != null) {
            return prefixStyle.color()
        }
        
        // If no color in prefix, try from display name
        val displayNameComponent = getDisplayNameComponent()
        val displayNameStyle = displayNameComponent.style()
        if (displayNameStyle.color() != null) {
            return displayNameStyle.color()
        }
        
        // Default to white if no color found
        return NamedTextColor.WHITE
    }
    

}
