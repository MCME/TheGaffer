# TheGaffer Velocity plugin (groundwork)

This module contains a minimal Velocity plugin that provides the groundwork for allowing players to request joining a job from any backend server.

What is included:

- `VelocityGafferPlugin` — logs on enable and registers a `gafferjoin` command that sends a plugin-message on channel `minecraft:thegaffer:join` to the player's current backend server.
- `velocity.toml` — basic plugin metadata.

Notes and next steps:

- Implement server-side handlers in each Bukkit/Spigot backend to listen for the plugin-message and perform the actual job-join logic.
- Consider using a structured payload format (JSON, Protobuf, or a small binary spec) for more complex data.
- Optionally add a parent multi-module build if you want `mvn -pl` integration with the main project.

Build:
Run `mvn -f thegaffer-velocity/pom.xml package` to build this plugin jar separately.
