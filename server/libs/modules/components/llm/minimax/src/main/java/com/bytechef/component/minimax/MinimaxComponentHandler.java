/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.minimax;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.minimax.constant.MinimaxConstants.MINIMAX;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.minimax.action.MinimaxChatAction;
import com.bytechef.component.minimax.connection.MinimaxConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MinimaxComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MINIMAX)
        .title("Minimax")
        .description(
            "MiniMax 是领先的通用人工智能科技公司，致力于与用户共创智能。")
        .icon("path:assets/minimax.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(MinimaxConnection.CONNECTION_DEFINITION)
        .actions(MinimaxChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
