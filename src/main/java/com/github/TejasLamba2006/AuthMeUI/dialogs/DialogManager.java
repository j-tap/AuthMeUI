package com.github.TejasLamba2006.AuthMeUI.dialogs;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import com.github.TejasLamba2006.AuthMeUI.configuration.SettingsManager;
import io.papermc.paper.dialog.Dialog;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class DialogManager {

    private final SettingsManager settings;
    private final LoginDialogBuilder loginBuilder;
    private final RegistrationDialogBuilder registerBuilder;
    private final RulesDialogBuilder rulesBuilder;

    public DialogManager(AuthMeUIPlugin plugin, SettingsManager settings) {
        this.settings = settings;
        this.loginBuilder = new LoginDialogBuilder(settings);
        this.registerBuilder = new RegistrationDialogBuilder(plugin, settings);
        this.rulesBuilder = new RulesDialogBuilder(plugin, settings);
    }

    public Dialog createLoginDialog(Player player) {
        return loginBuilder.construct(player);
    }

    public Dialog createLoginDialog(Player player, Component errorMessage) {
        return loginBuilder.construct(player, errorMessage);
    }

    public Dialog createRegistrationDialog(Player player) {
        return registerBuilder.construct(player);
    }

    public Dialog createRegistrationDialog(Player player, Component errorMessage) {
        return registerBuilder.construct(player, errorMessage);
    }

    public Dialog createRulesDialog(Player player) {
        return rulesBuilder.construct(player);
    }

    public void presentAuthDialog(Player player, boolean isRegistered) {
        if (isRegistered) {
            player.showDialog(createLoginDialog(player));
        } else {
            if (settings.isRulesDialogEnabled()) {
                player.showDialog(createRulesDialog(player));
            } else {
                player.showDialog(createRegistrationDialog(player));
            }
        }
    }

    public LoginDialogBuilder getLoginBuilder() {
        return loginBuilder;
    }

    public RegistrationDialogBuilder getRegisterBuilder() {
        return registerBuilder;
    }

    public RulesDialogBuilder getRulesBuilder() {
        return rulesBuilder;
    }
}
