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

package com.bytechef.component.ai.agent.guardrails.secretkeys;

import static com.bytechef.component.ai.agent.guardrails.secretkeys.SecretKeysComponentHandler.SECRET_KEYS;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.guardrails.secretkeys.cluster.SecretKeys;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(SECRET_KEYS + "_v1_ComponentHandler")
public class SecretKeysComponentHandler implements ComponentHandler {

    public static final String SECRET_KEYS = "secretKeys";

    private final GuardrailComponentDefinition componentDefinition;

    public SecretKeysComponentHandler() {
        this.componentDefinition = new SecretKeysComponentDefinitionImpl(
            component(SECRET_KEYS)
                .title("Secret Keys")
                .description("Detects — and optionally masks — API keys and credential-shaped secrets.")
                .icon("path:assets/secret-keys.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(
                    SecretKeys.ofCheck(),
                    SecretKeys.ofSanitize()));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class SecretKeysComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements GuardrailComponentDefinition {

        public SecretKeysComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }

        @Override
        public boolean requiresModel() {
            return false;
        }
    }
}
