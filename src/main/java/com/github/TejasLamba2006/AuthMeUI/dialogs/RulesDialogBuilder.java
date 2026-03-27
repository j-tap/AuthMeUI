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
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RulesDialogBuilder {

    private final SettingsManager settings;

    public RulesDialogBuilder(SettingsManager settings) {
        this.settings = settings;
    }

    public Dialog construct(Player player) {
        List<DialogBody> contentSections = buildBodyContent(player);
        List<DialogInput> inputFields = buildInputFields(player);
        ActionButton confirmButton = buildConfirmButton(player);

        DialogBase dialogBase = DialogBase.builder(settings.getRulesTitle(player))
                .canCloseWithEscape(settings.canCloseWithEscape())
                .afterAction(DialogAfterAction.CLOSE)
                .body(contentSections)
                .inputs(inputFields)
                .build();

        return Dialog.create(builder -> {
            ((Builder) builder.empty())
                    .base(dialogBase)
                    .type(DialogType.multiAction(
                            List.of(confirmButton),
                            null,
                            settings.getButtonColumns()));
        });
    }

    public Dialog constructForLocale(String localeTag) {
        return constructForLocale(localeTag, null);
    }

    public Dialog constructForLocale(String localeTag, OfflinePlayer placeholderTarget) {
        List<DialogBody> contentSections = buildBodyContent(localeTag, placeholderTarget);
        List<DialogInput> inputFields = buildInputFields(localeTag, placeholderTarget);
        ActionButton confirmButton = buildConfirmButton(localeTag, placeholderTarget);

        DialogBase dialogBase = DialogBase.builder(settings.getRulesTitle(localeTag, placeholderTarget))
                .canCloseWithEscape(settings.canCloseWithEscape())
                .afterAction(DialogAfterAction.CLOSE)
                .body(contentSections)
                .inputs(inputFields)
                .build();

        return Dialog.create(builder -> {
            ((Builder) builder.empty())
                    .base(dialogBase)
                    .type(DialogType.multiAction(
                            List.of(confirmButton),
                            null,
                            settings.getButtonColumns()));
        });
    }

    private List<DialogBody> buildBodyContent(String localeTag, OfflinePlayer placeholderTarget) {
        List<DialogBody> content = new ArrayList<>();

        for (String line : settings.getRulesBodyRaw(localeTag)) {
            content.add(DialogBody.plainMessage(settings.parseText(line, placeholderTarget)));
        }

        if (content.isEmpty()) {
            content.add(DialogBody.plainMessage(Component.empty()));
        }

        return content;
    }

    private List<DialogBody> buildBodyContent(Player player) {
        List<DialogBody> content = new ArrayList<>();

        for (String line : settings.getRulesBodyRaw(player)) {
            content.add(DialogBody.plainMessage(settings.parseText(line, player)));
        }

        if (content.isEmpty()) {
            content.add(DialogBody.plainMessage(Component.empty()));
        }

        return content;
    }

    private List<DialogInput> buildInputFields(String localeTag, OfflinePlayer placeholderTarget) {
        List<DialogInput> inputs = new ArrayList<>();

        if (settings.isAgreementRequired()) {
            inputs.add(
                    DialogInput.bool(settings.getAgreementKey(), settings.getAgreementLabel(localeTag, placeholderTarget))
                            .initial(false)
                            .build());
        }

        return inputs;
    }

    private List<DialogInput> buildInputFields(Player player) {
        List<DialogInput> inputs = new ArrayList<>();

        if (settings.isAgreementRequired()) {
            inputs.add(
                    DialogInput.bool(settings.getAgreementKey(), settings.getAgreementLabel(player))
                            .initial(false)
                            .build());
        }

        return inputs;
    }

    private ActionButton buildConfirmButton(String localeTag, OfflinePlayer placeholderTarget) {
        return ActionButton.builder(settings.getRulesConfirmButton(localeTag, placeholderTarget))
                .action(DialogAction.customClick(DialogIdentifiers.RULES_CONFIRM, null))
                .build();
    }

    private ActionButton buildConfirmButton(Player player) {
        return ActionButton.builder(settings.getRulesConfirmButton(player))
                .action(DialogAction.customClick(DialogIdentifiers.RULES_CONFIRM, null))
                .build();
    }
}
