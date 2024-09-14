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

package com.bytechef.component.pipedrive;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.pipedrive.action.PipedriveAddDealAction;
import com.bytechef.component.pipedrive.action.PipedriveAddLeadAction;
import com.bytechef.component.pipedrive.action.PipedriveAddOrganizationAction;
import com.bytechef.component.pipedrive.action.PipedriveAddPersonAction;
import com.bytechef.component.pipedrive.action.PipedriveDeleteDealAction;
import com.bytechef.component.pipedrive.action.PipedriveDeleteLeadAction;
import com.bytechef.component.pipedrive.action.PipedriveDeleteOrganizationAction;
import com.bytechef.component.pipedrive.action.PipedriveDeletePersonAction;
import com.bytechef.component.pipedrive.action.PipedriveGetDealDetailsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetDealsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetLeadDetailsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetLeadsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetOrganizationDetailsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetOrganizationsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetPersonDetailsAction;
import com.bytechef.component.pipedrive.action.PipedriveGetPersonsAction;
import com.bytechef.component.pipedrive.action.PipedriveSearchDealsAction;
import com.bytechef.component.pipedrive.action.PipedriveSearchLeadsAction;
import com.bytechef.component.pipedrive.action.PipedriveSearchOrganizationAction;
import com.bytechef.component.pipedrive.action.PipedriveSearchPersonsAction;
import com.bytechef.component.pipedrive.connection.PipedriveConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPipedriveComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("pipedrive")
            .title("Pipedrive")
            .description(null))
                .actions(modifyActions(PipedriveGetDealsAction.ACTION_DEFINITION,
                    PipedriveAddDealAction.ACTION_DEFINITION, PipedriveSearchDealsAction.ACTION_DEFINITION,
                    PipedriveDeleteDealAction.ACTION_DEFINITION, PipedriveGetDealDetailsAction.ACTION_DEFINITION,
                    PipedriveGetLeadsAction.ACTION_DEFINITION, PipedriveAddLeadAction.ACTION_DEFINITION,
                    PipedriveDeleteLeadAction.ACTION_DEFINITION, PipedriveGetLeadDetailsAction.ACTION_DEFINITION,
                    PipedriveSearchLeadsAction.ACTION_DEFINITION, PipedriveGetOrganizationsAction.ACTION_DEFINITION,
                    PipedriveAddOrganizationAction.ACTION_DEFINITION,
                    PipedriveSearchOrganizationAction.ACTION_DEFINITION,
                    PipedriveDeleteOrganizationAction.ACTION_DEFINITION,
                    PipedriveGetOrganizationDetailsAction.ACTION_DEFINITION,
                    PipedriveGetPersonsAction.ACTION_DEFINITION, PipedriveAddPersonAction.ACTION_DEFINITION,
                    PipedriveSearchPersonsAction.ACTION_DEFINITION, PipedriveDeletePersonAction.ACTION_DEFINITION,
                    PipedriveGetPersonDetailsAction.ACTION_DEFINITION))
                .connection(modifyConnection(PipedriveConnection.CONNECTION_DEFINITION))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
