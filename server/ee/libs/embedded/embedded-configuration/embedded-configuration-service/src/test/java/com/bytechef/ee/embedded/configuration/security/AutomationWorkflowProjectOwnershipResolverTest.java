/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.repository.AutomationWorkflowProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationWorkflowProjectOwnershipResolverTest {

    private final AutomationWorkflowProjectRepository repository = mock(AutomationWorkflowProjectRepository.class);
    private final AutomationWorkflowProjectOwnershipResolver resolver =
        new AutomationWorkflowProjectOwnershipResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("AutomationWorkflowProject");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(repository.findByProjectId(7L)).thenReturn(Optional.of(new AutomationWorkflowProject(7L)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(repository.findByProjectId(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }
}
