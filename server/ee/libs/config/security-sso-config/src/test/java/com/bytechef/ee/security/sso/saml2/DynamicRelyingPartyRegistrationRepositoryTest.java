/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.ee.security.sso.saml2.ServiceProviderSigningCredentials.PemCredential;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.tenant.TenantContext;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DynamicRelyingPartyRegistrationRepositoryTest {

    private static final String REGISTRATION_ID = "saml-7";

    private final IdentityProviderLocator identityProviderLocator = mock(IdentityProviderLocator.class);
    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository =
        new DynamicRelyingPartyRegistrationRepository(identityProviderLocator, identityProviderService);

    @Test
    void testCreatesASigningCredentialForAnIdentityProviderThatHasNone() {
        IdentityProvider identityProvider = samlIdentityProvider();

        locate(identityProvider);

        when(identityProviderService.createServiceProviderSigningCredentialIfAbsent(eq(7L), anyString(), anyString()))
            .thenAnswer(invocation -> {
                IdentityProvider storedIdentityProvider = samlIdentityProvider();

                storedIdentityProvider.setServiceProviderSigningCertificate(invocation.getArgument(1));
                storedIdentityProvider.setServiceProviderSigningKey("encrypted:" + invocation.getArgument(2));

                return storedIdentityProvider;
            });
        when(identityProviderService.getDecryptedServiceProviderSigningKey(any()))
            .thenAnswer(invocation -> {
                IdentityProvider storedIdentityProvider = invocation.getArgument(0);

                return storedIdentityProvider.getServiceProviderSigningKey()
                    .substring("encrypted:".length());
            });

        RelyingPartyRegistration relyingPartyRegistration =
            dynamicRelyingPartyRegistrationRepository.findByRegistrationId(REGISTRATION_ID);

        assertThat(relyingPartyRegistration).isNotNull();
        assertThat(relyingPartyRegistration.getAssertingPartyMetadata()
            .getWantAuthnRequestsSigned()).isTrue();
        assertThat(relyingPartyRegistration.getSigningX509Credentials()).singleElement()
            .extracting(Saml2X509Credential::isSigningCredential)
            .isEqualTo(true);
    }

    @Test
    void testReusesTheStoredSigningCredential() {
        PemCredential pemCredential = ServiceProviderSigningCredentials.generate("ByteChef " + REGISTRATION_ID);

        IdentityProvider identityProvider = samlIdentityProvider();

        identityProvider.setServiceProviderSigningCertificate(pemCredential.certificate());
        identityProvider.setServiceProviderSigningKey("encrypted");

        locate(identityProvider);

        when(identityProviderService.getDecryptedServiceProviderSigningKey(identityProvider))
            .thenReturn(pemCredential.privateKey());

        RelyingPartyRegistration relyingPartyRegistration =
            dynamicRelyingPartyRegistrationRepository.findByRegistrationId(REGISTRATION_ID);

        assertThat(relyingPartyRegistration.getSigningX509Credentials()).singleElement()
            .extracting(Saml2X509Credential::getCertificate)
            .isEqualTo(ServiceProviderSigningCredentials.toSigningCredential(
                pemCredential.certificate(), pemCredential.privateKey())
                .getCertificate());

        verify(identityProviderService, never()).createServiceProviderSigningCredentialIfAbsent(
            anyLong(), anyString(), anyString());
    }

    @Test
    void testServiceProviderMetadataPublishesTheSigningCertificate() {
        PemCredential pemCredential = ServiceProviderSigningCredentials.generate("ByteChef " + REGISTRATION_ID);

        IdentityProvider identityProvider = samlIdentityProvider();

        identityProvider.setServiceProviderSigningCertificate(pemCredential.certificate());
        identityProvider.setServiceProviderSigningKey("encrypted");

        locate(identityProvider);

        when(identityProviderService.getDecryptedServiceProviderSigningKey(identityProvider))
            .thenReturn(pemCredential.privateKey());

        String metadata = new OpenSaml5MetadataResolver().resolve(
            dynamicRelyingPartyRegistrationRepository.findByRegistrationId(REGISTRATION_ID));

        String certificateBase64 = pemCredential.certificate()
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");

        assertThat(metadata).contains("<md:KeyDescriptor use=\"signing\">");
        assertThat(metadata.replaceAll("\\s", "")).contains(certificateBase64);
    }

    private void locate(IdentityProvider identityProvider) {
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(new LocatedIdentityProvider(TenantContext.DEFAULT_TENANT_ID, identityProvider)));
    }

    private static IdentityProvider samlIdentityProvider() {
        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setEnabled(true);
        identityProvider.setId(7L);
        identityProvider.setMetadataUri("classpath:saml2/keycloak-idp-metadata.xml");
        identityProvider.setType(UserConstants.AUTH_PROVIDER_SAML);

        return identityProvider;
    }
}
