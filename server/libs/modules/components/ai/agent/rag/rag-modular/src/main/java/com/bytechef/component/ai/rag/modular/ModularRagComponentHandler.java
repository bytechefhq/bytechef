/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.rag.modular;

import static com.bytechef.component.ai.rag.modular.ModularRagComponentHandler.MODULAR_RAG;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.rag.modular.cluster.ModularRag;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ModularRagComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(MODULAR_RAG + "_v1_ComponentHandler")
public class ModularRagComponentHandler implements ComponentHandler {

    public static final String MODULAR_RAG = "modularRag";

    private final ModularRagComponentDefinition componentDefinition;

    public ModularRagComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new ModularRagComponentDefinitionImpl(
            component(MODULAR_RAG)
                .title("Modular RAG")
                .description(
                    "A modular RAG (Retrieval-Augmented Generation) component that provides a flexible and customizable approach to building RAG systems. It allows you to combine different retrieval, augmentation, and generation strategies into a cohesive pipeline for enhanced AI-driven information processing and response generation.")
                .icon("path:assets/modular-rag.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new ModularRag(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ModularRagComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ModularRagComponentDefinition {

        public ModularRagComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
