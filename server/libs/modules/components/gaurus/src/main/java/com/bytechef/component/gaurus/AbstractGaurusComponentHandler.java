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

package com.bytechef.component.gaurus;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.gaurus.action.GaurusDeleteExternalUserAction;
import com.bytechef.component.gaurus.action.GaurusGetAccountTransactionsAction;
import com.bytechef.component.gaurus.action.GaurusGetAccountsAction;
import com.bytechef.component.gaurus.action.GaurusGetExternalUsersAction;
import com.bytechef.component.gaurus.action.GaurusPostExternalUsersAction;
import com.bytechef.component.gaurus.action.GaurusPutExternalUserAction;
import com.bytechef.component.gaurus.connection.GaurusConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 *
 * @author Igor Beslic
 */
public abstract class AbstractGaurusComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("gaurus")
            .title("Bank Connect")
            .description("Bank Connect API specification")
            .version(1))
                .actions(modifyActions(GaurusDeleteExternalUserAction.ACTION_DEFINITION,
                    GaurusGetAccountsAction.ACTION_DEFINITION, GaurusGetAccountTransactionsAction.ACTION_DEFINITION,
                    GaurusGetExternalUsersAction.ACTION_DEFINITION, GaurusPostExternalUsersAction.ACTION_DEFINITION,
                    GaurusPutExternalUserAction.ACTION_DEFINITION))
                .connection(modifyConnection(GaurusConnection.CONNECTION_DEFINITION))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
