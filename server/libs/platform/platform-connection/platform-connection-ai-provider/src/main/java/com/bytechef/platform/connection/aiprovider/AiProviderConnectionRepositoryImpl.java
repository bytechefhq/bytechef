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

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiProviderConnectionRepositoryImpl implements AiProviderConnectionRepository {

    private static final int CONNECTION_VERSION = 1;

    private final AiProviderConnectionSource source;

    @SuppressFBWarnings("EI2")
    public AiProviderConnectionRepositoryImpl(AiProviderConnectionSource source) {
        this.source = source;
    }

    @Override
    public Optional<Connection> findById(long id) {
        if (!AiProviderConnectionId.isAiProviderConnectionId(id)) {
            return Optional.empty();
        }

        int providerId = AiProviderConnectionId.providerId(id);
        int environmentId = AiProviderConnectionId.environmentId(id);

        return findProvider(providerId)
            .flatMap(provider -> project(provider, environmentId));
    }

    @Override
    public List<Connection> find(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId) {

        if (connectionVersion != null && connectionVersion != CONNECTION_VERSION) {
            return List.of();
        }

        List<Integer> environmentIds = environmentId != null
            ? List.of(environmentId)
            : Arrays.stream(Environment.values())
                .map(Enum::ordinal)
                .toList();

        List<Connection> connections = new ArrayList<>();

        for (Provider provider : source.getSupportedProviders()) {
            if (componentName != null && !componentName.equals(provider.getName())) {
                continue;
            }

            for (int currentEnvironmentId : environmentIds) {
                project(provider, currentEnvironmentId).ifPresent(connections::add);
            }
        }

        return connections;
    }

    @Override
    public List<Connection> findAllByIdIn(List<Long> ids) {
        List<Connection> connections = new ArrayList<>();

        for (Long id : ids) {
            if (id != null && AiProviderConnectionId.isAiProviderConnectionId(id)) {
                findById(id).ifPresent(connections::add);
            }
        }

        return connections;
    }

    private Optional<Connection> project(Provider provider, int environmentId) {
        if (!source.isEnabled(provider, environmentId)) {
            return Optional.empty();
        }

        Optional<String> apiKey = source.getApiKey(provider, environmentId);

        if (apiKey.isEmpty()) {
            return Optional.empty();
        }

        Connection connection = new Connection();

        connection.setVisibility(ResourceVisibility.WORKSPACE);
        connection.setStatus(ConnectionStatus.ACTIVE);
        connection.setId(AiProviderConnectionId.encode(provider.getId(), environmentId));
        connection.setName(provider.getLabel());
        connection.setComponentName(provider.getName());
        connection.setConnectionVersion(CONNECTION_VERSION);
        connection.setAuthorizationType(AuthorizationType.BEARER_TOKEN);
        connection.setEnvironmentId(environmentId);
        connection.setType(PlatformType.AUTOMATION);
        connection.setCreatedBy("system");
        connection.setParameters(Map.of(Authorization.TOKEN, apiKey.get()));
        connection.setManaged(true);

        return Optional.of(connection);
    }

    private Optional<Provider> findProvider(int providerId) {
        return source.getSupportedProviders()
            .stream()
            .filter(provider -> provider.getId() == providerId)
            .findFirst();
    }
}
