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

package com.bytechef.component.ai.agent.guardrails.checkforviolations;

import static com.bytechef.component.ai.agent.guardrails.checkforviolations.CheckForViolationsComponentHandler.CHECK_FOR_VIOLATIONS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.checkforviolations.cluster.CheckForViolations;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.CheckForViolationsComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CHECK_FOR_VIOLATIONS + "_v1_ComponentHandler")
public class CheckForViolationsComponentHandler implements ComponentHandler {

    public static final String CHECK_FOR_VIOLATIONS = "checkForViolations";

    private final CheckForViolationsComponentDefinition componentDefinition;

    public CheckForViolationsComponentHandler(CheckForViolations checkForViolations) {
        this.componentDefinition = new CheckForViolationsComponentDefinitionImpl(
            component(CHECK_FOR_VIOLATIONS)
                .title("Check for Violations")
                .description("Runs configured guardrail checks on the user prompt.")
                .icon("path:assets/check-for-violations.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(checkForViolations.of()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class CheckForViolationsComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements CheckForViolationsComponentDefinition {

        public CheckForViolationsComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
