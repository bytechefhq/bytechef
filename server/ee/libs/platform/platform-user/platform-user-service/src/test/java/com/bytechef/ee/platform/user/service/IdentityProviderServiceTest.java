/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.repository.IdentityProviderRepository;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.user.constant.UserConstants;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    void testCreateServiceProviderSigningCredentialIfAbsentStoresAnEncryptedGeneratedKey() {
        IdentityProvider storedIdentityProvider = identityProvider(7L);

        when(encryption.encrypt(anyString())).thenAnswer(invocation -> "encrypted:" + invocation.getArgument(0));
        when(identityProviderRepository.findById(7L)).thenReturn(Optional.of(storedIdentityProvider));

        IdentityProvider identityProvider = identityProviderService.createServiceProviderSigningCredentialIfAbsent(7L);

        ArgumentCaptor<String> certificateArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyArgumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(identityProviderRepository).updateServiceProviderSigningCredentialIfAbsent(
            eq(7L), certificateArgumentCaptor.capture(), keyArgumentCaptor.capture());

        assertThat(certificateArgumentCaptor.getValue()).startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(keyArgumentCaptor.getValue()).startsWith("encrypted:-----BEGIN PRIVATE KEY-----");
        assertThat(identityProvider).isSameAs(storedIdentityProvider);
    }

    @Test
    void testCreateGeneratesASigningCredentialForASamlIdentityProviderOnly() {
        when(encryption.encrypt(anyString())).thenReturn("encrypted");
        when(identityProviderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProvider samlIdentityProvider = identityProvider(7L);

        samlIdentityProvider.setType(UserConstants.AUTH_PROVIDER_SAML);

        IdentityProvider createdSamlIdentityProvider = identityProviderService.create(samlIdentityProvider);

        assertThat(createdSamlIdentityProvider.getServiceProviderSigningCertificate())
            .startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(createdSamlIdentityProvider.getServiceProviderSigningKey()).isEqualTo("encrypted");

        IdentityProvider createdOidcIdentityProvider = identityProviderService.create(identityProvider(8L));

        assertThat(createdOidcIdentityProvider.getServiceProviderSigningCertificate()).isNull();
        assertThat(createdOidcIdentityProvider.getServiceProviderSigningKey()).isNull();
    }

    @Test
    void testRegenerateReplacesTheSigningCredential() {
        IdentityProvider existingIdentityProvider = identityProvider(7L);

        existingIdentityProvider.setServiceProviderSigningCertificate("old-certificate");
        existingIdentityProvider.setServiceProviderSigningKey("old-encrypted-key");
        existingIdentityProvider.setType(UserConstants.AUTH_PROVIDER_SAML);

        when(encryption.encrypt(anyString())).thenReturn("new-encrypted-key");
        when(identityProviderRepository.findById(7L)).thenReturn(Optional.of(existingIdentityProvider));
        when(identityProviderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        IdentityProvider regeneratedIdentityProvider =
            identityProviderService.regenerateServiceProviderSigningCredential(7L);

        assertThat(regeneratedIdentityProvider.getServiceProviderSigningCertificate())
            .startsWith("-----BEGIN CERTIFICATE-----");
        assertThat(regeneratedIdentityProvider.getServiceProviderSigningKey()).isEqualTo("new-encrypted-key");

        verify(identityProviderRepository).save(existingIdentityProvider);
    }

    @Test
    void testRegenerateRejectsAnOidcIdentityProvider() {
        when(identityProviderRepository.findById(7L)).thenReturn(Optional.of(identityProvider(7L)));

        assertThatThrownBy(() -> identityProviderService.regenerateServiceProviderSigningCredential(7L))
            .isInstanceOf(IllegalArgumentException.class);

        verify(identityProviderRepository, never()).save(any());
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
