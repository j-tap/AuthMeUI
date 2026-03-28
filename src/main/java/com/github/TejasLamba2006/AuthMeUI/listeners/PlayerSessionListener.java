package com.github.TejasLamba2006.AuthMeUI.listeners;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreSessionEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
    private final Set<UUID> mustShowAuthDialogOnJoin = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> pendingJoinTasks = new ConcurrentHashMap<>();

    public PlayerSessionListener(AuthMeUIPlugin plugin, AuthenticationBridge authBridge, DialogManager dialogManager) {
        this.plugin = plugin;
        this.authBridge = authBridge;
        this.dialogManager = dialogManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAuthMeAsyncPreLogin(AuthMeAsyncPreLoginEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!event.canLogin()) {
            mustShowAuthDialogOnJoin.add(playerId);
            debug(player, "AuthMeAsyncPreLoginEvent: canLogin=false, marking forced dialog on join");
            return;
        }

        mustShowAuthDialogOnJoin.remove(playerId);
        debug(player, "AuthMeAsyncPreLoginEvent: canLogin=true, clearing forced dialog flag");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        UUID playerId = joiningPlayer.getUniqueId();

        if (!authBridge.isConnected()) {
            debug(joiningPlayer, "Join: AuthMe bridge is not connected, skipping");
            return;
        }

        if (shouldBypass(joiningPlayer)) {
            mustShowAuthDialogOnJoin.remove(playerId);
            cancelPendingTask(playerId);
            debug(joiningPlayer, "Join: player has bypass permission, skipping");
            return;
        }

        boolean forcedByPreLogin = mustShowAuthDialogOnJoin.remove(playerId);
        cancelPendingTask(playerId);
        debug(joiningPlayer, "Join: scheduling polling, forcedByPreLogin=" + forcedByPreLogin);
        startJoinPolling(joiningPlayer, playerId, forcedByPreLogin);
    }

    private void startJoinPolling(Player joiningPlayer, UUID playerId, boolean forcedByPreLogin) {
        long delayTicks = Math.max(0L, plugin.getSettingsManager().getPostJoinOpenDelayTicks());
        long recheckInterval = Math.max(1L, plugin.getSettingsManager().getPostJoinOpenRecheckIntervalTicks());
        int maxRechecks = Math.max(1, plugin.getSettingsManager().getPostJoinOpenMaxRechecks());
        AtomicInteger checksPerformed = new AtomicInteger(0);
        debug(joiningPlayer,
                "Polling start: delay=" + delayTicks + ", interval=" + recheckInterval + ", maxRechecks=" + maxRechecks);
        BukkitTask pendingTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> performPostJoinAuthCheck(joiningPlayer, checksPerformed, maxRechecks, forcedByPreLogin),
                delayTicks,
                recheckInterval);
        pendingJoinTasks.put(playerId, pendingTask);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(LoginEvent event) {
        debug(event.getPlayer(), "LoginEvent: clearing post-join state");
        clearSessionState(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRestoreSession(RestoreSessionEvent event) {
        debug(event.getPlayer(), "RestoreSessionEvent: clearing post-join state");
        clearSessionState(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        debug(event.getPlayer(), "PlayerQuitEvent: clearing post-join state");
        clearSessionState(event.getPlayer().getUniqueId());
    }

    public void clearDialogShownState(UUID playerId) {
        clearSessionState(playerId);
    }

    private void performPostJoinAuthCheck(
            Player targetPlayer,
            AtomicInteger checksPerformed,
            int maxRechecks,
            boolean forcedByPreLogin) {
        UUID playerId = targetPlayer.getUniqueId();
        int currentCheck = checksPerformed.incrementAndGet();
        if (currentCheck > maxRechecks) {
            debug(targetPlayer, "Polling: reached max rechecks, canceling task");
            cancelPendingTask(playerId);
            return;
        }
        debug(targetPlayer, "Polling check #" + currentCheck + ", forcedByPreLogin=" + forcedByPreLogin);

        if (!targetPlayer.isOnline()) {
            debug(targetPlayer, "Polling: player offline, canceling task");
            cancelPendingTask(playerId);
            return;
        }
        if (!authBridge.isConnected()) {
            debug(targetPlayer, "Polling: AuthMe bridge disconnected, canceling task");
            cancelPendingTask(playerId);
            return;
        }
        if (shouldBypass(targetPlayer)) {
            debug(targetPlayer, "Polling: bypass permission detected, canceling task");
            cancelPendingTask(playerId);
            return;
        }

        if (forcedByPreLogin) {
            if (dialogShownInSession.add(playerId)) {
                boolean hasAccount = authBridge.isPlayerRegistered(targetPlayer.getName());
                debug(targetPlayer, "Polling: forced by pre-login, opening dialog (registered=" + hasAccount + ")");
                dialogManager.presentAuthDialog(targetPlayer, hasAccount);
            } else {
                debug(targetPlayer, "Polling: forced branch skipped, dialog already shown in session");
            }
            cancelPendingTask(playerId);
            return;
        }

        boolean authenticated = authBridge.isPlayerAuthenticated(targetPlayer);
        debug(targetPlayer, "Polling: AuthMe isAuthenticated=" + authenticated);
        if (authenticated) {
            debug(targetPlayer, "Polling: authenticated=true in fallback branch, canceling task");
            cancelPendingTask(playerId);
            return;
        }

        if (!dialogShownInSession.add(playerId)) {
            debug(targetPlayer, "Polling: dialog already shown in session, canceling task");
            cancelPendingTask(playerId);
            return;
        }

        boolean hasAccount = authBridge.isPlayerRegistered(targetPlayer.getName());
        debug(targetPlayer, "Polling: unauthenticated fallback, opening dialog (registered=" + hasAccount + ")");
        dialogManager.presentAuthDialog(targetPlayer, hasAccount);
        cancelPendingTask(playerId);
    }

    private void cancelPendingTask(UUID playerId) {
        BukkitTask task = pendingJoinTasks.remove(playerId);
        if (task != null) {
            task.cancel();
            debug(playerId, "Canceled pending join task");
        }
    }

    private void clearSessionState(UUID playerId) {
        mustShowAuthDialogOnJoin.remove(playerId);
        dialogShownInSession.remove(playerId);
        debug(playerId, "Cleared session state");
        cancelPendingTask(playerId);
    }

    private void debug(Player player, String message) {
        if (!plugin.getSettingsManager().isDebugEnabled()) {
            return;
        }
        plugin.getLogger().info("[PostJoinDebug] player=" + player.getName() + " uuid=" + player.getUniqueId() + " | " + message);
    }

    private void debug(UUID playerId, String message) {
        if (!plugin.getSettingsManager().isDebugEnabled()) {
            return;
        }
        plugin.getLogger().info("[PostJoinDebug] uuid=" + playerId + " | " + message);
    }

    private boolean shouldBypass(Player player) {
        if (!plugin.getSettingsManager().isBypassPermissionEnabled()) {
            if (player.hasPermission("authmeui.bypass")) {
                debug(player, "Bypass permission exists but is disabled by config");
            }
            return false;
        }
        return player.hasPermission("authmeui.bypass");
    }

}
