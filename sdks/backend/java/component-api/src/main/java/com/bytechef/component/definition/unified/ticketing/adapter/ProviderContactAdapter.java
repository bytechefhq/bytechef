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

package com.bytechef.component.definition.unified.ticketing.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.ticketing.TicketingModelType;
import com.bytechef.component.definition.unified.ticketing.model.ProviderContactInputModel;
import com.bytechef.component.definition.unified.ticketing.model.ProviderContactOutputModel;

/**
 * Provider-specific adapter for the ticketing {@code contact} model. Implementations invoke a concrete provider's API
 * to create, read, update, delete and page over contacts, exchanging {@link ProviderContactInputModel} and
 * {@link ProviderContactOutputModel} instances in the provider's native shape.
 *
 * @author Ivica Cardic
 */
public interface ProviderContactAdapter
    extends ProviderModelAdapter<ProviderContactInputModel, ProviderContactOutputModel> {

    /**
     * Creates a new contact at the provider from the given provider-native input model.
     *
     * @param inputModel           the provider-native contact data to create
     * @param connectionParameters the connection parameters used to authenticate against the provider
     * @param context              the execution context providing access to platform services
     * @return the identifier of the newly created contact
     */
    @Override
    String create(ProviderContactInputModel inputModel, Parameters connectionParameters, Context context);

    /**
     * Deletes the contact identified by the given id at the provider.
     *
     * @param id                   the identifier of the contact to delete
     * @param connectionParameters the connection parameters used to authenticate against the provider
     * @param context              the execution context providing access to platform services
     */
    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    /**
     * Retrieves a single contact by its identifier from the provider.
     *
     * @param id                   the identifier of the contact to retrieve
     * @param connectionParameters the connection parameters used to authenticate against the provider
     * @param context              the execution context providing access to platform services
     * @return the provider-native representation of the requested contact
     */
    @Override
    ProviderContactOutputModel get(String id, Parameters connectionParameters, Context context);

    /**
     * Returns the model type handled by this adapter, always {@link TicketingModelType#CONTACT}.
     *
     * @return {@link TicketingModelType#CONTACT}
     */
    @Override
    default TicketingModelType getModelType() {
        return TicketingModelType.CONTACT;
    }

    /**
     * Retrieves a single page of contacts from the provider using cursor-based pagination.
     *
     * @param connectionParameters the connection parameters used to authenticate against the provider
     * @param cursorParameters     the cursor parameters identifying which page to fetch
     * @param context              the execution context providing access to platform services
     * @return a page of provider-native contacts together with the cursor for the next page
     */
    @Override
    Page<ProviderContactOutputModel> getPage(
        Parameters connectionParameters, Parameters cursorParameters, Context context);

    /**
     * Updates the contact identified by the given id with the supplied provider-native data.
     *
     * @param id                   the identifier of the contact to update
     * @param inputModel           the provider-native contact data to apply
     * @param connectionParameters the connection parameters used to authenticate against the provider
     * @param context              the execution context providing access to platform services
     */
    @Override
    void update(String id, ProviderContactInputModel inputModel, Parameters connectionParameters, Context context);
}
