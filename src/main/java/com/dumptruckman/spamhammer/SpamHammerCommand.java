package com.dumptruckman.spamhammer;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * SpamHammer command processor.
 * @author dumptruckman, modified by Tulonsae.
 */
public class SpamHammerCommand implements CommandExecutor {

    SpamHammer plugin;

    public SpamHammerCommand(SpamHammer plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("spamunban")) {
            if (!sender.isOp() && !sender.hasPermission("spamhammer.unban")) {
                sender.sendMessage("You do not have permission to access this command.");
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            if (plugin.isBanned(args[0])) {
                plugin.unBanPlayer(args[0]);
                sender.sendMessage("Player has been unbanned.");
            } else {
                sender.sendMessage("Player is not banned by SpamHammer.");
            }
            return true;
        } else if (command.getName().equals("spamunmute")) {
            if (!sender.isOp() && !sender.hasPermission("spamhammer.unmute")) {
                sender.sendMessage("You do not have permission to access this command.");
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            if (plugin.isMuted(args[0])) {
                plugin.unMutePlayer(args[0]);
                plugin.beenMuted.remove(args[0]);
                sender.sendMessage("Player has been unmuted.");
            } else {
                sender.sendMessage("Player is not muted.");
            }
            return true;
        } else if (command.getName().equals("spamreset")) {
            if (!sender.isOp() && !sender.hasPermission("spamhammer.reset")) {
                sender.sendMessage("You do not have permission to access this command.");
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            if (plugin.beenMuted(args[0])) {
                plugin.beenMuted.remove(args[0]);
            }
            if (plugin.beenKicked(args[0])) {
                plugin.beenKicked.remove(args[0]);
            }
            sender.sendMessage("Player's punishment level has been reset.");
            return true;
        } else {
            return false;
        }
    }
}
