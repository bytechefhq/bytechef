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

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.repository.SigningKeyRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class SigningKeyOwnershipResolverTest {

    private final SigningKeyRepository repository = mock(SigningKeyRepository.class);
    private final SigningKeyOwnershipResolver resolver = new SigningKeyOwnershipResolver(repository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(resolver.resourceType()).isEqualTo("SigningKey");
    }

    @Test
    void testResolvesAnEmbeddedKeyToItsOwner() {
        when(repository.findById(7L)).thenReturn(Optional.of(signingKey(PlatformType.EMBEDDED)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofEmbedded(42L));
        assertThat(resolver.resolveOwner(Long.valueOf(7L))).isEqualTo(ResourceOwner.ofEmbedded(42L));
    }

    @Test
    void testAnAutomationKeyIsUnknown() {
        when(repository.findById(7L)).thenReturn(Optional.of(signingKey(PlatformType.AUTOMATION)));

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testAMissingRowIsUnknown() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(resolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }

    private static SigningKey signingKey(PlatformType type) {
        SigningKey signingKey = new SigningKey();

        signingKey.setType(type);
        signingKey.setUserId(42L);

        return signingKey;
    }
}
