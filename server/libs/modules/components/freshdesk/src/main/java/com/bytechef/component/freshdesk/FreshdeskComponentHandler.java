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

package com.bytechef.component.freshdesk;

import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.freshdesk.constant.FreshdeskConstants.DOMAIN;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDSL.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableConnectionDefinition;
import com.bytechef.component.definition.ComponentDSL.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableObjectProperty;
import com.bytechef.component.definition.ComponentDSL.ModifiableProperty;
import com.bytechef.definition.BaseProperty;
import com.google.auto.service.AutoService;
import java.util.Objects;

/**
 * @author Monika Domiter
 */
@AutoService(OpenApiComponentHandler.class)
public class FreshdeskComponentHandler extends AbstractFreshdeskComponentHandler {

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .customAction(true)
            .icon("path:assets/freshdesk.svg");
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .authorizations(
                authorization(
                    AuthorizationType.BASIC_AUTH.toLowerCase(), AuthorizationType.BASIC_AUTH)
                        .title("Basic Auth")
                        .properties(
                            string(DOMAIN)
                                .label("Domain")
                                .description(
                                    "Your helpdesk domain name, e.g. https://{your_domain}.freshdesk.com/api/v2")
                                .required(true),
                            string(USERNAME)
                                .label("API key")
                                .required(true)))
            .baseUri((connectionParameters, context) -> "https://" + connectionParameters.getRequiredString(DOMAIN)
                + ".freshdesk.com/api/v2");
    }

    @Override
    public ModifiableProperty<?>
        modifyProperty(ActionDefinition actionDefinition, ModifiableProperty<?> modifiableProperty) {

        if (Objects.equals(actionDefinition.getName(), "createTicket")) {

            for (BaseProperty baseProperty : ((ModifiableObjectProperty) modifiableProperty).getProperties()
                .get()) {
                if (Objects.equals(baseProperty.getName(), "priority")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .options(
                            option("Low", 1),
                            option("Medium", 2),
                            option("High", 3),
                            option("Urgent", 4))
                        .defaultValue(1);
                } else if (Objects.equals(baseProperty.getName(), "status")) {
                    ((ModifiableIntegerProperty) baseProperty)
                        .options(
                            option("Open", 2),
                            option("Pending", 3),
                            option("Resolved", 4),
                            option("Closed", 5))
                        .defaultValue(2);
                }
            }
        }

        return modifiableProperty;
    }
}
