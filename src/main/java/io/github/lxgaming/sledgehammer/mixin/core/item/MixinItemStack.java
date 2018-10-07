/*
 * Copyright 2018 Alex Thomson
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

package io.github.lxgaming.sledgehammer.mixin.core.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemStack.class, priority = 1337)
public abstract class MixinItemStack {
    
    @Shadow
    public int stackSize;
    
    @Shadow
    private Item item;
    
    @Shadow
    private int itemDamage;
    
    /**
     * @author LX_Gaming
     * @reason Fix NPE
     */
    @Overwrite
    public String toString() {
        if (this.item != null) {
            return this.stackSize + "x" + this.item.getUnlocalizedName() + "@" + this.itemDamage;
        }
        
        return this.stackSize + "xUnknown@" + this.itemDamage;
    }
}