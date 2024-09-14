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

package com.bytechef.component.active.campaign;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.active.campaign.action.ActiveCampaignCreateAccountAction;
import com.bytechef.component.active.campaign.action.ActiveCampaignCreateContactAction;
import com.bytechef.component.active.campaign.action.ActiveCampaignCreateTaskAction;
import com.bytechef.component.active.campaign.connection.ActiveCampaignConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractActiveCampaignComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("active-campaign")
            .title("ActiveCampaign")
            .description(
                "ActiveCampaign is a customer experience automation platform that offers email marketing, marketing automation, sales automation, and CRM tools."))
                    .actions(modifyActions(ActiveCampaignCreateAccountAction.ACTION_DEFINITION,
                        ActiveCampaignCreateContactAction.ACTION_DEFINITION,
                        ActiveCampaignCreateTaskAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ActiveCampaignConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
