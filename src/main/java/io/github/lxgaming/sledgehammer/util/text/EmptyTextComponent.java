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

package io.github.lxgaming.sledgehammer.util.text;

import com.google.common.base.MoreObjects;
import net.minecraft.util.text.StringTextComponent;

public final class EmptyTextComponent extends StringTextComponent {
    
    public EmptyTextComponent() {
        this("");
    }
    
    private EmptyTextComponent(String text) {
        super(text);
    }
    
    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
                .add("siblings", getSiblings())
                .add("style", getStyle())
                .toString();
    }
}