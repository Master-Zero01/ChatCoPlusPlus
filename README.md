# ChatCoPlus

**THIS IS NOT INTENDED FOR PRODUCTION-USE; THIS IS A PERSONAL FORK MODIFIED USING AI**

ChatCoPlusPlus is a plugin for the Minecraft server **3b3tpe** that provides an efficient chat system with colored text, ignores, whispers, announcements, antispam, muting, word blacklisting, and optional logging.

![logo](https://github.com/AnarchadiaMC/ChatCoPlus/blob/main/logo.jpg?raw=true)

[![release](https://github.com/AnarchadiaMC/ChatCoPlus/actions/workflows/release.yml/badge.svg)](https://github.com/AnarchadiaMC/ChatCoPlus/actions/workflows/release.yml)
![repo size](https://img.shields.io/github/languages/code-size/AnarchadiaMC/ChatCoPlus.svg?label=repo%20size)
[![downloads](https://img.shields.io/github/downloads/AnarchadiaMC/ChatCoPlus/total)](https://github.com/AnarchadiaMC/ChatCoPlus/releases)

## Features

- Players can customize their chat message using various prefixes (e.g., '>' for green-text). Supports inline colors and formatting tags like `<BOLD>`, `<RED>`.
- Optional Unicode text blocking to prevent invisible text or crashes.
- Advanced word blacklist with fuzzy matching to detect variations, leetspeak, reversals, and bypass attempts.
- Players can whisper other players (customizable format, defaults to purple).
- Players can ignore another player (works for both world and whisper chat). Ignore lists are persistent between logins. Players can also un-ignore or clear the list.
- Periodic announcements broadcast to all players (configurable messages and delay).
- Mute system (/mute, /unmute) to blacklist players from chatting, with optional console visibility toggle (/consolemute).
- Players can toggle chat on/off or disable tells (whispers).
- Full permission support to limit access to colors, features, and admin commands.
- Whisper logging and monitoring (logs to file or console).
- Click-to-whisper on player names in chat.

## Source

ChatCoPlusPlus is a rebuild and fork of the famous chat plugin ChatCo created by [jj20051](https://github.com/WiredTombstone), who is the founder and current owner of 9b9t. Original source can be found here: https://github.com/2builders2tool/ChatCo

This fork includes significant updates, enhancements, and AI-assisted modifications for modern Minecraft versions.

## Popular servers running derivatives of this plugin

- Anarchadia.org
- 0b0t.org
- 2b2t.net
- 9b9t.com
- Anarchy.pw

## Administrative commands

Most admin commands use the format `/chatco {component} {e|d}` where 'e' enables and 'd' disables (e.g., `/chatco whisperlog e`). Reload the plugin after changes.

### Components

- **whispers** - Enables/disables whisper functionality (enabled by default).
- **newcommands** - Enables/disables new whisper commands like `/r`, `/l` (enabled by default).
- **whisperlog** - Enables/disables whisper logging to `/whisperlog.txt` (disabled by default).
- **blacklist** - Manage word blacklist:
  - `/chatco blacklist add <word>` - Add a word (requires `ChatCo.admin.blacklist` permission).
  - `/chatco blacklist remove <word>` - Remove a word.
  - `/chatco blacklist list` - List blacklisted words.
  - `/chatco blacklist test <message>` - Test if a message would be blocked.
- **reload** - `/chatco reload` - Reloads config, announcements, and blacklist.

### Mute Commands (requires OP or console)

- `/mute <player>` or `/mute reload` - Mutes a player or reloads mute config.
- `/unmute <player>` - Unmutes a player.
- `/consolemute <player>` - Toggles if muted player's messages show in console.

## Player Commands

- `/ignore <player>` - Ignore/un-ignore a player.
- `/ignored` or `/ignorelist` - List ignored players.
- `/unignoreall` - Clear ignore list.
- `/togglechat` - Toggle public chat (not persistent).
- `/toggletells` - Toggle receiving whispers (not persistent).

### Whisper Commands

- `/w <player> <message>`, `/tell <player> <message>`, `/msg <player> <message>`, `/t <player> <message>`, `/whisper <player> <message>`, `/pm <player> <message>` - Send a whisper.
- `/r <message>` or `/reply <message>` - Reply to last whisper sender.
- `/l <message>` or `/last <message>` - Reply to last whisper recipient.

## Config

Located in `config.yml`. Key sections:

- **ChatCo.chatPrefixes** / **ChatCo.chatColors**: Define prefixes and inline codes for colors (e.g., GREEN: '>'). Set to `NULL` to disable.
- **ChatCo.ignoresEnabled**: Enable ignores (true).
- **ChatCo.chatDisabled**: Globally disable chat (false).
- **ChatCo.blockUnicodeText**: Block non-ASCII text (false).
- **ChatCo.wordBlacklist**: List of banned words (fuzzy matching enabled).
- **ChatCo.whisperFormat**: Customize send/receive formats with placeholders (%SENDER%, %RECEIVER%) and colors (%RED%, etc.).
- **ChatCo.whisperLog**: Enable logging (false).
- **ChatCo.whisperMonitoring**: Log whispers to console (false).
- **ChatCo.chatToConsole**: Log public chat to console (true).
- **ChatCo.announcements**: Enable (true), messages list, prefix, delay (seconds).
- **ChatCo.newCommands**: Enable short whisper commands (true).
- **ChatCo.whisperOnClick**: Enable click-to-whisper (true).
- **ChatCo.bStats**: Enable metrics (false).
- Debug options: `debugUnicodeBlocking`, `debugBlacklistBlocking`.

Permissions in `permissionConfig.yml` or via plugin (e.g., `ChatCo.chatPrefixes.GREEN`).

Color codes and prefixes can be restricted by permissions. Usage of features requires appropriate perms (e.g., `ChatCo.admin.blacklist`).

## Examples

- Toggle chat: `/togglechat`.
- Green-text: Start message with '>' (if enabled).
- Whisper: `/w Player Hello!`.
- Mute player: `/mute PlayerName`.
- Add blacklist word: `/chatco blacklist add badword`.
- Announcement config example:
  ```
  ChatCo:
    announcements:
      enabled: true
      prefix: "&6[&eAnarchadia&6] "
      delay: 300
      messages:
        - "Welcome to the server!"
        - "Rules: No doxxing."
  ```

## Tested Minecraft Versions

- 1.12.2 (original)
- 1.16+ (Adventure API integration for modern text components)
- 1.21 (tested in development)

Compatible with Spigot/Paper.

## Warranty

The Software is provided "as is" and without warranties of any kind, express or implied, including but not limited to the warranties of merchantability, fitness for a particular purpose, and non-infringement. In no event shall the Authors or copyright owners be liable for any claims, damages or other liability, whether in an action in contract, tort or otherwise, arising from, out of or in connection with the Software or the use or other dealings in the Software.
