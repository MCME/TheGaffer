# TheGaffer

**Build protection and build *jobs* for the MC Middle Earth network.**

The map is read-only by default — nobody can place or break a block. When work needs doing, a staff
member starts a **job**: a bounded area where invited builders may build for the length of the
session. When the job ends, the area locks again.

TheGaffer is what turns "the map is protected" into "…except right here, right now, for these people".

| | |
|---|---|
| **Version** | 3.0.0 |
| **Runs on** | Paper **26.2** backends + a **Velocity 4** proxy |
| **Jars** | `TheGaffer-3.0.0.jar` (every backend) · `thegaffer-velocity-3.0.0.jar` (the proxy) |
| **Optional hooks** | DiscordSRV · MCME-Connect · Dynmap — all absent-safe |

This page has three parts. Jump to the one that is you:

- **[For builders](#for-builders)** — you want to join a job and build
- **[For job leaders](#for-job-leaders)** — you run jobs and look after the people in them
- **[For developers](#for-developers)** — you build, deploy or change the plugin

---

## For builders

### Getting into a job

When a job starts you get an announcement with a button. Click it and you are in — including when
the job is on a different server, in which case you are moved there first.

If you missed the announcement:

| Command | What it does |
|---|---|
| `/job check` | Lists every job running **anywhere on the network**, each with a join button |
| `/job join` | Joins the job, if exactly one is running |
| `/job join <job>` | Joins that job by name, moving you to its server if it is elsewhere |
| `/job leave` | Leaves the job you are in |
| `/job mine` | Which job you are in, your role, and whether it is paused |

Job names are **case-sensitive**: `/job join Minas_Tirith` works, `minas_tirith` does not.

If you leave the job's server for more than five minutes, you are taken off the job. Join it again
when you are back.

### While you are building

Inside the job's area you are switched to **Creative** automatically, and back to **Survival** when
you step outside it or the job ends. That is the plugin doing it — do not fight it.

| Command | What it does |
|---|---|
| `/job border` | Toggles a visible outline of the build area |
| `/job warpto <job>` | Teleports you to the job's warp point |
| `/job info <job>` | Shows the job's details and who is in it |
| `/job who <job>` | Lists the people in a job |
| `/jobchat` or `/jc` | Toggles job chat. `/jc <message>` sends one message without toggling |

Job chat only reaches people in your job, and you only see theirs.

### Statistics

Blocks you place and break inside a job are counted, along with your active build time.

| Command | What it does |
|---|---|
| `/job stats <player>` | That player's totals across all jobs |
| `/job stats <job>` | A job's totals, broken down by builder |
| `/job leaderboard` | Top builders by blocks placed |
| `/job leaderboard broke` / `active` / `time` | Rank by blocks broken, jobs joined, or build time |

The leaderboard counts a job **once it has finished**, so a job running right now contributes
nothing to it yet however long you have been at it.

### If you cannot build

You will be told why: no job, wrong world, outside the job's bounds, or the job is paused. Being
*in* a job is not enough — you have to be inside its area, and it must not be paused.

---

## For job leaders

Everything here needs `thegaffer.create` (staff).

### Starting a job

`/createjob` opens a form. `/job create` and `/job start` do the same thing.

It asks for a name, an area radius, and optionally a description, a starting kit, whether the job is
private, whether members glow, whether it is announced to Discord, and which project it belongs to.

Duplicate names are **auto-numbered** rather than rejected — start a second `Gate` and you get
`Gate_2`. Spaces in a name become underscores.

### Running it

| Command | What it does |
|---|---|
| `/job stop <job>` | Ends the job; the area locks again and the stats record is written |
| `/job pause <job>` / `/job unpause <job>` | Suspends building without ending the job |
| `/job manage <job>` | Opens the management panel — roles, kicks, bans, the lot |
| `/job transfer <helper>` | Hands ownership to a helper. They must already *be* a helper |
| `/job teleportall` | Summons every online worker to you |
| `/job listen` | Toggles warnings when someone tries to edit the map outside a job |
| `/job archive` | Lists archived jobs |
| `/job prep` | Stores your inventory, and restores it when run again |

### Roles

| Role | Who | What they get |
|---|---|---|
| **Owner** | Whoever started the job | Full control. Cannot `/job leave` — hand over with `/job transfer` or stop the job |
| **Helper** | Staff assisting | Can manage the job and its members |
| **Worker** | Anyone who joined | Builds inside the area while the job runs |

Workers and helpers can be made to **glow** in different colours so you can see who is who — set per
job at creation, configured server-wide under `glowing` in the config.

**If the owner goes offline**, a helper who is online takes the job over after a few minutes, and the
old owner stays on as a helper. With no helper online the job pauses instead, and resumes when the
owner or a helper comes back. Helpers stay in the job while they are away; workers are taken off
after five minutes.

### Managing people

`/jobadmin` walks you through it. The one-liner form is `/job admin <job> <action>`:

`kickworker` · `banworker` · `unbanworker` · `inviteworker` · `uninviteworker` · `promote` ·
`demote` · `addhelper` · `listworkers` · `setwarp` · `setradius` · `clearworkerinven` ·
`teleport` · `teleportall`

**Bans are by UUID**, so they survive a rename.

Kicking or banning someone takes every role they hold, so a helper stops being a helper too. The
owner can be neither kicked nor banned — hand the job over with `/job transfer` first — and a banned
player cannot be made a helper until they are unbanned.

`setwarp` moves the whole job to where you stand: its area, its map marker and its statistics, even
into another world.

### Projects

A project groups jobs that belong to the same build — every job on Minas Tirith, say — and rolls
their statistics together.

| Command | What it does |
|---|---|
| `/project create <name>` | Creates it; you become its lead |
| `/project list [active\|completed\|archived]` | Lists projects, **active** by default |
| `/project info <name>` | Totals, builders, and the jobs in it |
| `/project attach <name> <job>` | Adds a job **and its statistics** to the project |
| `/project detach <job>` | Removes a job from its project |
| `/project setdescription` / `setgoal` / `setlead` | Edits it |
| `/project addmanager` / `removemanager` | Who else may manage it |
| `/project complete` / `archive` / `reopen` | Status |
| `/project export` | CSV of the project's statistics |
| `/project announce` | Posts the project to Discord |

`/pj` is short for `/project`. Project names may contain letters, digits, spaces, underscores,
hyphens and apostrophes.

Attaching a job moves its existing blocks, builders and build time into the project — for a job
that has already finished as well as one still running.

---

## For developers

### Layout

Three Maven modules, two deployable jars:

| Module | Artifact | Goes on | Java |
|---|---|---|---|
| `thegaffer-core` | `thegaffer-core` | *(shaded into both)* | 17 |
| `thegaffer-paper` | **`TheGaffer-3.0.0.jar`** | every Paper backend | 17 |
| `thegaffer-velocity` | **`thegaffer-velocity-3.0.0.jar`** | the Velocity proxy | 21 |

`thegaffer-core` holds only the wire protocol the two sides share — `Channels`, `Subchannel` and the
message records. **Both jars shade it**, because Paper and Velocity each load exactly one jar and
know nothing about the reactor. Drop the shade and the plugin enables cleanly and then throws
`NoClassDefFoundError` the first time a job starts.

### Building

```bash
export JAVA_HOME="/path/to/jdk-25"
rm -rf target */target && mvn -B package
```

**JDK 25 is required to build**, even though the output targets 17 and 21: paper-api 26.2 ships
Java 25 class files, and an older `javac` cannot read them — it fails with `cannot access
org.bukkit.*` on every import, which looks like a broken classpath and is not. The poms use
`-source`/`-target` rather than `--release` precisely so a newer compiler can read the newer API jar
while still emitting older bytecode.

`mvn clean` is avoided above because it fails inside a OneDrive-synced checkout — `maven-clean-plugin`
cannot delete `target/maven-status/**` while the sync filter holds it, and the reactor then aborts
at the clean phase, leaving a *stale* jar for anything that looks afterwards.

### Dependencies

| | |
|---|---|
| paper-api | `26.2.build.126-stable` |
| velocity-api | `4.2.1-SNAPSHOT` (Velocity 4 — use `com.google.inject.Inject`, not `javax.inject`) |
| MockBukkit | `4.116.1`, which must match paper-api or every `mock()` throws |
| DiscordSRV | `1.26.0`, `provided` |

### How protection works

Every place / break / interact goes through `getBuildProtection(player, location)`. A player may
build when **one** of these holds:

1. they have `thegaffer.ignoreprotection`, or
2. the world is listed under `unprotectedworlds`, or
3. they are a member of a **running, unpaused** job and are **inside its area** — in the job's
   world and within its bounds.

Everything else is refused with a reason.

### Optional integrations, and the trap

DiscordSRV, MCME-Connect and Dynmap are all soft dependencies and all absent-safe — but note *how*:

> Bukkit registers event listeners **per class**. Naming an optional plugin's type anywhere in a
> listener class kills **every** handler in it via `NoClassDefFoundError` when that plugin is
> missing — and that is an `Error`, so `catch (Exception)` does not see it.

So `JobDiscordAnnouncer` and `JobMapIntegration` keep every optional type inside a nested class that
is only loaded after an `isPluginEnabled` check, and the boundary catches `Throwable`. Follow that
pattern for any new integration. This is not hypothetical: it shipped once, and it silently killed
the Discord announcements and the `/job listen` warnings on every server without DiscordSRV.

### The proxy side

The proxy keeps an in-memory registry of which job is on which server, fed over the plugin-message
channel `mcme:gaffer`:

| Subchannel | Sent when | Proxy does |
|---|---|---|
| `JOB_CREATED` | a job starts | registers it **and announces it to the rest of the network** |
| `JOB_DELETED` | a job ends | removes it |
| `JOB_SYNC` | a player joins a backend | registers it **silently** |

`JOB_SYNC` exists because a plugin message needs a player connection, so a backend cannot say
anything at boot. Without it, restarting the proxy loses every job, and restarting a backend that
holds a persisted running job never re-announces it — in both cases `/job check` goes blank while
the job runs perfectly well. Replaying `JOB_CREATED` instead would re-announce every old job to the
whole network on every login, which is why the silent subchannel is separate.

`JoinRouting` decides whether the proxy intercepts a typed `/job join <name>`. It is a pure static
function with no Velocity types, so all of it is unit-tested, and it steps in **only** when the named
job is on another backend — a local job, an unknown name, a case mismatch or a name two backends
share all fall through so the backend keeps its own lookup, permissions, bans and messages.

### Configuration

`plugins/TheGaffer/config.yml`:

| Key | Default | Meaning |
|---|---|---|
| `general.debug` | `false` | Verbose logging |
| `unprotectedworlds` | `[plotworld]` | Worlds with no build protection |
| `jobDescription` | `true` | Ask for a description when creating a job |
| `proxyAnnouncesJobs` | `false` | **Set `true` when the proxy runs `thegaffer-velocity`** — see below |
| `showJobBorder` | `true` | Allow the client-side build-area outline |
| `enableFlight` | `true` | Restore flight when returning a player to Survival |
| `glowing.enabled` / `workerColor` / `helperColor` | `true` / `DARK_PURPLE` / `AQUA` | Member glow |
| `discord.channel` | `job-general` | DiscordSRV channel for job posts |
| `discord.emoji` | `ringmcme` | Emoji decorating Discord posts; remove for none |
| `allowRolePing` | `[Jobber]` | The **only** roles a job announcement may ping |
| `stats.activeIdleThresholdSeconds` | `60` | Gap above which build time stops accumulating |
| `externalProtectionHandlers` | *(commented)* | Let another plugin allow or deny building — see the config comments |

**`proxyAnnouncesJobs` matters.** A backend relays its own job announcement network-wide through
MCME-Connect. The proxy plugin also announces, with a button that transfers the player. Run both and
every player sees the same job announced twice. Setting this `true` makes the backend announce only
to its own players and leaves the rest of the network to the proxy. It defaults to `false` so that
upgrading *without* the proxy plugin cannot silently leave remote players hearing nothing at all.

### Permissions

| Node | Default | Grants |
|---|---|---|
| `thegaffer.join` | everyone | Join and use jobs |
| `thegaffer.create` | op | Create and run jobs |
| `thegaffer.ignoreprotection` | op | Build anywhere |
| `thegaffer.project.create` | op | Create and lead projects |
| `thegaffer.project.admin` | op | Manage any project |

On the **proxy**, `thegaffer.join` must be granted by your permissions plugin. A bare Velocity proxy
grants players nothing, so without it `/job check` and cross-server `/job join` answer
"You don't have permission" before any routing happens.

### Storage

```
plugins/TheGaffer/
  jobs/<name>.yml            one file per job, written atomically
  projects/<name>.yml
  stats/<job>-<endTime>.yml  finished job records
  stats/active/<job>-0.yml   live snapshots, recovered after a crash
```

Jobs are held in memory with a dirty flag, flushed every 60s asynchronously and synchronously on
disable. Identity is by **UUID** throughout; the only name-keyed thing left is the scoreboard glow
team, because Bukkit's API is name-based there.

### Deploying

Both jars must go out together — they share `thegaffer-core`, and the wire protocol between them is
not versioned. The filenames carry the version, so an upgrade leaves the old jar behind unless it is
removed: delete `TheGaffer-<old>.jar` and repoint the symlink rather than dropping the new one
alongside it, or the server loads both.

### Tests

```bash
mvn -B test        # 197 tests
```

MockBukkit cannot boot a Paper plugin of this vintage, reach Discord or render a Dialog, and it does
not deserialize `ItemStack`s from YAML — so kit round-trips are asserted at the serialize step only,
and anything involving a real client is verified in game instead. In-game QA records are kept
internally by MCME staff rather than in this repository.

---

## History

TheGaffer is old. Persistence was **deliberately disabled in February 2020** (commit `242c5b1`),
when the vulnerable codehaus Jackson 1.x it depended on was removed and the load/save calls were
commented out rather than rewritten — so jobs were ephemeral from then until the 2026 rework. The
statistics, projects, UUID identity and the proxy module are all new in 3.0.0.

Authors: meggawatts, DonoA, Eriol_Eandur, Planetology, Fraspace5, Jubo — and the proxy module
originally by Drayz.
