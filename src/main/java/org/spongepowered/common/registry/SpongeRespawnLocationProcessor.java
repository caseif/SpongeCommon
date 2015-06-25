/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.registry;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataPriority;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.entity.RespawnLocationData;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.entity.player.User;
import org.spongepowered.api.service.persistence.InvalidDataException;
import org.spongepowered.api.world.World;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.DataTransactionBuilder;
import org.spongepowered.common.data.SpongeDataProcessor;
import org.spongepowered.common.data.manipulator.entity.SpongeRespawnLocationData;
import org.spongepowered.common.interfaces.IMixinEntityPlayer;
import org.spongepowered.common.util.VecHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpongeRespawnLocationProcessor implements SpongeDataProcessor<RespawnLocationData> {

    @Override
    public Optional<RespawnLocationData> build(DataView container) throws InvalidDataException {
        // TODO Auto-generated method stub
        return Optional.absent();
    }

    @Override
    public RespawnLocationData create() {
        return new SpongeRespawnLocationData();
    }

    @Override
    public Optional<RespawnLocationData> createFrom(DataHolder dataHolder) {
        if (dataHolder instanceof User) {
            if (dataHolder instanceof Player) {
                RespawnLocationData data = new SpongeRespawnLocationData();
                for (Map.Entry<Integer, BlockPos> entry : ((IMixinEntityPlayer) dataHolder).getAllBedLocations().entrySet()) {
                    UUID uuid = ((World) MinecraftServer.getServer().worldServerForDimension(entry.getKey())).getUniqueId();
                    data.set(uuid, VecHelper.toVector(entry.getValue()).toDouble());
                }
                return Optional.of(data);
            }
            // TODO
            return Optional.absent();
        }
        return Optional.absent();
    }

    @Override
    public Optional<RespawnLocationData> getFrom(DataHolder dataHolder) {
        // TODO This is wrong
        return this.createFrom(dataHolder);
    }

    @Override
    public Optional<RespawnLocationData> fillData(DataHolder dataHolder, RespawnLocationData manipulator, DataPriority priority) {
        // TODO Auto-generated method stub
        return Optional.absent();
    }

    @Override
    public DataTransactionResult setData(DataHolder dataHolder, RespawnLocationData manipulator, DataPriority priority) {
        if (dataHolder instanceof User) {
            if (dataHolder instanceof Player) {
                HashMap<Integer, BlockPos> locations = Maps.newHashMap();
                for (Map.Entry<UUID, Vector3d> entry : manipulator.asMap().entrySet()) {
                    Optional<World> optWorld = Sponge.getGame().getServer().getWorld(entry.getKey());
                    if (optWorld.isPresent()) {
                        Integer dim = ((net.minecraft.world.World) optWorld.get()).provider.getDimensionId();
                        locations.put(dim, VecHelper.toBlockPos(entry.getValue()));
                    }
                }
                ((IMixinEntityPlayer) dataHolder).setAllBedLocations(locations);
                return DataTransactionBuilder.successNoData();
            }
            // TODO
            return DataTransactionBuilder.fail(manipulator);
        }
        return DataTransactionBuilder.fail(manipulator);
    }

    @Override
    public boolean remove(DataHolder dataHolder) {
        // TODO Auto-generated method stub
        return false;
    }

}
