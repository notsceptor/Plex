package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.cache.MongoPlayerData;
import dev.plex.cache.PlayerCache;
import dev.plex.cache.SQLPlayerData;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener extends PlexListener
{
    private final MongoPlayerData mongoPlayerData = plugin.getMongoPlayerData() != null ? plugin.getMongoPlayerData() : null;
    private final SQLPlayerData sqlPlayerData = plugin.getSqlPlayerData() != null ? plugin.getSqlPlayerData() : null;

    // setting up a player's data
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSetup(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        PlexPlayer plexPlayer;

        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            player.setOp(true);
            PlexLog.debug("Automatically opped " + player.getName() + " since ranks are enabled.");
        }
        else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            player.setOp(false);
            PlexLog.debug("Automatically deopped " + player.getName() + " since ranks are disabled.");
        }

        if (!DataUtils.hasPlayedBefore(player.getUniqueId()))
        {
            PlexLog.log("A player with this name has not joined the server before, creating new entry.");
            plexPlayer = new PlexPlayer(player.getUniqueId()); // it doesn't! okay so now create the object
            plexPlayer.setName(player.getName()); // set the name of the player
            plexPlayer.setIps(Arrays.asList(player.getAddress().getAddress().getHostAddress().trim())); // set the arraylist of ips
            DataUtils.insert(plexPlayer); // insert data in some wack db
        }
        else
        {
            plexPlayer = DataUtils.getPlayer(player.getUniqueId());
            List<String> ips = plexPlayer.getIps();
            String currentIP = player.getAddress().getAddress().getHostAddress().trim();
            if (!ips.contains(currentIP))
            {
                PlexLog.debug("New IP address detected for player: " + player.getName() + ". Adding " + currentIP + " to the database.");
                ips.add(currentIP);
                plexPlayer.setIps(ips);
                DataUtils.update(plexPlayer);
            }
            if (!plexPlayer.getName().equals(player.getName()))
            {
                PlexLog.log(plexPlayer.getName() + " has a new name. Changing it to " + player.getName());
                plexPlayer.setName(player.getName());
                DataUtils.update(plexPlayer);
            }
        }

        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        PlayerCache.getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
        punishedPlayer.convertPunishments();
        if (punishedPlayer.isLockedUp())
        {
            player.openInventory(player.getInventory());
        }

        assert plexPlayer != null;
        String loginMessage = plugin.getRankManager().getLoginMessage(plexPlayer);
        if (!loginMessage.isEmpty())
        {
            PlexUtils.broadcast(MiniMessage.miniMessage().deserialize("<aqua>" + player.getName() + " is " + loginMessage));
        }
    }

    // saving the player's data
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSave(PlayerQuitEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId()); //get the player because it's literally impossible for them to not have an object

        if (plugin.getRankManager().isAdmin(plexPlayer))
        {
            plugin.getAdminList().removeFromCache(UUID.fromString(plexPlayer.getUuid()));
        }

        DataUtils.update(plexPlayer);
        PlayerCache.getPlexPlayerMap().remove(event.getPlayer().getUniqueId()); //remove them from cache
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInventoryClose(InventoryCloseEvent event)
    {
        PunishedPlayer player = PlayerCache.getPunishedPlayer(event.getPlayer().getUniqueId());
        if (player.isLockedUp())
        {
            Bukkit.getScheduler().runTaskLater(plugin, () -> event.getPlayer().openInventory(event.getInventory()), 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        PunishedPlayer player = PlayerCache.getPunishedPlayer(event.getWhoClicked().getUniqueId());
        if (player.isLockedUp())
        {
            event.setCancelled(true);
        }
    }
}
