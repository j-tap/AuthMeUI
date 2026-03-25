package com.github.TejasLamba2006.AuthMeUI.configuration;

import com.github.TejasLamba2006.AuthMeUI.AuthMeUIPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class LocalizationManager {

    private static final String DEFAULT_LANGUAGE = "en";

    private final AuthMeUIPlugin plugin;

    private final Map<String, FileConfiguration> translations = new HashMap<>();
    private final Map<String, File> translationFiles = new HashMap<>();
    private String configuredDefaultLanguage = DEFAULT_LANGUAGE;
    private boolean autoDetectPlayerLocale = true;

    public LocalizationManager(AuthMeUIPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        translations.clear();
        translationFiles.clear();

        configuredDefaultLanguage = normalizeLanguageTag(
                plugin.getConfig().getString("localization.default-language", DEFAULT_LANGUAGE));
        if (configuredDefaultLanguage.isBlank()) {
            configuredDefaultLanguage = DEFAULT_LANGUAGE;
        }

        autoDetectPlayerLocale = plugin.getConfig().getBoolean("localization.auto-detect-player-locale", true);

        ensureBundledTranslation("translations/en.yml");
        loadTranslationFiles();

        if (!translations.containsKey(DEFAULT_LANGUAGE)) {
            plugin.getLogger().warning("Default fallback translation file translations/en.yml is missing or invalid.");
        }
    }

    public String resolveLanguage(Player player) {
        if (!autoDetectPlayerLocale || player == null) {
            return configuredDefaultLanguage;
        }

        return resolveLanguage(extractPlayerLocale(player));
    }

    public String resolveLanguage(String localeTag) {
        String playerLocale = normalizeLanguageTag(localeTag);
        if (playerLocale.isBlank()) {
            return configuredDefaultLanguage;
        }

        if (translations.containsKey(playerLocale)) {
            return playerLocale;
        }

        int separatorIndex = playerLocale.indexOf('_');
        if (separatorIndex > 0) {
            String baseLanguage = playerLocale.substring(0, separatorIndex);
            if (translations.containsKey(baseLanguage)) {
                return baseLanguage;
            }
        }

        return configuredDefaultLanguage;
    }

    public String getString(Player player, String path, String defaultValue) {
        return getStringByLanguage(resolveLanguage(player), path, defaultValue);
    }

    public String getStringByLocaleTag(String localeTag, String path, String defaultValue) {
        return getStringByLanguage(resolveLanguage(localeTag), path, defaultValue);
    }

    public String getStringByLanguage(String language, String path, String defaultValue) {
        String normalizedLanguage = normalizeLanguageTag(language);
        String value = getStringFromTranslation(normalizedLanguage, path);

        if (value != null) {
            return value;
        }

        if (!normalizedLanguage.equals(configuredDefaultLanguage)) {
            value = getStringFromTranslation(configuredDefaultLanguage, path);
            if (value != null) {
                return value;
            }
        }

        if (!configuredDefaultLanguage.equals(DEFAULT_LANGUAGE)) {
            value = getStringFromTranslation(DEFAULT_LANGUAGE, path);
            if (value != null) {
                return value;
            }
        }

        return defaultValue;
    }

    public List<String> getStringList(Player player, String path, List<String> defaultValue) {
        return getStringListByLanguage(resolveLanguage(player), path, defaultValue);
    }

    public List<String> getStringListByLocaleTag(String localeTag, String path, List<String> defaultValue) {
        return getStringListByLanguage(resolveLanguage(localeTag), path, defaultValue);
    }

    public List<String> getStringListByLanguage(String language, String path, List<String> defaultValue) {
        List<String> value = getStringListFromTranslation(normalizeLanguageTag(language), path);
        if (!value.isEmpty()) {
            return value;
        }

        if (!normalizeLanguageTag(language).equals(configuredDefaultLanguage)) {
            value = getStringListFromTranslation(configuredDefaultLanguage, path);
            if (!value.isEmpty()) {
                return value;
            }
        }

        if (!configuredDefaultLanguage.equals(DEFAULT_LANGUAGE)) {
            value = getStringListFromTranslation(DEFAULT_LANGUAGE, path);
            if (!value.isEmpty()) {
                return value;
            }
        }

        return defaultValue;
    }

    public FileConfiguration getTranslation(String language) {
        return translations.get(normalizeLanguageTag(language));
    }

    public boolean saveTranslation(String language) {
        String normalizedLanguage = normalizeLanguageTag(language);
        FileConfiguration translation = translations.get(normalizedLanguage);
        File translationFile = translationFiles.get(normalizedLanguage);

        if (translation == null || translationFile == null) {
            return false;
        }

        try {
            translation.save(translationFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.WARNING,
                    "Failed to save translation file: " + translationFile.getName(),
                    exception);
            return false;
        }
    }

    public String getConfiguredDefaultLanguage() {
        return configuredDefaultLanguage;
    }

    private String getStringFromTranslation(String language, String path) {
        FileConfiguration translation = translations.get(language);
        if (translation == null) {
            return null;
        }

        String value = translation.getString(path);
        if (value == null || value.isBlank()) {
            return null;
        }

        return value;
    }

    private List<String> getStringListFromTranslation(String language, String path) {
        FileConfiguration translation = translations.get(language);
        if (translation == null || !translation.contains(path)) {
            return List.of();
        }

        List<String> values = translation.getStringList(path);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return new ArrayList<>(values);
    }

    private void ensureBundledTranslation(String resourcePath) {
        File targetFile = new File(plugin.getDataFolder(), resourcePath);
        if (targetFile.exists()) {
            return;
        }

        plugin.saveResource(resourcePath, false);
    }

    private void loadTranslationFiles() {
        File translationsDir = new File(plugin.getDataFolder(), "translations");
        if (!translationsDir.exists() && !translationsDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create translations directory.");
            return;
        }

        File[] files = translationsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            int extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex <= 0) {
                continue;
            }

            String language = normalizeLanguageTag(fileName.substring(0, extensionIndex));
            if (language.isBlank()) {
                continue;
            }

            try {
                FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                translations.put(language, configuration);
                translationFiles.put(language, file);
            } catch (Exception exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to load translation file: " + fileName, exception);
            }
        }
    }

    private String normalizeLanguageTag(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
    }

    @SuppressWarnings("deprecation")
    private String extractPlayerLocale(Player player) {
        try {
            Method localeMethod = player.getClass().getMethod("locale");
            Object localeValue = localeMethod.invoke(player);
            if (localeValue instanceof Locale locale) {
                return locale.toLanguageTag();
            }
            return Objects.toString(localeValue, "");
        } catch (ReflectiveOperationException ignored) {
            return player.getLocale();
        }
    }
}
