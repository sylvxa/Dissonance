# Extra Setup

This section will cover how to get some of the extra features of Dissonance working.

## Linking

Discord linking allows for two features currently: Whitelisting and Proximity Chat

For any of these features to work, you need to set the following options:

```toml
[discord.linking]
enabled = true
guild_id = SERVER_ID
```

Where `SERVER_ID` is the ID of your Discord server (if this is set to 0, it will default to the server ID that contains your output channel)

### Whitelist

You can enable a whitelist so only members who are in your Discord server (or have/do not have a role) may join.

> If this is enabled while the `guild_id` is invalid, players won't be able to join the server. This is to make sure nobody can take advantage of a configuration error and get on to your server without permission! 

Here are the configuration options:

```toml
[discord.linking.whitelist]
enabled = false
kick_message = "§cYou are not allowed to join this server!"
allowed_roles = []
disallowed_roles = []
link_message = """
§c§lThis server requires that you link with their Discord to join!

§7Run the §i/link §r§7command with the code §l%code%§r§7 to link your account."""
```

Setting `enabled` to true will mean

