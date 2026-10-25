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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.automation.data.table.web.graphql.config.AutomationDataTableGraphQlTestConfiguration;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationDataTableGraphQlTestConfiguration.class, DataTableGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@MockitoBean(types = {
    DataTableService.class, EnvironmentService.class
})
class DataTableGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @Test
    void testDataTableStorageUsage() {
        when(workspaceDataTableFacade.getStorageUsage())
            .thenReturn(new DataTableStorageUsage(10L, 100L, 10.0, false));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableStorageUsage {
                        usedBytes
                        limitBytes
                        percentage
                        unlimited
                    }
                }
                """)
            .execute();

        response.path("dataTableStorageUsage.usedBytes")
            .entity(Long.class)
            .isEqualTo(10L);
        response.path("dataTableStorageUsage.limitBytes")
            .entity(Long.class)
            .isEqualTo(100L);
        response.path("dataTableStorageUsage.percentage")
            .entity(Double.class)
            .isEqualTo(10.0);
        response.path("dataTableStorageUsage.unlimited")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(workspaceDataTableFacade).getStorageUsage();
    }

    @Test
    void testDataTableStorageUsageUnlimited() {
        when(workspaceDataTableFacade.getStorageUsage())
            .thenReturn(new DataTableStorageUsage(26_214_400L, 0L, 0.0, true));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableStorageUsage {
                        usedBytes
                        unlimited
                    }
                }
                """)
            .execute();

        response.path("dataTableStorageUsage.usedBytes")
            .entity(Long.class)
            .isEqualTo(26_214_400L);
        response.path("dataTableStorageUsage.unlimited")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
}
