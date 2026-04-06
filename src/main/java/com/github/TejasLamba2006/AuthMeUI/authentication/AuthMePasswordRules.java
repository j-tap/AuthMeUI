package com.github.TejasLamba2006.AuthMeUI.authentication;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Mirrors AuthMe {@code ValidationService.validatePassword} using AuthMe's own config values,
 * so dialog errors match what AuthMe would reject (unsafe list, regex, name-as-password, length).
 */
public final class AuthMePasswordRules {

    /** Defaults match AuthMe {@code SecuritySettings.UNSAFE_PASSWORDS}. */
    private static final Set<String> DEFAULT_UNSAFE_PASSWORDS = Set.of(
            "123456", "password", "qwerty", "12345", "54321", "123456789", "help");

    private static final String PATH_MIN_LEN = "settings.security.minPasswordLength";
    private static final String PATH_MAX_LEN = "settings.security.passwordMaxLength";
    private static final String PATH_REGEX = "settings.restrictions.allowedPasswordCharacters";
    private static final String PATH_UNSAFE = "settings.security.unsafePasswords";
    private static final String PATH_REG_ENABLED = "settings.registration.enabled";

    private static final String LEGACY_MIN = "security.minPasswordLength";
    private static final String LEGACY_MAX = "security.passwordMaxLength";

    private final Plugin authMePlugin;

    public AuthMePasswordRules(Plugin authMePlugin) {
        this.authMePlugin = authMePlugin;
    }

    /**
     * @return null if the password is allowed, otherwise the reason to show in the UI
     */
    public AuthenticationBridge.RegistrationResult validate(String password, String playerName) {
        if (authMePlugin == null || !authMePlugin.isEnabled()) {
            return null;
        }
        FileConfiguration cfg = authMePlugin.getConfig();

        if (!cfg.getBoolean(PATH_REG_ENABLED, true)) {
            return AuthenticationBridge.RegistrationResult.REGISTRATION_DISABLED;
        }

        if (password == null || password.isBlank()) {
            return AuthenticationBridge.RegistrationResult.INVALID_PASSWORD;
        }

        String passLow = password.toLowerCase(Locale.ROOT);
        String patternStr = cfg.getString(PATH_REGEX, "[!-~]*");
        Pattern pattern = safePattern(patternStr);
        if (!pattern.matcher(passLow).matches()) {
            return AuthenticationBridge.RegistrationResult.PASSWORD_FORBIDDEN_CHARACTERS;
        }

        if (passLow.equalsIgnoreCase(playerName)) {
            return AuthenticationBridge.RegistrationResult.PASSWORD_SAME_AS_USERNAME;
        }

        int minLen = getInt(cfg, PATH_MIN_LEN, LEGACY_MIN, 5);
        int maxLen = getInt(cfg, PATH_MAX_LEN, LEGACY_MAX, 30);
        if (password.length() < minLen) {
            return AuthenticationBridge.RegistrationResult.PASSWORD_TOO_SHORT;
        }
        if (password.length() > maxLen) {
            return AuthenticationBridge.RegistrationResult.PASSWORD_TOO_LONG;
        }

        Set<String> unsafe = loadUnsafePasswords(cfg);
        if (unsafe.contains(passLow)) {
            return AuthenticationBridge.RegistrationResult.PASSWORD_UNSAFE;
        }

        return null;
    }

    public String allowedPasswordPatternForDisplay() {
        if (authMePlugin == null || !authMePlugin.isEnabled()) {
            return "[!-~]*";
        }
        return authMePlugin.getConfig().getString(PATH_REGEX, "[!-~]*");
    }

    private static int getInt(FileConfiguration cfg, String primary, String legacy, int def) {
        if (cfg.contains(primary)) {
            return cfg.getInt(primary);
        }
        if (cfg.contains(legacy)) {
            return cfg.getInt(legacy);
        }
        return def;
    }

    private static Pattern safePattern(String patternStr) {
        try {
            return Pattern.compile(patternStr);
        } catch (PatternSyntaxException e) {
            return Pattern.compile("[!-~]*");
        }
    }

    private static Set<String> loadUnsafePasswords(FileConfiguration cfg) {
        if (!cfg.isSet(PATH_UNSAFE)) {
            return new HashSet<>(DEFAULT_UNSAFE_PASSWORDS);
        }
        List<String> list = cfg.getStringList(PATH_UNSAFE);
        Set<String> out = new HashSet<>();
        for (String entry : list) {
            if (entry != null && !entry.isEmpty()) {
                out.add(entry.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}
