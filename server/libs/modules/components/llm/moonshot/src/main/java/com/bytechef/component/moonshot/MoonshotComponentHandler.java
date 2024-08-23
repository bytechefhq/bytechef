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

package com.bytechef.component.moonshot;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.moonshot.constant.MoonshotConstants.MOONSHOT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.moonshot.action.MoonshotChatAction;
import com.bytechef.component.moonshot.connection.MoonshotConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MoonshotComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MOONSHOT)
        .title("MoonshotAI")
        .description(
            "探索月之暗面，寻求将能源转化为智能的最优解.")
        .icon("path:assets/moonshot.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(MoonshotConnection.CONNECTION_DEFINITION)
        .actions(MoonshotChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
