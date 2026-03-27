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
    private final Map<UUID, BukkitTask> pendingJoinTasks = new ConcurrentHashMap<>();

    public PlayerSessionListener(AuthMeUIPlugin plugin, AuthenticationBridge authBridge, DialogManager dialogManager) {
        this.plugin = plugin;
        this.authBridge = authBridge;
        this.dialogManager = dialogManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();
        UUID playerId = joiningPlayer.getUniqueId();

        if (!authBridge.isConnected()) {
            return;
        }

        if (joiningPlayer.hasPermission("authmeui.bypass")) {
            return;
        }

        long delayTicks = Math.max(0L, plugin.getSettingsManager().getPostJoinOpenDelayTicks());
        long recheckInterval = Math.max(1L, plugin.getSettingsManager().getPostJoinOpenRecheckIntervalTicks());
        int maxRechecks = Math.max(1, plugin.getSettingsManager().getPostJoinOpenMaxRechecks());

        cancelPendingTask(playerId);

        AtomicInteger checksPerformed = new AtomicInteger(0);
        BukkitTask pollTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> performPollingCheck(joiningPlayer, checksPerformed, maxRechecks),
                delayTicks,
                recheckInterval);
        pendingJoinTasks.put(playerId, pollTask);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        dialogShownInSession.remove(playerId);
        cancelPendingTask(playerId);
    }

    public void clearDialogShownState(UUID playerId) {
        dialogShownInSession.remove(playerId);
    }

    private void performPollingCheck(Player targetPlayer, AtomicInteger checksPerformed, int maxRechecks) {
        UUID playerId = targetPlayer.getUniqueId();
        int currentAttempt = checksPerformed.incrementAndGet();
        if (currentAttempt > maxRechecks) {
            cancelPendingTask(playerId);
            return;
        }

        if (!targetPlayer.isOnline()) {
            cancelPendingTask(playerId);
            return;
        }
        if (!authBridge.isConnected()) {
            cancelPendingTask(playerId);
            return;
        }
        if (targetPlayer.hasPermission("authmeui.bypass")) {
            cancelPendingTask(playerId);
            return;
        }
        if (authBridge.isPlayerAuthenticated(targetPlayer)) {
            dialogShownInSession.remove(playerId);
            cancelPendingTask(playerId);
            return;
        }
        if (!dialogShownInSession.add(playerId)) {
            cancelPendingTask(playerId);
            return;
        }

        boolean hasAccount = authBridge.isPlayerRegistered(targetPlayer.getName());
        dialogManager.presentAuthDialog(targetPlayer, hasAccount);
        cancelPendingTask(playerId);
    }

    private void cancelPendingTask(UUID playerId) {
        BukkitTask task = pendingJoinTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

}
