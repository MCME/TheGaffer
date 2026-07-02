# TheGaffer — v3 (2026 rework) — Technical Change Summary

**Branch:** `2026-rework` · **Base:** `master` (v2.8) · **Scope:** 129 commits · 98 files · **+13,266 / −1,873** lines · 18 new classes
**Target platform:** built against **Paper 26.1.2** (`paper-api:26.1.2.build.72-stable`), runs on the 26.2 dev server · Java 17 · Maven
**Status:** all 125 automated tests green; in-game QA in progress; not yet merged to `master`.

This document is a design-oriented summary for review. It groups the work by theme rather than by commit, and calls out the **design choices** worth scrutiny. A commit-map appendix is at the end.

---

## 0. TL;DR — headline changes

- **Platform jump 1.19.3 → 26.1.x** to unlock the native **Dialog API**; `/createjob` is now a server-driven form, not a chat conversation.
- **New subsystems:** per-job **Statistics** (+ leaderboards, CSV export, Discord recap), **Projects** (grouping jobs with roll-up stats and management commands), **web-map** integration (Dynmap/LiveAtlas), native **job team chat**, and a **visual job boundary**.
- **Identity migrated names → UUIDs**; **persistence** rewritten on `YamlConfiguration`; **all player-facing output migrated to Adventure** `Component`.
- **Discord** messaging modernised to rich embeds with a **graceful text fallback** and a **role-ping whitelist**.
- **Removed** dead integrations (TeamSpeak, VentureChat), the per-job kit feature, and the external McMeProject coupling.
- **Test suite from zero → 125 tests** (JUnit 5 + MockBukkit) with CI; the reflective **protection API contract is pinned by a test**.

---

## 1. Platform & build migration (MC 26.1.x + Dialog API)

**What.** Bumped `paper-api` from `1.19.3-R0.1-SNAPSHOT` to `26.1.2.build.72-stable`, and MockBukkit from `com.github.seeseemelk:MockBukkit-v1.19:3.1.0` to `org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.114.0` (package `be.seeseemelk.mockbukkit` → `org.mockbukkit.mockbukkit`, renamed across 19 test files). Only one production-code break across the whole span: `Material.GRASS` → `Material.SHORT_GRASS` (renamed in MC 1.20.3). Zero deprecation warnings otherwise.

**Design choices & rationale:**

- **Compile against 26.1.2-stable, not 26.2 exactly.** The dev server runs 26.2, but Paper's new versioning (`YY.N[.P].build.NN-{alpha|beta|stable}`) only publishes 26.2 as **alpha**, and — decisively — **MockBukkit has no 26.2 build**. MockBukkit ships a per-version registry snapshot; against a mismatched `paper-api` every `MockBukkit.mock()` throws `InternalDataLoadException` (it detected 26.2's new `sulfur_cube_archetype` registry entry), which would have taken out 66 tests. Maven can't hold two `paper-api` versions in one module, so main and test must agree. Compiling against the **stable 26.1.2** API and running forward-compatibly on the 26.2 server is the standard, low-risk resolution and keeps the suite fully green. The Dialog API has existed since long before 26.1.2, so nothing is lost.
- **No runtime version-detection / no fallback.** An earlier plan gated the Dialog behind a capability check with the chat conversation as a fallback. That was dropped: the plugin now simply **requires** a modern server, giving a single UI path and no reflection/isolation complexity. (The Dialog API's rich builder/registry surface makes reflection impractical anyway.)
- **`api-version` left at `1.19`.** It's a compatibility *floor*, not a match-the-server field, and the current jar already loads on the 26.2 box. The version bump was only ever needed at **compile** time to see the Dialog classes; runtime access to Dialog is registry-based and independent of `api-version`. Left as-is to avoid any load regression during active QA.
- **`waterfall-api` kept.** It supplies `net.md_5.bungee.api.ChatColor` (used for the gradient job-start broadcast), which `paper-api` does not bundle.

---

## 2. `/createjob` → native Dialog form

**What.** `/createjob` (and its `/job create` / `/job start` aliases) now open a native Minecraft **Dialog** — a single-screen form — replacing the multi-prompt `JobCreationConversation` (deleted). Fields: name, description (if enabled), private toggle, radius slider (1–1000), Discord-announce toggle (if Discord present), project dropdown (if active projects exist), glow toggle (if enabled) — only the config-enabled inputs are rendered.

**Design choices:**

- **One funnel, one swap.** All three entry commands already dispatched through the `createjob` command executor, so replacing that single executor (`JobCreationDialog`) migrated every entry point at once.
- **UI/domain separation via the existing event seam.** Job creation goes through `JobDatabase.activateJob(...)`, which fires the job-start event. The Dialog therefore inherited the entire downstream pipeline — chat broadcast, Discord embed, particle border, web-map marker — **without touching any domain logic**. Swapping a UI in front of an event boundary is exactly what that seam buys.
- **Single authoritative creation path (`JobCreationService`).** Validation + build + activate live in one place, with the pure decision logic (`normalizeName`, `clampRadius`, `validateName`) split out as a **testable seam** — 12 unit tests cover it with no server. `validateName` prefers the "already running" outcome over "run before" (an active job also has a saved file on disk, so history-first would mask the clearer message — a deliberate improvement over the old flow).
- **Thread discipline.** The Dialog's custom-click submit callback may fire off the main thread; all Bukkit-touching work (membership re-check, creation, messaging) hops back via `runTask`. The form re-checks "already in a job" at submit as well as at open, because the form can stay open arbitrarily long.
- **Dialog API notes for reviewers.** Built via `Dialog.create(factory -> factory.empty().base(DialogBase.builder(...)…).type(DialogType.confirmation(create, cancel)))`; inputs via `DialogInput.text/bool/numberRange/singleOption`; responses read back by input key via `DialogResponseView.getText/getBoolean/getFloat`. Single-option selections are read with `getText(key)` (returns the selected option `id`). The visual rendering (form layout, slider label format, close-after-submit) is verified by in-game QA — MockBukkit cannot render a Dialog.

---

## 3. New features

### 3.1 Statistics
Per-job counters for **blocks placed / broken** and **participating builders**, counted only **in-job and in-bounds** by `StatsListener` (which replaced the old empty `JobProtection` handlers). `StatsManager` owns live counting, finish/flush, a leaderboard aggregate, and player/leaderboard queries. **Durable active snapshots** are written during a job so counts survive a crash/restart mid-job. Surfaced by `/job stats <job|player>` and `/job leaderboard [placed|broke|active]`, exported by `/job stats export` (CSV), and posted as a **Discord job-end recap**.
- *Design:* storage is `YamlConfiguration` with an idempotent restore and a **test-seam directory override** so persistence round-trips are unit-tested; the Discord recap is a **pure builder** (string-buildable and asserted in tests) wired separately into `onJobEnd`; player-name lookups are **non-blocking** with an explicit not-found/empty state.

### 3.2 Projects
A **Project** entity groups jobs, with `ProjectDatabase` + `ProjectStorage` persistence and **project-level stats roll-up** across member jobs. Commands: `/project create|list|info|setdescription|setgoal|setlead|addmanager|removemanager|complete|archive|reopen|attach|detach|delete` (alias `/pj`), gated by `thegaffer.project.create` / `thegaffer.project.admin`. Membership is by job creation-time selection or `/project attach`, with case-insensitive multi-word name matching.
- *Design:* replaced the stale external **McMeProject** soft-dependency with a native picker in `/createjob`; `hasActiveProjects()` is non-allocating; create is atomic via return value; orphan-detach is admin-gated and `completedTime` clears on reopen.

### 3.3 Web-map integration (Dynmap / LiveAtlas)
`JobMapIntegration` draws area markers for active job zones on Dynmap (LiveAtlas is a Dynmap frontend, so it works transparently).
- *Design — soft-dependency isolation:* **all** Dynmap types are quarantined in a nested class that is only touched after a runtime `getPlugin("dynmap")` guard, with `catch(Throwable)` at the boundaries. This is deliberate: a missing optional dependency raises `NoClassDefFoundError` (an `Error`, **not** caught by `catch(Exception)`), so the types must never be referenced by the outer class's signatures. Verified at the bytecode level (0 `org/dynmap` references in the outer class).

### 3.4 Native job team chat
`/jobchat` (alias `/jc`, one-off `/jc <message>`) provides in-job team chat (`JobChat` + `JobChatListener`), replacing the removed VentureChat dependency.

### 3.5 Visual job boundary
`/job border` toggles a **client-visible particle wall** (`END_ROD`) around the job zone (`JobBorderManager`), rendered per-player on a repeating task within a 64-block range.
- *Design:* the first implementation used a per-player **world border**, but that is a hard cage — it physically blocks movement, contradicting a "visual indicator only" requirement. Switched to particles, which are purely cosmetic and per-player. The boundary shows for anyone *working* the job (owner/helper/worker), not just workers.

### 3.6 Role features (builder / helper / owner / project-lead)
`/job who` (online-aware roster), a **clickable `[Accept]` invite** button, `/job teleportall` + `/job teleport <player>` (renamed from the awkward "bring*"), a **management panel** (`/job manage`: clickable kick/ban/promote/demote), **ownership `/job transfer`** (target must already be a helper; original creator preserved), `/project announce`, `/project export`, and a **project completion prompt**.

---

## 4. Changed / modernised features

- **Discord messaging → rich embeds with fallback.** Job-start and job-end posts are now embeds (relative timestamps, stat fields) sharing one `sendEmbedWithFallback` helper that **degrades to plain text if the embed send fails** (e.g. missing "Embed Links" permission), logging the reason. Channel resolution was corrected to use the DiscordSRV *game-channel name* resolver rather than a raw snowflake id. Cosmetic job sounds are guarded so a third-party packet listener (PremiumVanish) can't abort the announcement.
- **Discord ping safety → `allowRolePing` whitelist.** Free-text `discordTags` were removed in favour of a configured allow-list of pingable roles; the announcement can no longer be coerced into pinging `@everyone` or arbitrary users.
- **All player-facing output → Adventure `Component`.** Join notices, build-protection messages, `/job`, `/jobchat`, admin commands, and invites were migrated off legacy strings; `/job check` and leaderboard names are **clickable**. A small `Msg` helper standardises buttons/suggestions; `PromptStyle` colour-codes the remaining `/jobadmin` conversation.
- **Identity names → UUIDs (H5).** All player identity migrated to UUIDs for rename-safety and stable membership/ownership.
- **Persistence → `YamlConfiguration` (P1/C3).** Job persistence was rewritten on Bukkit-native YAML (human-readable, robust round-trip), with periodic cleanup made **synchronous** to avoid off-thread state mutation.
- **One active job per player** is now enforced.
- **`/job info` roster rework** distinguishes current **Owner** from the original **Started by** creator (tracked separately), and shows Helpers/Workers/Project.
- **Admin parity:** `/job admin …` one-liners and the `/jobadmin` conversation share a single executor, with confirm-gating on destructive actions (`teleportall`, `clearworkerinven`).
- **Build-protection hot path** optimised and the handler relocated (H1/H2).
- **Lifecycle polish:** players are reverted Creative → Survival on leave/kick/job-end; unattended jobs auto-pause and auto-resume; tab-complete suggests only active jobs.

---

## 5. Removed

- **TeamSpeak integration** — dead since the server moved to Discord years ago.
- **VentureChat integration** — replaced by native `/jobchat`.
- **Per-job kit feature** — unused; removed with its storage fields.
- **External McMeProject coupling** — replaced by the native Projects subsystem.
- **Unused `PluginUtils` dependency** and dead BungeeCord/try-catch scaffolding.

---

## 6. Cross-cutting architecture & design principles

- **Preserve the external protection contract.** `hasBuildPermission(Player, Location) → boolean` and `getBuildProtectionMessage(Player, Location)` are called **reflectively** by sibling plugins (MCME-Architect and PlotBuild), which fail *open* if the signature breaks. These signatures were held stable throughout and are now **pinned by `ProtectionApiContractTest`** so a future refactor can't silently break the integration.
- **Testable seams over end-to-end mocking.** Logic that matters (stats math, radius/name validation, leaderboard sort/limit, project roll-up, geometry) is factored into pure functions and unit-tested; storage classes expose package-private directory overrides so persistence round-trips run against a temp dir. This keeps the MockBukkit surface small and the suite fast.
- **Event-driven pipeline.** `activateJob` firing the start event is the single integration point that fans out to broadcast, Discord, border, and map — letting features (and the whole `/createjob` UI) be added/replaced independently.
- **Graceful degradation for externals.** Discord embed→text fallback, soft-dependency class-load isolation, and non-blocking name lookups all assume the external service may be absent, downgraded, or slow.
- **Snapshot durability.** Active job stats are snapshotted mid-run so a crash doesn't lose in-progress counts.

---

## 7. Testing & CI

- **125 tests** (JUnit 5 + MockBukkit), from a starting point of zero. CI runs `mvn verify` (compile + tests + jar) on every push and PR.
- **MockBukkit pinned to `mockbukkit-v26.1.2`** to match the compiled Paper version (see §1 — a mismatch breaks registry loading).
- **QA-only surface** (documented in `docs/qa/2026-rework-qa-checklist.md`): real block protection, glow, the cross-server broadcast, Discord delivery, and the **`/createjob` Dialog rendering** — none of which MockBukkit can exercise.

---

## 8. Known limitations & follow-ups

- **Single-option read** in the Dialog (`getText(key)` → selected id) is correct by the dialog protocol but is confirmed empirically by the QA project-linkage check; if a job comes back unlinked, the accessor is swapped.
- **Radius slider label format** left at the API default — flag if it renders oddly.
- **Closed-job retention:** `/job info`/tab-complete were trimmed to active jobs, but long-term archive growth is noted as a scaling follow-up.
- **Minor, pre-existing:** helper kick/ban can leave the helper flag set; empty-project CSV export writes a header-only file. Both unrelated to this branch's features.
- **Version string:** `pom.xml` still reads `2.8`; bump to `3.0` at release.
- **`config.yml` cleanup** flagged as a post-merge task.

---

## Appendix — commit map (grouped)

- **Foundation / cleanup:** `eabbf19` (audit seed) · `cde4098` TeamSpeak removal · `b761f7f` VentureChat removal · `3b6b103` PluginUtils drop · `628b473` sync cleanup · `4eddb3d` one-job-per-player · `2374803` protection hot-path.
- **Persistence & identity:** `26b3dbb` YAML persistence · `e5b4ed9` name→UUID · `10e90b6` test suite · `8c8dfdc` CI · `07549da` protection contract test.
- **Adventure migration:** `6fedbad`, `c9d4ebb`, `720ed70`, `447fc18`.
- **Job team chat:** `22fd7c2`.
- **Statistics:** `8453501` → `10fdca8` (model, StatsManager, leaderboard, snapshots, StatsListener, commands, Discord recap, CSV export).
- **Projects:** `cd02e11` → `1848eff` (entity, roll-up, commands, management, native picker, McMeProject removal).
- **Discord:** `3412b19`/`76697fe` (allowRolePing) · `d660d5a` (start embed) · `9b6aa12` (end embed + join button) · `524308a`/`ce56e87`/`0d8b1e9` (fixes + fallback).
- **UX quick wins & round 2:** `26eed8c`…`1db718c` (Q1–Q4) · `e703a37`…`f74c8d2` (R1–R3, J2) · `4469101` creator tracking · kit removal `a890b85`.
- **Visual border:** `11a5187` (world border) → `3d1c63c` (particle wall) → `d0c77fc`/`047e813` (visibility/range).
- **Role features:** `c3440b4` (`/job who`, invite) · `778650b`/`f74c8d2` (teleport, manage, transfer) · `ff82b9b` (project announce/export/completion) · `1376580`/`2f9fe0b` (web-map + isolation fix).
- **26.1.2 + Dialog (this cycle):** `edf3e6c` (migration) · `89aa2bb` (Dialog `/createjob`) · `bd73725` (review fixes).
