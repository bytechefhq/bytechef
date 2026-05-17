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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import java.util.List;

/**
 * Recommended base class for guardrail component definitions — pins {@code getClusterElementTypes()} as {@code final}
 * so the no-child-types invariant cannot be accidentally overridden.
 *
 * @author Ivica Cardic
 */
public abstract class AbstractGuardrailComponentDefinition extends AbstractComponentDefinitionWrapper
    implements GuardrailComponentDefinition {

    protected AbstractGuardrailComponentDefinition(ComponentDefinition componentDefinition) {
        super(componentDefinition);
    }

    @Override
    public final List<ClusterElementType> getClusterElementTypes() {
        return NO_CHILD_TYPES;
    }
}
