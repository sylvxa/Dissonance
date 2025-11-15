package lol.sylvie.dissonance.discord.linking;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import lol.sylvie.dissonance.Constants;
import lol.sylvie.dissonance.config.DissonanceConfig;
import lol.sylvie.dissonance.discord.DiscordClient;
import lol.sylvie.dissonance.discord.command.DiscordCommands;
import lol.sylvie.dissonance.discord.proximity.DiscordProximity;
import lol.sylvie.dissonance.minecraft.MinecraftToDiscordBridge;
import lol.sylvie.dissonance.platform.Services;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

// There's so much repeated code here
// I suck at SQL
public class DiscordLinking extends ListenerAdapter {
    public static Connection CONNECTION;
    public static final HashMap<String, Pair<Long, NameAndId>> LINK_CODES = new HashMap<>();
    public static final int LINK_CODE_LIFESPAN = 5 * 60 * 1000;

    public static final HashMap<UUID, String> MC_TO_DISCORD_CACHE = new HashMap<>();

    public static boolean init(MinecraftServer server) throws SQLException {
        if (DissonanceConfig.GUILD_ID.get() == 0 && MinecraftToDiscordBridge.ENABLED) {
            TextChannel channel = MinecraftToDiscordBridge.getOutputChannel();
            if (channel != null) DissonanceConfig.GUILD_ID.set(channel.getGuild().getIdLong());
        }

        Guild guild = getGuild();
        if (guild == null) return false;
        Path path = server.getServerDirectory().resolve("config");

        CONNECTION = DriverManager.getConnection("jdbc:sqlite:" + path.resolve("dissonance.db").toAbsolutePath());
        try (Statement statement = CONNECTION.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS LINKS(discord_id TEXT UNIQUE, minecraft_id TEXT UNIQUE)");
        }

        DiscordCommands.register(guild);
        DiscordProximity.init();
        return true;
    }

    public static void close() {
        try {
            if (isConnected()) CONNECTION.close();
        } catch (SQLException ignored) {}
        CONNECTION = null;
    }

    public static boolean isConnected() {
        return CONNECTION != null;
    }

    public static @Nullable Guild getGuild() {
        if (DiscordClient.CLIENT == null) return null;
        try {
            return DiscordClient.CLIENT.getGuildById(DissonanceConfig.GUILD_ID.get());
        } catch (NumberFormatException exception) {
            Constants.LOG.error("Linking guild ID is invalid!");
        }
        return null;
    }

    public static boolean hasCodeExpired(Pair<Long, NameAndId> code) {
        return System.currentTimeMillis() - code.getFirst() > LINK_CODE_LIFESPAN;
    }

    public static boolean isWhitelistEnabled() {
        return DissonanceConfig.LINKING_ENABLED.get() && DissonanceConfig.WHITELIST_ENABLED.get();
    }

    // Returns null if they can, returns reason why if not
    private static final Component SKILL_ISSUE = Component.literal("There is a configuration error with Discord linking, please contact the server owner.").withStyle(ChatFormatting.RED);

    public static String generateCode(GameProfile profile) {
        for (Map.Entry<String, Pair<Long, NameAndId>> code : new HashSet<>(LINK_CODES.entrySet())) {
            if (!code.getValue().getSecond().id().equals(profile.id())) continue;

            LINK_CODES.remove(code.getKey());
            break;
        }

        // This is a little pedantic (I doubt many are going to RNG manipulate link codes), but just in case
        String codeAsString;
        do {
            int linkCode;
            try {
                linkCode = SecureRandom.getInstanceStrong().nextInt(0, 1_000_000);
            } catch (NoSuchAlgorithmException exception) {
                linkCode = new Random().nextInt(0, 100_000_000);
            }

            codeAsString = String.format("%06d", linkCode);
        } while (LINK_CODES.containsKey(codeAsString));

        LINK_CODES.put(codeAsString, Pair.of(System.currentTimeMillis(), new NameAndId(profile)));
        return codeAsString;
    }

    public static @Nullable Component canPlayerJoin(MinecraftServer server, GameProfile profile) {
        if (!isWhitelistEnabled()) return null;
        if (server.getProfilePermissions(new NameAndId(profile)) >= 4) return null;
        if (DiscordClient.CLIENT == null || !isConnected()) return Component.literal("The server is still starting, please wait a moment and try again.").withStyle(ChatFormatting.RED);

        for (Map.Entry<String, Pair<Long, NameAndId>> codes : LINK_CODES.entrySet().stream().toList()) {
            if (hasCodeExpired(codes.getValue())) LINK_CODES.remove(codes.getKey());
        }

        // handle linking
        String discordId = getDiscordFromMinecraft(profile.id());
        if (discordId == null) {
            return Component.literal(DissonanceConfig.LINK_MESSAGE_TEMPLATE.get().replace("%code%", generateCode(profile)));
        }

        Guild guild = getGuild();
        if (guild == null) {
            Constants.LOG.error("The bot cannot find the linking guild, so players cannot join!");
            return SKILL_ISSUE;
        }

        Member member = guild.retrieveMemberById(discordId).complete();
        Component notAllowed = Component.literal(DissonanceConfig.WHITELIST_MESSAGE.get());
        if (member == null)
            return notAllowed;

        Set<Long> roleIds = member.getUnsortedRoles().stream().map(ISnowflake::getIdLong).collect(Collectors.toSet());
        for (Long id : DissonanceConfig.BLACKLISTED_ROLES.get()) {
            if (roleIds.contains(id)) return notAllowed;
        }

        List<Long> whitelistedRoles = DissonanceConfig.WHITELISTED_ROLES.get();
        if (whitelistedRoles.isEmpty()) {
            return null;
        }

        for (Long id : whitelistedRoles) {
            if (roleIds.contains(id)) {
                return null;
            }
        }

        return notAllowed;
    }

    // SQL stuff
    public static void addLink(User user, UUID id) throws SQLException {
        try (PreparedStatement statement = CONNECTION.prepareStatement("INSERT INTO LINKS (discord_id, minecraft_id) VALUES (?, ?)")) {
            statement.setString(1, user.getId());
            statement.setString(2, id.toString());
            statement.execute();
        }
    }

    private static void removeEntry(String sql, String condition) throws SQLException {
        try (PreparedStatement statement = CONNECTION.prepareStatement(sql)) {
            statement.setString(1, condition);
            statement.execute();
        }
    }

    public static void removeLinkFromDiscord(String id) throws SQLException {
        removeEntry("DELETE FROM LINKS WHERE discord_id = ?", id);
    }

    public static void removeLinkFromMinecraft(UUID uuid) throws SQLException {
        removeEntry("DELETE FROM LINKS WHERE minecraft_id = ?", uuid.toString());
    }

    private static String getString(String sql, String key, String value) {
        try (PreparedStatement statement = CONNECTION.prepareStatement(sql)) {
            statement.setString(1, key);
            ResultSet results = statement.executeQuery();
            return results.getString(value);
        } catch (SQLException exception) {
            return null;
        }
    }

    public static @Nullable UUID getMinecraftFromDiscord(String id) {
        String minecraftId = getString("SELECT minecraft_id FROM LINKS WHERE discord_id = ?", id, "minecraft_id");
        if (minecraftId == null) return null;
        return UUID.fromString(minecraftId);
    }

    public static @Nullable String getDiscordFromMinecraft(UUID id) {
        return MC_TO_DISCORD_CACHE.computeIfAbsent(id, uuid -> getString("SELECT discord_id FROM LINKS WHERE minecraft_id = ?", uuid.toString(), "discord_id"));
    }

    public static Member getMemberFromMinecraft(UUID uuid) {
        Guild guild = getGuild();
        String discordId = getDiscordFromMinecraft(uuid);
        if (discordId == null || guild == null) return null;
        return guild.retrieveMemberById(discordId).complete();
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        try {
            removeLinkFromDiscord(event.getUser().getId());
        } catch (SQLException ignored) {}
    }
}
