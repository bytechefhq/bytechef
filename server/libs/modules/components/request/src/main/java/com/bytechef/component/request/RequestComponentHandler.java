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

package com.bytechef.component.request;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.request.trigger.RequestAutoRespondWithHTTP200Trigger;
import com.bytechef.component.request.trigger.RequestAwaitWorkflowAndRespondTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RequestComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("request")
        .title("Request")
        .description(
            "Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response.")
        .icon("path:assets/request.svg")
        .categories(ComponentCategory.HELPERS)
        .triggers(
            RequestAutoRespondWithHTTP200Trigger.TRIGGER_DEFINITION,
            RequestAwaitWorkflowAndRespondTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
