
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@EmbeddedSql
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
public class IntegrationRepositoryIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @BeforeEach
    public void beforeEach() {
        integrationRepository.deleteAll();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testCreate() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        assertThat(integration).isEqualTo(integrationRepository.findById(integration.getId())
            .get());
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        Integration resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        assertThat(resultIntegration).isEqualTo(integration);

        integrationRepository.deleteById(resultIntegration.getId());

        assertThat(integrationRepository.findById(integration.getId())).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testFindById() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        Integration resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        assertThat(resultIntegration).isEqualTo(integration);

        integration = getIntegration(List.of("workflowId"));

        integration = integrationRepository.save(integration);

        resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        assertThat(resultIntegration.getWorkflowIds()).isEqualTo(integration.getWorkflowIds());

        resultIntegration.removeWorkflow("workflowId");

        integrationRepository.save(resultIntegration);

        resultIntegration = integrationRepository.findById(integration.getId())
            .orElseThrow();

        assertThat(resultIntegration.getWorkflowIds()).isEmpty();
    }

    @Test
    @SuppressFBWarnings("NP")
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration(List.of("workflow1")));

        integration.addWorkflow("workflow2");
        integration.setName("name2");

        integrationRepository.save(integration);

        assertThat(integrationRepository.findById(integration.getId())).hasValue(integration);
    }

    private static Integration getIntegration(List<String> workflowIds) {
        Integration integration = new Integration();

        integration.setWorkflowIds(workflowIds);

        integration.setDescription("name");
        integration.setName("name");

        return integration;
    }
}
