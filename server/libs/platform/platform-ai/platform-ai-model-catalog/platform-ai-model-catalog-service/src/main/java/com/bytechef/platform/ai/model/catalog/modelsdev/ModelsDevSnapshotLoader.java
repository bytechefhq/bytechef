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

package com.bytechef.platform.ai.model.catalog.modelsdev;

import com.bytechef.platform.ai.model.catalog.CatalogProvider;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Loads the bundled models.dev snapshot from the classpath.
 *
 * <p>
 * A missing, unreadable, or content-free snapshot throws rather than degrading to an empty catalog: it is a packaging
 * error, not a runtime condition, and silently serving nothing would make every model look uncatalogued. The
 * content-free check mirrors {@link ModelsDevRefresher}'s zero-model guard — a total model count across providers, not
 * a provider count, since {@link ModelsDevParser} builds one {@link CatalogProvider} per top-level JSON key with no
 * validation that it carries any models.
 *
 * @author Ivica Cardic
 */
public class ModelsDevSnapshotLoader {

    static final String SNAPSHOT_RESOURCE = "/config/model-catalog/models-dev-api.json";

    public Map<String, CatalogProvider> load() {
        try (InputStream inputStream = ModelsDevSnapshotLoader.class.getResourceAsStream(SNAPSHOT_RESOURCE)) {
            if (inputStream == null) {
                throw new IllegalStateException("Bundled models.dev snapshot not found at " + SNAPSHOT_RESOURCE);
            }

            return parseAndValidate(inputStream);
        } catch (IOException ioException) {
            throw new IllegalStateException("Failed to read bundled models.dev snapshot", ioException);
        }
    }

    /**
     * Package-visible seam so {@code ModelsDevSnapshotLoaderTest} can pin the content-free guard against a synthetic
     * stream without swapping the fixed classpath resource {@link #SNAPSHOT_RESOURCE} out from under {@link #load()}.
     */
    static Map<String, CatalogProvider> parseAndValidate(InputStream inputStream) {
        Map<String, CatalogProvider> providers = ModelsDevParser.parse(inputStream);

        int modelCount = providers.values()
            .stream()
            .mapToInt(provider -> provider.models()
                .size())
            .sum();

        if (modelCount == 0) {
            throw new IllegalStateException(
                "Bundled models.dev snapshot at " + SNAPSHOT_RESOURCE + " parsed to " + providers.size()
                    + " providers but zero models in total");
        }

        return providers;
    }
}
