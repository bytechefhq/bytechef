
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.integration.facade;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.test.annotation.EmbeddedSql;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(
    classes = IntegrationIntTestConfiguration.class,
    properties = "bytechef.workflow.workflow-repository.jdbc.enabled=true")
public class IntegrationFacadeIntTest {

    @Autowired
    private IntegrationFacade integrationFacade;

    @Test
    public void testAdd() {
        Integration integration = new Integration();

        integration.setName("name");
        integration.setDescription("description");

        integration = integrationFacade.initialize(integration);

        Assertions.assertEquals("description", integration.getDescription());
        Assertions.assertEquals("name", integration.getName());
        Assertions.assertNotNull(integration.getId());
        Assertions.assertEquals(1, integration.getIntegrationWorkflows()
            .size());
    }
}
