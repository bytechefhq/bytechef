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

package com.bytechef.component.wolfram.alpha.full.results;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.wolfram.alpha.full.results.action.WolframAlphaFullResultsGetFullResultAction;
import com.bytechef.component.wolfram.alpha.full.results.connection.WolframAlphaFullResultsConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractWolframAlphaFullResultsComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("wolfram-alpha-full-results")
            .title("Wolfram Alpha Full Results")
            .description(
                "Wolfram Alpha Full Results returns the computed results of your query in a variety of formats."))
                    .actions(modifyActions(WolframAlphaFullResultsGetFullResultAction.ACTION_DEFINITION))
                    .connection(modifyConnection(WolframAlphaFullResultsConnection.CONNECTION_DEFINITION))
                    .clusterElements(
                        modifyClusterElements(tool(WolframAlphaFullResultsGetFullResultAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
