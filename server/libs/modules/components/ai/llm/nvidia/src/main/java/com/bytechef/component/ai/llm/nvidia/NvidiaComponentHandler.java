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

package com.bytechef.component.ai.llm.nvidia;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.nvidia.action.NvidiaChatAction;
import com.bytechef.component.ai.llm.nvidia.connection.NvidiaConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class NvidiaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("nvidia")
        .title("NVIDIA LLM")
        .description(
            "Generative AI and digitalization are reshaping the $3 trillion automotive industry, from design and " +
                "engineering to manufacturing, autonomous driving, and customer experience. NVIDIA is at the " +
                "epicenter of this industrial transformation.")
        .icon("path:assets/nvidia.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(NvidiaConnection.CONNECTION_DEFINITION)
        .actions(NvidiaChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
