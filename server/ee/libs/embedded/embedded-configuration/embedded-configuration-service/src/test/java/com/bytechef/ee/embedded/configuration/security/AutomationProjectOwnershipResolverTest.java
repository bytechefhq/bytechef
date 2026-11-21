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
import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
import com.bytechef.ee.embedded.configuration.repository.AutomationProjectRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationProjectOwnershipResolverTest {

    private final AutomationProjectRepository repository = mock(AutomationProjectRepository.class);
    private final AutomationProjectOwnershipResolver resolver =
        new AutomationProjectOwnershipResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("AutomationProject");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(repository.findByProjectId(7L)).thenReturn(Optional.of(new AutomationProject(7L)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(repository.findByProjectId(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }
}
