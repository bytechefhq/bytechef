
            /**
             * The ByteChef Enterprise license (the "Enterprise License")
             * Copyright (c) 2023 - present ByteChef Inc.
             *
             * With regard to the ByteChef Software:
             *
             * This software and associated documentation files (the "Software") may only be
             * used in production, if you (and any entity that you represent) have agreed to,
             * and are in compliance with, the ByteChef Subscription Terms of Service, available
             * via email (support@bytechef.io) (the "Enterprise Terms"), or other
             * agreement governing the use of the Software, as agreed by you and ByteChef,
             * and otherwise have a valid ByteChef Enterprise license for the
             * correct number of user seats. Subject to the foregoing sentence, you are free to
             * modify this Software and publish patches to the Software. You agree that ByteChef
             * and/or its licensors (as applicable) retain all right, title and interest in and
             * to all such modifications and/or patches, and all such modifications and/or
             * patches may only be used, copied, modified, displayed, distributed, or otherwise
             * exploited with a valid ByteChef Enterprise license for the  correct
             * number of user seats.  Notwithstanding the foregoing, you may copy and modify
             * the Software for development and testing purposes, without requiring a
             * subscription.  You agree that ByteChef and/or its licensors (as applicable) retain
             * all right, title and interest in and to all such modifications.  You are not
             * granted any other rights beyond what is expressly stated herein.  Subject to the
             * foregoing, it is forbidden to copy, merge, publish, distribute, sublicense,
             * and/or sell the Software.
             *
             * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
             * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
             * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
             * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
             * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
             * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
             * SOFTWARE.
             *
             * For all third party components incorporated into the ByteChef Software, those
             * components are licensed under the original license provided by the owner of the
             * applicable component.
             */
            
package com.bytechef.athena.configuration.repository;

import com.bytechef.athena.configuration.config.IntegrationIntTestConfiguration;
import com.bytechef.athena.configuration.domain.Integration;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = IntegrationIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class IntegrationRepositoryIntTest {

    @Autowired
    private IntegrationRepository integrationRepository;

    @AfterEach
    public void afterEach() {
        integrationRepository.deleteAll();
    }

    @Test
    public void testCreate() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        assertThat(integration).isEqualTo(
            OptionalUtils.get(integrationRepository.findById(Validate.notNull(integration.getId(), "id"))));
    }

    @Test
    public void testDelete() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);

        integrationRepository.deleteById(Validate.notNull(resultIntegration.getId(), "id"));

        assertThat(integrationRepository.findById(integration.getId()))
            .isEmpty();
    }

    @Test
    public void testFindById() {
        Integration integration = integrationRepository.save(getIntegration(Collections.emptyList()));

        Integration resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration).isEqualTo(integration);

        integrationRepository.deleteById(Validate.notNull(integration.getId(), "id"));

        integration = getIntegration(List.of("workflowId"));

        integration = integrationRepository.save(integration);

        resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration.getWorkflowIds()).isEqualTo(integration.getWorkflowIds());

        resultIntegration.removeWorkflow("workflowId");

        integrationRepository.save(resultIntegration);

        resultIntegration = OptionalUtils.get(
            integrationRepository.findById(Validate.notNull(integration.getId(), "id")));

        assertThat(resultIntegration.getWorkflowIds()).isEmpty();
    }

    @Test
    public void testUpdate() {
        Integration integration = integrationRepository.save(getIntegration(List.of("workflow1")));

        integration.addWorkflowId("workflow2");
        integration.setName("name2");

        integrationRepository.save(integration);

        assertThat(integrationRepository.findById(Validate.notNull(integration.getId(), "id")))
            .hasValue(integration);
    }

    private static Integration getIntegration(List<String> workflowIds) {
        return Integration.builder()
            .description("description")
            .integrationVersion(1)
            .name("name")
            .status(Integration.Status.UNPUBLISHED)
            .workflowIds(workflowIds)
            .build();
    }
}
