package lol.sylvie.dissonance.discord.proximity;

import lol.sylvie.dissonance.discord.linking.DiscordLinking;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ProximityGroup {
    private final UUID groupId;
    public final HashSet<UUID> players;
    private final Guild guild;
    private VoiceChannel channel;

    private boolean markedForRemoval = false;

    public ProximityGroup() {
        this.groupId = UUID.randomUUID();
        this.players = new HashSet<>();
        this.guild = DiscordLinking.getGuild();
    }

    public ProximityGroup(List<UUID> players) {
        this();
        players.forEach(p -> this.add(p, false));
        createVoiceChannel();
    }

    public void createVoiceChannel() {
        Category category = DiscordProximity.getCategory();
        if (category == null) return;

        category.createVoiceChannel(this.groupId.toString())
                .addPermissionOverride(guild.getPublicRole(), List.of(Permission.VOICE_SPEAK), List.of(Permission.VIEW_CHANNEL))
                .addPermissionOverride(guild.getSelfMember(), List.of(Permission.VIEW_CHANNEL, Permission.VOICE_MOVE_OTHERS), List.of())
                .queue(this::onChannelCreate);
    }

    private void movePlayer(UUID player, AudioChannel channel, Runnable onMove) {
        Member member = DiscordLinking.getMemberFromMinecraft(player);
        if (member == null || !DiscordProximity.isInProximityCategory(member)) return;

        this.guild.moveVoiceMember(member, channel).queue(unused -> onMove.run());
    }


    private void movePlayer(UUID player, AudioChannel channel) {
        movePlayer(player, channel, () -> {
            if (this.channel != null && this.channel.getMembers().isEmpty()) {
                this.channel.delete().queue();
                this.channel = null;
            }
        });
    }

    private void movePlayerHere(UUID player) {
        movePlayer(player, this.channel, () -> {});
    }

    public void onChannelCreate(VoiceChannel channel) {
        this.channel = channel;

        for (UUID uuid : players) {
            movePlayerHere(uuid);
        }
    }

    public void implode(@Nullable VoiceChannel destination) {
        for (UUID stillHere : new HashSet<>(players)) {
            remove(stillHere, destination == null, false);
            if (destination != null) {
                movePlayer(stillHere, destination);
            }
        }

    }

    public HashSet<UUID> recursiveNetworkSplit(MinecraftServer server, ServerPlayer current, HashSet<UUID> found, int distanceSquared) {
        for (UUID uuid : players) {
            if (found.contains(uuid) || uuid.equals(current.getUUID())) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null && player.isAlive() && player.distanceToSqr(current) < distanceSquared) {
                found.add(uuid);
                found.addAll(recursiveNetworkSplit(server, current, found, distanceSquared));
            }
        }
        return found;
    }

    public void implodeIfNeeded() {
        if (!markedForRemoval) return;

        implode(null);
    }

    public List<UUID> collectDisconnectedMembers() {
        ArrayList<UUID> removed = new ArrayList<>();
        for (UUID uuid : players) {
            Member member = DiscordLinking.getMemberFromMinecraft(uuid);
            if (member == null || !DiscordProximity.isInProximityCategory(member)) {
                removed.add(uuid);
            }
        }
        return removed;
    }

    public void add(UUID player, boolean move) {
        this.players.add(player);
        if (move) movePlayerHere(player);
    }

    public void remove(UUID player, boolean moveToLobby, boolean shouldKick) {
        this.players.remove(player);

        if (moveToLobby) {
            VoiceChannel lobby = DiscordProximity.getLobbyChannel(guild);
            if (lobby != null)
                movePlayer(player, lobby);
        } else if (shouldKick) {
            Member member = DiscordLinking.getMemberFromMinecraft(player);
            if (member != null && DiscordProximity.isInProximityCategory(member))
                this.guild.kickVoiceMember(member).queue();
        }

        if (this.players.size() < 2)
            markedForRemoval = true;
    }

    public boolean isInRadius(MinecraftServer server, ServerPlayer player, int distanceSquared) {
        for (UUID uuid : players) {
            if (uuid.equals(player.getUUID())) continue;
            ServerPlayer participant = server.getPlayerList().getPlayer(uuid);
            if (participant != null && participant.isAlive() && participant.distanceToSqr(player) < distanceSquared) return true;
        }
        return false;
    }

    public void merge(ProximityGroup group) {
        group.implode(this.channel);
    }

    public Set<Set<UUID>> split(MinecraftServer server, int distanceSquared) {
        HashSet<Set<UUID>> groups = new HashSet<>();
        HashSet<UUID> accountedFor = new HashSet<>();
        for (UUID uuid : players) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (accountedFor.contains(uuid)) continue;

            Set<UUID> uuids = recursiveNetworkSplit(server, player, new HashSet<>(Set.of(uuid)), distanceSquared);
            accountedFor.addAll(uuids);
            groups.add(uuids);
        }
        return groups;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }
}
