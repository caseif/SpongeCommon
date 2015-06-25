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
package org.spongepowered.common.data.manipulator.entity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spongepowered.api.data.DataQuery.of;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.base.Optional;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataTransactionResult;
import org.spongepowered.api.data.MemoryDataContainer;
import org.spongepowered.api.data.manipulator.entity.RespawnLocationData;
import org.spongepowered.common.Sponge;
import org.spongepowered.common.data.DataTransactionBuilder;
import org.spongepowered.common.data.manipulator.AbstractMappedData;

import java.util.Map;
import java.util.UUID;

public class SpongeRespawnLocationData extends AbstractMappedData<UUID, Vector3d, RespawnLocationData> implements RespawnLocationData {

    public static final DataQuery RESPAWN_LOCATION = of("RespawnLocation");

    public SpongeRespawnLocationData() {
        super(RespawnLocationData.class);
    }

    @Override
    public DataTransactionResult set(UUID key, Vector3d value) {
        if (!Sponge.getGame().getServer().getWorld(key).isPresent()) {
            return DataTransactionBuilder.fail(this);
        }
        this.keyValueMap.put(key, checkNotNull(value, "value"));
        return DataTransactionBuilder.successNoData();
    }

    @Override
    public DataTransactionResult set(Map<UUID, Vector3d> mapped) {
        // TODO Auto-generated method stub
        return DataTransactionBuilder.builder().result(DataTransactionResult.Type.CANCELLED).build();
    }

    @Override
    public DataTransactionResult set(UUID... mapped) {
        // TODO Auto-generated method stub
        return DataTransactionBuilder.builder().result(DataTransactionResult.Type.CANCELLED).build();
    }

    @Override
    public RespawnLocationData copy() {
        SpongeRespawnLocationData data = new SpongeRespawnLocationData();
        data.keyValueMap.putAll(this.keyValueMap);
        return data;
    }

    @Override
    public int compareTo(RespawnLocationData o) {
        return 0;
    }

    @Override
    public DataContainer toContainer() {
        return new MemoryDataContainer()
                .set(RESPAWN_LOCATION, this.asMap());
    }

    @Override
    public Optional<Vector3d> getRespawnPosition(UUID worldUuid) {
        return this.get(worldUuid);
    }

    @Override
    public RespawnLocationData setRespawnPosition(UUID worldUuid, Vector3d position) {
        checkArgument(Sponge.getGame().getServer().getWorld(checkNotNull(worldUuid, "worldUuid")).isPresent(), "UUID is not a valid world");
        if (position == null) {
            this.keyValueMap.remove(worldUuid);
        } else {
            this.keyValueMap.put(worldUuid, position);
        }
        return this;
    }
}
