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

package com.bytechef.automation.datasync.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.repository.DataSyncElementRepository;
import com.bytechef.automation.datasync.repository.DataSyncRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataSyncOwnershipResolversTest {

    private final DataSyncRepository dataSyncRepository = mock(DataSyncRepository.class);
    private final DataSyncElementRepository dataSyncElementRepository = mock(DataSyncElementRepository.class);

    @Test
    void testDataSyncResolvesToItsWorkspace() {
        DataSync dataSync = new DataSync();

        dataSync.setWorkspaceId(10L);

        when(dataSyncRepository.findById(1L)).thenReturn(Optional.of(dataSync));

        DataSyncOwnershipResolver resolver = new DataSyncOwnershipResolver(dataSyncRepository);

        assertThat(resolver.resourceType()).isEqualTo("DataSync");
        assertThat(resolver.resolveOwner(1L)).isEqualTo(ResourceOwner.ofWorkspace(10L));
        assertThat(resolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testElementResolvesThroughItsDataSync() {
        DataSync dataSync = new DataSync();

        dataSync.setWorkspaceId(10L);

        DataSyncElement element = new DataSyncElement(1L, Kind.SOURCE);

        when(dataSyncElementRepository.findById(5L)).thenReturn(Optional.of(element));
        when(dataSyncRepository.findById(1L)).thenReturn(Optional.of(dataSync));

        DataSyncElementOwnershipResolver resolver =
            new DataSyncElementOwnershipResolver(dataSyncElementRepository, dataSyncRepository);

        assertThat(resolver.resourceType()).isEqualTo("DataSyncElement");
        assertThat(resolver.resolveOwner(5L)).isEqualTo(ResourceOwner.ofWorkspace(10L));
        assertThat(resolver.resolveOwner(6L)).isEqualTo(ResourceOwner.unknown());
    }
}
