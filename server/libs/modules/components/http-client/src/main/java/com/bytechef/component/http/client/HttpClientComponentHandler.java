/*
 * Copyright 2023-present ByteChef Inc.
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

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.http.client.action.HttpClientDeleteAction;
import com.bytechef.component.http.client.action.HttpClientGetAction;
import com.bytechef.component.http.client.action.HttpClientHeadAction;
import com.bytechef.component.http.client.action.HttpClientPatchAction;
import com.bytechef.component.http.client.action.HttpClientPostAction;
import com.bytechef.component.http.client.action.HttpClientPutAction;
import com.bytechef.component.http.client.connection.HttpClientConnection;
import com.bytechef.component.http.client.constant.HttpClientConstants;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class HttpClientComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(HttpClientConstants.HTTP_CLIENT)
        .title("HTTP Client")
        .description("Makes an HTTP request and returns the response data.")
        .icon("path:assets/http-client.svg")
        .connection(HttpClientConnection.CONNECTION_DEFINITION)
        .actions(
            HttpClientGetAction.ACTION_DEFINITION,
            HttpClientPostAction.ACTION_DEFINITION,
            HttpClientPutAction.ACTION_DEFINITION,
            HttpClientPatchAction.ACTION_DEFINITION,
            HttpClientDeleteAction.ACTION_DEFINITION,
            HttpClientHeadAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}
