
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

package com.bytechef.hermes.integration.repository;

import com.bytechef.hermes.integration.config.IntegrationIntTestConfiguration;
import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.test.annotation.EmbeddedSql;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
public class IntegrationRepositoryIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        Assertions.assertEquals(
            integration, integrationRepository.findById(integration.getId())
                .get());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Integration integration = getIntegration(Collections.emptyList());

        integration = integrationRepository.save(integration);

        Integration resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        Assertions.assertEquals(resultIntegration, integration);

        integrationRepository.deleteById(resultIntegration.getId());

        Assertions.assertFalse(
            integrationRepository.findById(integration.getId())
                .isPresent());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testFindById() {
        Integration integration = getIntegration(Collections.emptyList());

        integration = integrationRepository.save(integration);

        Integration resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        Assertions.assertEquals(resultIntegration, integration);

        integration = getIntegration(List.of("workflowId"));

        integration = integrationRepository.save(integration);

        resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        Assertions.assertEquals(resultIntegration.getIntegrationWorkflows(), integration.getIntegrationWorkflows());

        resultIntegration.removeWorkflow("workflowId");

        integrationRepository.save(resultIntegration);

        resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        Assertions.assertEquals(0, resultIntegration.getIntegrationWorkflows()
            .size());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration(List.of("workflow1")));

        integration.addWorkflow("workflow2");
        integration.setName("name2");

        integrationRepository.save(integration);

        Integration updatedIntegration = integrationRepository.findById(integration.getId())
            .get();

        Assertions.assertEquals("name2", updatedIntegration.getName());
        Assertions.assertEquals(2, updatedIntegration.getIntegrationWorkflows()
            .size());
    }

    private static Integration getIntegration(List<String> workflowIds) {
        return new Integration("name", "description", workflowIds);
    }
}
