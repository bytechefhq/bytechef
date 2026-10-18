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

package com.bytechef.component.ai.rag.modular.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentPostProcessorFunction;
import com.bytechef.platform.component.definition.ai.agent.rag.DocumentRetrieverFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ModularRagTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);

    @Test
    void testApplyAddsDocumentPostProcessorsInOrder() throws Exception {
        List<String> appliedDocumentPostProcessors = new ArrayList<>();

        DocumentPostProcessor filter = (query, documents) -> {
            appliedDocumentPostProcessors.add("filter");

            return documents;
        };
        DocumentPostProcessor reranker = (query, documents) -> {
            appliedDocumentPostProcessors.add("reranker");

            return documents;
        };

        DocumentRetrieverFunction documentRetrieverFunction =
            (inputParameters, connectionParameters, extensions, componentConnections) -> query -> List.of();

        when(clusterElementDefinitionService.getClusterElement(eq("vectorStore"), eq(1), eq("retriever")))
            .thenReturn(documentRetrieverFunction);
        when(clusterElementDefinitionService.getClusterElement(eq("typesafe"), eq(1), eq("jevDocumentFilter")))
            .thenReturn((DocumentPostProcessorFunction) (inputParameters, connectionParameters) -> filter);
        when(clusterElementDefinitionService.getClusterElement(eq("typesafe"), eq(1), eq("jevDocumentReranker")))
            .thenReturn((DocumentPostProcessorFunction) (inputParameters, connectionParameters) -> reranker);

        Map<String, Object> extensions = Map.of(
            "clusterElements", Map.of(
                "documentRetriever", element("retriever_1", "vectorStore/v1/retriever"),
                "documentPostProcessor", List.of(
                    element("jevDocumentFilter_1", "typesafe/v1/jevDocumentFilter"),
                    element("jevDocumentReranker_1", "typesafe/v1/jevDocumentReranker"))));

        ModularRag modularRag = new ModularRag(clusterElementDefinitionService);

        RetrievalAugmentationAdvisor retrievalAugmentationAdvisor =
            modularRag.apply(ParametersFactory.create(Map.of()), null, ParametersFactory.create(extensions), Map.of());

        ChatClientRequest chatClientRequest = ChatClientRequest.builder()
            .prompt(new Prompt("Where did Milton make landfall?"))
            .build();

        retrievalAugmentationAdvisor.before(chatClientRequest, null);

        assertEquals(List.of("filter", "reranker"), appliedDocumentPostProcessors);
    }

    private static Map<String, Object> element(String name, String type) {
        return Map.of("name", name, "type", type, "parameters", Map.of());
    }
}
