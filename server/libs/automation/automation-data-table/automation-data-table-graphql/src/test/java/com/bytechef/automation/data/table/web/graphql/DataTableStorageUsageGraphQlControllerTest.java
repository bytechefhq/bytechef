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

package com.bytechef.automation.data.table.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataTableStorageUsageGraphQlControllerTest {

    @Test
    void testDataTableStorageUsageReturnsFacadeValue() {
        WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);

        when(facade.getStorageUsage()).thenReturn(new DataTableStorageUsage(10L, 100L, 10.0, false));

        DataTableGraphQlController controller =
            new DataTableGraphQlController(mock(EnvironmentService.class), facade);

        DataTableStorageUsage usage = controller.dataTableStorageUsage();

        assertThat(usage.percentage()).isEqualTo(10.0);
        assertThat(usage.limitBytes()).isEqualTo(100L);
    }
}
