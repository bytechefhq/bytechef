
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

package com.bytechef.component.pipedrive;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.pipedrive.action.AddDealAction;
import com.bytechef.component.pipedrive.action.AddLeadAction;
import com.bytechef.component.pipedrive.action.AddOrganizationAction;
import com.bytechef.component.pipedrive.action.AddPersonAction;
import com.bytechef.component.pipedrive.action.DeleteDealAction;
import com.bytechef.component.pipedrive.action.DeleteLeadAction;
import com.bytechef.component.pipedrive.action.DeleteOrganizationAction;
import com.bytechef.component.pipedrive.action.DeletePersonAction;
import com.bytechef.component.pipedrive.action.GetDealAction;
import com.bytechef.component.pipedrive.action.GetDealsAction;
import com.bytechef.component.pipedrive.action.GetLeadAction;
import com.bytechef.component.pipedrive.action.GetLeadsAction;
import com.bytechef.component.pipedrive.action.GetOrganizationAction;
import com.bytechef.component.pipedrive.action.GetOrganizationsAction;
import com.bytechef.component.pipedrive.action.GetPersonAction;
import com.bytechef.component.pipedrive.action.GetPersonsAction;
import com.bytechef.component.pipedrive.action.SearchDealsAction;
import com.bytechef.component.pipedrive.action.SearchLeadsAction;
import com.bytechef.component.pipedrive.action.SearchOrganizationAction;
import com.bytechef.component.pipedrive.action.SearchPersonsAction;
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPipedriveComponentHandler implements RestComponentHandler {
    private final ComponentDefinition componentDefinition = component("pipedrive")
        .display(
            modifyDisplay(
                display("Pipedrive")
                    .description(null)))
        .actions(modifyActions(GetDealsAction.ACTION_DEFINITION, AddDealAction.ACTION_DEFINITION,
            SearchDealsAction.ACTION_DEFINITION, DeleteDealAction.ACTION_DEFINITION, GetDealAction.ACTION_DEFINITION,
            GetLeadsAction.ACTION_DEFINITION, AddLeadAction.ACTION_DEFINITION, DeleteLeadAction.ACTION_DEFINITION,
            GetLeadAction.ACTION_DEFINITION, SearchLeadsAction.ACTION_DEFINITION,
            GetOrganizationsAction.ACTION_DEFINITION, AddOrganizationAction.ACTION_DEFINITION,
            SearchOrganizationAction.ACTION_DEFINITION, DeleteOrganizationAction.ACTION_DEFINITION,
            GetOrganizationAction.ACTION_DEFINITION, GetPersonsAction.ACTION_DEFINITION,
            AddPersonAction.ACTION_DEFINITION, SearchPersonsAction.ACTION_DEFINITION,
            DeletePersonAction.ACTION_DEFINITION, GetPersonAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
