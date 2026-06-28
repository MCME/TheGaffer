# Design note — Job team model: vanilla team chat + glow consolidation

**Status:** Proposal for review · Branch `2026-rework` · 2026-06
**Relates to:** AUDIT.md findings around the VentureChat dependency (build wart L5) and the glow feature.

## Goal

Make a job's **scoreboard Team** the single source of truth for "who is in this job," and let vanilla Minecraft render the rest:

- **Team chat** via the built-in `/teammsg` (`/tm`) command → **replaces the VentureChat per-job channel** and removes that dependency (and its hardcoded `D:/MCME/dev/jars/VentureChat.jar` build path).
- **Glow** keeps working off the same team (it already uses team colour).
- **Locator bar** colour-coding comes along for free later, once on a supporting version (see *Future*, parked).

One team membership → up to three native features, zero third-party plugins.

## Current implementation (what exists today)

- **Glow** (`Job.setGlowing()` / `addHelperTeam` / `addWorkerTeam` / `setGlow`): creates a **per-job custom scoreboard** (`Bukkit.getScoreboardManager().getNewScoreboard()`), registers **two teams per job** — `<job>H` (helpers) and `<job>W` (workers) — sets their colours, adds members, and assigns that custom scoreboard to each member via `player.setScoreboard(...)`.
- **VentureChat** (`VentureChatUtil`): the *only* use is `addListening` / `removeListening` to subscribe/unsubscribe job members to a chat channel (named after the Discord channel). ~11 call sites in `Job`/`JobEventListener`. There is **no chat listener** in TheGaffer, and `Job.jobChat(...)` is **dead code** (never called).

## Proposed model

1. **One team per job**, on the **main scoreboard** (`getMainScoreboard()`), not a per-job custom board.
2. All members (owner + helpers + workers) join that one team.
3. Team colour = the job's colour (config). Glow uses it as today.
4. **Chat:** members use vanilla `/teammsg` / `/tm`. No listener needed for basic per-message team chat.
5. **Remove** `VentureChatUtil`, its ~11 call sites, the VentureChat dependency, and the hardcoded systemPath. Remove or repurpose the dead `jobChat()`.

## Why main scoreboard (the crux)

Vanilla `/teammsg` resolves teams from the **main** scoreboard. The current glow uses **per-player custom** scoreboards, which the vanilla command does not consult. So team chat only works if the job teams live on the main scoreboard. **This must be confirmed by testing on the target server version before committing to the approach.**

Moving to the main scoreboard also changes glow visibility: today only job members (who hold the custom board) see the glow colour; on the main board it's server-wide. That is likely acceptable (or desirable) but is a behaviour change to confirm.

## Open decisions (need head-dev input)

1. **One team per job vs keep the helper/worker split.**
   - *One team* → unified team chat, one locator colour, simpler. Helper/worker distinction would move to another signal (e.g. a name prefix or a separate indicator).
   - *Two teams* (status quo) → keeps the visual helper/worker colour split, but means **two** `/teammsg` channels and two locator colours per job.
   - **Recommendation:** one team per job.
2. **One job per player.** Vanilla allows a player on **only one team at a time**. The current code does not stop a player being a worker in two active jobs simultaneously, and the glow already shares this latent assumption. The team model makes it a hard constraint: either **enforce one job per player** (add a guard in `addWorker`), or accept that the team reflects only their latest/primary job. **Recommendation:** enforce one active job per player.
3. **Chat UX.** `/teammsg <msg>` is per-message. VentureChat let players *set* an active channel so all chat routed there. If "set job chat as my default" is wanted, add a small `/jobchat` toggle backed by `AsyncChatEvent` that reroutes the player's chat to the team — still using the team as the delivery target.
4. **Team options.** Decide defaults for `Team.Option` (nametag visibility, collision, friendly fire) now that teams are server-visible.

## Caveats & risks

- Main-scoreboard teams are **server-wide**: team names must be unique (the existing 16-char limit handling helps) and must be cleaned up on job end (already done in `setRunning(false)` — extend to the single-team model). Watch for conflicts with any other plugin that manages main-scoreboard teams or nametag colours for the same players.
- No persistence today (audit P1) → teams are rebuilt on job activation; no migration of saved state needed yet.
- `/teammsg` requires the player to actually be on the team (handled by membership sync).

## What this removes

- The **VentureChat dependency** and its hardcoded `systemPath` (closes part of audit L5 — non-reproducible build).
- `VentureChatUtil` + its call sites.
- The dead `jobChat()` (or repurpose it for the optional `/jobchat` toggle).

## Effort & sequencing

Moderate — refactor `setGlowing`/`add*Team`/`remove*Team`/`setGlow` to use the main scoreboard with one team per job; delete VentureChat usage + dep; optional `/jobchat` toggle. Fits after the safety/cleanup steps and pairs naturally with the "remove dead integrations" pass.

## Future (parked) — Locator bar

Not part of this rework; revisit after the version bump.

- The locator bar is **Java 1.21.6+**. TheGaffer currently targets **1.19**, so this needs the modernisation/version-bump step first (and the server on 1.21.6+).
- Mechanic check: teams **colour-code** players on the locator bar; they do **not** gate visibility — by default everyone sees everyone. "Team-only visibility" is not a vanilla feature (open feature request). So the payoff is: once on a coloured job team and on a supporting version, **job members are colour-coded on everyone's locator bar automatically — no extra code**, just the version.
- Action when revisited: confirm the running server version, then it's essentially free given the team model above.

## References

- [Locator Bar – Minecraft Wiki](https://minecraft.wiki/w/Locator_Bar)
- [Java Edition 1.21.6 – Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_1.21.6)
- Bukkit `Scoreboard` / `Team` API; vanilla `/teammsg` (`/tm`)
