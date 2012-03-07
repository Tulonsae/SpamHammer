package com.dumptruckman.spamhammer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Anti-spam bukkit plugin.
 * @author dumptruckman, modified by Tulonsae.
 */
public class SpamHammer extends JavaPlugin {

    private Logger log;
    private String version;

    private int messageLimit;
    private int repeatLimit;
    private long timePeriod;
    public boolean useRepeatLimit;
    public boolean preventMessages;
    private boolean useMute;
    private boolean useBan;
    private boolean useKick;
    private int muteLength;
    private int cooloffLength;
    private String muteMessage;
    private String unmuteMessage;
    private String mutedMessage;
    private String kickMessage;
    private String banMessage;
    private String cooloffMessage;
    private String rateMessage;
    public List<String> spamCommands;

    private Map<String, ArrayDeque<Long>> playerChatTimes;
    private Map<String, ArrayDeque<String>> playerChatHistory;
    private List<String> mutedPlayers;
    private Map<String, Long> actionTime;
    public List<String> beenMuted;
    public List<String> beenKicked;

    private Timer timer;

    public YamlConfiguration banList;
    private static String banListFileName = "banlist.yml";

    public SpamHammer() {

        playerChatTimes = new HashMap<String, ArrayDeque<Long>>();
        playerChatHistory = new HashMap<String, ArrayDeque<String>>();
        mutedPlayers = new ArrayList<String>();
        beenMuted = new ArrayList<String>();
        beenKicked = new ArrayList<String>();
        actionTime = new HashMap<String, Long>();
    }

    /**
     * Called when plugin is enabled.
     */
    public void onEnable() {

        // get plugin info
        version = this.getDescription().getVersion();
        log = this.getLogger();
        PluginManager pm = getServer().getPluginManager();

        // initial enable message
        log.info("enabling version " + version + ".");

        // load configuration
        loadConfig();

        // load ban list
        banList = YamlConfiguration.loadConfiguration(new File(banListFileName));

        // register events
        pm.registerEvents(new SpamHammerListener(this), this);

        // register commands
        SpamHammerCommand cmd = new SpamHammerCommand(this);
        getCommand("spamunban").setExecutor(cmd);
        getCommand("spamunmute").setExecutor(cmd);
        getCommand("spamreset").setExecutor(cmd);

        // setup timer task
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SpamHammer.this.checkTimes();
            }
        }, 0, 1000);

        // final enable message
        log.info("version " + version + " enabled.");
    }

    /**
     * Called when plugin is disabled.
     */
    public void onDisable() {

        // save configuration
        saveConfig();

        // disable message
        log.info("version " + version + " disabled.");
    }

    /**
     * Get configuration settings from config file.
     */
    private void loadConfig() {
        // load defaults
        getConfig().options().copyDefaults(true);

        // get settings
        messageLimit = getConfig().getInt("message.limit");
        repeatLimit = getConfig().getInt("message.repeat.limit");
        timePeriod = getConfig().getInt("message.period");
        useRepeatLimit = getConfig().getBoolean("message.repeat.block");
        preventMessages = getConfig().getBoolean("message.preventabovelimit");
        useBan = getConfig().getBoolean("punishments.ban");
        useKick = getConfig().getBoolean("punishments.kick");
        useMute = getConfig().getBoolean("punishments.mute");
        muteLength = getConfig().getInt("mute.length");
        cooloffLength = getConfig().getInt("cooloff.time");
        String message = getConfig().getString("mute.message.mute");
        Integer length = (Integer)muteLength;
        muteMessage = message.replace("%t", length.toString());
        unmuteMessage = getConfig().getString("mute.message.unmute");
        mutedMessage = getConfig().getString("mute.message.muted");
        kickMessage = getConfig().getString("kick.message");
        banMessage = getConfig().getString("ban.message");
        cooloffMessage = getConfig().getString("cooloff.message");
        rateMessage = getConfig().getString("abovelimit.message");
        spamCommands = getConfig().getStringList("commandlist.possiblespam");

        // save config
        saveConfig();
    }

    /**
     * Get muted message.
     */
    public String getMutedMessage() {
        return mutedMessage;
    }

    /**
     * Get ban message.
     */
    public String getBanMessage() {
        return banMessage;
    }

    /**
     * Get rate limit message.
     */
    public String getRateLimitMessage() {
        return rateMessage;
    }

    /**
     * Get prevent messages flag.
     */
    public Boolean isPreventMessages() {
        return preventMessages;
    }

    public boolean addChatMessage(String name, String message) {
        boolean isSpamming = false;

        // Detect rate limited messages
        ArrayDeque<Long> times = playerChatTimes.get(name);
        if (times == null) times = new ArrayDeque<Long>();
        long curtime = System.currentTimeMillis();
        times.add(curtime);
        if (times.size() > messageLimit) {
            times.remove();
        }
        long timediff = times.getLast() - times.getFirst();
        if (timediff > timePeriod) {
            times.clear();
            times.add(curtime);
        }
        if (times.size() == messageLimit) {
            isSpamming = true;
        }
        playerChatTimes.put(name, times);

        // Detect duplicate messages
        if (useRepeatLimit && !isSpamming) {
            ArrayDeque<String> player = playerChatHistory.get(name);
            if (player == null) player = new ArrayDeque<String>();
            player.add(message);
            if (player.size() > (repeatLimit + 1)) {
                player.remove();
            }
            playerChatHistory.put(name, player);
            isSpamming = hasDuplicateMessages(name);
        }
        
        if (isSpamming) {
            playerIsSpamming(name);
        }
        return isSpamming;
    }

    public boolean hasDuplicateMessages(String name) {
        boolean isSpamming = false;
        int samecount = 1;
        String lastMessage = null;
        for (Object m : playerChatHistory.get(name).toArray()) {
            String message = m.toString();
            if (lastMessage == null) {
                lastMessage = message;
                continue;
            }
            if (message.equals(lastMessage)) {
                samecount++;
            } else {
                playerChatHistory.get(name).clear();
                playerChatHistory.get(name).add(message);
                break;
            }
            isSpamming = (samecount > repeatLimit);
        }
        return isSpamming;
    }

    public void playerIsSpamming(String name) {
        if(useMute && (!beenMuted(name) || (!useKick && !useBan))) {
            mutePlayer(name);
            return;
        }
        if (useKick && (!beenKicked(name) || !useBan)) {
            kickPlayer(name);
            return;
        }
        if (useBan) {
            banPlayer(name);
        }
    }

    public boolean isMuted(String name) {
        return mutedPlayers.contains(name);
    }

    public boolean isBanned(String name) {
        List<String> bannedplayers = banList.getStringList("banned");
        if (bannedplayers == null) return false;
        return bannedplayers.contains(name);
    }

    public boolean beenMuted(String name) {
        return beenMuted.contains(name);
    }

    public boolean beenKicked(String name) {
        return beenKicked.contains(name);
    }

    public void mutePlayer(String name) {
        mutedPlayers.add(name);
        beenMuted.add(name);
        actionTime.put(name, System.currentTimeMillis() / 1000);
        getServer().getPlayer(name).sendMessage(muteMessage);
    }

    public void unMutePlayer(String name) {
        mutedPlayers.remove(name);
        if (getServer().getPlayer(name) != null) {
            getServer().getPlayer(name).sendMessage(unmuteMessage);
        }
    }

    public void kickPlayer(String name) {
        beenKicked.add(name);
        actionTime.put(name, System.currentTimeMillis() / 1000);
        if (getServer().getPlayer(name) != null) {
            getServer().getPlayer(name).kickPlayer(kickMessage);
        }
    }

    public void banPlayer(String name) {
        if (getServer().getPlayer(name) != null) {
            getServer().getPlayer(name).kickPlayer(banMessage);
        }
        List<String> bannedplayers = banList.getStringList("banned");
        if (bannedplayers == null) bannedplayers = new ArrayList<String>();
        bannedplayers.add(name);
        banList.set("banned", bannedplayers);
        saveBanList();
    }

    private void saveBanList() {
        try {
            banList.save(banListFileName);
        } catch (IOException e) {
            log.severe("Cannot save banlist file.");
            e.printStackTrace();
        }
    }

    public void unBanPlayer(String name) {
        List<String> bannedplayers = banList.getStringList("banned");
        if (bannedplayers == null) return;
        bannedplayers.remove(name);
        banList.set("banned", bannedplayers);
        saveBanList();
    }

    public void checkTimes() {
        Long time = System.currentTimeMillis() / 1000;
        for (String player : actionTime.keySet()) {
            if (isMuted(player)) {
                if (time > (actionTime.get(player) + muteLength)) {
                    unMutePlayer(player);
                }
            }
            if ((time > (actionTime.get(player) + cooloffLength))
                    && (cooloffLength != 0)) {
                if (beenKicked(player)) beenKicked.remove(player);
                if (beenMuted(player)) {
                    if (getServer().getPlayer(player) != null) {
                        getServer().getPlayer(player).sendMessage(cooloffMessage);
                    }
                    beenMuted.remove(player);
                }
            }
        }
    }
}
