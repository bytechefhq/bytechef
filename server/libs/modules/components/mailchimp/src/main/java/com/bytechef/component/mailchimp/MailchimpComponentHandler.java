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

package com.bytechef.component.mailchimp;

import static com.bytechef.component.definition.Authorization.ACCESS_TOKEN;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.mailchimp.trigger.MailchimpSubscribeTrigger;
import com.bytechef.component.mailchimp.util.MailchimpUtils;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@AutoService(OpenApiComponentHandler.class)
public class MailchimpComponentHandler extends AbstractMailchimpComponentHandler {

    @Override
    public List<ModifiableTriggerDefinition> getTriggers() {
        return List.of(MailchimpSubscribeTrigger.TRIGGER_DEFINITION);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/mailchimp.svg")
            .categories(ComponentCategory.MARKETING_AUTOMATION);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters, context) -> "https://%s.api.mailchimp.com/3.0".formatted(
                MailchimpUtils.getMailChimpServer(connectionParameters.getRequiredString(ACCESS_TOKEN), context)));
    }
}
