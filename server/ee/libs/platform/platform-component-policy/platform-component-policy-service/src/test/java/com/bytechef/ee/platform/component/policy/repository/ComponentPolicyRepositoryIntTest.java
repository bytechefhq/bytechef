/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.platform.component.policy.ComponentOperationPolicy;
import com.bytechef.ee.platform.component.policy.ComponentPolicy;
import com.bytechef.ee.platform.component.policy.config.ComponentPolicyIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ComponentPolicyIntTestConfiguration.class, properties = "bytechef.edition=ee")
@Import(PostgreSQLContainerConfiguration.class)
public class ComponentPolicyRepositoryIntTest {

    @Autowired
    private ComponentOperationPolicyRepository componentOperationPolicyRepository;

    @Autowired
    private ComponentPolicyRepository componentPolicyRepository;

    @AfterEach
    public void afterEach() {
        componentOperationPolicyRepository.deleteAll();
        componentPolicyRepository.deleteAll();
    }

    @Test
    public void testInsertThenUpdateRoundTrip() {
        ComponentPolicy componentPolicy = new ComponentPolicy("slack");

        componentPolicy.setEnabled(false);

        componentPolicyRepository.save(componentPolicy);

        assertThat(componentPolicyRepository.findByEnabled(false))
            .extracting(ComponentPolicy::getComponentName)
            .containsExactly("slack");

        ComponentPolicy reloaded = componentPolicyRepository.findById("slack")
            .orElseThrow();

        reloaded.setEnabled(true);

        componentPolicyRepository.save(reloaded);

        assertThat(componentPolicyRepository.findByEnabled(false)).isEmpty();
    }

    @Test
    void testComponentOperationPolicyRoundTrip() {
        componentOperationPolicyRepository.save(
            new ComponentOperationPolicy(
                "slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage"));

        List<ComponentOperationPolicy> componentOperationPolicies =
            componentOperationPolicyRepository.findAllByComponentName("slack");

        assertThat(componentOperationPolicies).hasSize(1);
        assertThat(componentOperationPolicies.getFirst()
            .getOperationType()).isEqualTo(ComponentOperationPolicy.OperationType.ACTION);
        assertThat(componentOperationPolicyRepository.findByComponentNameAndOperationTypeAndOperationName(
            "slack", ComponentOperationPolicy.OperationType.ACTION.ordinal(), "sendMessage")).isPresent();
    }
}
