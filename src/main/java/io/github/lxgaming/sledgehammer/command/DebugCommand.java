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

package io.github.lxgaming.sledgehammer.command;

import io.github.lxgaming.sledgehammer.Sledgehammer;
import io.github.lxgaming.sledgehammer.configuration.Config;
import io.github.lxgaming.sledgehammer.configuration.category.GeneralCategory;
import io.github.lxgaming.sledgehammer.util.Toolbox;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.List;

public class DebugCommand extends AbstractCommand {
    
    public DebugCommand() {
        addAlias("debug");
        setPermission("sledgehammer.debug");
    }
    
    @Override
    public CommandResult execute(CommandSource commandSource, List<String> arguments) {
        GeneralCategory generalCategory = Sledgehammer.getInstance().getConfig().map(Config::getGeneralCategory).orElse(null);
        if (generalCategory == null) {
            commandSource.sendMessage(Text.of(Toolbox.getTextPrefix(), TextColors.RED, "Configuration error"));
            return CommandResult.empty();
        }
        
        if (generalCategory.isDebug()) {
            generalCategory.setDebug(false);
            commandSource.sendMessage(Text.of(Toolbox.getTextPrefix(), TextColors.RED, "Debugging disabled"));
        } else {
            generalCategory.setDebug(true);
            commandSource.sendMessage(Text.of(Toolbox.getTextPrefix(), TextColors.GREEN, "Debugging enabled"));
        }
        
        return CommandResult.success();
    }
}