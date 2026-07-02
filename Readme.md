# TheGaffer

**A build-protection and "jobs" plugin for the MCME (MC Middle Earth) Paper server.**

The world map is read-only by default — nobody can place or break blocks. To let builders work on a specific area without opening up the whole map, a staff member starts a **job**: a bounded region where invited players may build for the duration of the session. When the job ends, the area is protected again. Staff with a bypass permission can build anywhere.

| | |
|---|---|
| **Version** | 2.8 |
| **Minecraft / API** | Paper 1.19 (`api-version: 1.19`) |
| **Java** | 17 |
| **Build** | Maven → `target/TheGaffer-2.8.jar` |
| **Soft dependencies** | DiscordSRV, MCME-Connect, Dynmap (all optional) |

---

## How protection works

Every block place / break / interact is checked against `getBuildProtection(player, location)`. A player is allowed to build only when **one** of these is true:

1. they have the **`thegaffer.ignoreprotection`** permission (staff build anywhere), or
2. the world is listed under `unprotectedworlds` in the config, or
3. they are a **worker in a running job**, the location is **inside that job's area**, and the job is **not paused**.

Anything else is blocked, with a message explaining why (no active job, wrong world, out of the job's bounds, job paused, etc.).

While a worker stands inside their job's area they are automatically switched to **Creative**, and back to **Survival** when they leave it or the job ends. (Staff with the bypass permission manage their own gamemode.)

---

## Jobs & roles

A job is a named, bounded build session. It has an owner, an area (a centre point + radius), a world, an optional description, and three kinds of participant:

| Role | Who | What they can do |
|---|---|---|
| **Owner** | The staff member who started the job | Full control; can manage members; counts as a builder. Usually has `thegaffer.ignoreprotection`. |
| **Helper** | Staff assisting the owner | Help run and build the job; can manage it; added via `/job admin <job> addhelper`. |
| **Worker** | Players who `/job join` | Build inside the job area while it runs; auto-switched to Creative in-bounds. |

A player can be in **only one job at a time** (enforced). Jobs may be **private** (invite-only) and support **banned** and **invited** lists. Optionally, helpers and workers can be given a coloured **glow** (scoreboard teams) so everyone can see who's on the job.

**Why helpers matter:** if the owner logs off, after a short grace period TheGaffer promotes an online **helper** to keep the job running. If no helper is online, the job is paused and moved to the archive. Workers who stay offline too long are removed automatically. A helper-takeover changes who is listed as **Owner** in `/job info`, but the original starter is recorded separately and shown as "Started by" whenever it differs from the current owner.

**Lifecycle:** a job is created → started (broadcast in-game, and optionally to Discord and across the network) → run → stopped, at which point it moves to the **archive**. Jobs are **persisted to disk** and reload when the server restarts.

---

## Commands

### For everyone (permission: `thegaffer.join`, default **true**)

| Command | Description |
|---|---|
| `/job check` | List the running jobs (click a name to join). |
| `/job join <job>` | Join a running job (or the only one running). |
| `/job leave` | Leave your current job. |
| `/job mine` | Show your current job status: name, role (Owner / Helper / Worker), paused/glow state, and clickable `[warpto]` / `[leave]` shortcuts. |
| `/job info <job>` | Show a job's details: current owner, helpers (by name), worker count, location, and status. If the job was taken over by a helper, also shows the original starter ("Started by"). |
| `/job warpto <job>` | Teleport to a job's warp point. |
| `/job archive [page]` | Browse finished (archived) jobs. |
| `/job stats <job\|player>` | Show a job's recap, or a player's lifetime totals. |
| `/job leaderboard [placed\|broke\|active]` | Top builders (alias: `/job top`). |
| `/job who [job]` | Show the live roster for a job: Owner, Helpers, and Workers, each coloured **green** (online) or **grey** (offline). Omit `[job]` to see your current job's roster. |
| `/job border` | Toggle the particle outline marking your current job's build-area perimeter (purely visual, fly-through — no movement effect). |
| `/jobchat [message]` | Toggle job-only chat, or send a one-off message to your job (alias: `/jc`). |

### For staff (permission: `thegaffer.create`, default **op**)

| Command | Description |
|---|---|
| `/createjob` (or `/job create` / `/job start`) | Launch the guided job-creation conversation. |
| `/job stop <job>` | End a running job (moves it to the archive). |
| `/job pause <job>` / `/job unpause <job>` | Temporarily suspend / resume building in a job. |
| `/job listen` | Toggle alerts when someone tries to edit the map outside a job. |
| `/job prep` | Stash your inventory (and restore it) while setting up. |
| `/job stats export` | Export all recorded stats to a CSV file. |
| `/jobadmin` (or `/job admin <job> <action> …`) | Manage a job — see below. |

### `/job manage [job]` (staff)

Print a clickable roster for a job (defaults to your current job). Each **worker** gets `[Kick]`, `[Ban]`, and `[Promote]` buttons; each **helper** gets `[Kick]`, `[Ban]`, and `[Demote]` buttons. The owner is shown with a `[Transfer…]` hint. Buttons fire the matching `/job admin` one-liners instantly.

### `/job transfer <player>` (owner or `thegaffer.project.admin`)

Transfer ownership of your current job to another player. The target **must already be a helper** (promote them first if needed). On transfer: the target becomes Owner; you become a Helper; the original "Started by" creator record is unchanged so `/job info` still shows who started the job.

### `/job admin <job> <action>` subcommands

`addhelper <player>`, `removehelper <player>`, `kickworker <player>`, `banworker <player>`, `unbanworker <player>`, `inviteworker <player>`, `uninviteworker <player>`, `promote <player>`, `demote <player>`, `setwarp`, `setradius <n>`, `clearworkerinven`, `teleportall`, `teleport <player>`, `listworkers`.

- **`promote <player>`** — promotes a worker to helper status. The player stays in the workers list, so they keep their build rights.
- **`demote <player>`** — demotes a helper back to a standard worker. The player remains in the workers list so build rights are preserved.

---

## Permissions

| Node | Default | Grants |
|---|---|---|
| `thegaffer.join` | `true` | Join and use jobs; view stats/leaderboard. |
| `thegaffer.create` | `op` | Create, manage, stop jobs; export stats; admin commands. |
| `thegaffer.ignoreprotection` | `op` | Bypass build protection — build anywhere. |
| `thegaffer.project.create` | `op` | Create and lead projects. |
| `thegaffer.project.admin` | `op` | Manage any project (head-builder bypass). |

---

## Configuration (`config.yml`)

| Key | Purpose |
|---|---|
| `general.debug` | Verbose debug logging. |
| `jobDescription` | Prompt for a job description during creation. |
| `discord.channel` | DiscordSRV channel name for job announcements (omit to disable). |
| `discord.emoji` | Emoji prefix for Discord messages. |
| `allowRolePing` | Discord roles pinged when a job is announced (e.g. `Jobber`). Only these roles are pinged — never `@everyone` or individual players. Empty/omit = announce with no ping. |
| `glowing.enabled` / `glowing.helperColor` / `glowing.workerColor` | Team-glow toggle and colours. |
| `showJobBorder` | Show players a particle outline (`END_ROD`, white glow) tracing the job's build-area perimeter while they're in a job. Purely visual — no movement effect, players can cross freely. Toggle per-player with `/job border`. |
| `unprotectedworlds` | Worlds where the map protection does not apply. |
| `externalProtectionHandlers` | Allow/deny hooks for integrating other protection plugins. |

---

## Integrations

All are **soft dependencies** — TheGaffer runs fine without any of them; the relevant feature simply no-ops if the plugin is absent.

- **DiscordSRV** — posts a **rich embed** announcement (with relative timestamps that localize to each viewer) to a Discord channel when a job **starts** (pinging the roles in `allowRolePing` — e.g. a `Jobber` opt-in role — never `@everyone`), and a **rich embed recap** when it ends (duration, blocks placed/broken, builder count — inline fields; muted red colour). Both embeds fall back to plain-text automatically if the bot lacks the "Embed Links" permission in the channel. Controlled per-job by the "send to Discord" flag and globally by `discord.channel`.
- **MCME-Connect** — broadcasts job-start announcements **across the BungeeCord network**, so players on other servers see that a job has started. Falls back to a local broadcast when not present.
- **Dynmap → LiveAtlas** — when [Dynmap](https://github.com/webbukkit/dynmap) is installed (LiveAtlas is just its web frontend — it renders the same marker layer), active jobs are automatically drawn as coloured **area markers** on the live web map. Each marker shows the job's square build area (MinX/MaxX/MinZ/MaxZ corners), its project colour (hashed from the project name — unattached jobs use a neutral grey), and a clickable HTML popup with the job name, owner, project, radius, and a `/job join <name>` hint. Markers are added on job start, removed on job end, and updated whenever the radius or warp changes (`/job admin setradius` / `setwarp`). The marker layer is named `thegaffer.jobs` (visible in the LiveAtlas/Dynmap layer selector as "Jobs"). If Dynmap is absent or its MarkerAPI is unavailable, everything no-ops with a single informational log line.

---

## Statistics

TheGaffer records what happens during each job and exposes it four ways.

- **What's tracked:** per job — owner, project, world, location, duration, the set of participants, and per-builder **blocks placed / broken**. Only *successful, in-job, in-bounds* actions are counted (staff building outside a job's area are not credited).
- **`/job stats <job>`** — a recap of a finished (or running) job. **`/job stats <player>`** — a player's lifetime totals.
- **`/job leaderboard [placed|broke|active]`** — cross-job rankings, with clickable names.
- **Discord recap** — appended to the job-end Discord post.
- **`/job stats export`** — writes every record to `plugins/TheGaffer/stats/export-<timestamp>.csv` (UTF-8) for spreadsheets or dashboards.

Live counts survive a restart (they ride the same periodic save as jobs), so stats aren't lost if the server cycles mid-job.

---

## Projects

A **Project** (e.g. "Minas Tirith") is a named, managed collection of jobs, run with `/project` (alias `/pj`). Stats from every job in a project roll up to the project level, so you can see total blocks, builders, and build time across an entire effort.

- **What it holds:** a description, a goal, a **lead** + optional **managers**, and a lifecycle status (active / completed / archived). Stored as plain YAML under `plugins/TheGaffer/projects/<name>.yml`.
- **Membership** is by name: a job belongs to a project when it's created under it (picked at `/createjob`) or attached with `/project attach`. Project names match case-insensitively, so "Minas Tirith" and "minas tirith" are the same project.
- **Ownership is enforced:** only a project's lead/managers may edit it, change its status, or attach jobs — except a holder of `thegaffer.project.admin` (the head-builder bypass), who may manage any project.

| Command | Who | Description |
|---|---|---|
| `/project list [active\|completed\|archived]` | everyone | List projects (click a name for details). |
| `/project info <name>` | everyone | Description, goal, lead, managers, status, and rolled-up stats. |
| `/project create <name>` | `thegaffer.project.create` | Create a project; you become its lead. |
| `/project setdescription\|setgoal <name> <text>` | lead/manager | Edit details. |
| `/project setlead <name> <player>` | lead / admin | Reassign the lead. |
| `/project addmanager\|removemanager <name> <player>` | lead/manager | Manage the manager list. |
| `/project complete\|archive\|reopen <name>` | lead/manager | Change lifecycle status. |
| `/project attach <name> <job>` / `/project detach <job>` | lead/manager | Link / unlink a job. |
| `/project delete <name>` | lead / admin | Remove the project record (job & stats history keep the name). |
| `/project announce <name> <message>` | lead/manager | Send a prefixed message to every online member of every active job in this project (deduped). |
| `/project export <name>` | lead/manager | Export stats for all jobs in this project to `stats/export-<name>-<timestamp>.csv`. |

At `/createjob`, if any active projects exist you'll be asked which one this job belongs to (or `nothing`).

---

## Data & storage

No database — everything is plain YAML under the plugin folder, so it's human-readable and dependency-free.

```
plugins/TheGaffer/
├── config.yml
├── jobs/
│   └── <job>.yml                  # one file per active/archived job
└── stats/
    ├── <job>-<endMillis>.yml      # one record per finished job
    ├── active/<job>-0.yml         # in-progress counters (durable across restarts)
    └── export-<timestamp>.csv     # produced by /job stats export
```

Players are identified by **UUID** throughout (so a rename can't dodge a ban or lose job membership); names are resolved for display only.

---

## Building & testing

Requires JDK 17+ and Maven.

```bash
mvn package        # compile, run tests, build target/TheGaffer-2.8.jar
mvn test           # run the unit-test suite only
```

Tests use **JUnit 5 + MockBukkit** (a mock Paper server) — no real server is needed for the suite. CI runs `mvn verify` on every push and pull request (`.github/workflows/build.yml`).

> **Note:** MockBukkit cannot fully boot a Paper 1.19 plugin or reach Discord, so a handful of runtime behaviours (real block protection, glow, the cross-server broadcast, Discord delivery) are verified by in-game QA rather than the automated suite.

---

## What's new in the 2026 rework

This branch is a substantial overhaul focused on stability, performance, security, and UX:

- **Persistence restored** — jobs survive restarts again, rebuilt on safe YAML (replacing a vulnerable library that had been removed in 2020, which left jobs ephemeral).
- **UUID identity** — members, owners, and bans are keyed by UUID instead of name, closing a ban-evasion-by-rename hole and removing blocking name lookups.
- **Statistics** — the full feature described above (new).
- **Adventure UI** — chat output migrated to Adventure components with **clickable** actions (e.g. `/job check` entries join with one click).
- **Visual job boundary** — workers see a per-player **particle outline** (`END_ROD`) tracing the active job's build-area perimeter. Purely visual — fly-through, zero movement effect. Toggle with `/job border`.
- **Native `/jobchat`** — job-team chat without an external chat plugin.
- **Performance** — the build-protection hot path and the player-move handler were optimised.
- **Security & cleanup** — dead TeamSpeak code and a hard-coded password removed; the build is dependency-clean and reproducible from public repositories.
- **Tests & CI** — the project's first automated test suite (MockBukkit) plus GitHub Actions.
- **Projects** — jobs can be grouped into managed, owned **projects** with rolled-up stats; this replaces the old (defunct) McMeProject integration with a native, self-contained system.

---

## Credits

Authors: meggawatts, DonoA, Eriol_Eandur, Planetology, Fraspace5, Jubo, q220.

Licensed under the **GNU General Public License v3** — see the source-file headers.
