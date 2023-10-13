/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.helios.configuration.facade;

import com.bytechef.helios.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = ProjectIntTestConfiguration.class,
    properties = {
        "bytechef.workflow.repository.jdbc.enabled=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
public class ProjectInstanceFacadeIntTest {

    @Disabled
    @Test
    public void testCreateProjectInstance() {
        // TODO
    }

    @Disabled
    @Test
    public void testCreateProjectInstanceJob() {
        // TODO
    }

    @Disabled
    @Test
    public void testDeleteProjectInstance() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetProjectInstance() {
        // TODO
    }

    @Disabled
    @Test
    public void testGetProjectInstanceTags() {
        // TODO
    }

    @Disabled
    @Test
    public void testSearchProjectInstances() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdate() {
        // TODO
    }

    @Disabled
    @Test
    public void testUpdateProjectInstanceTags() {
        // TODO
    }
}
