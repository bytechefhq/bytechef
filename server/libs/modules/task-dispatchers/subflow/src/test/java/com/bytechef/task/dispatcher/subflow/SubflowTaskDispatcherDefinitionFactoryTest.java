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

package com.bytechef.task.dispatcher.subflow;

import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.task.dispatcher.registry.SubWorkflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.SubWorkflowEntry;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class SubflowTaskDispatcherDefinitionFactoryTest {

    @Test
    public void testGetTaskDispatcherDefinition() {
        SubWorkflowDataSource subWorkflowDataSource = new SubWorkflowDataSource() {

            @Override
            public OutputResponse getSubWorkflowInputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public OutputResponse getSubWorkflowOutputSchema(String workflowUuid) {
                return null;
            }

            @Override
            public List<SubWorkflowEntry> getSubWorkflows(PlatformType platformType, String search) {
                return List.of();
            }
        };

        JsonFileAssert.assertEquals(
            "definition/subflow_v1.json",
            new SubflowTaskDispatcherDefinitionFactory(subWorkflowDataSource).getDefinition());
    }
}
