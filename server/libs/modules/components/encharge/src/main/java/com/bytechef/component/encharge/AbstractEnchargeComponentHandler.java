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

package com.bytechef.component.encharge;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.encharge.action.EnchargeAddTagAction;
import com.bytechef.component.encharge.action.EnchargeCreateEmailAction;
import com.bytechef.component.encharge.action.EnchargeCreatePeopleAction;
import com.bytechef.component.encharge.connection.EnchargeConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractEnchargeComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("encharge")
            .title("Encharge")
            .description(
                "Encharge is a marketing automation platform that helps businesses automate their customer communication and marketing campaigns."))
                    .actions(modifyActions(EnchargeCreateEmailAction.ACTION_DEFINITION,
                        EnchargeCreatePeopleAction.ACTION_DEFINITION, EnchargeAddTagAction.ACTION_DEFINITION))
                    .connection(modifyConnection(EnchargeConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
