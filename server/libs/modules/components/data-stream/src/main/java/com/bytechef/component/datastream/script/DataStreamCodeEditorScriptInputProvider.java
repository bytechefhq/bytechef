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

package com.bytechef.component.datastream.script;

import static com.bytechef.component.datastream.constant.DataStreamConstants.DATA_STREAM;

import com.bytechef.component.datastream.item.ItemStreamExecutionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Provides script input values for DataStream cluster elements by reading the first record from the SOURCE ItemReader.
 *
 * @author Ivica Cardic
 */
@Component
public class DataStreamCodeEditorScriptInputProvider implements CodeEditorScriptInputProvider {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ContextFactory contextFactory;

    public DataStreamCodeEditorScriptInputProvider(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.contextFactory = contextFactory;
    }

    @Override
    public String getRootComponentName() {
        return DATA_STREAM;
    }

    @Override
    public Map<String, Object> getScriptInput(
        int rootComponentVersion, String sourceComponentName, int sourceComponentVersion,
        String sourceClusterElementName, Map<String, ?> sourceInputParameters,
        @Nullable ComponentConnection sourceComponentConnection) {

        ItemReader itemReader = clusterElementDefinitionService.getClusterElement(
            sourceComponentName, sourceComponentVersion, sourceClusterElementName);

        Parameters inputParameters = ParametersFactory.create(sourceInputParameters);

        Parameters connectionParameters = sourceComponentConnection == null
            ? ParametersFactory.create(Map.of())
            : ParametersFactory.create(sourceComponentConnection.getParameters());

        ClusterElementContext clusterElementContext = contextFactory.createClusterElementContext(
            sourceComponentName, sourceComponentVersion, sourceClusterElementName, sourceComponentConnection, true);

        ItemStreamExecutionContext executionContext = new ItemStreamExecutionContext(new ExecutionContext());

        try {
            itemReader.open(inputParameters, connectionParameters, clusterElementContext, executionContext);

            Map<String, Object> firstRecord = itemReader.read();

            if (firstRecord == null) {
                return Map.of("item", Map.of());
            }

            return Map.of("item", firstRecord);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read first record from ItemReader", exception);
        } finally {
            itemReader.close();
        }
    }
}
