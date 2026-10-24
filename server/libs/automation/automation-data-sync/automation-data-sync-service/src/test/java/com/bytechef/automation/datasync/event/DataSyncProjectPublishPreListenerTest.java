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

package com.bytechef.automation.datasync.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.facade.DataSyncFacade;
import com.bytechef.automation.datasync.service.DataSyncService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataSyncProjectPublishPreListenerTest {

    private final DataSyncFacade dataSyncFacade = mock(DataSyncFacade.class);
    private final DataSyncService dataSyncService = mock(DataSyncService.class);

    private final DataSyncProjectPublishPreListener dataSyncProjectPublishPreListener =
        new DataSyncProjectPublishPreListener(dataSyncFacade, dataSyncService);

    @Test
    void testSkipsProjectWithoutDataSyncs() {
        when(dataSyncService.getProjectDataSyncs(1L)).thenReturn(List.of());

        dataSyncProjectPublishPreListener.onBeforePublishProject(1L);

        verifyNoInteractions(dataSyncFacade);
    }

    @Test
    void testPreparesProjectWithDataSyncs() {
        when(dataSyncService.getProjectDataSyncs(2L)).thenReturn(List.of(new DataSync()));

        dataSyncProjectPublishPreListener.onBeforePublishProject(2L);

        verify(dataSyncFacade).prepareProjectPublish(2L);
    }
}
