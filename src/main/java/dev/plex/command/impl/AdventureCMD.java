package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandPermissions(level = Rank.OP, permission = "plex.gamemode.adventure", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adventure", aliases = "gma", description = "Set your own or another player's gamemode to adventure mode")
public class AdventureCMD extends PlexCommand
{

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        Player player = (Player) sender;
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException("You must define a player when using the console!");
            }
            player.setGameMode(GameMode.ADVENTURE);
            return tl("gameModeSetTo", "adventure");
        }

        if (checkRank(player, Rank.ADMIN, "plex.gamemode.adventure.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.ADVENTURE);
                }
                return tl("gameModeSetTo", "adventure");
            }

            Player nPlayer = getNonNullPlayer(args[0]);
            // use send
            send(nPlayer, tl("playerSetOtherGameMode", sender.getName(), "adventure"));
            nPlayer.setGameMode(GameMode.ADVENTURE);
            return tl("setOtherPlayerGameModeTo", nPlayer.getName(), "adventure");
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (isAdmin(sender))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }
}