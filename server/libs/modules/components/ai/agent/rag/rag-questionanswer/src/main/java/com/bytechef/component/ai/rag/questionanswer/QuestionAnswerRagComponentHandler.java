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

package com.bytechef.component.ai.rag.questionanswer;

import static com.bytechef.component.ai.rag.questionanswer.QuestionAnswerRagComponentHandler.QUESTION_ANSWER_RAG;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.rag.questionanswer.cluster.QuestionAnswerRag;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.QuestionAnswerRagComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(QUESTION_ANSWER_RAG + "_v1_ComponentHandler")
public class QuestionAnswerRagComponentHandler implements ComponentHandler {

    public static final String QUESTION_ANSWER_RAG = "questionAnswerRag";

    private final QuestionAnswerRagComponentDefinition componentDefinition;

    public QuestionAnswerRagComponentHandler(@Lazy ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new ModularRagComponentDefinitionImpl(
            component(QUESTION_ANSWER_RAG)
                .title("Modular RAG")
                .description(
                    "Retrieval Augmentation is a powerful tool that allows you to retrieve relevant information " +
                        "from a vector database, such as Pinecone, and use it to enhance your AI agent's responses.")
                .icon("path:assets/rag.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    new QuestionAnswerRag(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ModularRagComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements QuestionAnswerRagComponentDefinition {

        public ModularRagComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
