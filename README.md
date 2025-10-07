# AntiRaidSystem

RU / Русский
------------
Плагин защиты от рейда (offline-mode) для Spigot/Paper 1.21.x.

## Возможности
- Привязка никнейма к IP с «грейсом» на несколько IP и опцией разрешения той же /24
- Белые списки по никам и IP
- Ограничения для новых игроков (блок команд + кулдаун)
- Глобальная блокировка опасных команд (например, execute)
- RU/EN мультиязычность с автоопределением локали
- Режим строгой защиты для админ-ников (только заранее привязанные IP)

## Совместимость
- Spigot/Paper 1.21 – 1.21.x (`api-version: 1.21`)

## Установка
1. Скопируйте `build/libs/AntiRaidSystem-0.0.1.jar` в папку `plugins`.
2. Запустите сервер для генерации конфига.
3. Настройте `config.yml` под ваши нужды.

## Быстрый старт (рутинные шаги)
1. Оставьте строгие дефолты (1 IP, без /24, доверие через сутки).
2. Добавьте свои админ-ники в `protected.players` (или командой `/ars protect <ник>`).
3. Первые входы админов: авторизуйте их IP через `/ars authorize <ник> <ip>`.
4. Для игроков с частой сменой IP — по ситуации используйте `/ars authorize` или повышайте `general.ip_lock_auto_learn_up_to`.
5. Следите за логами плагина при блокировках (pre-login и команды).

## Сценарии и решения
- Динамический IP у игрока:
  - По умолчанию будет кик при смене IP. Решение: `/ars authorize <ник> <новый-ip>`.
  - Меняется часто? Повысить `general.ip_lock_auto_learn_up_to` (например, 2–3) или временно включить `/24` (`ip_lock_allow_same_subnet_24: true`).
- Админ-ник под атакой (подмена ника):
  - Добавьте ник в `protected.players`. Вход только с заранее авторизованных IP.
- Новые игроки спамят командами:
  - Увеличьте `security.command_cooldown_seconds` (например, 2–3).
- Пытаются ломать через WorldEdit:
  - Недоверенным уже блокируется `//` и базовые WE-команды. Можно оставить игрок недоверенным (24ч) или вручную выдать доверие проверенным `/ars trust <ник>`.
- Хотят подсмотреть плагины/версию:
  - Недоверенным блокируются `pl/plugins/version/help` и аналоги. Можно расширить `security.blocked_for_untrusted`.
- Нужно оперативно впустить игрока со всеми командами:
  - Выдать доверие: `/ars trust <ник>` (добавит в `trusted.nicknames`). Снять — `/ars untrust <ник>`.

## Команды
- `/ars reload` — перезагрузка конфига
- `/ars authorize <player> <ip>` — авторизовать IP для ника
- `/ars allowplayer <player>` — добавить разрешённого игрока
- `/ars allowip <ip>` — добавить разрешённый IP
- `/ars protect <player>` — включить строгую защиту для ника
- `/ars unprotect <player>` — выключить строгую защиту

Право: `ars.admin` (по умолчанию OP).

## Настройка (основное)
- `general.ip_lock_enabled` — включить привязку ник ⇄ IP
- `general.ip_lock_auto_learn_up_to` — автоматически запоминать до N IP для ника
- `general.ip_lock_allow_same_subnet_24` — разрешать ту же /24 подсеть
- `general.allowlist_players_only` / `general.allowlist_ips_only` — строгие режимы белого списка
- `general.block_dangerous_commands_globally` — блокировать команды для всех
- `security.command_cooldown_seconds` — кулдаун команд
- `security.blocked_for_untrusted` — команды, запрещённые новым игрокам (включая WorldEdit/FAWE, scoreboard, инфо о плагинах)
- `protected.players` — список ников со строгой защитой
- `messages` — RU/EN сообщения, двуязычные kick-сообщения при неизвестной локали

Полные комментарии с описанием опций — в `config.yml` (RU/EN).

## Лицензия (кратко)
- Разрешено: использовать плагин; распространять оригинальный JAR без изменений с указанием автора.
- Запрещено: продавать; модифицировать/декомпилировать/форкать и распространять производные версии; убирать автора; менять лицензию.
- Изменять исходники можно только для личного использования на своих серверах; публикация/распространение изменённых версий запрещены.
См. файл `LICENSE`.

---

EN / English
------------
Protection plugin against offline-mode raid/scanner attacks for Spigot/Paper 1.21.x.

## Features
- Nickname ⇄ IP lock with multi-IP grace and optional /24 subnet allowance
- Player/IP allowlists
- New-player restrictions (blocked commands + command cooldown)
- Global block for dangerous commands (e.g., execute)
- RU/EN i18n with auto locale detection
- Strict protection mode for admin nicknames (only pre-authorized IPs)

## Compatibility
- Spigot/Paper 1.21 – 1.21.x (`api-version: 1.21`)

## Installation
1. Place `build/libs/AntiRaidSystem-0.0.1.jar` into `plugins`.
2. Start server to generate the config.
3. Adjust `config.yml` to your needs.

## Quick start (routine steps)
1. Keep strict defaults (1 IP, no /24, trust after 24h).
2. Add your admin nicknames to `protected.players` (or use `/ars protect <player>`).
3. On first admin logins, authorize their IPs via `/ars authorize <player> <ip>`.
4. For users with frequently changing IPs, either use `/ars authorize` as needed or increase `general.ip_lock_auto_learn_up_to`.
5. Watch plugin logs for pre-login and command blocks.

## Scenarios & solutions
- Player with dynamic IP:
  - Default behavior: kick on IP change. Solution: `/ars authorize <nick> <new-ip>`.
  - Changes often? Increase `general.ip_lock_auto_learn_up_to` (e.g., 2–3) or enable `/24` (`ip_lock_allow_same_subnet_24: true`).
- Admin nickname targeted:
  - Add to `protected.players`. Only pre-authorized IPs may join.
- New players spamming commands:
  - Raise `security.command_cooldown_seconds` (e.g., 2–3).
- Attempts to abuse WorldEdit:
  - Untrusted already blocked for `//` and common WE commands. Keep them untrusted (24h) or grant trust manually `/ars trust <player>`.
- Curious users checking plugins/version:
  - Untrusted blocked for `pl/plugins/version/help`. Extend `security.blocked_for_untrusted` if needed.
- Need to quickly allow full commands to a user:
  - Grant trust: `/ars trust <player>` (adds to `trusted.nicknames`). Revoke: `/ars untrust <player>`.
## Commands
- `/ars reload` — reload config
- `/ars authorize <player> <ip>` — authorize IP for a nickname
- `/ars allowplayer <player>` — add allowed player
- `/ars allowip <ip>` — add allowed IP
- `/ars protect <player>` — enable strict protection for nickname
- `/ars unprotect <player>` — disable strict protection

Permission: `ars.admin` (default: OP).

## Config (highlights)
- `general.ip_lock_enabled`: enable nickname ⇄ IP lock
- `general.ip_lock_auto_learn_up_to`: auto-learn up to N IPs per nickname
- `general.ip_lock_allow_same_subnet_24`: allow same /24 subnet
- `general.allowlist_players_only` / `general.allowlist_ips_only`: strict allowlist modes
- `general.block_dangerous_commands_globally`: block commands for all
- `security.command_cooldown_seconds`: per-player command cooldown
- `security.blocked_for_untrusted`: blocked commands for new players (WorldEdit/FAWE, scoreboard, plugin info)
- `protected.players`: nicknames with strict protection
- `messages`: RU/EN messages, bilingual kick texts for unknown locale

See `config.yml` for fully documented options (RU/EN comments).

## License (short)
- Allowed: use the plugin; redistribute the original, unmodified JAR with attribution.
- Prohibited: selling; modifying/decompiling/forking and distributing derivatives; removing attribution; changing the license.
- You may modify source code only for personal use on your own servers. Publishing/distributing modified versions is prohibited.
See `LICENSE` for details.

