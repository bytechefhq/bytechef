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

package com.bytechef.component.ai.agent.utils;

import com.bytechef.component.definition.ClusterElementDefinition;

/**
 * Contributes an additional cluster element to the {@code aiAgentUtils} component. Implementations are discovered as
 * Spring beans and appended to the component's built-in cluster elements, allowing edition-specific tools (e.g. the EE
 * skills tool) to be plugged in without the core component depending on them.
 *
 * @author Ivica Cardic
 */
public interface AiAgentUtilsClusterElementContributor {

    ClusterElementDefinition<?> getClusterElementDefinition();
}
