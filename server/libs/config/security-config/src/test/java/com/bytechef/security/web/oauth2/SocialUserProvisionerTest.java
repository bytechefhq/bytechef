/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.security.web.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.SignUpEmailDomainValidator;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.ProvisionedUser;
import com.bytechef.security.web.oauth2.SocialUserProvisioner.SocialIdentity;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * @author Ivica Cardic
 */
class SocialUserProvisionerTest {

    private static final long ADMIN_AUTHORITY_ID = 1L;
    private static final String EMAIL = "someone@example.com";
    private static final SocialIdentity SOCIAL_IDENTITY = new SocialIdentity(
        "GOOGLE", "provider-id", EMAIL, "Some", "One", "https://example.com/avatar.png");

    private final ApplicationProperties applicationProperties = new ApplicationProperties();
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final SignUpEmailDomainValidator signUpEmailDomainValidator = mock(SignUpEmailDomainValidator.class);
    private final SsoEnforcementChecker ssoEnforcementChecker = mock(SsoEnforcementChecker.class);
    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);

    private SocialUserProvisioner socialUserProvisioner;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        ObjectProvider<SignUpEmailDomainValidator> signUpEmailDomainValidatorProvider = mock(ObjectProvider.class);

        ObjectProvider<SsoEnforcementChecker> ssoEnforcementCheckerProvider = mock(ObjectProvider.class);

        when(signUpEmailDomainValidatorProvider.getIfAvailable()).thenReturn(signUpEmailDomainValidator);
        when(ssoEnforcementCheckerProvider.getIfAvailable()).thenReturn(ssoEnforcementChecker);
        when(signUpEmailDomainValidator.isEmailDomainValid(anyString())).thenReturn(true);
        when(authorityService.fetchAuthority(ADMIN_AUTHORITY_ID))
            .thenReturn(Optional.of(new Authority().id(ADMIN_AUTHORITY_ID)
                .name(AuthorityConstants.ADMIN)));

        socialUserProvisioner = new SocialUserProvisioner(
            applicationProperties, authorityService, signUpEmailDomainValidatorProvider, ssoEnforcementCheckerProvider,
            tenantService, userService);
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testSingleTenantFirstUserRegistersAsAdmin() {
        when(userService.countActiveUsers()).thenReturn(0L);
        mockFindOrCreateSocialUser(new AtomicReference<>());

        ProvisionedUser provisionedUser = socialUserProvisioner.provision(SOCIAL_IDENTITY);

        verify(userService).findOrCreateSocialUser(
            EMAIL, "Some", "One", "https://example.com/avatar.png", "GOOGLE", "provider-id", true,
            AuthorityConstants.ADMIN);

        assertThat(provisionedUser.login()).isEqualTo(EMAIL);
        assertThat(provisionedUser.authorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly(AuthorityConstants.ADMIN);
    }

    @Test
    void testSingleTenantWithExistingOrganizationOnlySignsIn() {
        when(userService.countActiveUsers()).thenReturn(1L);
        mockFindOrCreateSocialUser(new AtomicReference<>());

        socialUserProvisioner.provision(SOCIAL_IDENTITY);

        verify(userService).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq(false), anyString());
        verify(signUpEmailDomainValidator, never()).isEmailDomainValid(anyString());
    }

    @Test
    void testSingleTenantWithExistingOrganizationRejectsUnknownEmail() {
        when(userService.countActiveUsers()).thenReturn(1L);
        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString()))
                    .thenThrow(new IllegalStateException("Auto-provisioning is disabled"));

        assertThatThrownBy(() -> socialUserProvisioner.provision(SOCIAL_IDENTITY))
            .isInstanceOf(OAuth2AuthenticationException.class)
            .extracting(exception -> ((OAuth2AuthenticationException) exception).getError()
                .getErrorCode())
            .isEqualTo("account_not_provisioned");
    }

    @Test
    void testSingleTenantFirstUserRejectedByEmailDomain() {
        when(userService.countActiveUsers()).thenReturn(0L);
        when(signUpEmailDomainValidator.isEmailDomainValid(EMAIL)).thenReturn(false);

        assertSocialLoginFails(SocialUserProvisioner.INVALID_EMAIL_DOMAIN_ERROR_CODE);

        verify(userService, never()).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void testMultiTenantExistingUserSignsInInsideTheirTenant() {
        AtomicReference<String> tenantIdDuringLookup = new AtomicReference<>();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of("000002"));
        mockFindOrCreateSocialUser(tenantIdDuringLookup);

        socialUserProvisioner.provision(SOCIAL_IDENTITY);

        assertThat(tenantIdDuringLookup.get()).isEqualTo("000002");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);

        verify(userService).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq(false), anyString());
        verify(tenantService, never()).createTenant();
    }

    @Test
    void testMultiTenantUnknownEmailRegistersIntoNewTenant() {
        AtomicReference<String> tenantIdDuringLookup = new AtomicReference<>();

        applicationProperties.getSignUp()
            .setEnabled(true);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of());
        when(tenantService.createTenant()).thenReturn("000007");
        mockFindOrCreateSocialUser(tenantIdDuringLookup);

        socialUserProvisioner.provision(SOCIAL_IDENTITY);

        assertThat(tenantIdDuringLookup.get()).isEqualTo("000007");

        verify(userService).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), eq(true),
            eq(AuthorityConstants.ADMIN));
    }

    @Test
    void testMultiTenantUnknownEmailRejectedWhenSignUpDisabled() {
        applicationProperties.getSignUp()
            .setEnabled(false);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of());

        assertSocialLoginFails(SocialUserProvisioner.SIGN_UP_DISABLED_ERROR_CODE);

        verify(tenantService, never()).createTenant();
    }

    @Test
    void testMultiTenantUnknownEmailRejectedByEmailDomain() {
        applicationProperties.getSignUp()
            .setEnabled(true);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of());
        when(signUpEmailDomainValidator.isEmailDomainValid(EMAIL)).thenReturn(false);

        assertSocialLoginFails(SocialUserProvisioner.INVALID_EMAIL_DOMAIN_ERROR_CODE);

        verify(tenantService, never()).createTenant();
    }

    @Test
    void testMultiTenantFailedRegistrationRemovesTheNewTenant() {
        applicationProperties.getSignUp()
            .setEnabled(true);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail(EMAIL)).thenReturn(List.of());
        when(tenantService.createTenant()).thenReturn("000007");
        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                anyString()))
                    .thenThrow(new IllegalArgumentException("boom"));

        assertThatThrownBy(() -> socialUserProvisioner.provision(SOCIAL_IDENTITY))
            .isInstanceOf(IllegalArgumentException.class);

        verify(tenantService).deleteTenant("000007");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    void testSsoEnforcedDomainRefusesSocialLogin() {
        when(ssoEnforcementChecker.isSsoEnforced(EMAIL)).thenReturn(true);

        assertSocialLoginFails(SocialUserProvisioner.SSO_REQUIRED_ERROR_CODE);

        verify(userService, never()).findOrCreateSocialUser(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    private void assertSocialLoginFails(String errorCode) {
        assertThatThrownBy(() -> socialUserProvisioner.provision(SOCIAL_IDENTITY))
            .isInstanceOf(OAuth2AuthenticationException.class)
            .extracting(exception -> ((OAuth2AuthenticationException) exception).getError()
                .getErrorCode())
            .isEqualTo(errorCode);
    }

    private void mockFindOrCreateSocialUser(AtomicReference<String> tenantIdDuringLookup) {
        when(
            userService.findOrCreateSocialUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyBoolean(),
                any()))
                    .thenAnswer(invocation -> {
                        tenantIdDuringLookup.set(TenantContext.getCurrentTenantId());

                        User user = new User();

                        user.setLogin(EMAIL);
                        user.setAuthorityIds(List.of(ADMIN_AUTHORITY_ID));

                        return user;
                    });
    }
}
