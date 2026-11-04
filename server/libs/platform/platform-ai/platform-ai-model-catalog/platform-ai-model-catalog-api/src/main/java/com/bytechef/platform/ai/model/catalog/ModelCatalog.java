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

package com.bytechef.platform.ai.model.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ModelCatalog {

    Optional<CatalogModel> fetchModel(String providerId, String modelId);

    Instant getLoadedAt();

    List<CatalogModel> getModels(String providerId);

    List<CatalogProvider> getProviders();
}
