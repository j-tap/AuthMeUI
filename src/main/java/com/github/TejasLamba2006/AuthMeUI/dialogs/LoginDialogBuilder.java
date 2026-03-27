package com.github.TejasLamba2006.AuthMeUI.dialogs;

import com.github.TejasLamba2006.AuthMeUI.configuration.SettingsManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry.Builder;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LoginDialogBuilder {

    private static final String PASSWORD_INPUT_KEY = "password";
    private static final int MAX_PASSWORD_INPUT_LENGTH = 128;

    private final SettingsManager settings;

    public LoginDialogBuilder(SettingsManager settings) {
        this.settings = settings;
    }

    public Dialog construct(Player player) {
        return construct(player, null);
    }

    public Dialog construct(Player player, Component errorNotice) {
        List<DialogBody> contentSections = buildBodyContent(player);

        if (errorNotice != null) {
            contentSections.add(DialogBody.plainMessage(errorNotice));
        }

        TextDialogInput passwordField = createPasswordInput(player);
        List<ActionButton> actionButtons = buildActionButtons(player);

        DialogBase dialogBase = DialogBase.builder(settings.getLoginTitle(player))
                .canCloseWithEscape(settings.canCloseWithEscape())
                .afterAction(DialogAfterAction.CLOSE)
                .body(contentSections)
                .inputs(List.of(passwordField))
                .build();

        return Dialog.create(builder -> {
            ((Builder) builder.empty())
                    .base(dialogBase)
                    .type(DialogType.multiAction(actionButtons, null, settings.getButtonColumns()));
        });
    }

    public Dialog constructForLocale(String localeTag) {
        return constructForLocale(localeTag, null, null);
    }

    public Dialog constructForLocale(String localeTag, Component errorNotice) {
        return constructForLocale(localeTag, null, errorNotice);
    }

    public Dialog constructForLocale(String localeTag, OfflinePlayer placeholderTarget, Component errorNotice) {
        List<DialogBody> contentSections = buildBodyContent(localeTag, placeholderTarget);

        if (errorNotice != null) {
            contentSections.add(DialogBody.plainMessage(errorNotice));
        }

        TextDialogInput passwordField = createPasswordInput(localeTag, placeholderTarget);
        List<ActionButton> actionButtons = buildActionButtons(localeTag, placeholderTarget);

        DialogBase dialogBase = DialogBase.builder(settings.getLoginTitle(localeTag, placeholderTarget))
                .canCloseWithEscape(settings.canCloseWithEscape())
                .afterAction(DialogAfterAction.CLOSE)
                .body(contentSections)
                .inputs(List.of(passwordField))
                .build();

        return Dialog.create(builder -> {
            ((Builder) builder.empty())
                    .base(dialogBase)
                    .type(DialogType.multiAction(actionButtons, null, settings.getButtonColumns()));
        });
    }

    private List<DialogBody> buildBodyContent(String localeTag, OfflinePlayer placeholderTarget) {
        List<DialogBody> content = new ArrayList<>();

        for (String line : settings.getLoginBodyRaw(localeTag)) {
            content.add(DialogBody.plainMessage(settings.parseText(line, placeholderTarget)));
        }

        if (content.isEmpty()) {
            content.add(DialogBody.plainMessage(Component.empty()));
        }

        return content;
    }

    private List<DialogBody> buildBodyContent(Player player) {
        List<DialogBody> content = new ArrayList<>();

        for (String line : settings.getLoginBodyRaw(player)) {
            content.add(DialogBody.plainMessage(settings.parseText(line, player)));
        }

        if (content.isEmpty()) {
            content.add(DialogBody.plainMessage(Component.empty()));
        }

        return content;
    }

    private TextDialogInput createPasswordInput(String localeTag, OfflinePlayer placeholderTarget) {
        return DialogInput.text(PASSWORD_INPUT_KEY, settings.getLoginPasswordLabel(localeTag, placeholderTarget))
                .width(settings.getInputWidth())
                .labelVisible(true)
                .maxLength(MAX_PASSWORD_INPUT_LENGTH)
                .initial("")
                .build();
    }

    private TextDialogInput createPasswordInput(Player player) {
        return DialogInput.text(PASSWORD_INPUT_KEY, settings.getLoginPasswordLabel(player))
                .width(settings.getInputWidth())
                .labelVisible(true)
                .maxLength(MAX_PASSWORD_INPUT_LENGTH)
                .initial("")
                .build();
    }

    private List<ActionButton> buildActionButtons(String localeTag, OfflinePlayer placeholderTarget) {
        ActionButton submitButton = ActionButton.builder(settings.getLoginSubmitButton(localeTag, placeholderTarget))
                .action(DialogAction.customClick(DialogIdentifiers.LOGIN_SUBMIT, null))
                .build();

        return settings.buildActionButtons("login-dialog", submitButton, localeTag, placeholderTarget);
    }

    private List<ActionButton> buildActionButtons(Player player) {
        ActionButton submitButton = ActionButton.builder(settings.getLoginSubmitButton(player))
                .action(DialogAction.customClick(DialogIdentifiers.LOGIN_SUBMIT, null))
                .build();

        return settings.buildActionButtons("login-dialog", submitButton, player);
    }
}
