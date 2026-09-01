package com.survivalutils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final SurvivalUtils plugin;

    public SpawnCommand(SurvivalUtils plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }
        if (plugin.getTeleportWarmupManager().isPending(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have a teleport pending.", NamedTextColor.RED));
            return true;
        }
        plugin.getTeleportWarmupManager().startWarmup(player, "spawn", () -> {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
            player.sendMessage(Component.text("Teleported to spawn!", NamedTextColor.GREEN));
        });
        return true;
    }
}
