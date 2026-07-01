# Event Queue System

Queue system used on Peli Events. 
IP is pelievents.cinros.net:25561

## Commands
All commands listed here require Operator status (OP)

/eventqueue reload
/queueinfo
/setqueuespawn
/openqueue
/closequeue

## Compiling EventQueueSystem
JDK 21 Required.

Linux:
Open a terminal session and run ./gradlew build

Windows:
Open the command prompt and run gradlew.bat build

# Techinical Summary
(written by ai cuz i aint writing all that shi myself)

## EventQueueSystem — Technical Summary

Core concept: Two player tiers exist in event worlds — participants (have event.savedata) and actors (everyone else). The plugin enforces different behavior for each on every world entry, death, and disconnect.

World detection: config.yml holds an explicit list of event world names. Every trigger starts with an isEventWorld() check — if the player isn’t in a listed world, the plugin does nothing.

Entry flow: PlayerJoinEvent at MONITOR priority (runs after all other plugins, including EssentialsXSpawn’s spawn-on-join) and PlayerChangedWorldEvent both feed into a shared handler. Participants with existing saved data are shown a chest GUI to selectively restore their state. Actors are immediately stripped and teleported to the queue spawn.

Death flow: PlayerDeathEvent at HIGH priority snapshots the participant’s state in memory and cancels all item/XP drops. After respawn, a separate death restore GUI opens (1-tick delay — required for the client to load in before a GUI can open). The snapshot is in-memory only; the last quit-save remains on disk as a fallback.

Restore GUI: 4-row chest inventory backed by a custom InventoryHolder. Each of the 14 toggleable fields maps to a fixed slot. Clicks are intercepted and cancelled (no item movement), toggling the selection state and refreshing the icon in place. Yes/No buttons close the GUI and either apply the selected subset of fields or discard. Closing with Esc counts as No.

Selective apply: PlayerDataManager.applyPlayerData() accepts a Set<RestoreOption> and only writes back the fields present in that set, leaving everything else untouched.

Persistence: Each participant’s state is serialized to a per-UUID YAML file under plugins/EventQueueSystem/playerdata/. The queue spawn is a separate queuespawn.yml. Both survive restarts. Death snapshots are not persisted.

LuckPerms integration: /openqueue and /closequeue use the LuckPerms Java API directly to add/remove queue.join.event on the default group asynchronously, then sync back to the main thread to update the in-memory queue state. The plugin soft-depends on LuckPerms so it still loads without it.

# Notes
No further explanation will be given on how to use this plugin because it is specifically made to work on my server. (which runs on a bunch of skripts). If you wanna write an explanation feel free to make a pull request and merge.