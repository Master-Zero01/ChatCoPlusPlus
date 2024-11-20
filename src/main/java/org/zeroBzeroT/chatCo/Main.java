package org.zeroBzeroT.chatCo;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static org.zeroBzeroT.chatCo.Utils.saveStreamToFile;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class Main extends JavaPlugin {
    public static File PermissionConfig;
    public static File WhisperLog;
    public static File dataFolder;
    private static File Help;
    private Announcer announcer;
    public Collection<ChatPlayer> playerList;

    public void onDisable() {
        if (announcer != null) {
            announcer.disable();
        }
        playerList.clear();
    }

    // Add this method to handle announcer reloading
    public void reloadAnnouncer() {
        if (announcer != null) {
            announcer.loadConfig();
        } else if (getConfig().getBoolean("ChatCo.announcements.enabled", true)) {
            announcer = new Announcer(this);
        }
    }

    public void onEnable() {
        playerList = Collections.synchronizedCollection(new ArrayList<>());
        getConfig().options().copyDefaults(true);
        getConfig().options().parseComments(true);

        saveResourceFiles();
        toggleConfigValue(0);

        final PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PublicChat(this), this);
        pm.registerEvents(new BlackholeModule(this), this);

        if (getConfig().getBoolean("ChatCo.whisperChangesEnabled", true)) {
            pm.registerEvents(new Whispers(this), this);
        }

        if (getConfig().getBoolean("ChatCo.spoilersEnabled", false)) {
            pm.registerEvents(new Spoilers(), this);
        }

        if (getConfig().getBoolean("ChatCo.announcements.enabled", true)) {
            announcer = new Announcer(this);
        }

        if (getConfig().getBoolean("ChatCo.bStats", false)) {
            new Metrics(this, 16309);
        }

        // Setup kill command listener
        setupKillCommandListener();
    }

    private void setupKillCommandListener() {
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            getLogger().warning("ProtocolLib not found! Kill command handling will not work properly.");
            return;
        }

        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, 
                PacketType.Play.Client.CHAT_COMMAND) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                String command = event.getPacket().getStrings().read(0);
                
                // Check if it's a kill command
                if (command.equalsIgnoreCase("kill")) {
                    Player player = event.getPlayer();
                    
                    // Let vanilla handle everything for OPs
                    if (player.isOp()) {
                        return;
                    }
                    
                    // For non-OPs, handle with our custom logic
                    event.setCancelled(true);
                    
                    // Schedule our custom kill logic
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (!getConfig().getBoolean("ChatCo.suicideCommand", true)) {
                                return;
                            }
                            
                            Optional.ofNullable(player.getVehicle()).ifPresent(Entity::eject);
                            EntityDamageEvent.DamageCause[] causes = EntityDamageEvent.DamageCause.values();
                            EntityDamageEvent.DamageCause randomCause = causes[new Random().nextInt(causes.length)];
                            player.setLastDamageCause(new EntityDamageEvent(player, randomCause, Double.MAX_VALUE));
                            player.setHealth(0);
                        }
                    }.runTask(Main.this);
                }
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void saveResourceFiles() {
        Main.dataFolder = getDataFolder();
        Main.PermissionConfig = new File(Main.dataFolder, "permissionConfig.yml");
        Main.WhisperLog = new File(Main.dataFolder, "whisperlog.txt");
        Main.Help = new File(Main.dataFolder, "help.txt");

        if (!Main.WhisperLog.exists()) {
            Main.WhisperLog.getParentFile().mkdirs();
            saveStreamToFile(getResource("whisperlog.txt"), Main.WhisperLog);
        }

        if (!Main.Help.exists()) {
            Main.Help.getParentFile().mkdirs();
            saveStreamToFile(getResource("help.txt"), Main.Help);
        }

        saveDefaultConfig();

        if (!Main.PermissionConfig.exists()) {
            Main.PermissionConfig.getParentFile().mkdirs();
            saveStreamToFile(getResource("permissionConfig.yml"), Main.PermissionConfig);
        }
    }

    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command cmd, final @NotNull String commandLabel, final String[] args) {
        if (sender instanceof Player) {
            if (cmd.getName().equalsIgnoreCase("togglechat") && getConfig().getBoolean("toggleChatEnabled", true)) {
                if (toggleChat((Player) sender)) {
                    sender.sendMessage(ChatColor.RED + "Your chat is now disabled until you type /togglechat or relog.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Your chat has been re-enabled, type /togglechat to disable it again.");
                }
                return true;
            } else if (cmd.getName().equalsIgnoreCase("toggletells")) {
                if (toggleTells((Player) sender)) {
                    sender.sendMessage(ChatColor.RED + "You will no longer receive tells, type /toggletells to see them again.");
                } else {
                    sender.sendMessage(ChatColor.RED + "You now receive tells, type /toggletells to disable them again.");
                }
                return true;
            } else if (cmd.getName().equalsIgnoreCase("unignoreall") && getConfig().getBoolean("ignoresEnabled", true)) {
                try {
                    unIgnoreAll((Player) sender);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else if (cmd.getName().equalsIgnoreCase("ignore") && getConfig().getBoolean("ignoresEnabled", true)) {
                try {
                    if (args.length < 1) {
                        sender.sendMessage(ChatColor.RED + "You forgot to type the name of the player.");
                        return true;
                    }

                    if (args[0].length() > 16) {
                        sender.sendMessage(ChatColor.RED + "You entered an invalid player name.");
                        return true;
                    }

                    final Player ignorable = Bukkit.getServer().getPlayer(args[0]);

                    if (ignorable == null) {
                        sender.sendMessage(ChatColor.RED + "You have entered a player who does not exist or is offline.");
                        return true;
                    }

                    ignorePlayer((Player) sender, args[0]);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (cmd.getName().equalsIgnoreCase("ignorelist") && getConfig().getBoolean("ignoresEnabled", true)) {
                sender.sendMessage(ChatColor.YELLOW + "Ignored players:");
                int i = 0;

                for (final String ignores : getChatPlayer((Player) sender).getIgnoreList()) {
                    sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + ignores);
                    ++i;
                }

                sender.sendMessage(ChatColor.YELLOW + "" + i + " players ignored.");
                return true;
            }

            if(cmd.getName().equalsIgnoreCase("suicide") && getConfig().getBoolean("ChatCo.suicideCommand", true)) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player player = (Player) sender;
                        Optional.ofNullable(player.getVehicle()).ifPresent(Entity::eject);
                        EntityDamageEvent.DamageCause randomDamageCause = getRandomDamageCause();
                        EntityDamageEvent damageEvent = new EntityDamageEvent(player, randomDamageCause, 999);
                        player.setLastDamageCause(damageEvent);
                        player.setHealth(0);
                    }
                }).runTask(this);
                return true;
            }

            if(cmd.getName().equalsIgnoreCase("bed")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                    return true;
                }
                
                (new BukkitRunnable() {
                    @Override
                    public void run() {
                        Player player = (Player) sender;
                        Location bedLocation = player.getBedSpawnLocation();

                        if (bedLocation != null) {
                            double x = bedLocation.getX();
                            double y = bedLocation.getY();
                            double z = bedLocation.getZ();

                            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.YELLOW + "Anarchadia" + ChatColor.GOLD + "] "
                                    + ChatColor.GREEN + "Your bed is set at: "
                                    + ChatColor.AQUA + "X: " + ChatColor.WHITE + String.format("%.2f", x) + ", "
                                    + ChatColor.AQUA + "Y: " + ChatColor.WHITE + String.format("%.2f", y) + ", "
                                    + ChatColor.AQUA + "Z: " + ChatColor.WHITE + String.format("%.2f", z));
                        } else {
                            player.sendMessage(ChatColor.GOLD + "[" + ChatColor.YELLOW + "Anarchadia" + ChatColor.GOLD + "] "
                                    + ChatColor.RED + "You don't have a bed set.");
                        }
                    }
                }).runTaskAsynchronously(this);
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("blackhole") && (sender.isOp() || sender instanceof ConsoleCommandSender)) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /blackhole <player> or /blackhole hide <player> or /blackhole reload");
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                BlackholeModule.reloadConfiguration();
                sender.sendMessage("Blackhole configuration reloaded.");
                return true;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("hidden") || args[0].equalsIgnoreCase("hide")) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                if (!BlackholeModule.isPlayerBlacklisted(target)) {
                    sender.sendMessage("Player must be blacklisted first.");
                    return true;
                }

                BlackholeModule.setPlayerHidden(target, !BlackholeModule.isPlayerHidden(target));
                sender.sendMessage("Player messages will " + (BlackholeModule.isPlayerHidden(target) ? "not" : "now") + " show in console.");
                return true;
            }

            if (args.length == 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                if (BlackholeModule.isPlayerBlacklisted(target)) {
                    BlackholeModule.removePlayerFromBlacklist(target);
                    sender.sendMessage("Removed from blacklist.");
                } else {
                    BlackholeModule.addPlayerToBlacklist(target, false);
                    sender.sendMessage("Added to blacklist.");
                }
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("chatco")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                saveConfig();
                reloadAnnouncer();
                sender.sendMessage("Config reloaded");
                return true;
            }

            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("spoilers")) {
                    if (args[1].equalsIgnoreCase("e")) {
                        toggleConfigValue(3);
                        sender.sendMessage("Spoilers enabled");
                    } else if (args[1].equalsIgnoreCase("d")) {
                        toggleConfigValue(4);
                        sender.sendMessage("Spoilers disabled");
                    }
                }

                if (args[0].equalsIgnoreCase("whispers")) {
                    if (args[1].equalsIgnoreCase("e")) {
                        toggleConfigValue(5);
                        sender.sendMessage("Whisper changes enabled");
                    } else if (args[1].equalsIgnoreCase("d")) {
                        toggleConfigValue(6);
                        sender.sendMessage("Whisper changes disabled");
                    }
                }

                if (args[0].equalsIgnoreCase("newcommands")) {
                    if (args[1].equalsIgnoreCase("e")) {
                        toggleConfigValue(7);
                        sender.sendMessage("New Whisper commands enabled");
                    } else if (args[1].equalsIgnoreCase("d")) {
                        toggleConfigValue(8);
                        sender.sendMessage("New whisper commands disabled");
                    }
                }

                if (args[0].equalsIgnoreCase("whisperlog")) {
                    if (args[1].equalsIgnoreCase("e")) {
                        toggleConfigValue(9);
                        sender.sendMessage("Whisper logging enabled");
                    } else if (args[1].equalsIgnoreCase("d")) {
                        toggleConfigValue(10);
                        sender.sendMessage("Whisper logging disabled");
                    }
                }

                return true;
            }
        }

        return false;
    }

    public ChatPlayer getChatPlayer(final Player p) {
        for (final ChatPlayer chatPlayer : playerList) {
            if (chatPlayer.playerUUID.equals(p.getUniqueId())) {
                return chatPlayer;
            }
        }

        ChatPlayer newChatPlayer = null;

        try {
            newChatPlayer = new ChatPlayer(p);
            playerList.add(newChatPlayer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return newChatPlayer;
    }

    private boolean toggleChat(final Player p) {
        if (getChatPlayer(p).chatDisabled) {
            return getChatPlayer(p).chatDisabled = false;
        }

        return getChatPlayer(p).chatDisabled = true;
    }

    private boolean toggleTells(final Player p) {
        if (getChatPlayer(p).tellsDisabled) {
            return getChatPlayer(p).tellsDisabled = false;
        }

        return getChatPlayer(p).tellsDisabled = true;
    }

    private void ignorePlayer(final Player p, final String target) throws IOException {
        String message = ChatColor.YELLOW + "Chat messages from " + target + " will be ";

        if (getChatPlayer(p).isIgnored(target)) {
            message += "shown.";
        } else {
            message += "hidden.";
        }

        p.sendMessage(message);
        getChatPlayer(p).saveIgnoreList(target);
    }

    private void unIgnoreAll(final Player p) throws IOException {
        getChatPlayer(p).unIgnoreAll();
        String message = ChatColor.YELLOW + "Ignore list deleted.";
        p.sendMessage(message);
    }

    private EntityDamageEvent.DamageCause getRandomDamageCause() {
        EntityDamageEvent.DamageCause[] causes = EntityDamageEvent.DamageCause.values();
        Random random = new Random();
        return causes[random.nextInt(causes.length)];
    }

    public void remove(Player player) {
        playerList.removeIf(p -> p.player.equals(player));
    }

    private void toggleConfigValue(final int change) {
        switch (change) {
            case 3:
                getConfig().set("ChatCo.spoilersEnabled", true);
                break;
            case 4:
                getConfig().set("ChatCo.spoilersEnabled", false);
                break;
            case 5:
                getConfig().set("ChatCo.whisperChangesEnabled", true);
                break;
            case 6:
                getConfig().set("ChatCo.whisperChangesEnabled", false);
                break;
            case 7:
                getConfig().set("ChatCo.newCommands", true);
                break;
            case 8:
                getConfig().set("ChatCo.newCommands", false);
                break;
            case 9:
                getConfig().set("ChatCo.whisperLog", true);
                break;
            case 10:
                getConfig().set("ChatCo.whisperLog", false);
                break;
        }

        saveConfig();
        reloadConfig();
    }
}
