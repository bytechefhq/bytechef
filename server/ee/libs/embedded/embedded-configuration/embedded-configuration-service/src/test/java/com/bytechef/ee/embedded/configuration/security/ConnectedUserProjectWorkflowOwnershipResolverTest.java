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
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ConnectedUserProjectWorkflowOwnershipResolverTest {

    private final ConnectedUserProjectWorkflowRepository repository =
        mock(ConnectedUserProjectWorkflowRepository.class);
    private final ConnectedUserProjectWorkflowOwnershipResolver resolver =
        new ConnectedUserProjectWorkflowOwnershipResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("ConnectedUserProjectWorkflow");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(repository.existsById(7L)).thenReturn(true);

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(resolver.resolveOwner(Long.valueOf(7L))).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(repository.existsById(7L)).thenReturn(false);

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }
}
