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
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import java.util.List;

/**
 * Common interface for guardrail component definitions. Guardrails declare no child cluster types of their own — the
 * parent {@code CheckForViolations} or {@code SanitizeText} cluster owns any shared {@code MODEL} child.
 *
 * @author Ivica Cardic
 */
public interface GuardrailComponentDefinition extends ClusterRootComponentDefinition {

    List<ClusterElementType> NO_CHILD_TYPES = List.of();

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return NO_CHILD_TYPES;
    }

    /**
     * Whether this guardrail needs its parent cluster root to wire a {@code MODEL} child.
     */
    boolean requiresModel();

    static void enforceNoChildTypes(GuardrailComponentDefinition definition) {
        List<ClusterElementType> types = definition.getClusterElementTypes();

        if (types != NO_CHILD_TYPES) {
            throw new IllegalStateException(
                "'" + definition.getName() + "' must not declare child cluster element types");
        }
    }
}
