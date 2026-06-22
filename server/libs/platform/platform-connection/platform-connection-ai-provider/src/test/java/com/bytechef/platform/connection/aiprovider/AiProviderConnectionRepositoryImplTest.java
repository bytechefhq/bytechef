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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.platform.ai.llm.Provider;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiProviderConnectionRepositoryImplTest {

    @Mock
    private AiProviderConnectionSource source;

    @Test
    void testFindByIdProjectsEnabledProvider() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(source.isEnabled(Provider.OPEN_AI, 0)).thenReturn(true);
        when(source.getApiKey(Provider.OPEN_AI, 0)).thenReturn(Optional.of("sk-1"));

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);

        Optional<Connection> connection = repository.findById(id);

        assertThat(connection).isPresent();
        assertThat(connection.get()
            .getId()).isEqualTo(id);
        assertThat(connection.get()
            .getComponentName()).isEqualTo("openAi");
        assertThat(connection.get()
            .getConnectionVersion()).isEqualTo(1);
        assertThat(connection.get()
            .getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(connection.get()
            .isManaged()).isTrue();
        assertThat(connection.get()
            .<String>getParameter("token")).isEqualTo("sk-1");
    }

    @Test
    void testFindByIdAbsentWhenDisabled() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(source.isEnabled(Provider.OPEN_AI, 0)).thenReturn(false);

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        assertThat(repository.findById(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0))).isEmpty();
    }

    @Test
    void testFindByIdAbsentWhenEnabledButApiKeyEmpty() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(source.isEnabled(Provider.OPEN_AI, 0)).thenReturn(true);
        when(source.getApiKey(Provider.OPEN_AI, 0)).thenReturn(Optional.empty());

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        assertThat(repository.findById(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0))).isEmpty();
    }

    @Test
    void testFindWithWrongConnectionVersionReturnsEmpty() {
        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        assertThat(repository.find("openAi", 2, 0)).isEmpty();
    }

    @Test
    void testFindFiltersByComponentName() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI, Provider.ANTHROPIC));
        when(source.isEnabled(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(true);
        when(source.getApiKey(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(Optional.of("k"));

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        List<Connection> connections = repository.find("anthropic", null, 0);

        assertThat(connections)
            .extracting(Connection::getComponentName)
            .containsExactly("anthropic");
    }
}
