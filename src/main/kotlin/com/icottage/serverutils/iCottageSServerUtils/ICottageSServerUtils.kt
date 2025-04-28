package com.icottage.serverutils.iCottageSServerUtils

import com.icottage.serverutils.iCottageSServerUtils.combat.CombatManager
import com.icottage.serverutils.iCottageSServerUtils.commands.AdminCommands
import com.icottage.serverutils.iCottageSServerUtils.commands.GeneralCommands
import com.icottage.serverutils.iCottageSServerUtils.commands.ModerationCommands
import com.icottage.serverutils.iCottageSServerUtils.commands.RankCommands
import com.icottage.serverutils.iCottageSServerUtils.integration.LuckPermsIntegration
import com.icottage.serverutils.iCottageSServerUtils.listeners.CombatListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.DoorKnockListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.RankChatListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.RankDisplayListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.RankJoinLeaveListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.ScoreboardListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.SittingListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.StatsListener
import com.icottage.serverutils.iCottageSServerUtils.listeners.TablistListener
import com.icottage.serverutils.iCottageSServerUtils.moderation.ModerationManager
import com.icottage.serverutils.iCottageSServerUtils.ranks.RankManager
import com.icottage.serverutils.iCottageSServerUtils.scoreboard.ScoreboardManager
import com.icottage.serverutils.iCottageSServerUtils.sitting.SittingManager
import com.icottage.serverutils.iCottageSServerUtils.stats.PlayerStatsManager
import com.icottage.serverutils.iCottageSServerUtils.tablist.TablistManager
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.util.StringUtil

class ICottageSServerUtils : JavaPlugin() {
    
    private lateinit var rankManager: RankManager
    private lateinit var rankDisplayListener: RankDisplayListener
    private lateinit var luckPermsIntegration: LuckPermsIntegration
    private lateinit var moderationManager: ModerationManager
    private lateinit var playerStatsManager: PlayerStatsManager
    private lateinit var scoreboardManager: ScoreboardManager
    private lateinit var tablistManager: TablistManager
    private lateinit var sittingManager: SittingManager
    private lateinit var combatManager: CombatManager
    private lateinit var adminCommands: AdminCommands
    private lateinit var generalCommands: GeneralCommands

    override fun onEnable() {
        try {
            logger.info("Starting plugin initialization...")
            
            // Load configuration
            saveDefaultConfig()
            reloadConfig()
            logger.info("Loaded configuration")
            
            // Set debug mode from config
            val debugMode = config.getBoolean("general.debug", false)
            if (debugMode) {
                logger.info("Debug mode enabled")
            }
            
            // Initialize rank system
            rankManager = RankManager(this)
            logger.info("Created RankManager instance")
            
            // Set default rank from config
            val defaultRank = config.getString("ranks.default-rank", "default") ?: "default"
            rankManager.setDefaultRankName(defaultRank)
            
            rankManager.initialize()
            logger.info("Initialized RankManager")
            
            // Initialize LuckPerms integration if enabled in config
            val lpEnabled = config.getBoolean("luckperms.enabled", true)
            if (lpEnabled) {
                try {
                    luckPermsIntegration = LuckPermsIntegration(this, rankManager)
                    luckPermsIntegration.initialize()
                    logger.info("Initialized LuckPerms integration")
                    
                    // Auto-sync ranks with LuckPerms if enabled
                    if (config.getBoolean("luckperms.auto-sync-on-start", true)) {
                        logger.info("Auto-syncing ranks with LuckPerms...")
                        luckPermsIntegration.exportRanksToLuckPerms().thenAccept {
                            logger.info("Auto-sync complete")
                        }
                    }
                } catch (e: Exception) {
                    logger.warning("Failed to initialize LuckPerms integration: ${e.message}")
                    if (debugMode) {
                        e.printStackTrace()
                    }
                }
            } else {
                logger.info("LuckPerms integration disabled in config")
            }
            
            // Initialize moderation system
            try {
                moderationManager = ModerationManager(this)
                moderationManager.initialize()
                logger.info("Initialized moderation system")
            } catch (e: Exception) {
                logger.severe("Failed to initialize moderation system: ${e.message}")
                if (debugMode) {
                    e.printStackTrace()
                }
            }
            
            // Initialize player stats system
            try {
                playerStatsManager = PlayerStatsManager(this)
                playerStatsManager.initialize()
                logger.info("Initialized player stats system")
            } catch (e: Exception) {
                logger.severe("Failed to initialize player stats system: ${e.message}")
                if (debugMode) {
                    e.printStackTrace()
                }
            }
            
            // Initialize scoreboard system
            try {
                scoreboardManager = ScoreboardManager(this, rankManager, playerStatsManager)
                scoreboardManager.initialize()
                logger.info("Initialized scoreboard system")
            } catch (e: Exception) {
                logger.severe("Failed to initialize scoreboard system: ${e.message}")
                if (debugMode) {
                    e.printStackTrace()
                }
            }
            
            // Initialize tablist system
            try {
                tablistManager = TablistManager(this, rankManager, playerStatsManager)
                tablistManager.initialize()
                logger.info("Initialized tablist system")
            } catch (e: Exception) {
                logger.severe("Failed to initialize tablist system: ${e.message}")
                if (debugMode) {
                    e.printStackTrace()
                }
            }
            
            // Register listeners
            try {
                rankDisplayListener = RankDisplayListener(rankManager)
                server.pluginManager.registerEvents(rankDisplayListener, this)
                logger.info("Registered RankDisplayListener")
                
                server.pluginManager.registerEvents(RankChatListener(rankManager), this)
                logger.info("Registered RankChatListener")
                
                server.pluginManager.registerEvents(RankJoinLeaveListener(rankManager), this)
                
                // Register stats, scoreboard, and tablist listeners
                server.pluginManager.registerEvents(StatsListener(playerStatsManager), this)
                server.pluginManager.registerEvents(ScoreboardListener(scoreboardManager), this)
                server.pluginManager.registerEvents(TablistListener(tablistManager), this)
                
                // Initialize sitting system
                sittingManager = SittingManager(this)
                sittingManager.initialize()
                server.pluginManager.registerEvents(SittingListener(this, sittingManager), this)
                logger.info("Initialized sitting system and registered listener")
                
                // Register door knock listener
                server.pluginManager.registerEvents(DoorKnockListener(this), this)
                logger.info("Registered door knock listener")
                
                // Initialize combat manager
                combatManager = CombatManager(this)
                combatManager.initialize()
                server.pluginManager.registerEvents(CombatListener(this, combatManager), this)
                logger.info("Initialized combat manager and registered listener")
                
                logger.info("Registered all listeners")
            } catch (e: Exception) {
                logger.severe("Error registering listeners: ${e.message}")
                e.printStackTrace()
            }
            
            // Register commands
            try {
                // Register general commands
                generalCommands = GeneralCommands(this)
                generalCommands.registerCommands()
                logger.info("Registered general commands")
                
                // Register admin commands
                adminCommands = AdminCommands(this)
                adminCommands.registerCommands()
                logger.info("Registered admin commands")
                
                val rankCommands = RankCommands(rankManager, rankDisplayListener)
                
                // Try to get the command from plugin.yml first
                val rankCommand = getCommand("rank")
                if (rankCommand != null) {
                    rankCommand.setExecutor(rankCommands)
                    rankCommand.tabCompleter = rankCommands
                    logger.info("Registered rank command via plugin.yml")
                } else {
                    // If that fails, register the command directly
                    logger.info("Attempting to register rank command directly...")
                    try {
                        val directRankCommand = object : Command("rank") {
                            init {
                                description = "Manage server ranks"
                                usage = "/rank <list|info|create|delete|set|reload>"
                                aliases = listOf("ranks")
                            }
                            
                            override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                                return rankCommands.onCommand(sender, this, commandLabel, args)
                            }
                            
                            override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                                val completions = mutableListOf<String>()
                                
                                if (args.isEmpty() || args.size == 1) {
                                    val subCommands = listOf("list", "info", "create", "edit", "permission", "delete", "set", "reload", "colors")
                                    StringUtil.copyPartialMatches(args.getOrElse(0) { "" }, subCommands, completions)
                                } else if (args.size == 2) {
                                    when (args[0].lowercase()) {
                                        "info", "delete" -> {
                                            // Complete with rank names
                                            val ranks = rankManager.getAllRanks().map { it.name }
                                            StringUtil.copyPartialMatches(args[1], ranks, completions)
                                        }
                                        "edit" -> {
                                            // Complete with rank names
                                            val ranks = rankManager.getAllRanks().map { it.name }
                                            StringUtil.copyPartialMatches(args[1], ranks, completions)
                                        }
                                        "permission" -> {
                                            // Complete with rank names
                                            val ranks = rankManager.getAllRanks().map { it.name }
                                            StringUtil.copyPartialMatches(args[1], ranks, completions)
                                        }
                                        "set" -> {
                                            // Complete with online player names
                                            val players = Bukkit.getOnlinePlayers().map { it.name }
                                            StringUtil.copyPartialMatches(args[1], players, completions)
                                        }
                                    }
                                } else if (args.size == 3) {
                                    when (args[0].lowercase()) {
                                        "edit" -> {
                                            // Complete with editable properties
                                            val properties = listOf("displayname", "prefix", "weight")
                                            StringUtil.copyPartialMatches(args[2], properties, completions)
                                        }
                                        "permission" -> {
                                            // Complete with permission actions
                                            val actions = listOf("add", "remove")
                                            StringUtil.copyPartialMatches(args[2], actions, completions)
                                        }
                                        "set" -> {
                                            // Complete with rank names
                                            val ranks = rankManager.getAllRanks().map { it.name }
                                            StringUtil.copyPartialMatches(args[2], ranks, completions)
                                        }
                                    }
                                }
                                
                                return completions
                            }
                        }
                        
                        server.commandMap.register("serverutils", directRankCommand)
                        logger.info("Registered rank command directly")
                    } catch (e: Exception) {
                        logger.severe("Failed to register rank command directly: ${e.message}")
                        e.printStackTrace()
                    }
                }
                
                // Register the icottageutils command directly
                try {
                    val icuCommand = object : Command("icottageutils") {
                        init {
                            description = "iCottage Server Utilities commands"
                            usage = "/icottageutils <command>"
                            aliases = listOf("icu")
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            val completions = mutableListOf<String>()
                            
                            if (args.isEmpty() || args.size == 1) {
                                val subCommands = listOf("lp", "luckperms")
                                StringUtil.copyPartialMatches(args.getOrElse(0) { "" }, subCommands, completions)
                            } else if (args.size == 2 && (args[0].equals("lp", ignoreCase = true) || args[0].equals("luckperms", ignoreCase = true))) {
                                val subCommands = listOf("import", "export")
                                StringUtil.copyPartialMatches(args[1], subCommands, completions)
                            }
                            
                            return completions
                        }
                    }
                    
                    server.commandMap.register("serverutils", icuCommand)
                    logger.info("Registered icottageutils command directly")
                    
                    // Register moderation commands
                    val moderationCommands = ModerationCommands(moderationManager)
                    
                    // Register mute command
                    val muteCommand = object : Command("mute") {
                        init {
                            description = "Mute a player"
                            usage = "/mute <player> <reason> <duration>"
                            permission = "serverutils.moderation.mute"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register unmute command
                    val unmuteCommand = object : Command("unmute") {
                        init {
                            description = "Unmute a player"
                            usage = "/unmute <player>"
                            permission = "serverutils.moderation.unmute"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register ban command
                    val banCommand = object : Command("ban") {
                        init {
                            description = "Ban a player"
                            usage = "/ban <player> <reason> <duration>"
                            permission = "serverutils.moderation.ban"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register unban command
                    val unbanCommand = object : Command("unban") {
                        init {
                            description = "Unban a player"
                            usage = "/unban <player>"
                            permission = "serverutils.moderation.unban"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register warn command
                    val warnCommand = object : Command("warn") {
                        init {
                            description = "Warn a player"
                            usage = "/warn <player> <reason>"
                            permission = "serverutils.moderation.warn"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register warnings command
                    val warningsCommand = object : Command("warnings") {
                        init {
                            description = "View a player's warnings"
                            usage = "/warnings <player>"
                            permission = "serverutils.moderation.warnings"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register clearwarnings command
                    val clearWarningsCommand = object : Command("clearwarnings") {
                        init {
                            description = "Clear a player's warnings"
                            usage = "/clearwarnings <player>"
                            permission = "serverutils.moderation.clearwarnings"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register kick command
                    val kickCommand = object : Command("kick") {
                        init {
                            description = "Kick a player"
                            usage = "/kick <player> <reason>"
                            permission = "serverutils.moderation.kick"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register history command
                    val historyCommand = object : Command("history") {
                        init {
                            description = "View a player's moderation history"
                            usage = "/history <player>"
                            permission = "serverutils.moderation.history"
                        }
                        
                        override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                            return moderationCommands.onCommand(sender, this, commandLabel, args)
                        }
                        
                        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                            return moderationCommands.onTabComplete(sender, this, alias, args).toMutableList()
                        }
                    }
                    
                    // Register all moderation commands
                    server.commandMap.register("serverutils", muteCommand)
                    server.commandMap.register("serverutils", unmuteCommand)
                    server.commandMap.register("serverutils", banCommand)
                    server.commandMap.register("serverutils", unbanCommand)
                    server.commandMap.register("serverutils", warnCommand)
                    server.commandMap.register("serverutils", warningsCommand)
                    server.commandMap.register("serverutils", clearWarningsCommand)
                    server.commandMap.register("serverutils", kickCommand)
                    server.commandMap.register("serverutils", historyCommand)
                    
                    logger.info("Registered moderation commands")
                } catch (e: Exception) {
                    logger.severe("Failed to register commands: ${e.message}")
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                logger.severe("Failed to register command directly: ${e.message}")
                logger.severe("Error registering commands: ${e.message}")
                e.printStackTrace()
            }
            
            logger.info("iCottage's Server Utils has been enabled!")
        } catch (e: Exception) {
            logger.severe("Failed to enable plugin: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Handle commands not registered in plugin.yml
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("icottageutils", ignoreCase = true) || command.name.equals("icu", ignoreCase = true)) {
            if (args.isEmpty()) {
                sender.sendMessage(Component.text("=== iCottage Server Utils ===").color(NamedTextColor.GOLD))
                sender.sendMessage(Component.text("/icu lp import - Import LuckPerms groups as ranks").color(NamedTextColor.YELLOW))
                sender.sendMessage(Component.text("/icu lp export - Export ranks to LuckPerms").color(NamedTextColor.YELLOW))
                return true
            }
            
            when (args[0].lowercase()) {
                "luckperms", "lp" -> {
                    if (args.size < 2) {
                        sender.sendMessage(Component.text("Usage: /icu lp <import|export>").color(NamedTextColor.RED))
                        return true
                    }
                    
                    when (args[1].lowercase()) {
                        "import" -> {
                            if (!sender.hasPermission("icottageutils.luckperms.import")) {
                                sender.sendMessage(Component.text("You don't have permission to import LuckPerms groups.").color(NamedTextColor.RED))
                                return true
                            }
                            
                            sender.sendMessage(Component.text("Importing LuckPerms groups as ranks...").color(NamedTextColor.YELLOW))
                            
                            luckPermsIntegration.importLuckPermsGroups().thenAccept {
                                sender.sendMessage(Component.text("Import complete. Use /rank list to see all ranks.").color(NamedTextColor.GREEN))
                            }.exceptionally { e ->
                                sender.sendMessage(Component.text("Error importing LuckPerms groups: ${e.message}").color(NamedTextColor.RED))
                                null
                            }
                            
                            return true
                        }
                        "export" -> {
                            if (!sender.hasPermission("icottageutils.luckperms.export")) {
                                sender.sendMessage(Component.text("You don't have permission to export ranks to LuckPerms.").color(NamedTextColor.RED))
                                return true
                            }
                            
                            sender.sendMessage(Component.text("Exporting ranks to LuckPerms...").color(NamedTextColor.YELLOW))
                            
                            luckPermsIntegration.exportRanksToLuckPerms().thenAccept {
                                sender.sendMessage(Component.text("Export complete. All ranks have been synchronized with LuckPerms.").color(NamedTextColor.GREEN))
                            }.exceptionally { e ->
                                sender.sendMessage(Component.text("Error exporting ranks to LuckPerms: ${e.message}").color(NamedTextColor.RED))
                                null
                            }
                            
                            return true
                        }
                        else -> {
                            sender.sendMessage(Component.text("Usage: /icu lp <import|export>").color(NamedTextColor.RED))
                            return true
                        }
                    }
                }
                else -> return false
            }
        }
        
        return false
    }



    override fun onDisable() {
        // Save all player ranks
        rankManager.savePlayerRanks()
        
        // Save all player stats
        playerStatsManager.saveStats()
        
        // Make all sitting players stand up
        if (::sittingManager.isInitialized) {
            sittingManager.standAllPlayers()
        }
        
        // Shut down combat manager
        if (::combatManager.isInitialized) {
            combatManager.shutdown()
        }
        
        logger.info("Plugin disabled")
    }
}
