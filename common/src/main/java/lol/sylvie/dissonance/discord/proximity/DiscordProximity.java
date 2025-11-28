package lol.sylvie.dissonance.discord.proximity;

import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/*
 * this is really shabbily put together
 * if anyone is reading this and wants to optimize this, please do
 */
public class DiscordProximity {
    public static boolean ENABLED = false;
    public static final HashSet<Long> MUTED = new HashSet<>();

    private static GuildVoiceState getNonEmptyState(Member member) {
        GuildVoiceState state = member.getVoiceState();
        if (state == null || state.getChannel() == null) return null;
        return state;
    }

    public static boolean isInProximityCategory(VoiceChannel channel) {
        return DissonanceConfig.PROXIMITY_CATEGORY_ID.get().equals(channel.getParentCategoryIdLong());
    }

    public static @Nullable VoiceChannel getVoiceChannel(Member member) {
        GuildVoiceState state = getNonEmptyState(member);
        if (state == null || state.getChannel() == null ||  state.getChannel().getType() != ChannelType.VOICE) return null;
        return state.getChannel().asVoiceChannel();
    }

    public static boolean isInProximityCategory(Member member) {
        VoiceChannel channel = getVoiceChannel(member);
        if (channel == null) return false;
        return isInProximityCategory(channel);
    }

    public static Category getCategory(Guild guild) {
        return guild.getCategoryById(DissonanceConfig.PROXIMITY_CATEGORY_ID.get());
    }

    public static Category getCategory() {
        Guild guild = DiscordLinking.getGuild();
        if (guild == null) return null;
        return getCategory(guild);
    }

    public static VoiceChannel getLobbyChannel(@Nullable Guild guild) {
        if (guild == null) return null;
        return guild.getVoiceChannelById(DissonanceConfig.PROXIMITY_LOBBY_ID.get());
    }

    public static void init() {
        ENABLED = DissonanceConfig.LINKING_ENABLED.get() && DissonanceConfig.PROXIMITY_ENABLED.get();
        if (!ENABLED) return;

        Guild guild = DiscordLinking.getGuild();
        if (guild == null || getLobbyChannel(guild) == null) {
            ENABLED = false;
            Constants.LOG.error("Missing either linking guild or proximity channel!");
            return;
        }

        Category category = getCategory(guild);
        if (category == null || !guild.getSelfMember().hasPermission(category, Set.of(Permission.MANAGE_CHANNEL, Permission.VOICE_MOVE_OTHERS))) {
            ENABLED = false;
            Constants.LOG.error("Cannot move members or delete channels in proximity category!");
            return;
        }


        if (ENABLED) {
            for (GuildChannel channel : category.getChannels()) {
                if (channel.getType() != ChannelType.VOICE || DissonanceConfig.PROXIMITY_LOBBY_ID.get().equals(channel.getIdLong())) continue;

                channel.delete().queue();
            }

            if (!guild.getSelfMember().hasPermission(category, Set.of(Permission.MANAGE_PERMISSIONS, Permission.VOICE_MUTE_OTHERS)) && DissonanceConfig.MUTE_LOBBY_MEMBERS.get()) {
                Constants.LOG.error("Cannot mute lobby members! (the bot must have the Manage Permissions and Mute Members permissions for the category!)");
            }

            Constants.LOG.warn("Proximity chat is experimental. Use with caution!");

        }
    }

    public static final HashMap<UUID, ProximityGroup> playerToGroup = new HashMap<>();
    public static void update(MinecraftServer server) {
        if (!ENABLED) return;

        int radius = DissonanceConfig.PROXIMITY_RADIUS.get();
        int grace = DissonanceConfig.PROXIMITY_GRACE_PERIOD.get();

        Set<ServerPlayer> connected = server.getPlayerList().getPlayers()
                .stream()
                .filter(p -> {
                    Member member = DiscordLinking.getMemberFromMinecraft(p.getUUID());
                    return member != null && isInProximityCategory(member);
                })
                .collect(Collectors.toSet());

        int graceDist = radius + grace;
        int longDist = graceDist * graceDist;
        int shortDist = radius * radius;

        grouping:
        for (ServerPlayer player : connected) {
            UUID uuid = player.getUUID();

            if (playerToGroup.containsKey(uuid)) {
                ProximityGroup currentGroup = playerToGroup.get(uuid);

                if (!currentGroup.isInRadius(server, player, longDist)) {
                    currentGroup.remove(uuid, true, false);
                    playerToGroup.remove(uuid);
                }
            } else {
                // See if we can join any groups
                for (ProximityGroup group : playerToGroup.values()) {
                    if (group.isInRadius(server, player, shortDist)) {
                        group.add(uuid, true);
                        playerToGroup.put(uuid, group);
                        continue grouping;
                    }
                }

                // Create a group if we aren't in one
                for (ServerPlayer otherPlayer : connected) {
                    if (player.isDeadOrDying() || otherPlayer.isDeadOrDying() || otherPlayer.equals(player) || otherPlayer.distanceToSqr(player) > shortDist) continue;

                    UUID otherUuid = otherPlayer.getUUID();
                    ProximityGroup group = new ProximityGroup(List.of(uuid, otherUuid));
                    playerToGroup.put(uuid, group);
                    playerToGroup.put(otherUuid, group);
                }
            }

            ProximityGroup ourGroup = playerToGroup.get(uuid);
            if (ourGroup != null) {
                for (ProximityGroup group : new HashSet<>(playerToGroup.values())) {
                    if (ourGroup == group) continue;

                    if (!group.isInRadius(server, player, shortDist)) continue;

                    Set<UUID> affectedPlayers;
                    ProximityGroup parentGroup;
                    if (group.players.size() > ourGroup.players.size()) {
                        affectedPlayers = ourGroup.players;
                        group.merge(ourGroup);
                        parentGroup = group;
                    } else {
                        affectedPlayers = group.players;
                        ourGroup.merge(group);
                        parentGroup = ourGroup;
                    }

                    affectedPlayers.forEach((id) -> playerToGroup.put(id, parentGroup));
                    break;
                }
            }
        }

        Set.copyOf(playerToGroup.values()).forEach(group -> {
            for (UUID uuid : group.collectDisconnectedMembers()) {
                playerToGroup.remove(uuid);
                group.remove(uuid, false, true);
            }
            group.implodeIfNeeded();

            if (!group.isMarkedForRemoval()) {
                ArrayList<Set<UUID>> groups = new ArrayList<>(group.split(server, longDist).stream().sorted(Comparator.comparing(Set::size)).toList().reversed());
                if (groups.size() == 1) return;
                groups.removeFirst();
                for (Set<UUID> newGroup : groups) {
                    ProximityGroup groupInstance = new ProximityGroup();
                    for (UUID member : newGroup) {
                        group.remove(member, false, false);
                        groupInstance.add(member, false);
                        playerToGroup.put(member, groupInstance);
                    }
                    groupInstance.createVoiceChannel();
                }
            }
        });
    }

    public static void onDisconnect(Player player) {
        UUID uuid = player.getUUID();
        if (!playerToGroup.containsKey(uuid)) return;

        ProximityGroup group = playerToGroup.get(uuid);
        group.remove(uuid, false, true);
        group.implodeIfNeeded();
        playerToGroup.remove(uuid);
    }

    public static void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        AudioChannelUnion oldChannel = event.getChannelLeft();
        AudioChannelUnion newChannel = event.getChannelJoined();
        Long lobbyId = DissonanceConfig.PROXIMITY_LOBBY_ID.get();
        Member member = event.getEntity();
        Long memberId = member.getIdLong();
        Guild guild = event.getGuild();

        Category category = getCategory(guild);
        boolean mayMute = category != null && guild.getSelfMember().hasPermission(category, Set.of(Permission.MANAGE_PERMISSIONS, Permission.VOICE_MUTE_OTHERS)) && DissonanceConfig.MUTE_LOBBY_MEMBERS.get();
        boolean wasMuted = MUTED.contains(memberId);
        if (!mayMute) return;
        if (((oldChannel != null && lobbyId.equals(oldChannel.getIdLong())) || newChannel == null) && wasMuted) {
            member.mute(false).queue();
        } else if (newChannel != null && lobbyId.equals(newChannel.getIdLong())) {
            member.mute(true).queue();
            MUTED.add(memberId);
        }
    }
}
