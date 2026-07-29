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

package com.bytechef.component.definition;

import com.bytechef.component.definition.unified.base.adapter.ProviderModelAdapter;
import com.bytechef.component.definition.unified.base.mapper.ProviderModelMapper;
import java.util.List;

/**
 * Defines how a component participates in a unified API category by supplying the adapters and mappers that translate
 * between the provider's own model and ByteChef's normalized, provider-agnostic model.
 *
 * @author Ivica Cardic
 */
public interface UnifiedApiDefinition {

    /**
     * Enumerates the categories of unified APIs supported by the platform, each grouping providers that expose an
     * equivalent normalized model.
     */
    enum UnifiedApiCategory {
        ACCOUNTING, ATS, CRM, E_COMMERCE, FILE_STORAGE, HRIS, MARKETING_AUTOMATION, TICKETING
    }

    /**
     * Returns the unified API category this component contributes to.
     *
     * @return the unified API category
     */
    UnifiedApiCategory getCategory();

    /**
     * Returns the adapters that bridge provider operations to the unified model.
     *
     * @return the list of provider model adapters
     */
    List<? extends ProviderModelAdapter<?, ?>> getProviderAdapters();

    /**
     * Returns the mappers that convert between provider-specific models and the unified model in both directions.
     *
     * @return the list of provider model mappers
     */
    List<? extends ProviderModelMapper<?, ?, ?, ?>> getProviderMappers();

    /**
     * Marker interface identifying the model types handled within a unified API category.
     */
    interface ModelType {
    }
}
