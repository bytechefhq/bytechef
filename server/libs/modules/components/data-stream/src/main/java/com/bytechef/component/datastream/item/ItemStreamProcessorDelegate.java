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

package com.bytechef.component.datastream.item;

import static com.bytechef.platform.component.definition.datastream.ItemProcessor.PROCESSOR;

import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.datastream.ItemProcessor;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.step.StepExecution;

/**
 * @author Ivica Cardic
 */
public class ItemStreamProcessorDelegate extends AbstractItemStreamDelegate
    implements org.springframework.batch.infrastructure.item.ItemProcessor<Map<String, Object>, Map<String, Object>> {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private @Nullable ItemProcessor<Object, Object> itemProcessor;

    public ItemStreamProcessorDelegate(
        ClusterElementDefinitionService clusterElementDefinitionService, ContextFactory contextFactory) {

        super(PROCESSOR, contextFactory);

        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> item) throws Exception {
        if (itemProcessor != null) {
            item = itemProcessor.process(item, inputParameters, connectionParameters, clusterElementContext);
        }

        return item;
    }

    @Override
    protected void doBeforeStep(StepExecution stepExecution) {
        itemProcessor = clusterElementDefinitionService.getClusterElement(
            componentName, componentVersion, clusterElementName);
    }
}
