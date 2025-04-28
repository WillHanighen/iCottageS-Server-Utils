package com.icottage.serverutils.iCottageSServerUtils.moderation

import java.util.UUID

/**
 * Represents a mute entry
 */
data class MuteEntry(
    val playerUUID: UUID,
    val playerName: String,
    val reason: String,
    val expiration: Long,
    val issuerUUID: UUID,
    val issuerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a ban entry
 */
data class BanEntry(
    val playerUUID: UUID,
    val playerName: String,
    val reason: String,
    val expiration: Long,
    val issuerUUID: UUID,
    val issuerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a warning entry
 */
data class WarnEntry(
    val playerUUID: UUID,
    val playerName: String,
    val reason: String,
    val issuerUUID: UUID,
    val issuerName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a kick entry
 */
data class KickEntry(
    val playerUUID: UUID,
    val playerName: String,
    val reason: String,
    val issuerUUID: UUID,
    val issuerName: String,
    val timestamp: Long = System.currentTimeMillis()
)
