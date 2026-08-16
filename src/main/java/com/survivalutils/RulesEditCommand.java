package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Registered in plugin.yml under the literal command name "/rules", so typing
 * //rules in chat (client strips one leading slash, leaving "/rules ...") reaches
 * this executor. Permission-gated to elemental.admin-style OPs only.
 */
public class RulesEditCommand implements CommandExecutor {

    private final SurvivalUtils plugin;

    public RulesEditCommand(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("survivalutils.rules.edit")) {
            sender.sendMessage(Component.text("You do not have permission to edit the rules.", NamedTextColor.RED));
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: //rules add <text>", NamedTextColor.RED));
                    return true;
                }
                String line = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getRulesManager().addRule(line);
                sender.sendMessage(Component.text("Added rule: " + line, NamedTextColor.GREEN));
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: //rules remove <number>", NamedTextColor.RED));
                    return true;
                }
                int index;
                try {
                    index = Integer.parseInt(args[1]) - 1;
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Rule number must be a number.", NamedTextColor.RED));
                    return true;
                }
                boolean removed = plugin.getRulesManager().removeRule(index);
                sender.sendMessage(removed
                        ? Component.text("Removed rule #" + (index + 1) + ".", NamedTextColor.GREEN)
                        : Component.text("No rule with that number.", NamedTextColor.RED));
            }
            case "clear" -> {
                plugin.getRulesManager().clearRules();
                sender.sendMessage(Component.text("Cleared all rules.", NamedTextColor.YELLOW));
            }
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Usage: //rules set <line1> | <line2> | ...", NamedTextColor.RED));
                    return true;
                }
                String joined = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                List<String> lines = Arrays.stream(joined.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
                plugin.getRulesManager().setRules(lines);
                sender.sendMessage(Component.text("Rules replaced with " + lines.size() + " line(s).", NamedTextColor.GREEN));
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("--- //rules usage ---", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("//rules add <text>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("//rules remove <number>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("//rules set <line1> | <line2> | ...", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("//rules clear", NamedTextColor.YELLOW));
    }
}
