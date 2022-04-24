package dev.plex.punishment;

import com.google.gson.GsonBuilder;
import dev.morphia.annotations.Entity;
import dev.plex.Plex;
import dev.plex.util.MojangUtils;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import java.time.ZonedDateTime;
import java.util.UUID;

import dev.plex.util.adapter.ZonedDateTimeDeserializer;
import dev.plex.util.adapter.ZonedDateTimeSerializer;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
@Setter
@Entity
public class Punishment
{
    private static final String banUrl = Plex.get().config.getString("banning.ban_url");
    private final UUID punished;
    private final UUID punisher;
    private String ip;
    private String punishedUsername;
    private PunishmentType type;
    private String reason;
    private boolean customTime;
    private boolean active; // Field is only for bans
    private ZonedDateTime endDate;

    public Punishment()
    {
        this.punished = null;
        this.punisher = null;
    }

    public Punishment(UUID punished, UUID punisher)
    {
        this.punished = punished;
        this.punisher = punisher;
    }

    public static Component generateBanMessage(Punishment punishment)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), TimeUtils.useTimezone(punishment.getEndDate()), punishment.getPunisher() == null ? "CONSOLE" : MojangUtils.getInfo(punishment.getPunisher().toString()).getUsername());
    }

    public static Component generateIndefBanMessage(String type)
    {
        return PlexUtils.messageComponent("indefBanMessage", type, banUrl);
    }

    public static Punishment fromJson(String json)
    {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer()).create().fromJson(json, Punishment.class);
    }

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer()).create().toJson(this);
    }
}