package com.github.TejasLamba2006.AuthMeUI.listeners;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles post-join authentication dialogs.
 * This listener is only registered when configuration phase authentication is
 * DISABLED.
 */
public class PlayerSessionListener implements Listener {

    private final AuthMeUIPlugin plugin;
    private final AuthenticationBridge authBridge;
    private final DialogManager dialogManager;
    private final Set<UUID> dialogShownInSession = ConcurrentHashMap.newKeySet();

    public PlayerSessionListener(AuthMeUIPlugin plugin, AuthenticationBridge authBridge, DialogManager dialogManager) {
        this.plugin = plugin;
        this.authBridge = authBridge;
        this.dialogManager = dialogManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        if (!authBridge.isConnected()) {
            return;
        }

        if (joiningPlayer.hasPermission("authmeui.bypass")) {
            return;
        }

        long delayTicks = Math.max(0L, plugin.getSettingsManager().getPostJoinOpenDelayTicks());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> presentPostJoinDialogIfNeeded(joiningPlayer), delayTicks);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        dialogShownInSession.remove(event.getPlayer().getUniqueId());
    }

    public void clearDialogShownState(UUID playerId) {
        dialogShownInSession.remove(playerId);
    }

    private void presentPostJoinDialogIfNeeded(Player targetPlayer) {
        if (!targetPlayer.isOnline()) {
            return;
        }
        if (!authBridge.isConnected()) {
            return;
        }
        if (targetPlayer.hasPermission("authmeui.bypass")) {
            return;
        }
        if (authBridge.isPlayerAuthenticated(targetPlayer)) {
            dialogShownInSession.remove(targetPlayer.getUniqueId());
            return;
        }
        if (!dialogShownInSession.add(targetPlayer.getUniqueId())) {
            return;
        }

        boolean hasAccount = authBridge.isPlayerRegistered(targetPlayer.getName());
        dialogManager.presentAuthDialog(targetPlayer, hasAccount);
    }
}
