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
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.bytechef.automation.datasync.facade.DataSyncFacade;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataSyncProjectDeleteEventListenerTest {

    private final DataSyncFacade dataSyncFacade = mock(DataSyncFacade.class);

    private final DataSyncProjectDeleteEventListener dataSyncProjectDeleteEventListener =
        new DataSyncProjectDeleteEventListener(dataSyncFacade);

    @Test
    void testDeletesTheProjectsDataSyncs() {
        dataSyncProjectDeleteEventListener.onBeforeDeleteProject(3L);

        verify(dataSyncFacade).deleteProjectDataSyncs(3L);
        verifyNoMoreInteractions(dataSyncFacade);
    }
}
