/*
 *       _____  _       _    _____                                _
 *      |  __ \| |     | |  / ____|                              | |
 *      | |__) | | ___ | |_| (___   __ _ _   _  __ _ _ __ ___  __| |
 *      |  ___/| |/ _ \| __|\___ \ / _` | | | |/ _` | '__/ _ \/ _` |
 *      | |    | | (_) | |_ ____) | (_| | |_| | (_| | | |  __/ (_| |
 *      |_|    |_|\___/ \__|_____/ \__, |\__,_|\__,_|_|  \___|\__,_|
 *                                    | |
 *                                    |_|
 *            PlotSquared plot management system for Minecraft
 *                  Copyright (C) 2021 IntellectualSites
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.plotsquared.core.command;

import com.google.inject.Inject;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.Templates;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.util.EventDispatcher;
import com.plotsquared.core.util.Permissions;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.TabCompletions;
import com.plotsquared.core.util.task.RunnableVal2;
import com.plotsquared.core.util.task.RunnableVal3;
import net.kyori.adventure.text.minimessage.Template;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@CommandDeclaration(command = "trust",
        aliases = {"t"},
        requiredType = RequiredType.PLAYER,
        usage = "/plot trust <player | *>",
        category = CommandCategory.SETTINGS)
public class Trust extends Command {

    private final EventDispatcher eventDispatcher;

    @Inject
    public Trust(final @NonNull EventDispatcher eventDispatcher) {
        super(MainCommand.getInstance(), true);
        this.eventDispatcher = eventDispatcher;
    }

    @Override
    public CompletableFuture<Boolean> execute(
            final PlotPlayer<?> player, String[] args,
            RunnableVal3<Command, Runnable, Runnable> confirm,
            RunnableVal2<Command, CommandResult> whenDone
    ) throws CommandException {
        final Plot currentPlot = player.getCurrentPlot();
        if (currentPlot == null) {
            throw new CommandException(TranslatableCaption.of("errors.not_in_plot"));
        }
        checkTrue(currentPlot.hasOwner(), TranslatableCaption.of("info.plot_unowned"));
        checkTrue(
                currentPlot.isOwner(player.getUUID()) || Permissions
                        .hasPermission(player, Permission.PERMISSION_ADMIN_COMMAND_TRUST),
                TranslatableCaption.of("permission.no_plot_perms")
        );

        checkTrue(args.length == 1, TranslatableCaption.of("commandconfig.command_syntax"),
                Templates.of("value", getUsage())
        );

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        PlayerManager.getUUIDsFromString(args[0], (uuids, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    player.sendMessage(TranslatableCaption.of("players.fetching_players_timeout"));
                } else {
                    player.sendMessage(
                            TranslatableCaption.of("errors.invalid_player"),
                            Template.of("value", args[0])
                    );
                }
                future.completeExceptionally(throwable);
                return;
            } else {
                checkTrue(!uuids.isEmpty(), TranslatableCaption.of("errors.invalid_player"),
                        Templates.of("value", args[0])
                );

                Iterator<UUID> iterator = uuids.iterator();
                int size = currentPlot.getTrusted().size() + currentPlot.getMembers().size();
                while (iterator.hasNext()) {
                    UUID uuid = iterator.next();
                    if (uuid == DBFunc.EVERYONE && !(
                            Permissions.hasPermission(player, Permission.PERMISSION_TRUST_EVERYONE) || Permissions
                                    .hasPermission(player, Permission.PERMISSION_ADMIN_COMMAND_TRUST))) {
                        player.sendMessage(
                                TranslatableCaption.of("errors.invalid_player"),
                                Template.of("value", PlayerManager.getName(uuid))
                        );
                        iterator.remove();
                        continue;
                    }
                    if (currentPlot.isOwner(uuid)) {
                        player.sendMessage(
                                TranslatableCaption.of("member.already_added"),
                                Template.of("value", PlayerManager.getName(uuid))
                        );
                        iterator.remove();
                        continue;
                    }
                    if (currentPlot.getTrusted().contains(uuid)) {
                        player.sendMessage(
                                TranslatableCaption.of("member.already_added"),
                                Template.of("value", PlayerManager.getName(uuid))
                        );
                        iterator.remove();
                        continue;
                    }
                    size += currentPlot.getMembers().contains(uuid) ? 0 : 1;
                }
                checkTrue(!uuids.isEmpty(), null);
                int maxTrustSize = Permissions.hasPermissionRange(player, Permission.PERMISSION_TRUST, Settings.Limit.MAX_PLOTS);
                if (size > maxTrustSize) {
                    player.sendMessage(
                            TranslatableCaption.of("members.plot_max_members_trusted"),
                            Template.of("amount", String.valueOf(size - 1))
                    );
                    return;
                }
                // Success
                confirm.run(this, () -> {
                    for (UUID uuid : uuids) {
                        if (uuid != DBFunc.EVERYONE) {
                            if (!currentPlot.removeMember(uuid)) {
                                if (currentPlot.getDenied().contains(uuid)) {
                                    currentPlot.removeDenied(uuid);
                                }
                            }
                        }
                        currentPlot.addTrusted(uuid);
                        this.eventDispatcher.callTrusted(player, currentPlot, uuid, true);
                        player.sendMessage(TranslatableCaption.of("trusted.trusted_added"));
                    }
                }, null);
            }
            future.complete(true);
        });
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public Collection<Command> tab(final PlotPlayer<?> player, final String[] args, final boolean space) {
        return TabCompletions.completePlayers(String.join(",", args).trim(), Collections.emptyList());
    }

}
