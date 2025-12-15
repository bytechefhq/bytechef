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

package com.bytechef.component.productboard;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.productboard.action.ProductboardCreateNoteAction;
import com.bytechef.component.productboard.action.ProductboardDeleteNoteAction;
import com.bytechef.component.productboard.action.ProductboardGetFeatureAction;
import com.bytechef.component.productboard.action.ProductboardGetNoteAction;
import com.bytechef.component.productboard.action.ProductboardUpdateNoteAction;
import com.bytechef.component.productboard.connection.ProductboardConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractProductboardComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("productboard")
            .title("Productboard")
            .description(
                "Productboard is a product management platform that helps teams prioritize features, gather customer feedback, and align their product strategy to deliver better products."))
                    .actions(modifyActions(ProductboardGetFeatureAction.ACTION_DEFINITION,
                        ProductboardCreateNoteAction.ACTION_DEFINITION, ProductboardDeleteNoteAction.ACTION_DEFINITION,
                        ProductboardGetNoteAction.ACTION_DEFINITION, ProductboardUpdateNoteAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ProductboardConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
