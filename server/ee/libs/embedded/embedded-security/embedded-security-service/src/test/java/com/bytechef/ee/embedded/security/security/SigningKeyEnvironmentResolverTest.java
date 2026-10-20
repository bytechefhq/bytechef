/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SigningKeyEnvironmentResolverTest {

    private final SigningKeyRepository repository = mock(SigningKeyRepository.class);
    private final SigningKeyEnvironmentResolver resolver = new SigningKeyEnvironmentResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("SigningKey");
    }

    @Test
    void testTheEnvironmentComesFromTheEnvironmentColumn() {
        SigningKey signingKey = new SigningKey();

        signingKey.setEnvironment(Environment.STAGING);

        when(repository.findById(7L)).thenReturn(Optional.of(signingKey));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.STAGING);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(resolver.fetchEnvironment("not-a-number")).isEmpty();
    }
}
