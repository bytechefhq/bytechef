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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A models.dev provider and its models, keyed by model id.
 *
 * @author Ivica Cardic
 */
public record CatalogProvider(String id, String name, @Nullable String doc, Map<String, CatalogModel> models) {

    /**
     * Defensively copies {@code models} so the record cannot be used to smuggle a caller-held mutable map into its
     * internal state or hand one back out. Deliberately {@code LinkedHashMap} wrapped in
     * {@link Collections#unmodifiableMap}, not {@link Map#copyOf} — {@code Map.copyOf}'s iteration order is unspecified
     * and salt-randomized per JVM run, which would make model-list ordering nondeterministic; the parser that builds
     * these maps preserves the document's upstream order for the same reason.
     */
    public CatalogProvider {
        models = Collections.unmodifiableMap(new LinkedHashMap<>(models));
    }
}
