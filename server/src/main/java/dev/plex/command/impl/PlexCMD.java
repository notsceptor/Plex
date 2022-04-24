package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import dev.plex.rank.enums.Rank;
import dev.plex.util.BuildInfo;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.plex", source = RequiredCommandSource.ANY)
@CommandParameters(name = "plex", usage = "/<command> [reload | redis | modules [reload]]", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand
{
    // Don't modify this command
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            send(sender, mmString("<light_purple>Plex - A new freedom plugin."));
            send(sender, mmString("<light_purple>Plugin version: <gold>" + plugin.getDescription().getVersion() + " #" + BuildInfo.getNumber() + " <light_purple>Git: <gold>" + BuildInfo.getHead()));
            send(sender, mmString("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
            send(sender, mmString("<light_purple>Built by: <gold>" + BuildInfo.getAuthor() + " <light_purple>on <gold>" + BuildInfo.getDate()));
            send(sender, mmString("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
            plugin.getUpdateChecker().getUpdateStatusMessage(sender, true, 2);
            return null;
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.reload");
            plugin.config.load();
            send(sender, "Reloaded config file");
            plugin.messages.load();
            send(sender, "Reloaded messages file");
            plugin.indefBans.load(false);
            plugin.getPunishmentManager().mergeIndefiniteBans();
            send(sender, "Reloaded indefinite bans");
            plugin.commands.load();
            send(sender, "Reloaded blocked commands file");
            plugin.getRankManager().importDefaultRanks();
            send(sender, "Imported ranks");
            plugin.setSystem(plugin.config.getString("system"));
            if (!plugin.setupPermissions() && plugin.getSystem().equalsIgnoreCase("permissions") && !plugin.getServer().getPluginManager().isPluginEnabled("Vault"))
            {
                throw new RuntimeException("Vault is required to run on the server if you use permissions!");
            }
            plugin.getServiceManager().endServices();
            plugin.getServiceManager().startServices();
            send(sender, "Restarted services.");
            TimeUtils.TIMEZONE = plugin.config.getString("server.timezone");
            send(sender, "Set timezone to: " + TimeUtils.TIMEZONE);
            send(sender, "Plex successfully reloaded.");
            return null;
        }
        else if (args[0].equalsIgnoreCase("redis"))
        {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.redis");
            if (!plugin.getRedisConnection().isEnabled())
            {
                throw new CommandFailException("&cRedis is not enabled.");
            }
            plugin.getRedisConnection().getJedis().set("test", "123");
            send(sender, "Set test to 123. Now outputting key test...");
            send(sender, plugin.getRedisConnection().getJedis().get("test"));
            plugin.getRedisConnection().getJedis().close();
            return null;
        }
        else if (args[0].equalsIgnoreCase("modules"))
        {
            if (args.length == 1)
            {
                return mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
            }
            if (args[1].equalsIgnoreCase("reload"))
            {
                checkRank(sender, Rank.EXECUTIVE, "plex.modules.reload");
                plugin.getModuleManager().reloadModules();
                return mmString("<green>All modules reloaded!");
            }
            else if (args[1].equalsIgnoreCase("update"))
            {
                if (sender instanceof Player && !PlexUtils.DEVELOPERS.contains(playerSender.getUniqueId().toString()))
                {
                    return messageComponent("noPermissionRank", "a developer");
                }
                for (PlexModule module : plugin.getModuleManager().getModules())
                {
                    plugin.getUpdateChecker().updateJar(sender, module.getPlexModuleFile().getName(), true);
                }
                plugin.getModuleManager().reloadModules();
                return mmString("<green>All modules updated and reloaded!");
            }
        }
        else if (args[0].equalsIgnoreCase("update"))
        {
            if (sender instanceof Player && !PlexUtils.DEVELOPERS.contains(playerSender.getUniqueId().toString()))
            {
                return messageComponent("noPermissionRank", "a developer");
            }
            if (!plugin.getUpdateChecker().getUpdateStatusMessage(sender, false, 0))
            {
                return mmString("<red>Plex is already up to date!");
            }
            plugin.getUpdateChecker().updateJar(sender, "Plex", false);
            return mmString("<red>Alert: Restart the server for the new JAR file to be applied.");
        }
        else
        {
            return usage();
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return Arrays.asList("reload", "redis", "modules");
        }
        else if (args[0].equalsIgnoreCase("modules"))
        {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}