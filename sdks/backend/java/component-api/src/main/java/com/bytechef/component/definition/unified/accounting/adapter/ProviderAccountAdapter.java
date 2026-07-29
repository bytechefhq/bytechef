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

package com.bytechef.component.definition.unified.accounting.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.unified.accounting.AccountingModelType;
import com.bytechef.component.definition.unified.accounting.model.ProviderAccountInputModel;
import com.bytechef.component.definition.unified.accounting.model.ProviderAccountOutputModel;
import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;

/**
 * Adapts a provider's native accounting account API to ByteChef's unified accounting model. Implementations translate
 * CRUD operations expressed in terms of {@link ProviderAccountInputModel} and {@link ProviderAccountOutputModel} into
 * the concrete calls required by a specific accounting provider.
 *
 * @author Ivica Cardic
 */
public interface ProviderAccountAdapter
    extends ProviderModelAdapter<ProviderAccountInputModel, ProviderAccountOutputModel> {

    /**
     * Creates a new account at the provider from the given provider-native input model.
     *
     * @param inputModel           the provider-native account data to create
     * @param connectionParameters the parameters describing the authenticated connection to the provider
     * @param context              the execution context used to perform provider calls
     * @return the identifier assigned to the newly created account by the provider
     */
    @Override
    String create(ProviderAccountInputModel inputModel, Parameters connectionParameters, Context context);

    /**
     * Deletes the account identified by the given id at the provider.
     *
     * @param id                   the provider identifier of the account to delete
     * @param connectionParameters the parameters describing the authenticated connection to the provider
     * @param context              the execution context used to perform provider calls
     */
    @Override
    void delete(String id, Parameters connectionParameters, Context context);

    /**
     * Retrieves a single account by its provider identifier.
     *
     * @param id                   the provider identifier of the account to retrieve
     * @param connectionParameters the parameters describing the authenticated connection to the provider
     * @param context              the execution context used to perform provider calls
     * @return the provider-native output model for the requested account
     */
    @Override
    ProviderAccountOutputModel get(String id, Parameters connectionParameters, Context context);

    /**
     * Returns the accounting model type handled by this adapter, always {@link AccountingModelType#ACCOUNT}.
     *
     * @return {@link AccountingModelType#ACCOUNT}
     */
    @Override
    default AccountingModelType getModelType() {
        return AccountingModelType.ACCOUNT;
    }

    /**
     * Retrieves a single page of accounts from the provider, using the supplied cursor parameters for pagination.
     *
     * @param connectionParameters the parameters describing the authenticated connection to the provider
     * @param cursorParameters     the pagination cursor parameters identifying the page to fetch
     * @param context              the execution context used to perform provider calls
     * @return a page of provider-native account output models together with the cursor for the next page
     */
    @Override
    Page<ProviderAccountOutputModel> getPage(
        Parameters connectionParameters, Parameters cursorParameters, Context context);

    /**
     * Updates the account identified by the given id at the provider with the supplied input model.
     *
     * @param id                   the provider identifier of the account to update
     * @param inputModel           the provider-native account data to apply
     * @param connectionParameters the parameters describing the authenticated connection to the provider
     * @param context              the execution context used to perform provider calls
     */
    @Override
    void update(String id, ProviderAccountInputModel inputModel, Parameters connectionParameters, Context context);
}
