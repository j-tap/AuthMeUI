# Changelog

All notable changes to AuthMeUI will be documented in this file.

---

## [1.2.2] - 2026-03-27

### Post-join authentication reliability

- Added deterministic join gating via `AuthMeAsyncPreLoginEvent`: when AuthMe marks a player as unable to login, the player UUID is flagged for post-join dialog.
- `PlayerJoinEvent` now schedules a delayed check only for flagged UUIDs using `dialogs.post-join-open-delay-ticks`.
- Dialog is shown only when the delayed check confirms the player is still unauthenticated:
  - registered players receive login dialog;
  - unregistered players receive rules/register flow based on settings.
- Join handler runs on `EventPriority.NORMAL` for safer integration with other plugins updating join state.

### Duplicate dialog protection

- Added in-session duplicate guard for post-join dialog opening using player UUID tracking.
- Added `pendingJoinTasks<UUID, BukkitTask>` tracking to avoid duplicate delayed join tasks.
- Added cleanup on `LoginEvent`, `RestoreSessionEvent`, and `PlayerQuitEvent` (task cancellation + state cleanup) to prevent stale state.

---

## [1.2.1] - 2026-03-27

### PlaceholderAPI support

- Added PlaceholderAPI resolution for translated dialog text and `messages.*`.
- PAPI placeholders are resolved with player context in-game and in configuration phase dialogs (using UUID-bound offline player context).
- Placeholder resolution remains safe when dialog flow runs from async contexts.

### Localization consistency

- Preserved per-player locale behavior from the multilingual system.
- Improved locale extraction fallback for configuration-phase connections to keep client language detection stable.

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
