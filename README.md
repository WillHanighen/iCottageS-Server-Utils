# iCottage's Server Utils

A comprehensive Minecraft server utility plugin that enhances server management and player experience with a variety of features.

## Features

### Rank System
- Custom rank management with permissions, prefixes, and colors
- Seamless LuckPerms integration for synchronizing ranks with permission groups
- Customizable rank display in chat, tab list, and player names
- Custom join/leave messages based on player ranks

### Moderation Tools
- Comprehensive moderation commands for muting, banning, kicking, and warning players
- Temporary and permanent punishment options with reason tracking
- Automatic punishment escalation based on warning thresholds
- Staff notifications for moderation actions
- Visual mute status display in scoreboard and tab list

### Player Statistics
- Tracks player kills, deaths, KDR (Kill/Death Ratio), and playtime
- Persistent statistics storage across server restarts
- Integration with scoreboard and tab list for display

### Custom Scoreboard
- Server information display (name, address, online players, TPS)
- Player statistics display (rank, kills, deaths, KDR, playtime)
- Mute status display with remaining time for muted players
- Fully customizable through configuration
- Uses modern Adventure API for text formatting

### Custom Tab List (Tablist)
- Custom header and footer with server information
- Player names with rank prefixes and optional stats display
- Mute status indicators in player names with time remaining and reason
- Detailed mute information in the footer for muted players
- Configurable update intervals and display options
- Seamless integration with the rank system

### Anti-Combat Logging
- Prevents players from logging out during combat
- Configurable combat timer with action bar countdown
- Creates a combat logger NPC when a player logs off during combat
- The NPC can be killed, resulting in the player's death upon reconnection

### Sitting Feature
- Allows players to sit on stairs, carpets, and bottom slabs
- Simple right-click interaction to sit down
- Automatic standing when player moves, logs out, or teleports
- Configurable options for allowed block types

### Utility Commands
- `/hat` - Wear any item as a hat
- `/rgbhat` - Wear a color-cycling RGB glass hat
- Various admin commands for server management
- Quality of life improvements for players and staff

## Installation

1. Download the latest release from the [releases page](https://github.com/WillHanighen/iCottageS-Server-Utils/releases)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Edit the configuration file at `plugins/iCottageS-Server-Utils/config.yml` to customize the plugin to your needs

## Configuration

The plugin is highly configurable through the `config.yml` file. See the comments in the configuration file for detailed explanations of each option.

### Key Configuration Sections:
- `general` - General plugin settings
- `ranks` - Rank system configuration
- `luckperms` - LuckPerms integration settings
- `moderation` - Moderation system settings
- `scoreboard` - Scoreboard display settings
- `tablist` - Tab list display settings
- `sitting` - Sitting feature configuration
- `combat` - Combat logging prevention settings

## Permissions

### General Permissions
- `serverutils.use` - Access to basic plugin features
- `serverutils.hat` - Allows using the hat command
- `serverutils.rgbhat` - Allows using the RGB hat command

### Rank System
- `serverutils.rank.use` - Access to basic rank commands
- `serverutils.rank.admin` - Access to rank management commands

### Moderation
- `serverutils.admin.heal` - Allows healing players
- `serverutils.admin.feed` - Allows feeding players
- `serverutils.admin.invsee` - Allows viewing player inventories
- `serverutils.admin.echest` - Allows viewing player enderchests

## Commands

### General Commands
- `/hat` - Wear the item in your hand as a hat
- `/rgbhat` - Toggle a color-cycling RGB glass hat

### Admin Commands
- `/heal [player]` - Heal yourself or another player
- `/feed [player]` - Feed yourself or another player
- `/invsee <player>` - View and modify a player's inventory
- `/echest [player]` - View and modify a player's enderchest

### Rank Commands
- `/rank list` - List all available ranks
- `/rank info <rank>` - View information about a rank
- `/rank set <player> <rank>` - Set a player's rank
- `/rank create <name> <prefix> <color>` - Create a new rank
- `/rank delete <rank>` - Delete a rank
- `/rank setprefix <rank> <prefix>` - Set a rank's prefix
- `/rank setcolor <rank> <color>` - Set a rank's color
- `/rank setpermission <rank> <permission>` - Add a permission to a rank
- `/rank removepermission <rank> <permission>` - Remove a permission from a rank

### Moderation Commands
- `/ban <player> [duration] [reason]` - Ban a player
- `/unban <player>` - Unban a player
- `/mute <player> [duration] [reason]` - Mute a player
- `/unmute <player>` - Unmute a player
- `/kick <player> [reason]` - Kick a player
- `/warn <player> [reason]` - Warn a player
- `/warnings <player>` - View a player's warnings
- `/clearwarnings <player>` - Clear a player's warnings

## Dependencies

- **Optional:** LuckPerms for permission group synchronization

## Technical Details

- Built with Kotlin for modern, type-safe code
- Uses the Adventure API for text components and formatting
- Uses UUIDs for player identification to prevent issues with name changes
- Designed with performance and reliability in mind

## License

This project is licensed under the BY-NC 4.0 License - see the LICENSE file for details.

## Support

For support, please open an issue on the GitHub repository or contact the developer directly.
