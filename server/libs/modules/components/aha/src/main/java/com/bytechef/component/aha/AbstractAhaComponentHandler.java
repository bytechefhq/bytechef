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

package com.bytechef.component.aha;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.aha.action.AhaCreateFeatureAction;
import com.bytechef.component.aha.action.AhaCreateIdeaAction;
import com.bytechef.component.aha.connection.AhaConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAhaComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("aha")
            .title("Aha!")
            .description(
                "Aha! is a comprehensive product management software platform that helps teams set strategy, capture ideas, and plan, prioritize, and track work to build products customers love."))
                    .actions(
                        modifyActions(AhaCreateFeatureAction.ACTION_DEFINITION, AhaCreateIdeaAction.ACTION_DEFINITION))
                    .connection(modifyConnection(AhaConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
