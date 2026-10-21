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

package com.bytechef.automation.configuration.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Set;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
@ProjectIntTestConfigurationSharedMocks
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOwnerContributorsResetListenerIntTest {

    private static final long OWNED_PROJECT_ID = 424242L;

    @Autowired
    private ProjectOwnershipService projectOwnershipService;

    @Autowired
    private TestOwnerContributors testOwnerContributors;

    @Test
    @Order(1)
    void testAnOwnedIdAddedByOneTest() {
        testOwnerContributors.addCatalogId(OWNED_PROJECT_ID);

        assertThat(getExcludeCatalogProjectsCondition()).contains(String.valueOf(OWNED_PROJECT_ID));
    }

    @Test
    @Order(2)
    void testIsGoneInTheNextTest() {
        assertThat(getExcludeCatalogProjectsCondition()).doesNotContain(String.valueOf(OWNED_PROJECT_ID));
    }

    private String getExcludeCatalogProjectsCondition() {
        return projectOwnershipService.getExcludeProjectsCondition(Set.of(ProjectOwner.EMBEDDED_CATALOG), "project.id");
    }
}
