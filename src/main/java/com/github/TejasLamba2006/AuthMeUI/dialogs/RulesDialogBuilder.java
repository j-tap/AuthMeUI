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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RulesDialogBuilder {

    private final SettingsManager settings;

    public RulesDialogBuilder(SettingsManager settings) {
        this.settings = settings;
    }

    public Dialog construct(Player player) {
        return constructForLocale(settings.extractLocaleTag(player));
    }

    public Dialog constructForLocale(String localeTag) {
        List<DialogBody> contentSections = buildBodyContent(localeTag);
        List<DialogInput> inputFields = buildInputFields(localeTag);
        ActionButton confirmButton = buildConfirmButton(localeTag);

        DialogBase dialogBase = DialogBase.builder(settings.getRulesTitle(localeTag))
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

    private List<DialogBody> buildBodyContent(String localeTag) {
        List<DialogBody> content = new ArrayList<>();

        for (String line : settings.getRulesBodyRaw(localeTag)) {
            content.add(DialogBody.plainMessage(settings.parseText(line)));
        }

        if (content.isEmpty()) {
            content.add(DialogBody.plainMessage(Component.empty()));
        }

        return content;
    }

    private List<DialogInput> buildInputFields(String localeTag) {
        List<DialogInput> inputs = new ArrayList<>();

        if (settings.isAgreementRequired()) {
            inputs.add(
                    DialogInput.bool(settings.getAgreementKey(), settings.getAgreementLabel(localeTag))
                            .initial(false)
                            .build());
        }

        return inputs;
    }

    private ActionButton buildConfirmButton(String localeTag) {
        return ActionButton.builder(settings.getRulesConfirmButton(localeTag))
                .action(DialogAction.customClick(DialogIdentifiers.RULES_CONFIRM, null))
                .build();
    }
}
