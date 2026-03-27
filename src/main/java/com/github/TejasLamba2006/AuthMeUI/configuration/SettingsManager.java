package com.github.TejasLamba2006.AuthMeUI.configuration;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class SettingsManager {

    private static final List<String> LEGACY_TRANSLATION_PATHS = List.of(
            "login-dialog.title",
            "login-dialog.body",
            "login-dialog.password-label",
            "login-dialog.submit-button",
            "register-dialog.title",
            "register-dialog.body",
            "register-dialog.password-label",
            "register-dialog.confirm-label",
            "register-dialog.submit-button",
            "rules-dialog.title",
            "rules-dialog.body",
            "rules-dialog.agreement.label",
            "rules-dialog.confirm-button",
            "messages.login.password-empty",
            "messages.login.password-incorrect",
            "messages.login.not-registered",
            "messages.login.success",
            "messages.register.fields-empty",
            "messages.register.passwords-mismatch",
            "messages.register.password-too-short",
            "messages.register.password-too-long",
            "messages.register.password-invalid",
            "messages.register.already-registered",
            "messages.register.failed",
            "messages.register.success",
            "messages.rules.must-accept",
            "messages.commands.no-permission",
            "messages.commands.player-only",
            "messages.commands.authme-unavailable",
            "messages.commands.config-reloaded",
            "messages.commands.usage",
            "messages.commands.dialog-opened",
            "messages.commands.invalid-dialog");

    private final AuthMeUIPlugin plugin;
    private final MiniMessage miniMessage;
    private final LocalizationManager localizationManager;
    private FileConfiguration config;

    public SettingsManager(AuthMeUIPlugin plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.localizationManager = new LocalizationManager(plugin);
        this.config = plugin.getConfig();
        this.localizationManager.reload();
        migrateLegacyTextConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        this.localizationManager.reload();
        migrateLegacyTextConfig();
    }

    public boolean useConfigurationPhase() {
        return config.getBoolean("dialogs.use-configuration-phase", false);
    }

    public int getConfigurationPhaseTimeout() {
        return config.getInt("dialogs.configuration-phase-timeout", 60);
    }

    public boolean canCloseWithEscape() {
        return config.getBoolean("dialogs.allow-escape-close", false);
    }

    public int getButtonColumns() {
        return config.getInt("dialogs.button-columns", 2);
    }

    public int getInputWidth() {
        return config.getInt("dialogs.input-width", 150);
    }

    public Component getLoginTitle() {
        return getLoginTitle((Player) null);
    }

    public Component getLoginTitle(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "login-dialog.title"), player);
    }

    public Component getLoginTitle(String localeTag) {
        return parseText(getLocalizedString(localeTag, "login-dialog.title"));
    }

    public Component getLoginTitle(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "login-dialog.title"), placeholderTarget);
    }

    public List<String> getLoginBodyRaw() {
        return getLoginBodyRaw((Player) null);
    }

    public List<String> getLoginBodyRaw(Player player) {
        return getLoginBodyRaw(resolveLocaleTag(player));
    }

    public List<String> getLoginBodyRaw(String localeTag) {
        return getLocalizedStringList(localeTag, "login-dialog.body");
    }

    public Component getLoginPasswordLabel() {
        return getLoginPasswordLabel((Player) null);
    }

    public Component getLoginPasswordLabel(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "login-dialog.password-label"), player);
    }

    public Component getLoginPasswordLabel(String localeTag) {
        return parseText(getLocalizedString(localeTag, "login-dialog.password-label"));
    }

    public Component getLoginPasswordLabel(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "login-dialog.password-label"), placeholderTarget);
    }

    public Component getLoginSubmitButton() {
        return getLoginSubmitButton((Player) null);
    }

    public Component getLoginSubmitButton(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "login-dialog.submit-button"), player);
    }

    public Component getLoginSubmitButton(String localeTag) {
        return parseText(getLocalizedString(localeTag, "login-dialog.submit-button"));
    }

    public Component getLoginSubmitButton(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "login-dialog.submit-button"), placeholderTarget);
    }

    public Component getRegisterTitle() {
        return getRegisterTitle((Player) null);
    }

    public Component getRegisterTitle(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "register-dialog.title"), player);
    }

    public Component getRegisterTitle(String localeTag) {
        return parseText(getLocalizedString(localeTag, "register-dialog.title"));
    }

    public Component getRegisterTitle(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "register-dialog.title"), placeholderTarget);
    }

    public List<String> getRegisterBodyRaw() {
        return getRegisterBodyRaw((Player) null);
    }

    public List<String> getRegisterBodyRaw(Player player) {
        return getRegisterBodyRaw(resolveLocaleTag(player));
    }

    public List<String> getRegisterBodyRaw(String localeTag) {
        return getLocalizedStringList(localeTag, "register-dialog.body");
    }

    public Component getRegisterPasswordLabel() {
        return getRegisterPasswordLabel((Player) null);
    }

    public Component getRegisterPasswordLabel(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "register-dialog.password-label"), player);
    }

    public Component getRegisterPasswordLabel(String localeTag) {
        return parseText(getLocalizedString(localeTag, "register-dialog.password-label"));
    }

    public Component getRegisterPasswordLabel(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "register-dialog.password-label"), placeholderTarget);
    }

    public Component getRegisterConfirmLabel() {
        return getRegisterConfirmLabel((Player) null);
    }

    public Component getRegisterConfirmLabel(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "register-dialog.confirm-label"), player);
    }

    public Component getRegisterConfirmLabel(String localeTag) {
        return parseText(getLocalizedString(localeTag, "register-dialog.confirm-label"));
    }

    public Component getRegisterConfirmLabel(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "register-dialog.confirm-label"), placeholderTarget);
    }

    public Component getRegisterSubmitButton() {
        return getRegisterSubmitButton((Player) null);
    }

    public Component getRegisterSubmitButton(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "register-dialog.submit-button"), player);
    }

    public Component getRegisterSubmitButton(String localeTag) {
        return parseText(getLocalizedString(localeTag, "register-dialog.submit-button"));
    }

    public Component getRegisterSubmitButton(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "register-dialog.submit-button"), placeholderTarget);
    }

    public boolean isRulesDialogEnabled() {
        return config.getBoolean("rules-dialog.enabled", true);
    }

    public Component getRulesTitle() {
        return getRulesTitle((Player) null);
    }

    public Component getRulesTitle(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "rules-dialog.title"), player);
    }

    public Component getRulesTitle(String localeTag) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.title"));
    }

    public Component getRulesTitle(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.title"), placeholderTarget);
    }

    public List<String> getRulesBodyRaw() {
        return getRulesBodyRaw((Player) null);
    }

    public List<String> getRulesBodyRaw(Player player) {
        return getRulesBodyRaw(resolveLocaleTag(player));
    }

    public List<String> getRulesBodyRaw(String localeTag) {
        return getLocalizedStringList(localeTag, "rules-dialog.body");
    }

    public boolean isAgreementRequired() {
        return config.getBoolean("rules-dialog.agreement.enabled", true);
    }

    public String getAgreementKey() {
        String key = config.getString("rules-dialog.agreement.checkbox-key", "rules_accepted");
        return (key != null && !key.isBlank()) ? key : "rules_accepted";
    }

    public Component getAgreementLabel() {
        return getAgreementLabel((Player) null);
    }

    public Component getAgreementLabel(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "rules-dialog.agreement.label"), player);
    }

    public Component getAgreementLabel(String localeTag) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.agreement.label"));
    }

    public Component getAgreementLabel(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.agreement.label"), placeholderTarget);
    }

    public Component getRulesConfirmButton() {
        return getRulesConfirmButton((Player) null);
    }

    public Component getRulesConfirmButton(Player player) {
        return parseText(getLocalizedString(resolveLocaleTag(player), "rules-dialog.confirm-button"), player);
    }

    public Component getRulesConfirmButton(String localeTag) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.confirm-button"));
    }

    public Component getRulesConfirmButton(String localeTag, OfflinePlayer placeholderTarget) {
        return parseText(getLocalizedString(localeTag, "rules-dialog.confirm-button"), placeholderTarget);
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("metrics.enabled", true);
    }

    public String extractLocaleTag(Player player) {
        return resolveLocaleTag(player);
    }

    public Component getMessage(String path) {
        return getMessage((String) null, path);
    }

    public Component getMessage(Player player, String path) {
        String raw = getLocalizedString(resolveLocaleTag(player), "messages." + path);
        return parseText(raw, player);
    }

    public Component getMessage(String localeTag, String path) {
        String raw = getLocalizedString(localeTag, "messages." + path);
        return parseText(raw);
    }

    public Component getMessage(String localeTag, String path, OfflinePlayer placeholderTarget) {
        String raw = getLocalizedString(localeTag, "messages." + path);
        return parseText(raw, placeholderTarget);
    }

    public Component getMessage(String path, Map<String, String> replacements) {
        return getMessage((String) null, path, replacements);
    }

    public Component getMessage(Player player, String path, Map<String, String> replacements) {
        String raw = getLocalizedString(resolveLocaleTag(player), "messages." + path);
        return parseText(applyReplacements(raw, replacements), player);
    }

    public Component getMessage(String localeTag, String path, Map<String, String> replacements) {
        String raw = getLocalizedString(localeTag, "messages." + path);
        return parseText(applyReplacements(raw, replacements));
    }

    public Component getMessage(
            String localeTag,
            String path,
            OfflinePlayer placeholderTarget,
            Map<String, String> replacements) {
        String raw = getLocalizedString(localeTag, "messages." + path);
        return parseText(applyReplacements(raw, replacements), placeholderTarget);
    }

    public Component parseText(String raw) {
        return miniMessage.deserialize(raw != null ? raw : "");
    }

    public Component parseText(String raw, OfflinePlayer placeholderTarget) {
        String sanitized = raw != null ? raw : "";
        return miniMessage.deserialize(applyPlaceholderApi(sanitized, placeholderTarget));
    }

    public List<ActionButton> buildActionButtons(String configSection, ActionButton primaryAction) {
        return buildActionButtons(configSection, primaryAction, (String) null);
    }

    public List<ActionButton> buildActionButtons(String configSection, ActionButton primaryAction, Player player) {
        return buildActionButtons(configSection, primaryAction, resolveLocaleTag(player), player);
    }

    public List<ActionButton> buildActionButtons(String configSection, ActionButton primaryAction, String localeTag) {
        return buildActionButtons(configSection, primaryAction, localeTag, null);
    }

    public List<ActionButton> buildActionButtons(
            String configSection,
            ActionButton primaryAction,
            String localeTag,
            OfflinePlayer placeholderTarget) {
        List<ActionButton> buttons = new ArrayList<>();
        List<Map<?, ?>> rawActions = config.getMapList(configSection + ".actions");

        boolean hasPrimaryAction = false;

        if (rawActions != null && !rawActions.isEmpty()) {
            for (int actionIndex = 0; actionIndex < rawActions.size(); actionIndex++) {
                Map<?, ?> actionMap = rawActions.get(actionIndex);
                String type = extractString(actionMap, "type", "submit").toLowerCase(Locale.ROOT);
                String labelText = extractString(actionMap, "label", "<gray>Button</gray>");
                String localizedLabel = getLocalizedString(
                        localeTag,
                        configSection + ".actions." + actionIndex + ".label",
                        labelText);
                Component label = parseText(localizedLabel, placeholderTarget);

                switch (type) {
                    case "submit" -> {
                        buttons.add(primaryAction);
                        hasPrimaryAction = true;
                    }
                    case "command" -> {
                        String commandTemplate = extractString(actionMap, "template", "/help");
                        buttons.add(
                                ActionButton.builder(label)
                                        .action(DialogAction.commandTemplate(commandTemplate))
                                        .build());
                    }
                }
            }
        }

        if (!hasPrimaryAction) {
            buttons.addFirst(primaryAction);
        }

        return buttons;
    }

    private String getLocalizedString(String localeTag, String path) {
        return getLocalizedString(localeTag, path, "");
    }

    private String getLocalizedString(String localeTag, String path, String defaultValue) {
        String legacyValue = config.getString(path, defaultValue);
        return localizationManager.getStringByLocaleTag(localeTag, path, legacyValue);
    }

    private List<String> getLocalizedStringList(String localeTag, String path) {
        List<String> legacyValue = config.getStringList(path);
        return localizationManager.getStringListByLocaleTag(localeTag, path, legacyValue);
    }

    private String extractString(Map<?, ?> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }

    private String resolveLocaleTag(Player player) {
        if (player == null) {
            return null;
        }

        try {
            Method localeMethod = player.getClass().getMethod("locale");
            Object localeValue = localeMethod.invoke(player);
            return Objects.toString(localeValue, null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private String applyReplacements(String raw, Map<String, String> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return raw;
        }

        String result = raw;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", Objects.toString(entry.getValue(), ""));
        }
        return result;
    }

    private String applyPlaceholderApi(String raw, OfflinePlayer placeholderTarget) {
        if (placeholderTarget == null || !isPlaceholderApiEnabled()) {
            return raw;
        }

        if (Bukkit.isPrimaryThread()) {
            return PlaceholderAPI.setPlaceholders(placeholderTarget, raw);
        }

        try {
            return Bukkit.getScheduler()
                    .callSyncMethod(plugin, () -> PlaceholderAPI.setPlaceholders(placeholderTarget, raw))
                    .get(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return raw;
        }
    }

    private boolean isPlaceholderApiEnabled() {
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        return pluginManager.isPluginEnabled("PlaceholderAPI");
    }

    private void migrateLegacyTextConfig() {
        FileConfiguration englishTranslation = localizationManager.getTranslation("en");
        if (englishTranslation == null) {
            return;
        }

        boolean translationUpdated = false;
        for (String path : LEGACY_TRANSLATION_PATHS) {
            if (!config.contains(path)) {
                continue;
            }

            if (config.isList(path)) {
                List<?> value = config.getList(path);
                if (value != null && !value.isEmpty()) {
                    englishTranslation.set(path, value);
                    translationUpdated = true;
                }
                continue;
            }

            if (config.isString(path)) {
                String value = config.getString(path);
                if (value != null && !value.isBlank()) {
                    englishTranslation.set(path, value);
                    translationUpdated = true;
                }
            }
        }

        if (translationUpdated) {
            localizationManager.saveTranslation("en");
        }

        if (config.getBoolean("localization.prune-legacy-config", false)) {
            pruneLegacyConfigKeys();
        }
    }

    private void pruneLegacyConfigKeys() {
        File dataFolder = plugin.getDataFolder();
        File configFile = new File(dataFolder, "config.yml");
        File backupFile = new File(dataFolder, "config.backup-before-legacy-prune.yml");

        if (configFile.exists() && !backupFile.exists()) {
            try {
                config.save(backupFile);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to create config backup before pruning legacy keys.", exception);
                return;
            }
        }

        boolean changed = false;
        for (String path : LEGACY_TRANSLATION_PATHS) {
            if (config.contains(path)) {
                config.set(path, null);
                changed = true;
            }
        }

        if (changed) {
            plugin.saveConfig();
            plugin.reloadConfig();
            this.config = plugin.getConfig();
        }
    }
}
