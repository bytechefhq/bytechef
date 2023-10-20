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

package com.bytechef.hermes.integration.service;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.domain.IntegrationWorkflow;
import com.bytechef.hermes.integration.repository.IntegrationRepository;
import com.bytechef.test.extension.PostgresTestContainerExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(PostgresTestContainerExtension.class)
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
public class IntegrationServiceIntTest {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Test
    public void testAdd() {
        Integration integration = integrationService.add(getIntegration());

        Assertions.assertEquals(
                List.of("workflow1"),
                integration.getIntegrationWorkflows().stream()
                        .map(IntegrationWorkflow::getWorkflowId)
                        .toList());
        Assertions.assertEquals("name", integration.getName());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration());

        integrationService.delete(integration.getId());

        Assertions.assertFalse(
                integrationRepository.findById(integration.getId()).isPresent());
    }

    @Test
    public void testGetIntegration() {
        Integration integration = integrationRepository.save(getIntegration());

        Assertions.assertEquals(integration, integrationService.getIntegration(integration.getId()));
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testGetIntegrations() {
        for (Integration integration : integrationRepository.findAll()) {
            integrationRepository.deleteById(integration.getId());
        }

        integrationRepository.save(getIntegration());

        Assertions.assertEquals(1, integrationService.getIntegrations().size());
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration());

        integration.setName("name2");

        Integration updatedConnection = integrationService.update(integration);

        Assertions.assertEquals("name2", updatedConnection.getName());
    }

    private static Integration getIntegration() {
        return new Integration("name", "description", List.of("workflow1"));
    }
}
