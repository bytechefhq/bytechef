/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.embedded.security.domain.SigningKey;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 * @version ee
 */
class SigningKeyOwnershipResolverTest {

    private final SigningKeyService signingKeyService = Mockito.mock(SigningKeyService.class);
    private final SigningKeyOwnershipResolver resolver = new SigningKeyOwnershipResolver(signingKeyService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("SigningKey");
    }

    @Test
    void testResolvesOwner() {
        SigningKey signingKey = new SigningKey();

        signingKey.setUserId(7L);

        when(signingKeyService.fetchSigningKey(1L)).thenReturn(Optional.of(signingKey));

        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).hasValue(7L);
        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).isEmpty();
    }

    @Test
    void testUnknownIsUnknown() {
        when(signingKeyService.fetchSigningKey(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .ownerUserId()).isEmpty();
    }
}
