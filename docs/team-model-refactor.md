# Design note — Native job chat + glow (decisions resolved)

**Status:** Decisions resolved — ready to implement · Branch `2026-rework` · 2026-06
**Relates to:** AUDIT.md — VentureChat dependency / non-reproducible build (L5), hot-path perf (H1/H2), threading (C1/C2).

## Goal

Replace the VentureChat per-job chat channel with **native job chat owned by TheGaffer**, remove the VentureChat dependency (and its hardcoded `D:/MCME/dev/jars/VentureChat.jar` path), keep the existing glow as-is, and **park the locator bar** as a future, version-gated enhancement.

## Decisions (resolved)

1. **Helper/worker split: kept.** It's a real role distinction — *helpers* are staff co-managers (need the create permission; can edit the job and add helpers), *workers* are builders (join permission). The glow shows them in different colours so managers are identifiable at a glance. Retained for glow; it does **not** affect chat under the approach below.
2. **One job per player: enforced.** A player may belong to at most one active job in any role (owner/helper/worker). Guarded in the join, invite, and create paths.
3. **No main-scoreboard dependency (for now).** Concurrency was never at risk — a scoreboard holds *many* teams, so unlimited concurrent jobs are fine regardless. The main scoreboard mattered only for vanilla `/teammsg`, which the sticky-chat requirement makes unnecessary (see #4). Glow stays on its current per-job scoreboards.
4. **Sticky team chat: yes.** Players can toggle into job chat so all their messages route to the job without typing a command each time.

## Why native chat instead of vanilla `/teammsg`

Vanilla `/teammsg` is per-message and reads teams from the **main** scoreboard. Making chat *sticky* requires intercepting chat anyway (a chat handler) — which removes the only benefit of the vanilla command and avoids entangling chat with the scoreboard. So chat is implemented directly over TheGaffer's existing membership data. This keeps **chat** (who receives a message) and **glow / locator** (how a player is rendered) as the two separate concerns they actually are.

## Approach

### a) Enforce one job per player — *do first*
- In `addWorker` / the `/job join` path, the invite & admin paths, and `createjob`: reject if the player is already in an active job (owner/helper/worker), with a clear message.
- Simplifies `getJobWorking` (unambiguous) and satisfies the future one-team-per-player rule.

### b) Native job chat — `/jobchat` (`/jc`) toggle
- A per-player toggle command `/jobchat` (alias `/jc`); `/jc <message>` also sends a one-off.
- Listen to Paper's **`io.papermc.paper.event.player.AsyncChatEvent`**: if the sender has sticky job chat on, **cancel the event** and re-dispatch on the main thread, delivering the message (with a `[Job]` prefix) to the job's online members. Toggle off → normal chat.
- Recipients come from TheGaffer's own membership (`getAllAsPlayersArray()`) — **no scoreboard and no VentureChat needed.**

> **Concurrency note.** `AsyncChatEvent` fires **off the main thread**, so the handler does *no* off-thread reads of job state: it checks only a concurrent toggle, then **cancels** the event and hops to the main thread (`Bukkit.getScheduler().runTask`) to resolve membership via the existing `JobDatabase.getJobWorking()` / `getAllAsPlayersArray()` and send. Safe by construction, no parallel index. (The `player → job` index for the H1/H2 hot-path fix is therefore **decoupled** — it comes with the perf step, not chat.) Cancelling also keeps job chat private — it does not reach the main chat or the Discord bridge, which is the intended channel behaviour.

### c) Glow — unchanged
Keep the helper/worker teams on the per-job custom scoreboard exactly as today (decision #1). No change required for this rework.

### d) Remove VentureChat
Delete `VentureChatUtil` and its ~11 call sites, the VentureChat dependency, and the hardcoded `systemPath`. Remove the dead `jobChat()` (or fold it into the new handler).

## Caveats & risks
- **Async chat thread-safety** — handled by cancelling the async event and doing all job reads/sends on the main thread (see the concurrency note); no parallel index needed.
- **No migration needed** — there's no persistence today, so one-job-per-player is simply enforced going forward.
- **Chat formatting** — match MCME's existing chat style (prefix/colour) so job chat reads consistently with the server.

## What this removes
- The **VentureChat dependency** + hardcoded `systemPath` (closes part of audit L5 — non-reproducible build).
- `VentureChatUtil` + its call sites, and the dead `jobChat()`.

## Implementation order
1. **Enforce one job per player** (join / invite / create guards).
2. **Add the `/jobchat` handler** — cancel the async chat event and re-dispatch to job members on the main thread (`utilities/JobChat`, `listeners/JobChatListener`, `commands/JobChatCommand`).
3. **Remove VentureChat** (util, call sites, dependency, `systemPath`).

## Future (parked) — main scoreboard + locator bar, together
- The locator bar (Java **1.21.6+**) colour-codes players by **team** on everyone's bar; it does **not** gate visibility (everyone sees everyone by default). To get job members colour-coded there, their team must live on the **main** scoreboard so all viewers resolve the colour.
- So "move glow teams to the main scoreboard" and "locator-bar colours" are one bundled, **version-gated** step — revisit after the modernisation / version bump (TheGaffer currently targets 1.19). Not part of this rework.

## References
- [Locator Bar – Minecraft Wiki](https://minecraft.wiki/w/Locator_Bar)
- Paper `AsyncChatEvent`; Adventure `Component`; Bukkit `Scoreboard` / `Team`
