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
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI2")
public class ModelsDevRefresher {

    private static final Logger log = LoggerFactory.getLogger(ModelsDevRefresher.class);

    private final BodySupplier bodySupplier;
    private final ModelCatalogImpl modelCatalog;

    public ModelsDevRefresher(ModelCatalogImpl modelCatalog, BodySupplier bodySupplier) {
        this.bodySupplier = bodySupplier;
        this.modelCatalog = modelCatalog;
    }

    @Scheduled(
        initialDelayString = "${bytechef.ai.model-catalog.refresh.interval:P1D}",
        fixedDelayString = "${bytechef.ai.model-catalog.refresh.interval:P1D}")
    public void refresh() {
        Map<String, CatalogProvider> providers;

        try {
            byte[] body = bodySupplier.get();

            providers = ModelsDevParser.parse(new ByteArrayInputStream(body));
        } catch (RuntimeException exception) {
            log.warn("models.dev catalog refresh failed; keeping the current catalog", exception);

            return;
        }

        int modelCount = providers.values()
            .stream()
            .mapToInt(provider -> provider.models()
                .size())
            .sum();

        if (modelCount == 0) {
            log.warn(
                "models.dev catalog refresh returned {} providers but zero models in total; keeping the current"
                    + " catalog",
                providers.size());

            return;
        }

        modelCatalog.replaceCatalog(providers, Instant.now());

        log.info("models.dev catalog refreshed with {} providers", providers.size());
    }

    @FunctionalInterface
    public interface BodySupplier {

        byte[] get();
    }
}
