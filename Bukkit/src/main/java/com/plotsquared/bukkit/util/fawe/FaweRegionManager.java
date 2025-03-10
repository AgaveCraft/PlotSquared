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
package com.plotsquared.bukkit.util.fawe;

import com.boydti.fawe.bukkit.regions.plotsquared.FaweDelegateRegionManager;
import com.google.inject.Inject;
import com.plotsquared.bukkit.util.BukkitRegionManager;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.generator.HybridPlotManager;
import com.plotsquared.core.inject.factory.ProgressSubscriberFactory;
import com.plotsquared.core.location.Location;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.PlotManager;
import com.plotsquared.core.queue.GlobalBlockQueue;
import com.plotsquared.core.queue.QueueCoordinator;
import com.plotsquared.core.util.WorldUtil;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FaweRegionManager extends BukkitRegionManager {

    private final FaweDelegateRegionManager delegate = new FaweDelegateRegionManager();

    @Inject
    public FaweRegionManager(
            @NonNull WorldUtil worldUtil, @NonNull GlobalBlockQueue blockQueue, @NonNull
            ProgressSubscriberFactory subscriberFactory
    ) {
        super(worldUtil, blockQueue, subscriberFactory);
    }

    @Override
    public boolean setCuboids(
            final @NonNull PlotArea area,
            final @NonNull Set<CuboidRegion> regions,
            final @NonNull Pattern blocks,
            int minY,
            int maxY,
            @Nullable PlotPlayer<?> actor,
            @Nullable QueueCoordinator queue
    ) {
        return delegate.setCuboids(area, regions, blocks, minY, maxY, queue.getCompleteTask());
    }

    @Override
    public boolean notifyClear(PlotManager manager) {
        if (!Settings.FAWE_Components.CLEAR || !(manager instanceof HybridPlotManager)) {
            return false;
        }
        return delegate.notifyClear(manager);
    }

    @Override
    public boolean handleClear(
            @NotNull Plot plot,
            @Nullable Runnable whenDone,
            @NotNull PlotManager manager,
            final @Nullable PlotPlayer<?> player
    ) {
        if (!Settings.FAWE_Components.CLEAR || !(manager instanceof HybridPlotManager)) {
            return false;
        }
        return delegate.handleClear(plot, whenDone, manager);
    }

    @Override
    public void swap(
            Location pos1,
            Location pos2,
            Location swapPos,
            final @Nullable PlotPlayer<?> player,
            final Runnable whenDone
    ) {
        delegate.swap(pos1, pos2, swapPos, whenDone);
    }

    @Override
    public void setBiome(CuboidRegion region, int extendBiome, BiomeType biome, String world, Runnable whenDone) {
        delegate.setBiome(region, extendBiome, biome, world, whenDone);
    }

    @Override
    public boolean copyRegion(
            final @NonNull Location pos1,
            final @NonNull Location pos2,
            final @NonNull Location pos3,
            final @Nullable PlotPlayer<?> player,
            final @NonNull Runnable whenDone
    ) {
        return delegate.copyRegion(pos1, pos2, pos3, whenDone);
    }

    @Override
    public boolean regenerateRegion(final Location pos1, final Location pos2, boolean ignore, final Runnable whenDone) {
        return delegate.regenerateRegion(pos1, pos2, ignore, whenDone);
    }

}
