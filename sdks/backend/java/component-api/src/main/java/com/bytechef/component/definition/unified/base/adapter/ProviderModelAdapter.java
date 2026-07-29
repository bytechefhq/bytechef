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

package com.bytechef.component.definition.unified.base.adapter;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.UnifiedApiDefinition;
import com.bytechef.component.definition.unified.base.model.ProviderInputModel;
import com.bytechef.component.definition.unified.base.model.ProviderOutputModel;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;

/**
 * Adapter that performs the provider-specific CRUD calls against a third-party API using provider input and output
 * models. It is the low-level counterpart of {@code ProviderModelMapper}: the mapper translates between unified and
 * provider models, while this adapter carries out the actual create, read, update, delete, and paginated list
 * operations for a given {@link UnifiedApiDefinition.ModelType}.
 *
 * @param <PI> the provider input model type consumed by create and update operations
 * @param <PO> the provider output model type returned by read and list operations
 *
 * @author Ivica Cardic
 */
public interface ProviderModelAdapter<PI extends ProviderInputModel, PO extends ProviderOutputModel> {

    /**
     * Creates a new resource on the provider from the given input model.
     *
     * @param inputModel           the provider input model describing the resource to create
     * @param connectionParameters the connection credentials
     * @param context              the execution context
     * @return the identifier of the newly created resource
     */
    String create(PI inputModel, Parameters connectionParameters, Context context);

    /**
     * Deletes the resource identified by the given id from the provider.
     *
     * @param id                   the identifier of the resource to delete
     * @param connectionParameters the connection credentials
     * @param context              the execution context
     */
    void delete(String id, Parameters connectionParameters, Context context);

    /**
     * Retrieves the resource identified by the given id from the provider.
     *
     * @param id                   the identifier of the resource to retrieve
     * @param connectionParameters the connection credentials
     * @param context              the execution context
     * @return the provider output model for the requested resource
     */
    PO get(String id, Parameters connectionParameters, Context context);

    /**
     * Returns the unified model type that this adapter handles.
     *
     * @return the model type served by this adapter
     */
    UnifiedApiDefinition.ModelType getModelType();

    /**
     * Retrieves a single page of resources from the provider, using the given cursor parameters for pagination.
     *
     * @param connectionParameters the connection credentials
     * @param cursorParameters     the pagination cursor parameters, empty for the first page
     * @param context              the execution context
     * @return a {@link Page} containing the retrieved resources and the cursor for the next page
     */
    Page<PO> getPage(Parameters connectionParameters, Parameters cursorParameters, Context context);

    /**
     * Updates the resource identified by the given id on the provider using the supplied input model.
     *
     * @param id                   the identifier of the resource to update
     * @param inputModel           the provider input model describing the updated resource
     * @param connectionParameters the connection credentials
     * @param context              the execution context
     */
    void update(String id, PI inputModel, Parameters connectionParameters, Context context);

    /**
     * A single page of provider output models together with the cursor parameters needed to fetch the following page.
     *
     * @param <PO>             the provider output model type contained in this page
     * @param content          the resources contained in this page
     * @param size             the number of resources in this page
     * @param cursorParameters the cursor parameters that locate the next page, empty when there are no further pages
     */
    @SuppressFBWarnings("EI")
    record Page<PO>(List<PO> content, int size, Map<String, ?> cursorParameters) {
    }
}
