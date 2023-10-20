
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

import static com.bytechef.hermes.component.constant.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constant.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constant.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constant.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

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
import com.bytechef.hermes.component.OpenApiComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.util.List;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPipedriveComponentHandler implements OpenApiComponentHandler {
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
            DeletePersonAction.ACTION_DEFINITION, GetPersonAction.ACTION_DEFINITION))
        .connection(modifyConnection(
            connection()
                .baseUri(connection -> "https://api.pipedrive.com/v1")
                .authorizations(authorization(
                    AuthorizationType.API_KEY.name()
                        .toLowerCase(),
                    AuthorizationType.API_KEY)
                        .display(
                            display("API Key"))
                        .properties(
                            string(KEY)
                                .label("Key")
                                .required(true)
                                .defaultValue("api_token")
                                .hidden(true),
                            string(VALUE)
                                .label("Value")
                                .required(true),
                            string(ADD_TO)
                                .label("Add to")
                                .required(true)
                                .defaultValue(ApiTokenLocation.QUERY_PARAMETERS.name())
                                .hidden(true)),
                    authorization(
                        AuthorizationType.OAUTH2_AUTHORIZATION_CODE.name()
                            .toLowerCase(),
                        AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                            .display(
                                display("OAuth2 Authorization Code"))
                            .properties(
                                string(CLIENT_ID)
                                    .label("Client Id")
                                    .required(true),
                                string(CLIENT_SECRET)
                                    .label("Client Secret")
                                    .required(true))
                            .authorizationUrl(connection -> "https://oauth.pipedrive.com/oauth/authorize")
                            .scopes(connection -> List.of("deals:full", "contacts:full", "search:read", "leads:read",
                                "leads:full", "contacts:read", "deals:read"))
                            .tokenUrl(connection -> "https://oauth.pipedrive.com/oauth/token")
                            .refreshUrl(connection -> "https://oauth.pipedrive.com/oauth/token"))));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
