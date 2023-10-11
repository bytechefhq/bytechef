
/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
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
