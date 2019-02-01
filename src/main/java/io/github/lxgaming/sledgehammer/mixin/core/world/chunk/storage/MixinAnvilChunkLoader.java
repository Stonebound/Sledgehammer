/*
 * Copyright 2019 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lxgaming.sledgehammer.mixin.core.world.chunk.storage;

import com.google.common.collect.Lists;
import io.github.lxgaming.sledgehammer.Sledgehammer;
import io.github.lxgaming.sledgehammer.configuration.Config;
import io.github.lxgaming.sledgehammer.configuration.category.MessageCategory;
import io.github.lxgaming.sledgehammer.configuration.category.MixinCategory;
import io.github.lxgaming.sledgehammer.exception.ChunkSaveException;
import io.github.lxgaming.sledgehammer.util.Broadcast;
import io.github.lxgaming.sledgehammer.util.Reference;
import io.github.lxgaming.sledgehammer.util.Toolbox;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(value = AnvilChunkLoader.class, priority = 1337)
public abstract class MixinAnvilChunkLoader {
    
    @Shadow
    protected abstract void writeChunkData(ChunkPos pos, NBTTagCompound compound) throws IOException;
    
    @Redirect(method = "writeNextIO",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/storage/AnvilChunkLoader;writeChunkData(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/nbt/NBTTagCompound;)V"
            )
    )
    private void onWriteChunkData(AnvilChunkLoader chunkLoader, ChunkPos pos, NBTTagCompound compound) throws IOException {
        if (sledgehammer$writeChunk(pos, compound)) {
            return;
        }
        
        // TAG_COMPOUND
        if (!compound.hasKey("Level", 10)) {
            Sledgehammer.getInstance().getLogger().error("Chunk ({}, {}) is missing its Level tag", pos.x, pos.z);
            return;
        }
        
        NBTTagCompound level = compound.getCompoundTag("Level");
        boolean saveRequired;
        
        // Remove Blacklisted items from Entities and TileEntities
        if (Sledgehammer.getInstance().getConfig().map(Config::getMixinCategory).map(MixinCategory::isChunkSavePurgeBlacklist).orElse(false)) {
            List<String> blacklist = Sledgehammer.getInstance().getConfig().map(Config::getMixinCategory).map(MixinCategory::getChunkSaveBlacklist).orElseGet(Lists::newArrayList);
            saveRequired = sledgehammer$removeEntityItems(level, item -> sledgehammer$checkItem(item, blacklist::contains));
            saveRequired |= sledgehammer$removeTileEntityItems(level, item -> sledgehammer$checkItem(item, blacklist::contains));
            if (saveRequired && sledgehammer$writeChunk(pos, compound)) {
                Sledgehammer.getInstance().getLogger().info("Chunk ({}, {}) saved after removing Blacklisted Items from Entities and TileEntities", pos.x, pos.z);
                return;
            }
        }
        
        // Remove all Entities and TileEntities
        if (Sledgehammer.getInstance().getConfig().map(Config::getMixinCategory).map(MixinCategory::isChunkSavePurgeAll).orElse(false)) {
            saveRequired = sledgehammer$removeEntities(level, entity -> true);
            saveRequired |= sledgehammer$removeTileEntities(level, tileEntity -> true);
            if (saveRequired && sledgehammer$writeChunk(pos, compound)) {
                Sledgehammer.getInstance().getLogger().info("Chunk ({}, {}) saved after removing all Entities and TileEntities", pos.x, pos.z);
                return;
            }
        }
        
        // Broadcast
        if (Sledgehammer.getInstance().getConfig().map(Config::getMixinCategory).map(MixinCategory::isChunkSaveAlert).orElse(false)) {
            Sledgehammer.getInstance().getConfig().map(Config::getMessageCategory).map(MessageCategory::getChunkSave).filter(StringUtils::isNotBlank).ifPresent(message -> {
                Broadcast.builder()
                        .message(Toolbox.convertColor(message.replace("[X]", String.valueOf(pos.x)).replace("[Z]", String.valueOf(pos.z))))
                        .permission(Reference.ID + ".broadcast.chunksave")
                        .type(Broadcast.Type.CHAT)
                        .build()
                        .sendMessage();
            });
        }
    }
    
    private boolean sledgehammer$writeChunk(ChunkPos pos, NBTTagCompound compound) throws IOException {
        try {
            this.writeChunkData(pos, compound);
            return true;
        } catch (ChunkSaveException ex) {
            return false;
        }
    }
    
    private boolean sledgehammer$removeEntities(NBTTagCompound level, Predicate<NBTTagCompound> predicate) {
        AtomicInteger removed = new AtomicInteger(0);
        sledgehammer$forEachEntity(level, entity -> {
            if (predicate.test(entity)) {
                removed.getAndIncrement();
                return true;
            }
            
            return false;
        });
        
        if (removed.get() > 0) {
            Sledgehammer.getInstance().getLogger().info("Removed {} {}", removed.get(), Toolbox.formatUnit(removed.get(), "Entity", "Entities"));
            return true;
        }
        
        return false;
    }
    
    private boolean sledgehammer$removeTileEntities(NBTTagCompound level, Predicate<NBTTagCompound> predicate) {
        AtomicInteger removed = new AtomicInteger(0);
        sledgehammer$forEachTileEntity(level, tileEntity -> {
            if (predicate.test(tileEntity)) {
                removed.getAndIncrement();
                return true;
            }
            
            return false;
        });
        
        if (removed.get() > 0) {
            Sledgehammer.getInstance().getLogger().info("Removed {} {}", removed.get(), Toolbox.formatUnit(removed.get(), "TileEntity", "TileEntities"));
            return true;
        }
        
        return false;
    }
    
    private boolean sledgehammer$removeEntityItems(NBTTagCompound level, Predicate<NBTTagCompound> predicate) {
        AtomicInteger removedEntities = new AtomicInteger(0);
        AtomicInteger removedItems = new AtomicInteger(0);
        sledgehammer$forEachEntity(level, entity -> {
            // TAG_COMPOUND
            if (entity.hasKey("Item", 10)) {
                NBTTagCompound item = entity.getCompoundTag("Item");
                if (predicate.test(item)) {
                    removedEntities.getAndIncrement();
                    return true;
                }
                
                // TAG_COMPOUND
                if (item.hasKey("tag", 10)) {
                    NBTTagCompound tag = item.getCompoundTag("tag");
                    // TAG_COMPOUND
                    if (tag.hasKey("BlockEntityTag", 10)) {
                        removedItems.getAndAdd(sledgehammer$removeItems(tag.getCompoundTag("BlockEntityTag"), predicate));
                    }
                }
            }
            
            return false;
        });
        
        boolean saveRequired = false;
        if (removedItems.get() > 0) {
            Sledgehammer.getInstance().getLogger().info("Removed {} {} from Entities", removedItems.get(), Toolbox.formatUnit(removedItems.get(), "Item", "Items"));
            saveRequired = true;
        }
        
        if (removedEntities.get() > 0) {
            Sledgehammer.getInstance().getLogger().info("Removed {} {}", removedEntities.get(), Toolbox.formatUnit(removedEntities.get(), "Entity", "Entities"));
            saveRequired = true;
        }
        
        return saveRequired;
    }
    
    private boolean sledgehammer$removeTileEntityItems(NBTTagCompound level, Predicate<NBTTagCompound> predicate) {
        AtomicInteger removed = new AtomicInteger(0);
        sledgehammer$forEachTileEntity(level, tileEntity -> {
            removed.getAndAdd(sledgehammer$removeItems(tileEntity, predicate));
            return false;
        });
        
        if (removed.get() > 0) {
            Sledgehammer.getInstance().getLogger().info("Removed {} {} from TileEntities", removed.get(), Toolbox.formatUnit(removed.get(), "Item", "Items"));
            return true;
        }
        
        return false;
    }
    
    private int sledgehammer$removeItems(NBTTagCompound compound, Predicate<NBTTagCompound> predicate) {
        int removed = 0;
        // TAG_LIST
        if (compound.hasKey("Items", 9)) {
            // TAG_COMPOUND
            NBTTagList items = compound.getTagList("Items", 10);
            for (int index = 0; index < items.tagCount(); index++) {
                NBTTagCompound item = items.getCompoundTagAt(index);
                if (predicate.test(item)) {
                    items.removeTag(index);
                    index--;
                    removed++;
                    continue;
                }
                
                // TAG_COMPOUND
                if (item.hasKey("tag", 10)) {
                    NBTTagCompound tag = item.getCompoundTag("tag");
                    // TAG_COMPOUND
                    if (tag.hasKey("BlockEntityTag", 10)) {
                        removed += sledgehammer$removeItems(tag.getCompoundTag("BlockEntityTag"), predicate);
                    }
                }
            }
        }
        
        return removed;
    }
    
    private boolean sledgehammer$checkItem(NBTTagCompound item, Predicate<String> predicate) {
        // TAG_STRING
        return item.hasKey("id", 8) && predicate.test(item.getString("id"));
    }
    
    private void sledgehammer$forEachEntity(NBTTagCompound level, Function<NBTTagCompound, Boolean> function) {
        // TAG_LIST
        if (level.hasKey("Entities", 9)) {
            // TAG_COMPOUND
            NBTTagList entities = level.getTagList("Entities", 10);
            for (int index = 0; index < entities.tagCount(); index++) {
                if (function.apply(entities.getCompoundTagAt(index))) {
                    entities.removeTag(index);
                    index--;
                }
            }
        }
    }
    
    private void sledgehammer$forEachTileEntity(NBTTagCompound level, Function<NBTTagCompound, Boolean> function) {
        // TAG_LIST
        if (level.hasKey("TileEntities", 9)) {
            // TAG_COMPOUND
            NBTTagList tileEntities = level.getTagList("TileEntities", 10);
            for (int index = 0; index < tileEntities.tagCount(); index++) {
                if (function.apply(tileEntities.getCompoundTagAt(index))) {
                    tileEntities.removeTag(index);
                    index--;
                }
            }
        }
    }
}