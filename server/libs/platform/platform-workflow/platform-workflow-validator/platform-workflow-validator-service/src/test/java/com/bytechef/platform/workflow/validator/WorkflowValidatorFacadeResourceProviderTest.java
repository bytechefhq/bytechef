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

package com.bytechef.platform.workflow.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.definition.BaseProperty.ResourceType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowValidatorFacadeResourceProviderTest {

    @Test
    void dispatchesToResolverForTheResourceTypeWithEnvironment() {
        ResourceReferenceResolver dataTableResolver = new ResourceReferenceResolver() {

            @Override
            public ResourceType getResourceType() {
                return ResourceType.DATA_TABLE;
            }

            @Override
            public String findProblem(String reference, long environmentId) {
                return reference + "@" + environmentId;
            }
        };

        WorkflowValidator.ResourceReferenceProvider provider =
            WorkflowValidatorFacadeImpl.createResourceReferenceProvider(
                Map.of(ResourceType.DATA_TABLE, dataTableResolver), 2L);

        assertEquals("conversations@2", provider.findProblem("DATA_TABLE", "conversations"));
    }

    @Test
    void unknownResourceTypeResolvesToNoProblem() {
        WorkflowValidator.ResourceReferenceProvider provider =
            WorkflowValidatorFacadeImpl.createResourceReferenceProvider(Map.of(), 0L);

        assertNull(provider.findProblem("KNOWLEDGE_BASE", "7"));
    }
}
