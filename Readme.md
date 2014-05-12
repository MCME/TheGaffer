# The Gaffer
[![Build Status](https://travis-ci.org/MCME/TheGaffer.svg?branch=master)](https://travis-ci.org/MCME/TheGaffer)

*The Gaffer* is a bukkit plugin that adds a job system to the MCME Server and protects the rest of the world.

### What is a "Job"
A job is basically a glorified list. This list contains who is working on the job, who is managing the workers, and where the job is. With MCME 2.0 we have decided to implement a more strict policy on building. In order to build on the new map, you must be a staff member, or be part of a job. *The Gaffer* handles building protection on its own, so it is very important to understand how the tool works.
##### Helpers
The list of helpers on a job is crucial. A few minutes after the main runner of the job goes offline, *The Gaffer* will select another person from the list of helpers to continue the job. If *The Gaffer* cannot find another online helper to continue the job, the job is put on hold and held in the archive.

# Usage
Staff have access to 3 commands, /createjob, /job, and /jobadmin.

/job is used to check running jobs and some plugin maintenance things.

/jobadmin is used to modify a currently running job.

/createjob is used to create a new job.

#### The Createjob command

The createjob command is used to start a new job. Simply use _/createjob_ and you will be launched into an interactive conversation. Simply follow the in chat instructions, and chat your responses.


#### The Job Command

The job command is used to list, manage, and get info on jobs.

The job command takes 2 arguments.

The first argument is the action you would like to perform. The current actions are:

* __stop__
* check
* join
* warpto
* info
* archive
* admit

*Those in bold are only available to Staff*

###### Example
/job start river *This will start a job called **river**.
/job start river private *This will start a job called **river** that is invite only.

### Stop
This is the action that will stop a job. When a job is stopped, the working area becomes protected again, and the job is moved to the archive.

###### Example
/job stop river *This will stop the job called river, but __not__ River*

### Check
This is the action that allows the issuer to see the list of currently running jobs.

### Join
This is the action that allows the issuer to join a job. Once a user joins a job, they can build in within the allowed area. The name of the job is __case-sensitive__.

###### Example
/job join river *This will join the job river but __not__ River or rIVer*

### Warpto
This is the action that will warp you to the jobs location. This location is set automatically during the job create conversation. It is the location where the conversation was handled. It can also be updated.

###### Example
/job warpto river *This will warp you to the location of river but __not__ River*

### Info
This action allows you to see the info of any past or present job.

###### Example
/job info river

### Archive
This action will list all of the past jobs that are no longer running.

###### Example
/job archive

### Admit
The admit command works with TeamSpeak, it can only be called by the job owner or a player that has already been admitted. The command will teleport the target player to the Job Warp, this can only be done once to each player. (This will soon be automatic) 

###### Example
/job admit q220

#### The Jobadmin Command

The jobadmin commad will launch you into an interactive conversation that allows ou to edit jobs. SImply follow the in chat instructions, and chat your response.

The current actions you can perform are:

* addhelper
* removehelper
* setwarp
* listworkers
* kickworker
* banworker
* unbanworker
* bringall
* inviteworker
* uninviteworker
* setkit
* setradius 
* clearworkerinven
* setTeamSpeakwarp

The second argument is the Job you would like to perform the action on. This can be any Job that is currently running. The name of the job is __case-sensitive__.

The third argument is the name of the player that you would like to add or remove.

Below is a description of each action and what they do.

### addhelper
This action allows you to add an additional Staff member to the job. This Staff member can modify the job information such as the warp and also add additional helpers. The name is __case-sensitive__.

### removehelper
This action allows you to remove a Staff member from the job. This Staff member will no longer be able to modify the job. The name is __case-sensitive__.

### kickworker
This action allows you to kick a worker from the job. This worker will no longer be able to modify the world, but can rejoin. The name is __case-sensitive__.

### banworker
This action allows you to ban a worker from the job. This worker will no longer be able to rejoin the job. The name is __case-sensitive__.

### unbanworker
This action allows you to unban a worker from the job. This worker will now be able to rejoin the job. The name is __case-sensitive__.

### listworkers
This action allows you to list the workers of a job.

### bringall
This action allows you to bring all workers on the job to your location.

### inviteworker
This action allows you to invite a worker to a job.

### uninviteworker
This action allows you to invite a worker to a job.

### clearworkerinven
This action allows you to clear a workers' inventory.

### setTeamSpeakwarp
This sets a warp for people not in TS. If a TS channel is specified and a TS warp set, then new workers will be sent to the TS warp. If there is no TS warp set workers are sent to the regular jobWarp. /job admit <playername> will send a player not in TS to the regular JobWarp.