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

package com.bytechef.component.reckon;

import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.OpenAPIComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.reckon.action.ReckonCreateContactAction;
import com.bytechef.component.reckon.action.ReckonCreateInvoiceAction;
import com.bytechef.component.reckon.action.ReckonCreatePaymentAction;
import com.bytechef.component.reckon.connection.ReckonConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractReckonComponentHandler implements OpenAPIComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("reckon")
            .title("Reckon")
            .description("Reckon is an accounting software used for financial management and bookkeeping tasks."))
                .actions(modifyActions(ReckonCreateContactAction.ACTION_DEFINITION,
                    ReckonCreateInvoiceAction.ACTION_DEFINITION, ReckonCreatePaymentAction.ACTION_DEFINITION))
                .connection(modifyConnection(ReckonConnection.CONNECTION_DEFINITION))
                .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
