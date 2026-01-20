package com.github.TejasLamba2006.AuthMeUI.listeners;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge.AuthResult;
import com.github.TejasLamba2006.AuthMeUI.authentication.AuthenticationBridge.RegistrationResult;
import com.github.TejasLamba2006.AuthMeUI.configuration.SettingsManager;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogIdentifiers;
import com.github.TejasLamba2006.AuthMeUI.dialogs.DialogManager;
import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;

public class DialogInteractionListener implements Listener {

    private static final String PASSWORD_FIELD = "password";
    private static final String CONFIRM_FIELD = "confirm";

    private final AuthMeUIPlugin plugin;
    private final AuthenticationBridge authBridge;
    private final DialogManager dialogManager;
    private final SettingsManager settings;

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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDialogInteraction(PlayerCustomClickEvent event) {
        DialogResponseView responseView = event.getDialogResponseView();
        if (responseView == null) {
            return;
        }

        PlayerCommonConnection connection = event.getCommonConnection();
        if (!(connection instanceof PlayerGameConnection gameConnection)) {
            return;
        }

        Player player = gameConnection.getPlayer();
        Key actionKey = event.getIdentifier();

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

    private void handleLoginSubmission(Player player, DialogResponseView response) {
        String enteredPassword = response.getText(PASSWORD_FIELD);

        if (isNullOrEmpty(enteredPassword)) {
            displayLoginWithError(player, "login.password-empty", "<red>Please enter your password!</red>");
            return;
        }

        if (!authBridge.isPlayerRegistered(player.getName())) {
            displayRegistrationWithError(player, "login.not-registered",
                    "<yellow>You don't have an account. Please register first!</yellow>");
            return;
        }

        AuthResult result = authBridge.attemptLogin(player, enteredPassword);

        switch (result) {
            case SUCCESS -> {
                // Successful login; no further action needed
            }
            case INVALID_PASSWORD -> displayLoginWithError(player, "login.password-incorrect",
                    "<red>Incorrect password. Please try again.</red>");
            case NOT_REGISTERED -> displayRegistrationWithError(player, "login.not-registered",
                    "<yellow>You don't have an account. Please register first!</yellow>");
            default -> displayLoginWithError(player, "login.password-incorrect",
                    "<red>Incorrect password. Please try again.</red>");
        }
    }

    private void handleRegistrationSubmission(Player player, DialogResponseView response) {
        String enteredPassword = response.getText(PASSWORD_FIELD);
        String confirmPassword = response.getText(CONFIRM_FIELD);

        if (isNullOrEmpty(enteredPassword) || isNullOrEmpty(confirmPassword)) {
            displayRegistrationWithError(player, "register.fields-empty",
                    "<red>Please fill in both password fields!</red>");
            return;
        }

        RegistrationResult result = authBridge.attemptRegistration(player, enteredPassword, confirmPassword);

        switch (result) {
            case SUCCESS -> scheduleRegistrationVerification(player);
            case ALREADY_EXISTS -> displayLoginWithError(player, "register.already-registered",
                    "<yellow>You already have an account. Please login instead!</yellow>");
            case PASSWORD_MISMATCH -> displayRegistrationWithError(player, "register.passwords-mismatch",
                    "<red>Passwords do not match. Please try again.</red>");
            case PASSWORD_TOO_SHORT -> {
                int minLength = authBridge.fetchMinPasswordLength();
                displayRegistrationWithError(player, "register.password-too-short",
                        "<red>Password must be at least <white>%min%</white> characters!</red>",
                        Map.of("min", String.valueOf(minLength)));
            }
            case PASSWORD_TOO_LONG -> {
                int maxLength = authBridge.fetchMaxPasswordLength();
                displayRegistrationWithError(player, "register.password-too-long",
                        "<red>Password cannot exceed <white>%max%</white> characters!</red>",
                        Map.of("max", String.valueOf(maxLength)));
            }
            case INVALID_PASSWORD -> displayRegistrationWithError(player, "register.password-invalid",
                    "<red>Invalid password format. Please try again.</red>");
            default -> displayRegistrationWithError(player, "register.failed",
                    "<red>Registration failed. Please try again.</red>");
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

    private void displayLoginWithError(Player player, String messageKey, String defaultMessage) {
        scheduleDialogReopen(() -> {
            if (!authBridge.isPlayerAuthenticated(player)) {
                Component errorMsg = settings.getMessage(messageKey, defaultMessage);
                player.showDialog(dialogManager.createLoginDialog(player, errorMsg));
            }
        });
    }

    private void displayRegistrationWithError(Player player, String messageKey, String defaultMessage) {
        displayRegistrationWithError(player, messageKey, defaultMessage, null);
    }

    private void displayRegistrationWithError(Player player, String messageKey, String defaultMessage,
            Map<String, String> placeholders) {
        scheduleDialogReopen(() -> {
            if (!authBridge.isPlayerAuthenticated(player)) {
                Component errorMsg = placeholders != null
                        ? settings.getMessage(messageKey, defaultMessage, placeholders)
                        : settings.getMessage(messageKey, defaultMessage);
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
                    Component errorMsg = settings.getMessage("register.failed",
                            "<red>Registration failed. Please try again.</red>");
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
}
