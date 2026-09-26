# Changelog

All notable changes to TheGaffer. Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [3.0.0] — unreleased

The first release since 2023. TheGaffer now runs on Paper 26.2 and Velocity 4, keeps its jobs across
restarts, counts what gets built, groups jobs into projects, and ships a proxy plugin so a builder can
join a job from any server on the network.

**Two jars now.** `TheGaffer-3.0.0.jar` goes on every backend, `thegaffer-velocity-3.0.0.jar` on the
proxy. See *Upgrading* at the bottom — this release needs more than dropping a jar in.

### Added

**Jobs survive a restart.** Persistence was deliberately switched off in February 2020, when the
vulnerable Jackson 1.x it relied on was removed and the load/save calls were commented out rather than
rewritten. Jobs were ephemeral for six years. They are now stored one YAML file per job, written
atomically, flushed every 60 seconds and synchronously on shutdown — kits included, as native
`ItemStack`s.

**Statistics.**
- Blocks placed and broken per builder per job, counted only in-job and in-bounds
- Active build time, accumulated by idle-gap so standing still does not count
- `/job stats <job|player>`, `/job leaderboard` with `placed` / `broke` / `active` / `time`
- Job-end recap posted to Discord
- CSV export, and a `leaderboard.json` feed via `/job stats export json`
- Live snapshots written to `stats/active/`, so a crash does not lose an in-progress job's counters

**Projects** — a group of jobs that belong to the same build, with their statistics rolled together.
`/project create|list|info|attach|detach|setdescription|setgoal|setlead|addmanager|removemanager|complete|archive|reopen|delete|announce|export`,
aliased `/pj`. Lead-and-manager ownership with a `thegaffer.project.admin` bypass. This replaces the
dead coupling to the McMeProject plugin, which had been stale since 2020.

**A proxy plugin** (`thegaffer-velocity`, originally by Drayz, ported and extended):
- `/job check` lists jobs running **anywhere on the network**, each with a join button
- `/job join` and `/job join <name>` move you to the job's server and join you there
- A network-wide announcement when a job starts, with a button that transfers you

**`/job border`** — a client-side outline of the build area, toggleable per player.

**Member glow** — workers and helpers glow in configurable colours, set per job at creation.

**`/createjob` is a Dialog**, replacing the old chat conversation. Duplicate names are auto-numbered
(`Gate` → `Gate_2`) instead of rejected.

**Smaller additions**
- `/job mine` — which job you are in, your role, whether it is paused
- `/job who <job>` — the roster, with a clickable `[Accept]` for invites
- `/job transfer <helper>` — hand ownership over
- `/job listen` — warnings when someone tries to edit the map outside a job
- `/job admin <job> <action>` mirrors every `/jobadmin` action as a one-liner
- Tab-completion for job names and actions throughout
- `recordExternalBuild` API, so another plugin can attribute blocks to a job

### Changed

- **Identity is now UUID-based**, not name-based. Bans survive a rename. No data migration was needed
  because the name-keyed format never shipped. The only name-keyed thing left is the scoreboard glow
  team, where Bukkit's API gives no choice.
- **Built for Paper 26.2** (`26.2.build.126-stable`) and **Velocity 4**. Building now requires **JDK 25**:
  paper-api 26.2 ships Java 25 class files and an older `javac` cannot read them. Output still targets
  Java 17 (Paper) and 21 (Velocity).
- **Split into three Maven modules** — `core`, `paper`, `velocity` — with the shared wire protocol in
  `core`, shaded into both jars.
- **All player-facing text migrated to Adventure**, with clickable buttons throughout. The few remaining
  `ChatColor` uses are String-API corners: scoreboard teams, `ChatPaginator`, and the cross-server relay.
- **Job announcements no longer go out twice.** The backend announces to its own players; the proxy
  announces to everyone else. Controlled by the new `proxyAnnouncesJobs` config key.
- **Discord announcements are rich embeds** with relative timestamps and a prominent join button, and
  fall back to plain text if the embed fails. Pings are restricted to roles listed in `allowRolePing` —
  free-text `discordTags` is gone, so `@everyone` can no longer be pinged from a job.
- **`/job leaderboard` empty state now tells the truth**: it said "No stats recorded yet" when it meant
  no *finished* jobs, while a job might have been running for hours.
- **`/project list` empty state** said "No projects yet" when it filters to active by default and there
  might be a dozen completed ones. It now names the filter and points at the others.
- **"No jobs currently running"** is now "No jobs are running on this server" — on a network the proxy
  will happily move you to one elsewhere.
- **Project names may contain underscores.** Job names are full of them, since spaces in a job name
  become underscores, so a project could not share the name of its own job.
- Job-start announcements no longer tell you to travel to another world first; the proxy moves you.
- Dead TeamSpeak integration removed. VentureChat and an unused MCME-Connect compile dependency dropped,
  along with a hardcoded local jar path that made the build unreproducible.
- `/jobchat` (`/jc`) is native, and a player can be in only one job at a time.

### Fixed

**Every high and medium finding from the June 2026 audit**, including the empty `JobProtection`
handlers, the build-protection hot path, and the protection handler's placement.

- **Optional integrations could take the whole listener down with them.** `JobEventListener` named
  DiscordSRV types directly, so on a server without DiscordSRV the class failed to register and *every*
  handler in it died — silently killing the Discord announcements and the `/job listen` warnings. The
  failure is a `NoClassDefFoundError`, an `Error`, so `catch (Exception)` never saw it. Optional types
  now live in nested classes loaded only after an `isPluginEnabled` check.
- **Attaching a job to a project moved no statistics.** `/project attach` changed the job's label while
  every project total aggregates from the project stored in the job's stats record — which was set when
  the job began and never written again. Blocks, builders and build time stayed behind, for finished and
  running jobs alike.
- **The proxy lost every job on restart.** It learned about a job only from the message sent when that
  job started, and kept the list in memory, so restarting the proxy — or restarting a backend holding a
  persisted running job — left `/job check` blank while the job ran perfectly well. Backends now restate
  their running jobs when a player joins.
- **`/job join <name>` ignored other servers.** Typing a job's name sent the command to whichever backend
  you stood on, which answered "no jobs running" whenever the job was elsewhere.
- `unbanWorker` had an inverted check, so unbanning did the opposite.
- `clearworkerinvens` was misspelled, so `/job admin` never dispatched it.
- The job boundary showed only to workers, not owners and helpers, and only rendered within 32 blocks —
  less than a typical job's radius, so it was invisible from the centre.
- Job cleanup ran asynchronously against non-thread-safe state.
- Auto-paused jobs were left in the owner-timeout queue.
- The Discord job-start embed resolved its channel by snowflake ID rather than DiscordSRV name, and the
  job-end embed was posted with no content.
- Cosmetic job sounds could abort the whole announcement when a third-party packet listener threw.
- The `/jobadmin` dispatch map was populated in the constructor rather than at class-init.
- `setradius` tab-completed player names for a numeric argument.
- CSV export was not UTF-8 and did not escape newlines.
- Player-name lookups blocked the main thread.
- Jobs already running at startup without a snapshot began with uninitialised counters.
- A job's glow setting was lost when the project step was skipped during creation.

### Deprecated

- The `servlet:` block in `config.yml` is inert — nothing reads it. Removed from the shipped config.

### Upgrading

1. **Two jars.** `TheGaffer-3.0.0.jar` on every backend, `thegaffer-velocity-3.0.0.jar` on the proxy.
   They share code and the protocol between them is not versioned, so deploy them together.
2. **Delete the old jar.** The filename carries the version, so `TheGaffer-2.8.jar` left in place means
   the server loads both. Repoint the symlink rather than dropping the new jar alongside the old.
3. **Set `proxyAnnouncesJobs: true`** in each backend's `config.yml`. Left at its default of `false`
   with the proxy plugin installed, every job is announced twice. It defaults to `false` so that
   upgrading *without* the proxy plugin cannot silently leave remote players hearing nothing.
4. **Grant `thegaffer.join` on the proxy.** A bare Velocity proxy grants players nothing, so without it
   `/job check` and cross-server `/job join` refuse with "You don't have permission".
5. No data migration is required. Existing job and stats files are read as-is.

### Not verified before release

Honest gaps, all requiring production or a real client:

- The Discord embed and job-end recap, which need a real bot token
- A ban surviving a **rename** — untestable offline, because offline-mode UUIDs derive from the name
- `proxyAnnouncesJobs` suppressing the MCME-Connect relay, which the test rig has no MCME-Connect to exercise

### Known issues

- Tab-completion for `/project attach <project> <job>` assumes a single-word project name.

---

## [2.6.2] — 2019-10-28

The last tagged release. `master` carried on to 2023-07-27 without a tag, and the pom read 2.8 from
then until this release. See the git history for earlier versions.
