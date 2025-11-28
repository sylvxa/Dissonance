# Configuration

This section will cover how to get Dissonance running in it's default configuration. If you've already done that, skip to the [Extra Setup](/setup/post_configuration.md) section.

## What you need

- [x] Administrator access to the Minecraft and Discord servers you are connecting
- - On Minecraft, you need editing access to the `config/dissonance-server.toml` file (and access to the console, preferably)
- - On Discord, you need permissions to invite the bot and give it several permissions (send messages, manage webhooks, etc.)

---

## Installing the Mod

Head over to the <a href="https://modrinth.com/mod/dissonance/versions" target="_blank">Modrinth</a> page and download the latest release for either NeoForge or Fabric, then drop it in your server's `mods` folder.

---

## Discord Preparation

1. Go to the <a href="https://discord.com/developers/applications" target="_blank">Discord Developer Portal</a> and sign in
2. Click the `New Application` button and name it whatever you want

> While you're in the `General Information` tab, you can set the bot's description, which will show up in its `About Me` tab.

<img src="/setup/images/create-an-application.png" alt="Creation of the Bot" width="400"/>


---

3. On the left-hand side, click `Bot`. 

> While you're here, you can set the bot's profile picture and banner if you'd like!

4. Click `Reset Token`, make sure to write this down somewhere since we'll need it later

![Result of resetting token](/setup/images/token.png)

5. Find the `Privileged Gateway Intents` section and check both the `Server Members Intent` and the `Message Content Intent`
6. Click `Save Changes` at the bottom.

---

7. Again, on the left sidebar, navigate to `OAuth2`.

8. Find the `OAuth2 URL Generator` and check the `bot` scope in the large grid of checkboxes.

![Scopes menu](/setup/images/scopes.png)


9. Scroll to the `Bot Permissions` section and enable the following permissions:
- [ ] Manage Channels *(required for proximity chat)*
- [ ] Manage Webhooks *(required to setup webhooks automatically)*
- [x] View Channels
- [x] Send Messages
- [ ] Manage Messages *(required for `/dissonance purge`)*
- [x] Embed Links
- [ ] Move Members *(required for proximity chat)*
- [ ] Mute Members *(suggested for proximity chat)*

![Permissions menu](/setup/images/permissions.png)

> Although, it'll work fine if you just give it `Administrator`, it's generally good practice to be specific with permissions. I won't stop you, though.

![Install URL](/setup/images/install-url.png)

10. Copy and open the `Generated URL`

<img src="/setup/images/add-bot.png" alt="Adding the bot" height="384"/>

---

11. Select your Discord server, select `Continue`, then `Authorize`
12. Open Discord and navigate to your server
13. Go to `User Settings` (the cog towards the bottom left), scroll down to `Advanced` (under `App Settings`), and check `Developer Mode` if it's not enabled already.

![Developer Mode](/setup/images/developer-mode.png)

14. Create or select a text channel and right-click the channel. Click `Copy Channel ID` and write it down

<img src="/setup/images/copy-channel-id.png" alt="Channel ID" height="384"/>

---

## Configuring the mod

1. Either start the server once or create `config/dissonance-server.toml` with this snippet:

```toml
[credentials]
	token = "DISCORD_TOKEN"

[discord]
	input_channels = [CHANNEL_ID]
	output_channel = CHANNEL_ID
```

> Don't worry, there are plenty more configuration options available! This is just the minimum you need to get everything working.

2. Fill out the options listed above, replacing `DISCORD_TOKEN` with the one we got in step 4 and `CHANNEL_ID` with the one we got in step 14.
3. Your base configuration is complete! If you are just looking for a basic bridge, you can stop here. Otherwise, continue to the [Extra Setup](/setup/post_configuration.md) section.