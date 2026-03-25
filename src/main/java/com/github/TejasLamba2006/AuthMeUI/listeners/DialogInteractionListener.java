package com.github.TejasLamba2006.AuthMeUI.listeners;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge.AuthResult;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge.RegistrationResult;
import com.github.TejasLamba2006.AuthMeUI.configuration.SettingsManager;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogIdentifiers;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogManager;
import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DialogInteractionListener implements Listener {

    private static final String PASSWORD_FIELD = "password";
    private static final String CONFIRM_FIELD = "confirm";

    private final AuthMeUIPlugin plugin;
    private final AuthenticationBridge authBridge;
    private final DialogManager dialogManager;
    private final SettingsManager settings;

    private ConfigurationPhaseListener configPhaseListener;

    public DialogInteractionListener(
            AuthMeUIPlugin plugin,
            AuthenticationBridge authBridge,
            DialogManager dialogManager,
            SettingsManager settings) {
        this.plugin = plugin;
        this.authBridge = authBridge;
        this.dialogManager = dialogManager;
        this.settings = settings;
    }

    /**
     * Set the configuration phase listener for coordinating authentication.
     */
    public void setConfigurationPhaseListener(ConfigurationPhaseListener listener) {
        this.configPhaseListener = listener;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDialogInteraction(PlayerCustomClickEvent event) {
        DialogResponseView responseView = event.getDialogResponseView();
        if (responseView == null) {
            return;
        }

        PlayerCommonConnection connection = event.getCommonConnection();
        Key actionKey = event.getIdentifier();

        // Handle configuration phase connections
        if (connection instanceof PlayerConfigurationConnection configConnection) {
            handleConfigurationPhaseInteraction(configConnection, actionKey, responseView);
            return;
        }

        // Handle in-game connections
        if (!(connection instanceof PlayerGameConnection gameConnection)) {
            return;
        }

        Player player = gameConnection.getPlayer();

        if (actionKey.equals(DialogIdentifiers.LOGIN_SUBMIT)) {
            handleLoginSubmission(player, responseView);
        } else if (actionKey.equals(DialogIdentifiers.REGISTER_SUBMIT)) {
            handleRegistrationSubmission(player, responseView);
        } else if (actionKey.equals(DialogIdentifiers.RULES_CONFIRM)) {
            handleRulesConfirmation(player, responseView);
        } else if (actionKey.equals(DialogIdentifiers.SUPPORT_ACTION)) {
            handleSupportAction(player);
        }
    }

    // ==================== Configuration Phase Handlers ====================

    private void handleConfigurationPhaseInteraction(
            PlayerConfigurationConnection connection,
            Key actionKey,
            DialogResponseView responseView) {

        UUID uniqueId = connection.getProfile().getId();
        String playerName = connection.getProfile().getName();
        String localeTag = extractLocaleTag(connection);
        Audience audience = connection.getAudience();

        if (uniqueId == null || playerName == null) {
            return;
        }

        if (actionKey.equals(DialogIdentifiers.LOGIN_SUBMIT)) {
            handleConfigPhaseLogin(connection, uniqueId, playerName, localeTag, audience, responseView);
        } else if (actionKey.equals(DialogIdentifiers.REGISTER_SUBMIT)) {
            handleConfigPhaseRegistration(connection, uniqueId, playerName, localeTag, audience, responseView);
        } else if (actionKey.equals(DialogIdentifiers.RULES_CONFIRM)) {
            handleConfigPhaseRulesConfirmation(connection, uniqueId, playerName, localeTag, audience, responseView);
        }
    }

    private void handleConfigPhaseLogin(
            PlayerConfigurationConnection connection,
            UUID uniqueId,
            String playerName,
            String localeTag,
            Audience audience,
            DialogResponseView response) {

        String enteredPassword = response.getText(PASSWORD_FIELD);

        if (isNullOrEmpty(enteredPassword)) {
            Component error = settings.getMessage(localeTag, "login.password-empty");
            audience.showDialog(dialogManager.createLoginDialogForAudience(playerName, localeTag, error));
            return;
        }

        if (!authBridge.isPlayerRegistered(playerName)) {
            Component error = settings.getMessage(localeTag, "login.not-registered");
            // Show registration dialog instead
            if (configPhaseListener != null) {
                configPhaseListener.updateRegistrationStatus(uniqueId, false);
            }
            if (settings.isRulesDialogEnabled()) {
                audience.showDialog(dialogManager.createRulesDialogForAudience(playerName, localeTag));
            } else {
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            return;
        }

        // Attempt authentication using AuthMe's direct password check
        AuthResult result = authBridge.attemptLoginByName(playerName, enteredPassword);

        switch (result) {
            case SUCCESS -> {
                // Complete the configuration phase authentication
                if (configPhaseListener != null) {
                    configPhaseListener.completeAuthentication(uniqueId, true);
                }
            }
            case INVALID_PASSWORD -> {
                Component error = settings.getMessage(localeTag, "login.password-incorrect");
                audience.showDialog(dialogManager.createLoginDialogForAudience(playerName, localeTag, error));
            }
            case NOT_REGISTERED -> {
                Component error = settings.getMessage(localeTag, "login.not-registered");
                if (configPhaseListener != null) {
                    configPhaseListener.updateRegistrationStatus(uniqueId, false);
                }
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            default -> {
                Component error = settings.getMessage(localeTag, "login.password-incorrect");
                audience.showDialog(dialogManager.createLoginDialogForAudience(playerName, localeTag, error));
            }
        }
    }

    private void handleConfigPhaseRegistration(
            PlayerConfigurationConnection connection,
            UUID uniqueId,
            String playerName,
            String localeTag,
            Audience audience,
            DialogResponseView response) {

        String enteredPassword = response.getText(PASSWORD_FIELD);
        String confirmPassword = response.getText(CONFIRM_FIELD);

        if (isNullOrEmpty(enteredPassword) || isNullOrEmpty(confirmPassword)) {
            Component error = settings.getMessage(localeTag, "register.fields-empty");
            audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            return;
        }

        RegistrationResult result = authBridge.attemptRegistrationByName(playerName, enteredPassword, confirmPassword);

        switch (result) {
            case SUCCESS -> {
                // Complete the configuration phase authentication
                if (configPhaseListener != null) {
                    configPhaseListener.updateRegistrationStatus(uniqueId, true);
                    configPhaseListener.completeAuthentication(uniqueId, true);
                }
            }
            case ALREADY_EXISTS -> {
                Component error = settings.getMessage(localeTag, "register.already-registered");
                if (configPhaseListener != null) {
                    configPhaseListener.updateRegistrationStatus(uniqueId, true);
                }
                audience.showDialog(dialogManager.createLoginDialogForAudience(playerName, localeTag, error));
            }
            case PASSWORD_MISMATCH -> {
                Component error = settings.getMessage(localeTag, "register.passwords-mismatch");
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            case PASSWORD_TOO_SHORT -> {
                int minLength = authBridge.fetchMinPasswordLength();
                Component error = settings.getMessage(localeTag, "register.password-too-short",
                        Map.of("min", String.valueOf(minLength)));
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            case PASSWORD_TOO_LONG -> {
                int maxLength = authBridge.fetchMaxPasswordLength();
                Component error = settings.getMessage(localeTag, "register.password-too-long",
                        Map.of("max", String.valueOf(maxLength)));
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            case INVALID_PASSWORD -> {
                Component error = settings.getMessage(localeTag, "register.password-invalid");
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
            default -> {
                Component error = settings.getMessage(localeTag, "register.failed");
                audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag, error));
            }
        }
    }

    private void handleConfigPhaseRulesConfirmation(
            PlayerConfigurationConnection connection,
            UUID uniqueId,
            String playerName,
            String localeTag,
            Audience audience,
            DialogResponseView response) {

        if (settings.isAgreementRequired()) {
            Boolean hasAgreed = response.getBoolean(settings.getAgreementKey());

            if (hasAgreed == null || !hasAgreed) {
                // Re-show the rules dialog
                audience.showDialog(dialogManager.createRulesDialogForAudience(playerName, localeTag));
                return;
            }
        }

        // Show registration dialog
        audience.showDialog(dialogManager.createRegistrationDialogForAudience(playerName, localeTag));
    }

    // ==================== In-Game Handlers ====================

    private void handleLoginSubmission(Player player, DialogResponseView response) {
        String enteredPassword = response.getText(PASSWORD_FIELD);

        if (isNullOrEmpty(enteredPassword)) {
            displayLoginWithError(player, "login.password-empty");
            return;
        }

        if (!authBridge.isPlayerRegistered(player.getName())) {
            displayRegistrationWithError(player, "login.not-registered");
            return;
        }

        AuthResult result = authBridge.attemptLogin(player, enteredPassword);

        switch (result) {
            case SUCCESS -> {
                // Successful login; no further action needed
            }
            case INVALID_PASSWORD -> displayLoginWithError(player, "login.password-incorrect");
            case NOT_REGISTERED -> displayRegistrationWithError(player, "login.not-registered");
            default -> displayLoginWithError(player, "login.password-incorrect");
        }
    }

    private void handleRegistrationSubmission(Player player, DialogResponseView response) {
        String enteredPassword = response.getText(PASSWORD_FIELD);
        String confirmPassword = response.getText(CONFIRM_FIELD);

        if (isNullOrEmpty(enteredPassword) || isNullOrEmpty(confirmPassword)) {
            displayRegistrationWithError(player, "register.fields-empty");
            return;
        }

        RegistrationResult result = authBridge.attemptRegistration(player, enteredPassword, confirmPassword);

        switch (result) {
            case SUCCESS -> scheduleRegistrationVerification(player);
            case ALREADY_EXISTS -> displayLoginWithError(player, "register.already-registered");
            case PASSWORD_MISMATCH -> displayRegistrationWithError(player, "register.passwords-mismatch");
            case PASSWORD_TOO_SHORT -> {
                int minLength = authBridge.fetchMinPasswordLength();
                displayRegistrationWithError(player, "register.password-too-short",
                        Map.of("min", String.valueOf(minLength)));
            }
            case PASSWORD_TOO_LONG -> {
                int maxLength = authBridge.fetchMaxPasswordLength();
                displayRegistrationWithError(player, "register.password-too-long",
                        Map.of("max", String.valueOf(maxLength)));
            }
            case INVALID_PASSWORD -> displayRegistrationWithError(player, "register.password-invalid");
            default -> displayRegistrationWithError(player, "register.failed");
        }
    }

    private void handleRulesConfirmation(Player player, DialogResponseView response) {
        if (settings.isAgreementRequired()) {
            Boolean hasAgreed = response.getBoolean(settings.getAgreementKey());

            if (hasAgreed == null || !hasAgreed) {
                scheduleDialogReopen(() -> {
                    if (!authBridge.isPlayerAuthenticated(player)) {
                        player.showDialog(dialogManager.createRulesDialog(player));
                    }
                });
                return;
            }
        }

        scheduleDialogReopen(() -> player.showDialog(dialogManager.createRegistrationDialog(player)));
    }

    private void handleSupportAction(Player player) {
        boolean hasAccount = authBridge.isPlayerRegistered(player.getName());
        dialogManager.presentAuthDialog(player, hasAccount);
    }

    private void displayLoginWithError(Player player, String messageKey) {
        scheduleDialogReopen(() -> {
            if (!authBridge.isPlayerAuthenticated(player)) {
                Component errorMsg = settings.getMessage(player, messageKey);
                player.showDialog(dialogManager.createLoginDialog(player, errorMsg));
            }
        });
    }

    private void displayRegistrationWithError(Player player, String messageKey) {
        displayRegistrationWithError(player, messageKey, null);
    }

    private void displayRegistrationWithError(Player player, String messageKey, Map<String, String> placeholders) {
        scheduleDialogReopen(() -> {
            if (!authBridge.isPlayerAuthenticated(player)) {
                Component errorMsg = placeholders != null
                        ? settings.getMessage(player, messageKey, placeholders)
                        : settings.getMessage(player, messageKey);
                player.showDialog(dialogManager.createRegistrationDialog(player, errorMsg));
            }
        });
    }

    private void scheduleRegistrationVerification(Player player) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                boolean isValid = authBridge.isPlayerAuthenticated(player)
                        || authBridge.isPlayerRegistered(player.getName());

                if (!isValid) {
                    Component errorMsg = settings.getMessage(player, "register.failed");
                    player.showDialog(dialogManager.createRegistrationDialog(player, errorMsg));
                }
            }
        }, 3L);
    }

    private void scheduleDialogReopen(Runnable action) {
        Bukkit.getScheduler().runTask(plugin, action);
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String extractLocaleTag(PlayerConfigurationConnection connection) {
        try {
            Method localeMethod = connection.getClass().getMethod("locale");
            Object localeValue = localeMethod.invoke(connection);
            return Objects.toString(localeValue, null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
