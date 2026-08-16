package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class RulesCommand implements CommandExecutor {

    private final SurvivalUtils plugin;

    public RulesCommand(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> lines = plugin.getRulesManager().getRules();
        sender.sendMessage(Component.text("--- Server Rules ---", NamedTextColor.GOLD, TextDecoration.BOLD));
        if (lines.isEmpty()) {
            sender.sendMessage(Component.text("No rules have been set yet.", NamedTextColor.GRAY));
            return true;
        }
        for (int i = 0; i < lines.size(); i++) {
            sender.sendMessage(Component.text((i + 1) + ". ", NamedTextColor.YELLOW)
                    .append(Component.text(lines.get(i), NamedTextColor.WHITE)));
        }
        return true;
    }
}
