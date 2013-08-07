# The Gaffer

*The Gaffer* is a bukkit plugin that adds a job system to the MCME Server.

### What is a "Job"
A job is basically a glorified list. This list contains who is working on the job, who is managing the workers, and where the job is. With MCME 2.0 we have decided to implement a more strict policy on building. In order to build on the new map, you must be a staff member, or be part of a job. *The Gaffer* handles building protection on its own, so it is very important to understand how the tool works.
##### Helpers
The list of helpers on a job is crucial. If the main runner of the job goes offline a few minutes later, *The Gaffer* will select another person from the list of helpers to continue the job. If *The Gaffer* cannot find another online helper to continue the job, the job is put on hold and held in the archive.

# Usage
## Staff
Staff have access to 2 commands, /job, and /jobadmin.

/job is used to create a new job, check running jobs and some plugin maintenance things.

/jobadmin is used to modify a currently running job.

#### The jobadmin Command

The jobadmin commad takes 2 -3 additional arguments.

The first argument is the action you would like to perform. The current actions you can perform are:

* addhelper
* removehelper
* setwarp

The second argument is the Job you would like to perform the action on. This can be any Job that is currently running. The name of the job is __case-sensitive__.

The third argument is currently only used in the helper actions, and is the name of the player that you would like to add or remove.

Below is a description of each action and what they do.

### addhelper
This action allows you to add an additional Staff member to the job. This Staff member can modify the job information such as the warp and also add additional helpers. The name is __case-sensitive__.

###### Example
/jobadmin addhelper GreenHills ryanturambar *This will add ryanturambar to the Staff list, but __not__ RyanTurambar*

### removehelper
This action allows you to remove a Staff member from the job. This Staff member will no longer be able to modify the job. The name is __case-sensitive__.

###### Example
/jobadmin removehelper GreenHills ryanturambar *This will remove ryanturambar to the Staff list for the job __GreenHills__, but __not__ RyanTurambar*

## User