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

package com.bytechef.component.definition.unified.crm.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.crm.CrmModelType;
import com.bytechef.component.definition.unified.crm.model.ProviderContactInputModel;
import com.bytechef.component.definition.unified.crm.model.ProviderContactOutputModel;

/**
 * Adapts a CRM provider's native contact API to the unified contact contract. Implementations issue the concrete
 * provider requests (create, read, update, delete and paged listing) using the provider-native
 * {@link ProviderContactInputModel} and {@link ProviderContactOutputModel} shapes, while callers interact with them
 * through the provider-agnostic {@link ProviderModelAdapter} abstraction.
 *
 * @param <PCI> the provider-native contact input model type consumed on write operations
 * @param <PCO> the provider-native contact output model type produced on read operations
 *
 * @author Ivica Cardic
 */
public interface ProviderContactAdapter<PCI extends ProviderContactInputModel, PCO extends ProviderContactOutputModel>
    extends ProviderModelAdapter<PCI, PCO> {

    /**
     * Creates a new contact at the provider from the given provider-native input model.
     *
     * @param inputModel           the provider-native contact payload to create
     * @param connectionParameters the parameters describing the authenticated provider connection
     * @param context              the execution context used to perform provider calls
     * @return the identifier assigned by the provider to the newly created contact
     */
    @Override
    String create(PCI inputModel, Parameters connectionParameters, Context context);

    /**
     * Deletes the contact identified by the given id at the provider.
     *
     * @param id                   the provider identifier of the contact to delete
     * @param connectionParameters the parameters describing the authenticated provider connection
     * @param context              the execution context used to perform provider calls
     */
    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    /**
     * Retrieves a single contact from the provider by its identifier.
     *
     * @param id                   the provider identifier of the contact to fetch
     * @param connectionParameters the parameters describing the authenticated provider connection
     * @param context              the execution context used to perform provider calls
     * @return the provider-native contact output model
     */
    @Override
    PCO get(String id, Parameters connectionParameters, Context context);

    /**
     * Returns the CRM model type handled by this adapter, which is always {@link CrmModelType#CONTACT}.
     *
     * @return {@link CrmModelType#CONTACT}
     */
    @Override
    default CrmModelType getModelType() {
        return CrmModelType.CONTACT;
    }

    /**
     * Retrieves a single page of contacts from the provider, honoring any cursor supplied for pagination.
     *
     * @param connectionParameters the parameters describing the authenticated provider connection
     * @param cursorParameters     the pagination cursor identifying the page to fetch
     * @param context              the execution context used to perform provider calls
     * @return a page of provider-native contact output models together with the cursor for the next page
     */
    @Override
    Page<PCO> getPage(Parameters connectionParameters, Parameters cursorParameters, Context context);

    /**
     * Updates the contact identified by the given id at the provider with the supplied input model.
     *
     * @param id                   the provider identifier of the contact to update
     * @param inputModel           the provider-native contact payload holding the updated values
     * @param connectionParameters the parameters describing the authenticated provider connection
     * @param context              the execution context used to perform provider calls
     */
    @Override
    void update(String id, PCI inputModel, Parameters connectionParameters, Context context);
}
