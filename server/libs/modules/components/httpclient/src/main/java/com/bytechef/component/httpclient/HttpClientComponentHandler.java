
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.httpclient;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.HTTP_CLIENT;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.httpclient.action.HttpClientDeleteAction;
import com.bytechef.component.httpclient.action.HttpClientGetAction;
import com.bytechef.component.httpclient.action.HttpClientHeadAction;
import com.bytechef.component.httpclient.action.HttpClientPatchAction;
import com.bytechef.component.httpclient.action.HttpClientPostAction;
import com.bytechef.component.httpclient.action.HttpClientPutAction;
import com.bytechef.component.httpclient.connection.HttpClientConnection;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class HttpClientComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(HTTP_CLIENT)
        .display(display("HTTP Client").description("Makes an HTTP request and returns the response data."))
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
