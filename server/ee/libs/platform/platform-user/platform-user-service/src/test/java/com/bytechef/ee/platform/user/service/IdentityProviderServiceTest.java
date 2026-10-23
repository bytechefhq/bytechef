/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import com.bytechef.encryption.Encryption;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IdentityProviderServiceTest {

    private final Encryption encryption = mock(Encryption.class);
    private final IdentityProviderRepository identityProviderRepository = mock(IdentityProviderRepository.class);
    private final IdentityProviderService identityProviderService =
        new IdentityProviderServiceImpl(encryption, identityProviderRepository);

    @Test
    void testCreateServiceProviderSigningCredentialIfAbsentEncryptsThePrivateKey() {
        IdentityProvider storedIdentityProvider = identityProvider(7L);

        when(encryption.encrypt("private-key")).thenReturn("encrypted-private-key");
        when(identityProviderRepository.findById(7L)).thenReturn(Optional.of(storedIdentityProvider));

        IdentityProvider identityProvider = identityProviderService.createServiceProviderSigningCredentialIfAbsent(
            7L, "certificate", "private-key");

        verify(identityProviderRepository).updateServiceProviderSigningCredentialIfAbsent(
            7L, "certificate", "encrypted-private-key");

        assertThat(identityProvider).isSameAs(storedIdentityProvider);
    }

    @Test
    void testUpdateKeepsTheServiceProviderSigningCredential() {
        IdentityProvider existingIdentityProvider = identityProvider(7L);

        existingIdentityProvider.setServiceProviderSigningCertificate("certificate");
        existingIdentityProvider.setServiceProviderSigningKey("encrypted-private-key");

        when(identityProviderRepository.findById(7L)).thenReturn(Optional.of(existingIdentityProvider));
        when(identityProviderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProvider updatedIdentityProvider = identityProviderService.update(identityProvider(7L));

        assertThat(updatedIdentityProvider.getServiceProviderSigningCertificate()).isEqualTo("certificate");
        assertThat(updatedIdentityProvider.getServiceProviderSigningKey()).isEqualTo("encrypted-private-key");
    }

    @Test
    void testGetDecryptedServiceProviderSigningKey() {
        IdentityProvider identityProvider = identityProvider(7L);

        identityProvider.setServiceProviderSigningKey("encrypted-private-key");

        when(encryption.decrypt("encrypted-private-key")).thenReturn("private-key");

        assertThat(identityProviderService.getDecryptedServiceProviderSigningKey(identityProvider))
            .isEqualTo("private-key");
    }

    private static IdentityProvider identityProvider(long id) {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(id);

        return identityProvider;
    }
}
