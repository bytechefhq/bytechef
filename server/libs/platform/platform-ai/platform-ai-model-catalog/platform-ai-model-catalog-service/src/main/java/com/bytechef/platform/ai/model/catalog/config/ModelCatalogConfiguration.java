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

package com.bytechef.platform.ai.model.catalog.config;

import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevRefresher;
import com.bytechef.platform.ai.model.catalog.modelsdev.ModelsDevSnapshotLoader;
import com.bytechef.platform.ai.model.catalog.service.ModelCatalogImpl;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
@EnableConfigurationProperties(ModelCatalogProperties.class)
public class ModelCatalogConfiguration {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    @Bean
    ModelCatalogImpl modelCatalog(ModelsDevSnapshotLoader modelsDevSnapshotLoader) {
        return new ModelCatalogImpl(modelsDevSnapshotLoader);
    }

    @Bean
    ModelsDevSnapshotLoader modelsDevSnapshotLoader() {
        return new ModelsDevSnapshotLoader();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "bytechef.ai.model-catalog.refresh", name = "enabled", havingValue = "true",
        matchIfMissing = true)
    ModelsDevRefresher modelsDevRefresher(ModelCatalogImpl modelCatalog, ModelCatalogProperties properties) {
        String url = properties.getRefresh()
            .getUrl();

        RestClient restClient = RestClient.builder()
            .requestFactory(modelsDevRequestFactory())
            .build();

        return new ModelsDevRefresher(
            modelCatalog,
            () -> restClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class));
    }

    /**
     * A scheduled background fetch with no timeout can wedge the shared scheduling thread indefinitely on a stalled
     * connection. The connect timeout fails fast when models.dev is unreachable; the read timeout bounds how long a
     * slow or stalled response is allowed to hold the thread once connected.
     */
    private static JdkClientHttpRequestFactory modelsDevRequestFactory() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build());

        requestFactory.setReadTimeout(READ_TIMEOUT);

        return requestFactory;
    }
}
