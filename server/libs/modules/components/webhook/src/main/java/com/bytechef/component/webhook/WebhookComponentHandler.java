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

package com.bytechef.component.webhook;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.webhook.constant.WebhookConstants;
import com.bytechef.component.webhook.trigger.WebhookAutoRespondWithHTTP200Trigger;
import com.bytechef.component.webhook.trigger.WebhookAwaitWorkflowAndRespondTrigger;
import com.bytechef.component.webhook.trigger.WebhookValidateAndRespondTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class WebhookComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(WebhookConstants.WEBHOOK)
        .title("Webhook")
        .description(
            "Webhook is a method utilized by applications to supply real-time information to other apps. Such a process usually delivers data immediately as and when it occurs. Webhook Trigger enables users to receive callouts whenever a service provides the option of distributing signals to a user-defined URL.")
        .icon("path:assets/webhook.svg")
        .categories(ComponentCategory.HELPERS)
        .triggers(
            WebhookAutoRespondWithHTTP200Trigger.TRIGGER_DEFINITION,
            WebhookValidateAndRespondTrigger.TRIGGER_DEFINITION,
            WebhookAwaitWorkflowAndRespondTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
