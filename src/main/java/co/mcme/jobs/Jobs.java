package co.mcme.jobs;

import co.mcme.jobs.commands.JobAdminCommand;
import co.mcme.jobs.commands.JobCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Jobs extends JavaPlugin {

    @Override
    public void onEnable() {
        getCommand("jobadmin").setExecutor(new JobAdminCommand());
        getCommand("job").setExecutor(new JobCommand());
    }
}
