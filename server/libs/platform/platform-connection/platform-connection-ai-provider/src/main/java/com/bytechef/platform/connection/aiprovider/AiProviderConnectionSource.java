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

package com.bytechef.platform.connection.aiprovider;

import com.bytechef.platform.ai.llm.Provider;
import java.util.List;
import java.util.Optional;

/**
 * Single source of truth for which AI providers are enabled and what API key each holds, reading the {@code property}
 * table (per-environment) with an {@code application.yml} fallback. Shared by the connection projector and the EE AI
 * Providers facade so the enable/apiKey logic lives in one place.
 *
 * @author Ivica Cardic
 */
public interface AiProviderConnectionSource {

    List<Provider> getSupportedProviders();

    boolean isEnabled(Provider provider, int environmentId);

    Optional<String> getApiKey(Provider provider, int environmentId);
}
