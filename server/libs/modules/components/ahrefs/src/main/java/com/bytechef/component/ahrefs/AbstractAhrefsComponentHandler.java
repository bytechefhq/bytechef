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

package com.bytechef.component.ahrefs;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.ahrefs.action.AhrefsGetMetricsAction;
import com.bytechef.component.ahrefs.action.AhrefsGetPageContentAction;
import com.bytechef.component.ahrefs.action.AhrefsGetSubscriptionInfoAction;
import com.bytechef.component.ahrefs.connection.AhrefsConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractAhrefsComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("ahrefs")
            .title("Ahrefs")
            .description(
                "Ahrefs is a comprehensive suite of SEO (Search Engine Optimization) tools used by digital marketers and businesses to improve their website's visibility in search engine results."))
                    .actions(modifyActions(AhrefsGetMetricsAction.ACTION_DEFINITION,
                        AhrefsGetSubscriptionInfoAction.ACTION_DEFINITION,
                        AhrefsGetPageContentAction.ACTION_DEFINITION))
                    .connection(modifyConnection(AhrefsConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(AhrefsGetMetricsAction.ACTION_DEFINITION),
                        tool(AhrefsGetSubscriptionInfoAction.ACTION_DEFINITION),
                        tool(AhrefsGetPageContentAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
