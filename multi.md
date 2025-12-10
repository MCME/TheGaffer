# Mutliserver support

## Duplicate
* Backend
  * Upon successful job creation/deletion msg the proxy
* Proxy
  * Copy the command structure in the proxy
    * .executes() only for (join & check)
      * join would need to be forwarded (spoofChatInput)
    * .suggestions for all others -> jobs would need to be filtered by current server connection!
  * Broadcast on playerJoin (creator, createdAt, world?)
  * Broadcast on job creation/deletion

## Intercepts
* Backend
  * Change the existing /job join command - removing the <name> param
  * Hook the existing /job join logic to a plugin message
  * Send a plugin msg to the proxy on job creation/deletion
* Proxy
  * Intercept /job check
  * Intercept /job join
    * If >1 job send a TextComponent
    * Else connects player + forward the /job join (or plugin msg or spoof)
  * New private command /_job_join <name>
    * Connects player + sends plugin message to join the player
  * Broadcast on playerJoin (creator, createdAt, world?)
  * Broadcast on job creation/deletion

## Details
* Send the world? For display purposes?
* Move the broadcast to the proxy
  * creator, world? 
* Add/remove the job from a Map<>

## Velocity
At a minimum a velocity plugin is needed to swap the player to the correct server when they try 
to join a job

* Also `/job check`

## Paper
* What can paper do and only paper do?
  * glowing players
  * inventory stuff
  * bounds
  * setwarp and warpto
  * setkit
  * setradius
  * clearworkerinven
* What would marketplace plugins do to be able to support non-proxy servers?
  * MySQL storage or redis pub/sub so that backends all know
  * By default everything should work without a proxy plugin
  * Listen to a redis pub/sub to populate its suggestions
  * proxy plugin consumes /job join commands

## Implementation
When a job is created/stopped:
* Message the proxy
* Add/remove the job from a HashMap
  * This allows the proxy to know which server to connect the player to when they join the job
* Broadcast to all online players

When a player wants to join a job:
* How to get suggestions for `/job join <job>`? Does it even matter with a click event?
  * `/job join` in backends and plugin message to alert backends of jobs from other plugins
    * The messages would need to be sent everytime a player connects to a server - since a 
      backend can't be communicated with if no player is on it!
  * `/job:join` in the proxy plugin

* When a backend starts up request the active jobs from the proxy
* When a job is created/stopped msg all backends
  * What if the backend has no players?
    * onPlayerJoin if player count == 1 then request the active jobs

## Redis
* Backends publish whenever a job has started or stopped
* Backends subscribe to this channel and update their internal list of jobs - used by `check` & 
  `join`

## Velocity Forwarding
* Copy the structure of the backend commands (with suggestions?) but don't include `executes` - 
  allows `/job join` and `/job check` to be on the proxy without breaking the backend commands
