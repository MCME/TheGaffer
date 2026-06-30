package com.mcmiddleearth.thegaffer.commands;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.Project;
import com.mcmiddleearth.thegaffer.storage.ProjectDatabase;
import com.mcmiddleearth.thegaffer.utilities.Msg;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import com.mcmiddleearth.thegaffer.utilities.StatsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProjectCommand implements CommandExecutor, TabCompleter {

    /** Allowed characters in a project name; keeps the derived YAML filename filesystem-safe. */
    private static final String NAME_PATTERN = "[A-Za-z0-9 '-]+";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /project <create|list|info|setdescription|setgoal|setlead|addmanager|removemanager|complete|archive|reopen|attach|detach|delete>", NamedTextColor.GRAY));
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create": return create(sender, args);
            case "list":   return list(sender, args);
            case "info":   return info(sender, args);
            case "setdescription": return setText(sender, args, false);
            case "setgoal":         return setText(sender, args, true);
            case "setlead":         return setLead(sender, args);
            case "addmanager":      return manager(sender, args, true);
            case "removemanager":   return manager(sender, args, false);
            case "complete":        return setStatus(sender, args, Project.Status.COMPLETED);
            case "archive":         return setStatus(sender, args, Project.Status.ARCHIVED);
            case "reopen":          return setStatus(sender, args, Project.Status.ACTIVE);
            case "attach":          return attach(sender, args);
            case "detach":          return detach(sender, args);
            case "delete":          return delete(sender, args);
            default:
                sender.sendMessage(Component.text("Unknown subcommand: " + sub + " — type /project for the list.", NamedTextColor.RED));
                return true;
        }
    }

    // ---- core subcommands ----

    private boolean create(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PermissionsUtil.getProjectCreatePermission())
                && !sender.hasPermission(PermissionsUtil.getProjectAdminPermission())) {
            return deny(sender);
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only a player can create a project (they become its lead).", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /project create <name>", NamedTextColor.RED));
            return true;
        }
        String name = joinFrom(args, 1).trim();
        if (name.isEmpty() || name.equalsIgnoreCase("nothing")) {
            sender.sendMessage(Component.text("That is not a valid project name.", NamedTextColor.RED));
            return true;
        }
        if (!name.matches(NAME_PATTERN)) {
            sender.sendMessage(Component.text("Project names may only contain letters, digits, spaces, hyphens, and apostrophes.", NamedTextColor.RED));
            return true;
        }
        Project p = new Project(name, ((Player) sender).getUniqueId(), System.currentTimeMillis());
        if (!ProjectDatabase.create(p)) {
            sender.sendMessage(Component.text("A project named '" + name + "' already exists.", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text("Created project ", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.GOLD))
                .append(Component.text(". You are its lead.", NamedTextColor.GREEN)));
        return true;
    }

    // list and info are intentionally open (view-only) to anyone who can run /project;
    // only create and the management subcommands are permission-gated.
    private boolean list(CommandSender sender, String[] args) {
        Project.Status filter = Project.Status.ACTIVE;
        if (args.length > 1) {
            try { filter = Project.Status.valueOf(args[1].toUpperCase()); }
            catch (IllegalArgumentException ex) {
                sender.sendMessage(Component.text("Status must be active, completed, or archived.", NamedTextColor.RED));
                return true;
            }
        }
        List<Project> shown = ProjectDatabase.byStatus(filter);
        // Compute each project's aggregate ONCE (getProjectAggregate scans all stats files);
        // never call it inside the comparator.
        Map<String, Long> placed = new HashMap<>();
        for (Project p : shown) {
            placed.put(Project.canonical(p.getName()), StatsManager.getProjectAggregate(p.getName()).getPlaced());
        }
        shown.sort(Comparator.comparingLong(
                (Project p) -> placed.getOrDefault(Project.canonical(p.getName()), 0L)).reversed());
        Component out = Component.text("Projects (" + filter.name().toLowerCase() + "):", NamedTextColor.GRAY);
        if (shown.isEmpty()) {
            out = out.append(Component.newline()).append(Component.text("  none", NamedTextColor.DARK_GRAY));
        }
        for (Project p : shown) {
            out = out.append(Component.newline())
                    .append(Msg.button(p.getName(), NamedTextColor.GOLD, "/project info " + p.getName(), "View project"));
        }
        sender.sendMessage(out);
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /project info <name>", NamedTextColor.RED));
            return true;
        }
        String name = joinFrom(args, 1);
        Project p = ProjectDatabase.get(name);
        StatsManager.ProjectAggregate agg = StatsManager.getProjectAggregate(name);
        if (p != null) {
            sender.sendMessage(StatsManager.renderProjectStats(p, agg));
        } else if (!agg.isEmpty()) {
            sender.sendMessage(StatsManager.renderOrphanProjectStats(name, agg));
        } else {
            sender.sendMessage(Component.text("No project by that name. Try /project list to see existing projects.", NamedTextColor.RED));
        }
        return true;
    }

    // ---- auth helpers (lead/manager, with the project.admin bypass) ----

    private boolean isAdmin(CommandSender s) {
        return s.hasPermission(PermissionsUtil.getProjectAdminPermission());
    }

    private boolean canManage(CommandSender s, Project p) {
        if (isAdmin(s)) { return true; }
        return s instanceof Player && p.canManage(((Player) s).getUniqueId());
    }

    private boolean canAdminister(CommandSender s, Project p) {
        if (isAdmin(s)) { return true; }
        return s instanceof Player && p.isLead(((Player) s).getUniqueId());
    }

    /** Resolves a player name to a UUID (cached/offline), matching the existing admin-command pattern. */
    private UUID resolve(String playerName) {
        return TheGaffer.getServerInstance().getOfflinePlayer(playerName).getUniqueId();
    }

    private Project require(CommandSender sender, String name) {
        Project p = ProjectDatabase.get(name);
        if (p == null) { noProject(sender); }
        return p;
    }

    private Job findJob(String name) {
        Job j = JobDatabase.getActiveJobs().get(name);
        return j != null ? j : JobDatabase.getInactiveJobs().get(name);
    }

    // ---- multi-word name matching (project names may contain spaces, e.g. "Minas Tirith") ----

    /** A matched project plus the index of the first arg AFTER its (possibly multi-word) name. */
    static final class Match {
        final Project project;
        final int next;
        Match(Project project, int next) { this.project = project; this.next = next; }
    }

    /**
     * Greedily matches the LONGEST registered project name that is a prefix of {@code args[from..]}.
     * Returns null if no registered project matches — this is how a multi-word name is separated
     * from any trailing player/job/text argument (the registry decides where the name ends).
     */
    Match matchProject(String[] args, int from) {
        for (int end = args.length; end > from; end--) {
            Project p = ProjectDatabase.get(joinRange(args, from, end));
            if (p != null) { return new Match(p, end); }
        }
        return null;
    }

    private String joinRange(String[] args, int from, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < end; i++) {
            if (i > from) { sb.append(" "); }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ---- management subcommands (all gated; project edits require manage/administer rights) ----

    private boolean setText(CommandSender sender, String[] args, boolean goal) {
        Match m = matchProject(args, 1);
        if (m == null) { return noProject(sender); }
        if (!canManage(sender, m.project)) { return denyManage(sender); }
        if (m.next >= args.length) {
            sender.sendMessage(Component.text("Usage: /project " + (goal ? "setgoal" : "setdescription") + " <name> <text>", NamedTextColor.RED));
            return true;
        }
        String text = joinFrom(args, m.next);
        if (goal) { m.project.setGoal(text); } else { m.project.setDescription(text); }
        ProjectDatabase.saveProject(m.project);
        sender.sendMessage(Component.text("Updated " + (goal ? "goal" : "description") + " for " + m.project.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean setLead(CommandSender sender, String[] args) {
        Match m = matchProject(args, 1);
        if (m == null) { return noProject(sender); }
        if (!canAdminister(sender, m.project)) { return denyAdminister(sender); }
        if (m.next >= args.length) {
            sender.sendMessage(Component.text("Usage: /project setlead <name> <player>", NamedTextColor.RED));
            return true;
        }
        String player = joinFrom(args, m.next);
        m.project.setLead(resolve(player));
        ProjectDatabase.saveProject(m.project);
        sender.sendMessage(Component.text("Lead of " + m.project.getName() + " is now " + player + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean manager(CommandSender sender, String[] args, boolean add) {
        Match m = matchProject(args, 1);
        if (m == null) { return noProject(sender); }
        if (!canManage(sender, m.project)) { return denyManage(sender); }
        if (m.next >= args.length) {
            sender.sendMessage(Component.text("Usage: /project " + (add ? "addmanager" : "removemanager") + " <name> <player>", NamedTextColor.RED));
            return true;
        }
        String player = joinFrom(args, m.next);
        UUID id = resolve(player);
        if (add) { m.project.addManager(id); } else { m.project.removeManager(id); }
        ProjectDatabase.saveProject(m.project);
        sender.sendMessage(Component.text((add ? "Added " : "Removed ") + player + " as a manager of " + m.project.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean setStatus(CommandSender sender, String[] args, Project.Status status) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /project <complete|archive|reopen> <name>", NamedTextColor.RED));
            return true;
        }
        Project p = require(sender, joinFrom(args, 1));
        if (p == null) { return true; }
        if (!canManage(sender, p)) { return denyManage(sender); }
        p.setStatus(status);
        if (status == Project.Status.COMPLETED) { p.setCompletedTime(System.currentTimeMillis()); }
        else if (status == Project.Status.ACTIVE) { p.setCompletedTime(0L); } // reopen clears the completion stamp
        ProjectDatabase.saveProject(p);
        sender.sendMessage(Component.text(p.getName() + " is now " + status.name().toLowerCase() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean attach(CommandSender sender, String[] args) {
        Match m = matchProject(args, 1);
        if (m == null) { return noProject(sender); }
        if (!canManage(sender, m.project)) { return denyManage(sender); }
        if (m.next >= args.length) {
            sender.sendMessage(Component.text("Usage: /project attach <name> <job>", NamedTextColor.RED));
            return true;
        }
        String jobName = joinFrom(args, m.next);
        Job job = findJob(jobName);
        if (job == null) { sender.sendMessage(Component.text("No job named '" + jobName + "'.", NamedTextColor.RED)); return true; }
        job.setProjectname(m.project.getName());
        job.setDirty(true);
        JobDatabase.saveJob(job);
        sender.sendMessage(Component.text("Attached job " + job.getName() + " to project " + m.project.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean detach(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /project detach <job>", NamedTextColor.RED));
            return true;
        }
        String jobName = joinFrom(args, 1);
        Job job = findJob(jobName);
        if (job == null) { sender.sendMessage(Component.text("No job named '" + jobName + "'.", NamedTextColor.RED)); return true; }
        String current = job.getProjectname();
        if (current == null || current.equalsIgnoreCase("nothing")) {
            sender.sendMessage(Component.text("That job isn't attached to a project.", NamedTextColor.GRAY));
            return true;
        }
        Project p = ProjectDatabase.get(current);
        if (p == null) {
            if (!isAdmin(sender)) { return denyManage(sender); } // dangling label: only admins may detach
        } else if (!canManage(sender, p)) {
            return denyManage(sender);
        }
        job.setProjectname("nothing");
        job.setDirty(true);
        JobDatabase.saveJob(job);
        sender.sendMessage(Component.text("Detached job " + job.getName() + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /project delete <name>", NamedTextColor.RED));
            return true;
        }
        Project p = require(sender, joinFrom(args, 1));
        if (p == null) { return true; }
        if (!canAdminister(sender, p)) { return denyAdminister(sender); }
        ProjectDatabase.delete(p.getName());
        sender.sendMessage(Component.text("Deleted project " + p.getName() + ". Job and stats history keep the name.", NamedTextColor.GREEN));
        return true;
    }

    // ---- shared helpers ----

    private boolean deny(CommandSender s) {
        s.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
    }

    private boolean denyManage(CommandSender s) {
        s.sendMessage(Component.text("Only the project lead or a manager can do that.", NamedTextColor.RED));
        return true;
    }

    private boolean denyAdminister(CommandSender s) {
        s.sendMessage(Component.text("Only the project lead can do that (or an admin).", NamedTextColor.RED));
        return true;
    }

    private boolean noProject(CommandSender s) {
        s.sendMessage(Component.text("No project by that name. Try /project list to see existing projects.", NamedTextColor.RED));
        return true;
    }

    /** Joins args[from..] into a single space-separated string. */
    private String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) { sb.append(" "); }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ---- tab complete ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"create", "list", "info", "setdescription", "setgoal", "setlead",
                    "addmanager", "removemanager", "complete", "archive", "reopen", "attach", "detach", "delete"}) {
                if (s.startsWith(args[0].toLowerCase())) { out.add(s); }
            }
            return out;
        }
        String sub = args[0].toLowerCase();
        if (args.length == 2) {
            if (sub.equals("list")) {
                for (String s : new String[]{"active", "completed", "archived"}) {
                    if (s.startsWith(args[1].toLowerCase())) { out.add(s); }
                }
                return out;
            }
            if (sub.equals("detach")) {
                for (String jobName : JobDatabase.getActiveJobs().keySet()) {
                    if (jobName.startsWith(args[1])) { out.add(jobName); }
                }
                for (String jobName : JobDatabase.getInactiveJobs().keySet()) {
                    if (jobName.startsWith(args[1]) && !out.contains(jobName)) { out.add(jobName); }
                }
                return out;
            }
            for (Project p : ProjectDatabase.all()) {
                if (Project.canonical(p.getName()).startsWith(Project.canonical(args[1]))) { out.add(p.getName()); }
            }
            return out;
        }
        // NOTE: assumes a single-word project name; job-name completion won't trigger for a
        // multi-word project (e.g. "Minas Tirith"). Cosmetic — the command itself still works.
        if (args.length == 3 && sub.equals("attach")) {
            for (String jobName : JobDatabase.getActiveJobs().keySet()) {
                if (jobName.startsWith(args[2])) { out.add(jobName); }
            }
            for (String jobName : JobDatabase.getInactiveJobs().keySet()) {
                if (jobName.startsWith(args[2]) && !out.contains(jobName)) { out.add(jobName); }
            }
            return out;
        }
        return out;
    }
}
