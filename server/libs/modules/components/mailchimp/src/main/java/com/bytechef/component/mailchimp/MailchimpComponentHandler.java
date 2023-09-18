
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

package com.bytechef.component.mailchimp;

import com.bytechef.component.mailchimp.trigger.MailchimpSubscribeTrigger;
import com.bytechef.component.mailchimp.util.MailchimpUtils;
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableTriggerDefinition;

import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty;
import com.bytechef.hermes.definition.DefinitionDSL.ModifiableProperty.ModifiableStringProperty;
import com.google.auto.service.AutoService;

import java.util.List;
import java.util.Objects;

import static com.bytechef.hermes.component.definition.constant.AuthorizationConstants.ACCESS_TOKEN;

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
            .icon("path:assets/mailchimp.svg");
    }

    @Override
    public ModifiableConnectionDefinition
        modifyConnection(ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .baseUri((connectionParameters) -> "https://%s.api.mailchimp.com/3.0".formatted(
                MailchimpUtils.getMailChimpServer(connectionParameters.getRequiredString(ACCESS_TOKEN))));
    }

    @Override
    public ModifiableProperty<?> modifyProperty(
        ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(modifiableProperty.getName(), "listId")) {
            ((ModifiableStringProperty) modifiableProperty).options(MailchimpUtils.getListIdOptions());
        }

        return modifiableProperty;
    }
}
