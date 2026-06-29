# Design note — Job persistence (P1)

**Status:** Proposal for review · Branch `2026-rework` · 2026-06
**Relates to:** AUDIT.md finding **P1** (jobs are in-memory only, lost on restart) and **C3** (`onDisable`); simplifies **L2/L3** (item-serialization fragility).

## Problem

Job persistence was removed in commit `242c5b1` (2020) when the vulnerable **codehaus Jackson 1.x** library was dropped for security — and never replaced. Today `JobDatabase.loadJobs()`/`saveJobs()` are commented out and the active/inactive jobs live only in in-memory `TreeMap`s, so **every job is lost on restart or crash.** We need to reintroduce persistence *safely* (the original was removed for a security reason — don't reintroduce that risk).

## Recommendation: Bukkit `YamlConfiguration` (not Jackson)

| | **Bukkit `YamlConfiguration`** (recommended) | `com.fasterxml.jackson` (modern Jackson) |
|---|---|---|
| Dependency | **None** — part of the Paper API | New dependency; **must be shaded** (the shade plugin was already removed from this pom) |
| Security | No third-party deserialization; addresses *why* persistence was removed | Safe only with care (fixed target type, no default typing); reintroduces a deserialization surface |
| ItemStacks (kits) | **Native** — `ItemStack` is `ConfigurationSerializable`; full NBT + cross-version data migration handled by Bukkit | Manual — needs the `storage/meta/` DTO layer (or custom serializers) |
| Net code | **Deletes** `JobItem` + `JobBookMeta`/`JobEnchantmentMeta`/`JobLeatherArmorMeta` | Keeps/rebuilds the DTO layer |

**Why YAML wins here:** it removes a dependency (the original sin was a dependency), it's the safe answer to the security reason persistence was pulled, and Bukkit serialises `ItemStack`s natively — which lets us **delete the entire `storage/meta/` DTO layer**. That in turn clears audit **L2** (`Material.valueOf` aborts a load on a renamed material; `Enchantment.getByName` NPE), **L3** (book author saved as the title), and makes kits version-robust (Bukkit migrates item data on load).

## What is persisted vs. transient

**Persist** (per job): `name`, `owner`, `running`, `paused`, `private`, `world`, `jobRadius`, `startTime`, `endTime`, `description`, `discordSend`, `discordTags`, `projectname`, `warp` (JobWarp), the member lists (`helpers`, `workers`, `bannedWorkers`, `invitedWorkers`), and `kit` (if set).

**Do NOT persist** (runtime/derived): `area` + `bounds` (regenerated from warp+radius via `generateBounds()`), `dirty`, `left`, and all the glow state (`glowing`, `helperTeam`/`workerTeam`, team names, `scoreboard`). With the old Jackson code these were `@JsonIgnore`; with YAML we simply don't write them.

## Storage layout & format

- **One YAML file per job**, as today: `plugins/TheGaffer/jobs/<name>.yml` (keeps corruption isolated; matches the existing per-`.job` model).
- **Atomic write** (already the pattern): write `<name>.yml.new`, then rename over `<name>.yml`.
- **Serialization:** explicit field mapping in a small `JobStorage` helper (keeps `Job` a plain POJO, no `ConfigurationSerializable` registration quirks). `JobWarp` → `x/y/z/yaw/pitch/world` keys; `kit` → native `ItemStack` serialization; lists → YAML lists.

Example `jobs/river.yml`:
```yaml
name: river
owner: Eriol_Eandur
running: true
paused: false
private: false
world: mainworld
radius: 100
warp: {x: 100.5, y: 64.0, z: -200.5, yaw: 90.0, pitch: 0.0, world: mainworld}
helpers: [Fraspace5]
workers: [PlayerB, PlayerC]
bannedWorkers: []
invitedWorkers: []
kit:
  contents: [ ... native ItemStack maps ... ]
  helmet: { ... }
```

## Save timing (and threading)

The original called `saveJobs()` **synchronously on every mutation, over the whole DB** (old audit H4). The new design avoids that:

- Mutations keep setting the existing **`dirty`** flag (the call sites are already there, just commented).
- A **periodic flush** (every ~30–60 s) and **lifecycle saves** (on activate/deactivate) write **only dirty jobs**.
- **Threading rule** (per the C1 discussion): build the `YamlConfiguration` from job fields **on the main thread** (safe reads), then perform the **file write asynchronously** (`runTaskAsynchronously`) — file I/O is the one thing that legitimately belongs off-thread; the config object is a snapshot, so no off-thread access to live job state.
- **`onDisable`** (closes **C3**): cancel tasks and **synchronously flush** all dirty jobs so nothing is lost on shutdown.

## Load (on enable)

Restore `loadJobs()` in `onEnable`: read `jobs/*.yml`, deserialize, re-add to active/inactive maps, and for running jobs re-register the listener, `generateBounds()`, and re-schedule the owner-timeout. **Skip unparseable files** with a logged warning rather than aborting startup (so one bad file can't take the plugin down — and any stale pre-2020 Jackson `.job` files are simply ignored, since the format differs; no migration needed).

## Decisions for review

1. **Approve YAML over Jackson?** (recommended — no dependency, safe, deletes the DTO layer)
2. **Per-job files vs. a single `jobs.yml`?** (recommended: per-job, for corruption isolation + atomic writes)
3. **OK to delete `storage/meta/` (`JobItem` + the three meta classes) and switch `JobKit` to native `ItemStack`s?** (recommended)
4. **Save cadence:** periodic flush + lifecycle + `onDisable` (recommended) vs. debounced per-mutation async.

## Risks & mitigations

- **World not loaded at load time** — `onEnable` can run before some worlds load; `JobWarp.toBukkitLocation()` returns null then. Mitigation: store the world *name* (string) and resolve lazily / defer `generateBounds()` until first use, or load jobs slightly later. **Needs care.**
- **Cross-version items** — handled natively by Bukkit's `ItemStack` data-version migration (a strict upgrade over the old `Material.valueOf` crash).
- **Concurrency** — config built on main thread, only the write is async; never touch live job collections from the writer.
- **Identity (UUID / H5)** — persistence here stores members by **name**, matching the current model. The future name→UUID migration (H5) will bump the on-disk format (one-time convert or version key); kept **decoupled** from this step.

## Work breakdown / sequencing

1. `JobStorage` helper: `toYaml(Job)` / `fromYaml(YamlConfiguration)` (+ `JobWarp` map helpers).
2. Switch `JobKit` to `ItemStack[]` + armor `ItemStack`s; **delete `storage/meta/` + `JobItem`**; update `JobKit.replaceInventory` and the `new JobKit(inventory)` capture.
3. Reinstate `JobDatabase.loadJobs()` / `saveJob(Job)` / `saveDirty()` (YAML + atomic write + async).
4. Re-enable the save calls (the commented `// saveJobs()` sites → mark dirty) and add the periodic flush.
5. `onEnable`: `loadJobs()`. `onDisable`: cancel tasks + synchronous flush (**C3**).
6. Compile-verify each step.

## Out of scope

UUID migration (H5), and the lower-impact H3 (per-job listener / empty-event gating) remain separate steps.

## References
- Bukkit `YamlConfiguration`, `ConfigurationSerializable`, `ItemStack` serialization
- AUDIT.md (P1, C3, L2, L3, and the obsolete H4 note)
