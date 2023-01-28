
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

import static com.bytechef.hermes.component.constants.ComponentConstants.ADD_TO;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_ID;
import static com.bytechef.hermes.component.constants.ComponentConstants.CLIENT_SECRET;
import static com.bytechef.hermes.component.constants.ComponentConstants.KEY;
import static com.bytechef.hermes.component.constants.ComponentConstants.VALUE;
import static com.bytechef.hermes.component.definition.Authorization.ApiTokenLocation;
import static com.bytechef.hermes.component.definition.Authorization.AuthorizationType;
import static com.bytechef.hermes.component.definition.ComponentDSL.authorization;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.connection;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.component.pipedrive.action.ActivitiesActions;
import com.bytechef.component.pipedrive.action.ActivityFieldsActions;
import com.bytechef.component.pipedrive.action.ActivityTypesActions;
import com.bytechef.component.pipedrive.action.DealFieldsActions;
import com.bytechef.component.pipedrive.action.DealsActions;
import com.bytechef.component.pipedrive.action.FiltersActions;
import com.bytechef.component.pipedrive.action.ItemSearchActions;
import com.bytechef.component.pipedrive.action.LeadsActions;
import com.bytechef.component.pipedrive.action.NotesActions;
import com.bytechef.component.pipedrive.action.OrganizationFieldsActions;
import com.bytechef.component.pipedrive.action.OrganizationsActions;
import com.bytechef.component.pipedrive.action.PersonFieldsActions;
import com.bytechef.component.pipedrive.action.PersonsActions;
import com.bytechef.component.pipedrive.action.PipelinesActions;
import com.bytechef.component.pipedrive.action.StagesActions;
import com.bytechef.component.pipedrive.action.UsersActions;
import com.bytechef.hermes.component.RestComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.util.List;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractPipedriveComponentHandler implements RestComponentHandler {
    private final ComponentDefinition componentDefinition = component("pipedrive")
        .display(
            display("Pipedrive")
                .description(null))
        .actions(ActivitiesActions.ACTIONS, ActivityFieldsActions.ACTIONS, ActivityTypesActions.ACTIONS,
            DealsActions.ACTIONS, DealFieldsActions.ACTIONS, FiltersActions.ACTIONS, ItemSearchActions.ACTIONS,
            LeadsActions.ACTIONS, NotesActions.ACTIONS, OrganizationsActions.ACTIONS, OrganizationFieldsActions.ACTIONS,
            PersonsActions.ACTIONS, PersonFieldsActions.ACTIONS, PipelinesActions.ACTIONS, StagesActions.ACTIONS,
            UsersActions.ACTIONS)
        .connection(connection()
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
                        .refreshUrl(connection -> "https://oauth.pipedrive.com/oauth/token")
                        .scopes(connection -> List.of("deals:read", "deals:full", "leads:read", "leads:full",
                            "activities:read", "activities:full", "contacts:read", "contacts:full", "recents:read",
                            "search:read", "products:read", "users:read", "base"))
                        .tokenUrl(connection -> "https://oauth.pipedrive.com/oauth/token")));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
