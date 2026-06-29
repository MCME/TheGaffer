# Design note — name → UUID identity (H5)

**Status:** Proposal for review · Branch `2026-rework` · 2026-06
**Relates to:** AUDIT.md finding **H5** (player identity stored by name, not UUID). Touches the persistence format added for **P1**.

## Problem

The plugin identifies players by **name** everywhere: `owner`, `workers`, `helpers`, `bannedWorkers`, `invitedWorkers` are `List<String>` of names; membership is `workers.contains(player.getName())`; and online/offline resolution goes through `Bukkit.getOfflinePlayer(String)`. Two concrete consequences:

1. **Identity isn't stable.** Names change. A **banned** player who renames evades the ban; a renamed worker silently loses membership. Bans/membership should key on something immutable.
2. **Blocking main-thread lookups.** `Bukkit.getOfflinePlayer(String)` is deprecated precisely because, for a name it hasn't cached, it can make a **blocking HTTP call to Mojang on the main thread**. The code calls it **28×** (13 in `Job`, 12 in `AdminMethods`, 3 in `CleanupUtil`) — several during broadcasts (`getAllAsPlayersArray`, `sendToHelpers/Workers`). `getOfflinePlayer(UUID)` never blocks.

## Cost (honest)

This is the **most invasive and most regression-prone** remaining batch: ~66 name/identity touchpoints in `Job.java` alone, plus the command layer and display. And it's the one change I **cannot meaningfully runtime-test here** — identity bugs (wrong player added/removed/banned) won't show up at compile time.

## Timing argument (why now is the cheap moment)

The `2026-rework` branch isn't deployed, so the new YAML persistence has **no production data yet**. If we move member storage to UUID **now**, we never ship the name-based format → **zero data migration**. Do it after deploy and we'd need a one-time name→UUID conversion of every `jobs/*.yml`.

## Proposed design (full migration)

- **Identity → UUID** for `owner`, `workers`, `helpers`, `bannedWorkers`, `invitedWorkers`. **Job names stay strings** (jobs are keyed by name; the `TreeMap` key is unchanged).
- **Display → resolve names lazily.** Show `Bukkit.getOfflinePlayer(uuid).getName()` (cached, non-blocking) with a UUID-string fallback. Optionally cache a `lastKnownName` per member for offline display in `/job check` / `getInfo`.
- **Command input boundary.** When an admin types a player *name* (`/job admin <job> addhelper <name>`), resolve via `getPlayerExact(name)` (online, instant) first; only fall back to a (blocking) offline lookup if necessary, or require the target be online. This is the **one** unavoidable name→UUID step — and it's almost always an online player.
- **Persistence.** Store UUID strings in the member lists. No migration (see timing).

## Decisions for review

1. **Full UUID migration vs. targeted hardening vs. defer?**
   - **Full** (recommended *if* in-game QA is available before merge): fixes ban-evasion + removes blocking lookups; ~66 touchpoints; needs testing.
   - **Targeted hardening** (lower risk): keep names as identity, but replace the blocking `getOfflinePlayer(String)` calls with online/cached lookups. Captures the **perf** win, not the **ban-integrity** win. Far less invasive.
   - **Defer**: leave H5 until a test server is available.
2. **Offline command targets:** require the target be online for admin add/ban, or accept one blocking lookup for offline names?
3. **Cache `lastKnownName`** per member for nicer offline display, or always resolve live?

## Recommendation

Because the branch is pre-deployment (no data to migrate) and H5 closes a real ban-evasion hole *and* removes main-thread Mojang stalls, I recommend the **full migration now** — on the condition that it gets an **in-game QA pass before merge** (this is the one batch where compile-green is not enough). If QA capacity is the blocker, **targeted hardening** is the safe partial win and we defer the rest.

## References
- `docs/persistence-redesign.md` (the YAML format this would change)
- AUDIT.md (H5); Bukkit `OfflinePlayer`, `getOfflinePlayer(UUID)` vs `(String)`
