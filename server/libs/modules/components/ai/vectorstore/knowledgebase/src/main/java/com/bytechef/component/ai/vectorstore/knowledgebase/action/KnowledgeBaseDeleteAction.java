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

package com.bytechef.component.ai.vectorstore.knowledgebase.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.METADATA_FILTER_PROPERTY;
import static com.bytechef.component.ai.vectorstore.knowledgebase.constant.KnowledgeBaseVectorStoreConstants.KNOWLEDGE_BASE_ID;
import static com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseVectorStore.createVectorStore;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.platform.component.definition.VectorStoreComponentDefinition.DELETE;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.ai.vectorstore.knowledgebase.util.KnowledgeBaseOptionsUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.owner.OwnerResolution;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseService;
import com.bytechef.platform.owner.OwnerResolver;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Marko Kriskovic
 */
public final class KnowledgeBaseDeleteAction {

    private KnowledgeBaseDeleteAction() {
    }

    public static ActionDefinition of(
        org.springframework.ai.vectorstore.VectorStore vectorStore, KnowledgeBaseService knowledgeBaseService,
        ObjectProvider<OwnerResolver> ownerResolverProvider) {

        VectorStore kbVectorStore = createVectorStore(vectorStore);

        return action(DELETE)
            .title("Delete Documents")
            .description("Delete documents from the knowledge base by metadata filter.")
            .properties(
                integer(KNOWLEDGE_BASE_ID)
                    .label("Knowledge Base")
                    .description("The knowledge base to delete documents from.")
                    .options(KnowledgeBaseOptionsUtils.knowledgeBaseActionOptions(
                        knowledgeBaseService, ownerResolverProvider))
                    .required(true),
                METADATA_FILTER_PROPERTY)
            .perform((MultipleConnectionsPerformFunction) (
                inputParameters, componentConnections, extensions, context) -> perform(
                    inputParameters, kbVectorStore, knowledgeBaseService, ownerResolverProvider,
                    (ActionContextAware) context));
    }

    private static Object perform(
        Parameters inputParameters, VectorStore vectorStore, KnowledgeBaseService knowledgeBaseService,
        ObjectProvider<OwnerResolver> ownerResolverProvider, ActionContextAware actionContextAware) {

        knowledgeBaseService.getKnowledgeBase(
            inputParameters.getRequiredLong(KNOWLEDGE_BASE_ID),
            OwnerResolution.resolve(actionContextAware, ownerResolverProvider));

        vectorStore.delete(inputParameters, ParametersFactory.create(Map.of()), null);

        return null;
    }

}
