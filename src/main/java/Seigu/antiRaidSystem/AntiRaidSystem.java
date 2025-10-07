package Seigu.antiRaidSystem;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.Bukkit;
import java.io.File;
import java.io.IOException;
 
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public final class AntiRaidSystem extends JavaPlugin {

    private FileConfiguration dataConfig;
    private File dataFile;

    private final Map<String, Set<String>> nicknameToIps = new ConcurrentHashMap<>();
    private final Set<String> allowedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private final Set<UUID> trustedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastCommandAtMs = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();
        getServer().getPluginManager().registerEvents(new LoginListener(), this);
        getServer().getPluginManager().registerEvents(new CommandListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        if (getCommand("ars") != null) {
            getCommand("ars").setExecutor((sender, command, label, args) -> {
                boolean isConsole = sender instanceof ConsoleCommandSender;
                boolean hasPerm = sender.hasPermission("ars.admin");
                if (!isConsole && !hasPerm) {
                    sender.sendMessage(color(msgDefaultLang("not_console")));
                    return true;
                }
                if (args.length < 1) {
                    sender.sendMessage("/ars reload | /ars protect <player> | /ars unprotect <player> | /ars authorize <player> <ip> | /ars allowplayer <player> | /ars allowip <ip> | /ars trust <player> | /ars untrust <player>");
                    return true;
                }
                String sub = args[0].toLowerCase();
                switch (sub) {
                    case "reload":
                        reloadConfig();
                        sender.sendMessage(color(msgDefaultLang("reloaded")));
                        return true;
                    case "trust":
                        if (args.length != 2) {
                            sender.sendMessage("/ars trust <player>");
                            return true;
                        }
                        {
                            String t = args[1];
                            Player online = Bukkit.getPlayerExact(t);
                            if (online != null) {
                                trustedPlayers.add(online.getUniqueId());
                            }
                            // Persist nickname as trusted for next logins
                            List<String> pretrusted = new ArrayList<>(dataConfig.getStringList("trusted.nicknames"));
                            if (!pretrusted.contains(t.toLowerCase())) pretrusted.add(t.toLowerCase());
                            dataConfig.set("trusted.nicknames", pretrusted);
                            saveData();
                            sender.sendMessage("Trusted: " + t);
                            return true;
                        }
                    case "untrust":
                        if (args.length != 2) {
                            sender.sendMessage("/ars untrust <player>");
                            return true;
                        }
                        {
                            String t = args[1];
                            Player online = Bukkit.getPlayerExact(t);
                            if (online != null) {
                                trustedPlayers.remove(online.getUniqueId());
                            }
                            List<String> pretrusted = new ArrayList<>(dataConfig.getStringList("trusted.nicknames"));
                            pretrusted.remove(t.toLowerCase());
                            dataConfig.set("trusted.nicknames", pretrusted);
                            saveData();
                            sender.sendMessage("Untrusted: " + t);
                            return true;
                        }
                    case "protect":
                        if (args.length != 2) {
                            sender.sendMessage("/ars protect <player>");
                            return true;
                        }
                        {
                            String prot = args[1].toLowerCase();
                            List<String> list = new ArrayList<>(getConfig().getStringList("protected.players"));
                            if (!list.contains(prot)) {
                                list.add(prot);
                                getConfig().set("protected.players", list);
                                saveConfig();
                            }
                            sender.sendMessage("Protected: " + prot);
                            return true;
                        }
                    case "unprotect":
                        if (args.length != 2) {
                            sender.sendMessage("/ars unprotect <player>");
                            return true;
                        }
                        {
                            String prot = args[1].toLowerCase();
                            List<String> list = new ArrayList<>(getConfig().getStringList("protected.players"));
                            list.remove(prot);
                            getConfig().set("protected.players", list);
                            saveConfig();
                            sender.sendMessage("Unprotected: " + prot);
                            return true;
                        }
                    case "authorize":
                        if (args.length != 3) {
                            sender.sendMessage("/ars authorize <player> <ip>");
                            return true;
                        }
                        String player = args[1];
                        String ip = args[2];
                        nicknameToIps.computeIfAbsent(player.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(ip);
                        saveData();
                        sender.sendMessage(color(msgDefaultLang("authorized").replace("%player%", player).replace("%ip%", ip)));
                        return true;
                    case "allowplayer":
                        if (args.length != 2) {
                            sender.sendMessage("/ars allowplayer <player>");
                            return true;
                        }
                        String p = args[1].toLowerCase();
                        allowedPlayers.add(p);
                        saveData();
                        sender.sendMessage(color(msgDefaultLang("allowed_player_added").replace("%player%", p)));
                        return true;
                    case "allowip":
                        if (args.length != 2) {
                            sender.sendMessage("/ars allowip <ip>");
                            return true;
                        }
                        String aip = args[1];
                        allowedIps.add(aip);
                        saveData();
                        sender.sendMessage(color(msgDefaultLang("allowed_ip_added").replace("%ip%", aip)));
                        return true;
                    default:
                        sender.sendMessage("/ars reload | /ars protect <player> | /ars unprotect <player> | /ars authorize <player> <ip> | /ars allowplayer <player> | /ars allowip <ip> | /ars trust <player> | /ars untrust <player>");
                        return true;
                }
            });
        }
    }

    @Override
    public void onDisable() {
        saveData();
    }

    private String color(String s) {
        return s == null ? "" : s.replace("&", "§");
    }

    private String msgDefaultLang(String key) {
        String lang = getConfig().getString("general.default_language", "ru");
        String prefix = getConfig().getString("messages.prefix", "");
        String value = getConfig().getString("messages." + lang + "." + key, null);
        if (value == null) {
            value = getConfig().getString("messages.en." + key, key);
        }
        return prefix + value;
    }

    private String msg(Player player, String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String lang = "en";
        try {
            // Paper: player.locale() returns Locale. Fallback to getLocale() string when available
            String raw = null;
            try {
                Object localeObj = Player.class.getMethod("locale").invoke(player);
                raw = localeObj != null ? localeObj.toString() : null;
            } catch (Throwable ignored) {
                try {
                    raw = (String) Player.class.getMethod("getLocale").invoke(player);
                } catch (Throwable ignored2) { }
            }
            if (raw != null && raw.toLowerCase().startsWith("ru")) {
                lang = "ru";
            } else {
                String def = getConfig().getString("general.default_language", "ru");
                lang = (def.equalsIgnoreCase("ru")) ? (raw != null && raw.toLowerCase().startsWith("ru") ? "ru" : "en") : "en";
            }
        } catch (Throwable ignored) { }
        String value = getConfig().getString("messages." + lang + "." + key, null);
        if (value == null) value = getConfig().getString("messages.en." + key, key);
        return prefix + value;
    }

    private void loadData() {
        try {
            if (dataFile == null) {
                dataFile = new File(getDataFolder(), "data.yml");
            }
            if (!dataFile.exists()) {
                getDataFolder().mkdirs();
                dataFile.createNewFile();
            }
            dataConfig = YamlConfiguration.loadConfiguration(dataFile);
            nicknameToIps.clear();
            allowedPlayers.clear();
            allowedIps.clear();
            trustedPlayers.clear();

            // Backward compatibility: old single-IP structure
            if (dataConfig.isConfigurationSection("nicknameToIp")) {
                for (String key : dataConfig.getConfigurationSection("nicknameToIp").getKeys(false)) {
                    String ip = dataConfig.getString("nicknameToIp." + key, "");
                    if (ip != null && !ip.isEmpty()) {
                        nicknameToIps.computeIfAbsent(key.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(ip);
                    }
                }
            }
            if (dataConfig.isConfigurationSection("nicknameToIps")) {
                for (String key : dataConfig.getConfigurationSection("nicknameToIps").getKeys(false)) {
                    List<String> ips = dataConfig.getStringList("nicknameToIps." + key);
                    if (ips != null) {
                        nicknameToIps.computeIfAbsent(key.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).addAll(ips);
                    }
                }
            }
            allowedPlayers.addAll(dataConfig.getStringList("allowlist.players"));
            allowedIps.addAll(dataConfig.getStringList("allowlist.ips"));
        } catch (IOException e) {
            getLogger().severe("Failed to load data.yml: " + e.getMessage());
        }
    }

    private void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        dataConfig.set("nicknameToIp", null); // cleanup old layout
        dataConfig.set("nicknameToIps", null);
        for (Map.Entry<String, Set<String>> e : nicknameToIps.entrySet()) {
            dataConfig.set("nicknameToIps." + e.getKey(), new ArrayList<>(e.getValue()));
        }
        dataConfig.set("allowlist.players", allowedPlayers.stream().sorted().toList());
        dataConfig.set("allowlist.ips", allowedIps.stream().sorted().toList());
        try {
            if (dataFile == null) {
                dataFile = new File(getDataFolder(), "data.yml");
            }
            dataConfig.save(dataFile);
        } catch (IOException e) {
            getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }

    private final class LoginListener implements Listener {
        @EventHandler
        public void onPreLogin(AsyncPlayerPreLoginEvent event) {
            String name = event.getName();
            String nameKey = name.toLowerCase();
            String ip = event.getAddress().getHostAddress();

            boolean allowlistPlayersOnly = getConfig().getBoolean("general.allowlist_players_only", false);
            boolean allowlistIpsOnly = getConfig().getBoolean("general.allowlist_ips_only", false);
            boolean ipLockEnabled = getConfig().getBoolean("general.ip_lock_enabled", true);
            int autoLearnUpTo = Math.max(1, getConfig().getInt("general.ip_lock_auto_learn_up_to", 3));
            boolean allowSameSubnet24 = getConfig().getBoolean("general.ip_lock_allow_same_subnet_24", true);
            List<String> protectedPlayers = getConfig().getStringList("protected.players");

                if (allowlistPlayersOnly && !allowedPlayers.contains(nameKey)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, color(kickBi("disallow_player_not_allowed_bi")));
                return;
            }
            if (allowlistIpsOnly && !allowedIps.contains(ip)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, color(kickBi("disallow_ip_not_allowed_bi")));
                return;
            }

            if (ipLockEnabled) {
                Set<String> known = nicknameToIps.computeIfAbsent(nameKey, k -> ConcurrentHashMap.newKeySet());
                boolean isProtected = protectedPlayers.stream().anyMatch(s -> s.equalsIgnoreCase(nameKey));
                if (isProtected) {
                    if (!known.contains(ip)) {
                        // For protected: no auto-learn, no /24
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, color(kickBi("disallow_ip_mismatch_bi")));
                        return;
                    }
                } else {
                    if (known.isEmpty()) {
                        known.add(ip);
                        saveData();
                    } else if (!known.contains(ip)) {
                        boolean sameSubnet = allowSameSubnet24 && known.stream().anyMatch(kip -> sameSubnet24(kip, ip));
                        if (sameSubnet) {
                            known.add(ip);
                            saveData();
                        } else if (known.size() < autoLearnUpTo) {
                            known.add(ip);
                            saveData();
                        } else {
                            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, color(kickBi("disallow_ip_mismatch_bi")));
                            return;
                        }
                    }
                }
            }

            // Mark player as untrusted for first join; they will be marked trusted after a delay in main thread.
        }
    }

    private final class CommandListener implements Listener {
        @EventHandler
        public void onCommand(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            String raw = event.getMessage();
            String base = raw.startsWith("/") ? raw.substring(1) : raw;
            String cmd = base.split(" ")[0].toLowerCase();

            boolean globalBlock = getConfig().getBoolean("general.block_dangerous_commands_globally", true);
            boolean restrictNew = getConfig().getBoolean("security.restrict_new_players", true);
            int cooldownSec = Math.max(0, getConfig().getInt("security.command_cooldown_seconds", 1));

            if (globalBlock) {
                for (String blocked : getConfig().getStringList("security.blocked_commands_global")) {
                    if (cmd.equalsIgnoreCase(blocked) || cmd.endsWith(":" + blocked.toLowerCase())) {
                        player.sendMessage(color(msg(player, "blocked_command_global")));
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            if (cooldownSec > 0) {
                long now = System.currentTimeMillis();
                Long last = lastCommandAtMs.get(uuid);
                if (last != null && now - last < cooldownSec * 1000L) {
                    long leftMs = (cooldownSec * 1000L) - (now - last);
                    long leftSec = Math.max(1, (leftMs + 999) / 1000);
                    player.sendMessage(color(msg(player, "command_cooldown").replace("%seconds%", String.valueOf(leftSec))));
                    event.setCancelled(true);
                    return;
                }
                lastCommandAtMs.put(uuid, now);
            }

            if (restrictNew && !trustedPlayers.contains(uuid)) {
                // Block WorldEdit '//' style too
                if (base.startsWith("//")) {
                    player.sendMessage(color(msg(player, "blocked_command_untrusted")));
                    event.setCancelled(true);
                    return;
                }
                for (String blocked : getConfig().getStringList("security.blocked_for_untrusted")) {
                    if (cmd.equalsIgnoreCase(blocked) || cmd.endsWith(":" + blocked.toLowerCase())) {
                        player.sendMessage(color(msg(player, "blocked_command_untrusted")));
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private final class JoinListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            // Apply pre-trusted by nickname
            List<String> pretrusted = dataConfig.getStringList("trusted.nicknames");
            if (pretrusted.stream().anyMatch(n -> n.equalsIgnoreCase(player.getName()))) {
                trustedPlayers.add(uuid);
                return;
            }
            if (trustedPlayers.contains(uuid)) {
                return;
            }
            int trustDelay = Math.max(10, getConfig().getInt("security.trust_after_seconds", 120));
            Bukkit.getScheduler().runTaskLater(AntiRaidSystem.this, () -> {
                trustedPlayers.add(uuid);
            }, trustDelay * 20L);
        }
    }

    private String kickBi(String key) {
        String prefix = getConfig().getString("messages.prefix", "");
        String value = getConfig().getString("messages.kick." + key, key);
        return prefix + value;
    }

    private boolean sameSubnet24(String ip1, String ip2) {
        String[] a = ip1.split("\\.");
        String[] b = ip2.split("\\.");
        if (a.length != 4 || b.length != 4) return false;
        return a[0].equals(b[0]) && a[1].equals(b[1]) && a[2].equals(b[2]);
    }
}
