/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.saml2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.tenant.TenantContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
    private static final String SP_SIGNING_CERTIFICATE = resource("sp-signing-certificate.pem");
    private static final String SP_SIGNING_KEY = resource("sp-signing-key.pem");

    private final IdentityProviderLocator identityProviderLocator = mock(IdentityProviderLocator.class);
    private final IdentityProviderService identityProviderService = mock(IdentityProviderService.class);
    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository =
        new DynamicRelyingPartyRegistrationRepository(identityProviderLocator, identityProviderService);

    @Test
    void testCreatesASigningCredentialForAnIdentityProviderThatHasNone() {
        locate(samlIdentityProvider());

        IdentityProvider storedIdentityProvider = withStoredSigningCredential(samlIdentityProvider());

        when(identityProviderService.createServiceProviderSigningCredentialIfAbsent(7L))
            .thenReturn(storedIdentityProvider);

        RelyingPartyRegistration relyingPartyRegistration =
            dynamicRelyingPartyRegistrationRepository.findByRegistrationId(REGISTRATION_ID);

        assertThat(relyingPartyRegistration).isNotNull();
        assertThat(relyingPartyRegistration.getAssertingPartyMetadata()
            .getWantAuthnRequestsSigned()).isTrue();
        assertThat(relyingPartyRegistration.getSigningX509Credentials()).singleElement()
            .satisfies(credential -> {
                assertThat(credential.isSigningCredential()).isTrue();
                assertThat(credential.getCertificate()
                    .getSubjectX500Principal()
                    .getName()).isEqualTo("CN=ByteChef SAML Service Provider");
            });
    }

    @Test
    void testReusesTheStoredSigningCredential() {
        RelyingPartyRegistration relyingPartyRegistration = find(withStoredSigningCredential(samlIdentityProvider()));

        assertThat(relyingPartyRegistration.getSigningX509Credentials()).singleElement()
            .extracting(Saml2X509Credential::getCertificate)
            .isEqualTo(ServiceProviderSigningCredentials.toSigningCredential(SP_SIGNING_CERTIFICATE, SP_SIGNING_KEY)
                .getCertificate());

        verify(identityProviderService, never()).createServiceProviderSigningCredentialIfAbsent(anyLong());
    }

    @Test
    void testServiceProviderMetadataPublishesTheSigningCertificate() {
        String metadata = new OpenSaml5MetadataResolver().resolve(
            find(withStoredSigningCredential(samlIdentityProvider())));

        String certificateBase64 = SP_SIGNING_CERTIFICATE.replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replaceAll("\\s", "");

        assertThat(metadata).contains("<md:KeyDescriptor use=\"signing\">");
        assertThat(metadata.replaceAll("\\s", "")).contains(certificateBase64);
    }

    @Test
    void testVerifiesAssertionsAgainstTheMetadataCertificateByDefault() {
        RelyingPartyRegistration relyingPartyRegistration = find(withStoredSigningCredential(samlIdentityProvider()));

        assertThat(relyingPartyRegistration.getAssertingPartyMetadata()
            .getVerificationX509Credentials()).singleElement()
                .extracting(credential -> credential.getCertificate()
                    .getSubjectX500Principal()
                    .getName())
                .isEqualTo("CN=bytechef-e2e");
    }

    @Test
    void testAPinnedSigningCertificateReplacesTheMetadataCertificates() {
        IdentityProvider identityProvider = withStoredSigningCredential(samlIdentityProvider());

        identityProvider.setSigningCertificate(resource("pinned-idp-certificate.pem"));

        RelyingPartyRegistration relyingPartyRegistration = find(identityProvider);

        assertThat(relyingPartyRegistration.getAssertingPartyMetadata()
            .getVerificationX509Credentials()).singleElement()
                .satisfies(credential -> {
                    assertThat(credential.isVerificationCredential()).isTrue();
                    assertThat(credential.getCertificate()
                        .getSubjectX500Principal()
                        .getName()).isEqualTo("CN=Pinned IdP");
                });
    }

    private RelyingPartyRegistration find(IdentityProvider identityProvider) {
        locate(identityProvider);

        return dynamicRelyingPartyRegistrationRepository.findByRegistrationId(REGISTRATION_ID);
    }

    private void locate(IdentityProvider identityProvider) {
        when(identityProviderLocator.findByRegistrationId(REGISTRATION_ID))
            .thenReturn(Optional.of(new LocatedIdentityProvider(TenantContext.DEFAULT_TENANT_ID, identityProvider)));
    }

    private IdentityProvider withStoredSigningCredential(IdentityProvider identityProvider) {
        identityProvider.setServiceProviderSigningCertificate(SP_SIGNING_CERTIFICATE);
        identityProvider.setServiceProviderSigningKey("encrypted");

        when(identityProviderService.getDecryptedServiceProviderSigningKey(identityProvider))
            .thenReturn(SP_SIGNING_KEY);

        return identityProvider;
    }

    private static String resource(String name) {
        try (InputStream inputStream = DynamicRelyingPartyRegistrationRepositoryTest.class.getResourceAsStream(
            "/saml2/" + name)) {

            return new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII);
        } catch (IOException ioException) {
            throw new UncheckedIOException(ioException);
        }
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
