# TheGaffer

**A build-protection and "jobs" plugin for the MCME (MC Middle Earth) Paper server.**

The world map is read-only by default — nobody can place or break blocks. To let builders work on a specific area without opening up the whole map, a staff member starts a **job**: a bounded region where invited players may build for the duration of the session. When the job ends, the area is protected again. Staff with a bypass permission can build anywhere.

| | |
|---|---|
| **Version** | 2.8 |
| **Minecraft / API** | Paper 1.19 (`api-version: 1.19`) |
| **Java** | 17 |
| **Build** | Maven → `target/TheGaffer-2.8.jar` |
| **Soft dependencies** | DiscordSRV, MCME-Connect (both optional) |

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

A job is a named, bounded build session. It has an owner, an area (a centre point + radius), a world, an optional description/kit, and three kinds of participant:

| Role | Who | What they can do |
|---|---|---|
| **Owner** | The staff member who started the job | Full control; can manage members; counts as a builder. Usually has `thegaffer.ignoreprotection`. |
| **Helper** | Staff assisting the owner | Help run and build the job; can manage it; added via `/job admin <job> addhelper`. |
| **Worker** | Players who `/job join` | Build inside the job area while it runs; auto-switched to Creative in-bounds. |

A player can be in **only one job at a time** (enforced). Jobs may be **private** (invite-only) and support **banned** and **invited** lists. Optionally, helpers and workers can be given a coloured **glow** (scoreboard teams) so everyone can see who's on the job.

**Why helpers matter:** if the owner logs off, after a short grace period TheGaffer promotes an online **helper** to keep the job running. If no helper is online, the job is paused and moved to the archive. Workers who stay offline too long are removed automatically.

**Lifecycle:** a job is created → started (broadcast in-game, and optionally to Discord and across the network) → run → stopped, at which point it moves to the **archive**. Jobs are **persisted to disk** and reload when the server restarts.

---

## Commands

### For everyone (permission: `thegaffer.join`, default **true**)

| Command | Description |
|---|---|
| `/job check` | List the running jobs (click a name to join). |
| `/job join <job>` | Join a running job (or the only one running). |
| `/job leave` | Leave your current job. |
| `/job info <job>` | Show a job's details (owner, location, status). |
| `/job warpto <job>` | Teleport to a job's warp point. |
| `/job archive [page]` | Browse finished (archived) jobs. |
| `/job stats <job\|player>` | Show a job's recap, or a player's lifetime totals. |
| `/job leaderboard [placed\|broke\|active]` | Top builders (alias: `/job top`). |
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

### `/job admin <job> <action>` subcommands

`addhelper <player>`, `removehelper <player>`, `kickworker <player>`, `banworker <player>`, `unbanworker <player>`, `inviteworker <player>`, `uninviteworker <player>`, `setwarp`, `setradius <n>`, `setkit`, `clearworkerinven`, `bringall`, `listworkers`.

---

## Permissions

| Node | Default | Grants |
|---|---|---|
| `thegaffer.join` | `true` | Join and use jobs; view stats/leaderboard. |
| `thegaffer.create` | `op` | Create, manage, stop jobs; export stats; admin commands. |
| `thegaffer.ignoreprotection` | `op` | Bypass build protection — build anywhere. |

---

## Configuration (`config.yml`)

| Key | Purpose |
|---|---|
| `general.debug` | Verbose debug logging. |
| `jobDescription` | Prompt for a job description during creation. |
| `jobKits` | Enable per-job kits (handed to workers on join). |
| `discord.channel` | DiscordSRV channel name for job announcements (omit to disable). |
| `discord.emoji` | Emoji prefix for Discord messages. |
| `glowing.enabled` / `glowing.helperColor` / `glowing.workerColor` | Team-glow toggle and colours. |
| `unprotectedworlds` | Worlds where the map protection does not apply. |
| `externalProtectionHandlers` | Allow/deny hooks for integrating other protection plugins. |

---

## Integrations

Both are **soft dependencies** — TheGaffer runs fine without them; the relevant feature simply no-ops if the plugin is absent.

- **DiscordSRV** — posts a call-to-action to a Discord channel when a job **starts** (with optional role pings), and a **recap** when it ends (builders, blocks placed/broken, duration). Controlled per-job by the "send to Discord" flag and globally by `discord.channel`.
- **MCME-Connect** — broadcasts job-start announcements **across the BungeeCord network**, so players on other servers see that a job has started. Falls back to a local broadcast when not present.

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
- **Native `/jobchat`** — job-team chat without an external chat plugin.
- **Performance** — the build-protection hot path and the player-move handler were optimised.
- **Security & cleanup** — dead TeamSpeak code and a hard-coded password removed; the build is dependency-clean and reproducible from public repositories.
- **Tests & CI** — the project's first automated test suite (MockBukkit) plus GitHub Actions.

---

## Credits

Authors: meggawatts, DonoA, Eriol_Eandur, Planetology, Fraspace5, Jubo, q220.

Licensed under the **GNU General Public License v3** — see the source-file headers.
