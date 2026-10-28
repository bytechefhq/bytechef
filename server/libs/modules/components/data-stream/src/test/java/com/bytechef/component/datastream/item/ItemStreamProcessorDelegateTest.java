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

import static com.bytechef.component.datastream.constant.DataStreamConstants.CLUSTER_ELEMENT_NAME;
import static com.bytechef.component.datastream.constant.DataStreamConstants.INPUT_PARAMETERS;
import static com.bytechef.component.datastream.constant.DataStreamConstants.TENANT_ID;
import static com.bytechef.platform.component.definition.datastream.ItemProcessor.PROCESSOR;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_NAME;
import static com.bytechef.platform.configuration.constant.WorkflowExtConstants.COMPONENT_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.definition.datastream.ItemProcessor;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ItemStreamProcessorDelegateTest {

    private final ClusterElementDefinitionService clusterElementDefinitionService =
        mock(ClusterElementDefinitionService.class);
    private ItemStreamProcessorDelegate itemStreamProcessorDelegate;
    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        itemStreamProcessorDelegate = new ItemStreamProcessorDelegate(
            clusterElementDefinitionService, mock(ContextFactory.class));

        Map<String, Object> processorValue = Map.of(
            COMPONENT_NAME, "script", COMPONENT_VERSION, 1, CLUSTER_ELEMENT_NAME, "javaScript", INPUT_PARAMETERS,
            Map.of());

        JobParameters jobParameters = new JobParameters(
            Set.of(
                new JobParameter<>(PROCESSOR.name(), processorValue, Map.class),
                new JobParameter<>(TENANT_ID, "public", String.class),
                new JobParameter<>(MetadataConstants.EDITOR_ENVIRONMENT, false, Boolean.class)));

        stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
    }

    @Test
    void testProcessPassesItemThroughWhenProcessorResolvesToNull() throws Exception {
        when(clusterElementDefinitionService.getClusterElement("script", 1, "javaScript")).thenReturn(null);

        itemStreamProcessorDelegate.beforeStep(stepExecution);

        assertThat(itemStreamProcessorDelegate.process(Map.of("name", "john"))).isEqualTo(Map.of("name", "john"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testProcessDelegatesToResolvedProcessor() throws Exception {
        ItemProcessor<Object, Object> itemProcessor = mock(ItemProcessor.class);

        when(clusterElementDefinitionService.getClusterElement("script", 1, "javaScript")).thenReturn(itemProcessor);
        when(itemProcessor.process(eq(Map.of("name", "john")), any(), any(), any()))
            .thenReturn(Map.of("name", "JOHN"));

        itemStreamProcessorDelegate.beforeStep(stepExecution);

        assertThat(itemStreamProcessorDelegate.process(Map.of("name", "john"))).isEqualTo(Map.of("name", "JOHN"));
    }
}
