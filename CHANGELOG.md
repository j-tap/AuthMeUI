# Changelog

All notable changes to AuthMeUI will be documented in this file.

---

## [1.2.0] - 2026-03-24

### Multilingual support (i18n)

- **Per-player language** — texts use the Minecraft client locale when `localization.auto-detect-player-locale` is enabled (including configuration phase / pre-join dialogs).
- **Translation files** — add `plugins/AuthMeUI/translations/<language>.yml` (e.g. `en.yml`, `ru.yml`). Bundled default: `translations/en.yml`.
- **Config** — `localization.default-language` (fallback when no matching file exists) and optional `localization.prune-legacy-config` (see migration below).

### Backward compatibility

- On startup and `/authmeui reload`, **legacy text keys** still present in `config.yml` (titles, bodies, labels, `messages.*`) are **copied into** `translations/en.yml` so existing customized servers keep their wording.
- New installs: dialog copy lives in translation files; `config.yml` keeps behaviour flags (e.g. rules enabled, agreement checkbox key, optional custom action buttons).

### Migration / cleanup

- Set `localization.prune-legacy-config: true` **once** to remove migrated text keys from `config.yml`. A backup is written first as `plugins/AuthMeUI/config.backup-before-legacy-prune.yml` (only created if it does not already exist).

### Developer / code

- Centralized message resolution through `LocalizationManager` and `SettingsManager`; removed duplicated hardcoded default strings from commands and listeners.

---

## [1.1.0] - 2026-01-24

### New Feature: Pre-Join Authentication

**Authenticate players BEFORE they enter your server!**

Ever wanted players to log in or register before they even see your spawn? Now you can! With the new "Configuration Phase" mode, the login/registration dialog appears during the loading screen - players can't enter your world until they've authenticated.

#### How it works

- **Default mode (off)**: Players join the server first, then see the login dialog (same as before)
- **New mode (on)**: Players see the login dialog while connecting - they can't join until they authenticate

#### Why use it?

- Prevents unauthenticated players from ever seeing your world
- Cleaner experience - no "please login" messages in chat
- Players who fail to authenticate are simply disconnected

#### How to enable

Add these lines to your `config.yml`:

```yaml
dialogs:
  use-configuration-phase: true
  configuration-phase-timeout: 60
```

The timeout is how many seconds a player has to authenticate before being disconnected (default: 60 seconds).

---

## [1.0.0] - Initial Release

- Native dialog windows for login and registration
- Server rules dialog with agreement checkbox
- Full MiniMessage formatting support
- PlaceholderAPI support
- Works with all AuthMe forks
- Fully customizable messages and buttons
