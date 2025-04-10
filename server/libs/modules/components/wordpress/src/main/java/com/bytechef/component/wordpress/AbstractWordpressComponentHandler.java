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

package com.bytechef.component.wordpress;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.wordpress.action.WordpressCreatePageAction;
import com.bytechef.component.wordpress.action.WordpressCreatePostAction;
import com.bytechef.component.wordpress.action.WordpressGetPostAction;
import com.bytechef.component.wordpress.action.WordpressUpdatePostAction;
import com.bytechef.component.wordpress.connection.WordpressConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractWordpressComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("wordpress")
            .title("Wordpress")
            .description("WordPress is a web content management system."))
                .actions(
                    modifyActions(WordpressGetPostAction.ACTION_DEFINITION, WordpressUpdatePostAction.ACTION_DEFINITION,
                        WordpressCreatePostAction.ACTION_DEFINITION, WordpressCreatePageAction.ACTION_DEFINITION))
                .connection(modifyConnection(WordpressConnection.CONNECTION_DEFINITION))
                .clusterElements(modifyClusterElements(tool(WordpressGetPostAction.ACTION_DEFINITION),
                    tool(WordpressUpdatePostAction.ACTION_DEFINITION),
                    tool(WordpressCreatePostAction.ACTION_DEFINITION),
                    tool(WordpressCreatePageAction.ACTION_DEFINITION)))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
