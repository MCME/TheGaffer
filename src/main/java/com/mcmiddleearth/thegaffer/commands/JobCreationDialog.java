/*  This file is part of TheGaffer.
 *
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer.commands;

import com.mcmiddleearth.thegaffer.TheGaffer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.storage.Project;
import com.mcmiddleearth.thegaffer.storage.ProjectDatabase;
import com.mcmiddleearth.thegaffer.utilities.JobCreationService;
import com.mcmiddleearth.thegaffer.utilities.PermissionsUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles {@code /createjob} (and, via dispatch, {@code /job create} and
 * {@code /job start}) by opening a native Minecraft Dialog form.
 *
 * <p>Requires Minecraft 26.1.2+ for the Dialog API. There is no chat-conversation
 * fallback: the plugin targets a modern server, so the single-screen form fully
 * replaces the old {@code JobCreationConversation}. All field values are read back
 * in {@link #onSubmit} and handed to the shared {@link JobCreationService}, which
 * fires the same job-start pipeline (broadcast, Discord, border, map) as before.
 */
public class JobCreationDialog implements CommandExecutor {

    private static final String KEY_NAME = "name";
    private static final String KEY_DESC = "description";
    private static final String KEY_PRIVATE = "private";
    private static final String KEY_RADIUS = "radius";
    private static final String KEY_DISCORD = "discord";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_GLOW = "glow";

    private static final int DEFAULT_RADIUS = 50;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("You must be a player to create a job.", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission(PermissionsUtil.getCreatePermission())) {
            String perm = PermissionsUtil.getCreatePermission().getName();
            player.sendMessage(Component.text("That's a staff action (", NamedTextColor.RED)
                    .append(Component.text(perm, NamedTextColor.YELLOW))
                    .append(Component.text(") — ask staff to run it.", NamedTextColor.RED)));
            return true;
        }

        Job current = JobDatabase.getJobWorking(player);
        if (current != null) {
            player.sendMessage(Component.text("You are already in the job ", NamedTextColor.RED)
                    .append(Component.text(current.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" - leave or stop it before creating a new one.", NamedTextColor.RED)));
            return true;
        }

        player.showDialog(buildDialog(player));
        return true;
    }

    /** Build a fresh Dialog for this player, including only the inputs the config enables. */
    private Dialog buildDialog(Player player) {
        List<DialogInput> inputs = new ArrayList<>();

        inputs.add(DialogInput.text(KEY_NAME, Component.text("Job name"))
                .maxLength(48).width(300).build());

        if (TheGaffer.isJobDescription()) {
            inputs.add(DialogInput.text(KEY_DESC, Component.text("Short description"))
                    .maxLength(128).width(300).build());
        }

        inputs.add(DialogInput.bool(KEY_PRIVATE, Component.text("Private (invite-only)"),
                false, "true", "false"));

        inputs.add(DialogInput.numberRange(KEY_RADIUS, Component.text("Build radius (blocks)"), 1f, 1000f)
                .initial((float) DEFAULT_RADIUS).step(1f).width(240).build());

        if (TheGaffer.isDiscordEnabled()) {
            inputs.add(DialogInput.bool(KEY_DISCORD, Component.text("Announce on Discord"),
                    true, "true", "false"));
        }

        if (ProjectDatabase.hasActiveProjects()) {
            List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
            options.add(SingleOptionDialogInput.OptionEntry.create("nothing",
                    Component.text("No project"), true));
            for (Project p : ProjectDatabase.byStatus(Project.Status.ACTIVE)) {
                options.add(SingleOptionDialogInput.OptionEntry.create(p.getName(),
                        Component.text(p.getName()), false));
            }
            inputs.add(DialogInput.singleOption(KEY_PROJECT, Component.text("Project"), options)
                    .width(240).build());
        }

        if (TheGaffer.isGlowing()) {
            inputs.add(DialogInput.bool(KEY_GLOW, Component.text("Glow outline for builders"),
                    false, "true", "false"));
        }

        ClickCallback.Options callbackOptions = ClickCallback.Options.builder().build();
        ActionButton createButton = ActionButton.builder(Component.text("Create job"))
                .tooltip(Component.text("Create and start the job with these settings"))
                .action(DialogAction.customClick((view, audience) -> onSubmit(player, view), callbackOptions))
                .build();
        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel"))
                .action(DialogAction.customClick((view, audience) -> onCancel(player), callbackOptions))
                .build();

        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Create a build job"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE)
                        .body(List.of(DialogBody.plainMessage(
                                Component.text("Set up your job below. Cancel or press Escape to abort."))))
                        .inputs(inputs)
                        .build())
                .type(DialogType.confirmation(createButton, cancelButton)));
    }

    /** Read the submitted form and create the job on the main thread. */
    private void onSubmit(Player player, DialogResponseView view) {
        String name = view.getText(KEY_NAME);
        String description = view.getText(KEY_DESC);
        Boolean priv = view.getBoolean(KEY_PRIVATE);
        Float radius = view.getFloat(KEY_RADIUS);
        Boolean discord = view.getBoolean(KEY_DISCORD);
        String project = view.getText(KEY_PROJECT);
        Boolean glow = view.getBoolean(KEY_GLOW);

        int radiusInt = (radius == null) ? DEFAULT_RADIUS : Math.round(radius);

        // The custom-click callback may fire off the main thread; job creation touches
        // Bukkit state and fires events, so always hop back onto the main thread.
        Bukkit.getScheduler().runTask(TheGaffer.getPluginInstance(), () -> {
            Job current = JobDatabase.getJobWorking(player);
            if (current != null) {
                player.sendMessage(Component.text("You are already in the job ", NamedTextColor.RED)
                        .append(Component.text(current.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" - no new job created.", NamedTextColor.RED)));
                return;
            }

            JobCreationService.Outcome outcome = JobCreationService.create(player, name,
                    description == null ? "" : description,
                    priv != null && priv,
                    radiusInt,
                    discord != null && discord,
                    project == null ? "nothing" : project,
                    glow != null && glow);

            switch (outcome) {
                case OK:
                    player.sendMessage(Component.text("Created the ", NamedTextColor.GREEN)
                            .append(Component.text(JobCreationService.normalizeName(name), NamedTextColor.AQUA))
                            .append(Component.text(" job!", NamedTextColor.GREEN)));
                    break;
                case EMPTY_NAME:
                    player.sendMessage(Component.text("A job name is required — nothing was created.",
                            NamedTextColor.RED));
                    break;
                case NAME_TAKEN_HISTORY:
                    player.sendMessage(Component.text(
                            "A job by that name has been run before — pick a different name.", NamedTextColor.RED));
                    break;
                case NAME_RUNNING:
                    player.sendMessage(Component.text(
                            "A job by that name is already running — pick another name.", NamedTextColor.RED));
                    break;
                default:
                    break;
            }
        });
    }

    private void onCancel(Player player) {
        player.sendMessage(Component.text("Job creation cancelled. No job was created.", NamedTextColor.GRAY));
    }
}
