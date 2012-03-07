package com.dumptruckman.spamhammer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;

/**
 * Handle specified bukkit events.
 * @author dumptruckman, modified by Tulonsae.
 */
public class SpamHammerListener implements Listener {

    private static String PERM_IGNORE = "spamhammer.ignore";

    private SpamHammer plugin;

    public SpamHammerListener(SpamHammer plugin) {
        this.plugin = plugin;
    }

    /**
     * Handle PlayerChatEvent.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(PlayerChatEvent event) {
        if (event.getPlayer().isOp() || event.getPlayer().hasPermission(PERM_IGNORE))
            return;
        if (plugin.isMuted(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(plugin.getMutedMessage());
            event.setCancelled(true);
            return;
        }
        if (plugin.addChatMessage(event.getPlayer().getName(), event.getMessage())
                && plugin.preventMessages) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getRateLimitMessage());
        }
    }

    /**
     * Handle PlayerLoginEvent.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (plugin.isBanned(event.getPlayer().getName())) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    plugin.getBanMessage());
        }
    }

    /**
     * Handle PlayerCommandPreprocessEvent.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.getPlayer().isOp() || event.getPlayer().hasPermission(PERM_IGNORE))
            return;
        if (plugin.isMuted(event.getPlayer().getName())) {
            event.getPlayer().sendMessage(plugin.getMutedMessage());
            event.setCancelled(true);
            return;
        }
        if (!plugin.spamCommands.contains(event.getMessage().split("\\s")[0])) return;
        if (plugin.addChatMessage(event.getPlayer().getName(), event.getMessage())
                && plugin.isPreventMessages()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getRateLimitMessage());
        }
    }
}
