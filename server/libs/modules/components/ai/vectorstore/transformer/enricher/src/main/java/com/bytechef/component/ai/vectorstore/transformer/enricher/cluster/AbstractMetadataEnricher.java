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

package com.bytechef.component.ai.vectorstore.transformer.enricher.cluster;

import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractMetadataEnricher {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    protected AbstractMetadataEnricher(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    protected ChatModel getModel(
        Parameters extensions, Map<String, ComponentConnection> componentConnections) {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(MODEL);

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        try {
            return (ChatModel) modelFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                ParametersFactory.create(componentConnection.getParameters()), false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
