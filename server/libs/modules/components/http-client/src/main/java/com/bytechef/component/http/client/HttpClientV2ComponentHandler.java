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

package com.bytechef.component.http.client;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.http.client.action.HttpClientV2DeleteAction;
import com.bytechef.component.http.client.action.HttpClientV2GetAction;
import com.bytechef.component.http.client.action.HttpClientV2HeadAction;
import com.bytechef.component.http.client.action.HttpClientV2PatchAction;
import com.bytechef.component.http.client.action.HttpClientV2PostAction;
import com.bytechef.component.http.client.action.HttpClientV2PutAction;
import com.bytechef.component.http.client.connection.HttpClientConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class HttpClientV2ComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("httpClient")
        .title("HTTP Client")
        .description("Makes an HTTP request and returns the response data.")
        .icon("path:assets/http-client.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(HttpClientConnection.CONNECTION_DEFINITION)
        .actions(
            HttpClientV2GetAction.ACTION_DEFINITION,
            HttpClientV2PostAction.ACTION_DEFINITION,
            HttpClientV2PutAction.ACTION_DEFINITION,
            HttpClientV2PatchAction.ACTION_DEFINITION,
            HttpClientV2DeleteAction.ACTION_DEFINITION,
            HttpClientV2HeadAction.ACTION_DEFINITION)
        .clusterElements(
            tool(HttpClientV2DeleteAction.ACTION_DEFINITION),
            tool(HttpClientV2GetAction.ACTION_DEFINITION),
            tool(HttpClientV2HeadAction.ACTION_DEFINITION),
            tool(HttpClientV2PatchAction.ACTION_DEFINITION),
            tool(HttpClientV2PostAction.ACTION_DEFINITION),
            tool(HttpClientV2PutAction.ACTION_DEFINITION))
        .version(2);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
