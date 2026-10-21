/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.domain.AppUserAuthMode;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * @version ee
 */
class AppUserTokenVerifierTest {

    private static final String ISSUER = "https://bytechef.example.com";
    private static final String CLIENT_ID = "app-6b0f2c4e";
    private static final String LOGIN = "member@example.com";
    private static final String TENANT_ID = "public";
    private static final long APP_INSTANCE_ID = 11L;
    private static final long USER_ID = 5L;

    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final PermissionService permissionService = mock(PermissionService.class);
    private final SelfIssuedTokenRevocationChecker revocationChecker = mock(SelfIssuedTokenRevocationChecker.class);
    private final UserService userService = mock(UserService.class);

    private AppInstance appInstance;
    private JwtDecoder jwtDecoder;
    private NimbusJwtEncoder jwtEncoder;
    private NimbusJwtEncoder otherKeyJwtEncoder;
    private User user;
    private AppUserTokenVerifier appUserTokenVerifier;

    @BeforeEach
    void beforeEach() throws JOSEException {
        RSAKey rsaKey = new RSAKeyGenerator(2048).keyID("signing")
            .generate();
        RSAKey otherRsaKey = new RSAKeyGenerator(2048).keyID("signing")
            .generate();

        jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey())
            .build();
        jwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        otherKeyJwtEncoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(otherRsaKey)));

        appInstance = new AppInstance();

        appInstance.setId(APP_INSTANCE_ID);
        appInstance.setRegisteredClientId(CLIENT_ID);
        appInstance.setUserAuthMode(AppUserAuthMode.REQUIRED);

        user = new User();

        user.setActivated(true);
        user.setAuthorityIds(List.of(1L));
        user.setId(USER_ID);
        user.setLogin(LOGIN);

        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName("ROLE_USER");

        when(authorityService.fetchAuthority(1L)).thenReturn(Optional.of(authority));
        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(user));
        when(revocationChecker.isRevoked(anyString(), anyString())).thenReturn(false);
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenReturn(true);

        appUserTokenVerifier = createVerifier(jwtDecoder, revocationChecker);
    }

    @Test
    void testValidTokenReturnsTheUser() {
        AppUser appUser = appUserTokenVerifier.authenticate(appInstance, TENANT_ID, token(claims()));

        assertThat(appUser).isEqualTo(new AppUser(USER_ID, LOGIN));
    }

    @Test
    void testAppUseIsCheckedAsTheUserInTheKeyTenant() {
        List<String> checkedAs = new ArrayList<>();

        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenAnswer(invocation -> {
            checkedAs.add(
                SecurityUtils.getCurrentUserLogin() + "@" + TenantContext.getCurrentTenantId() + " " +
                    SecurityUtils.fetchCurrentUserAuthorities());

            return true;
        });

        TenantContext.runWithTenantId(
            "other", () -> appUserTokenVerifier.authenticate(appInstance, TENANT_ID, token(claims())));

        assertThat(checkedAs).containsExactly(LOGIN + "@" + TENANT_ID + " [ROLE_USER]");
    }

    @Test
    void testExpiredTokenIs401() {
        Instant issuedAt = Instant.now()
            .minusSeconds(3600);

        String expiredToken = token(
            claims().issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(600)));

        assertTokenInvalid(expiredToken);
    }

    @Test
    void testTokenWithoutExpiryIs401() {
        assertTokenInvalid(
            token(
                JwtClaimsSet.builder()
                    .issuer(ISSUER)
                    .subject(LOGIN)
                    .audience(List.of(CLIENT_ID))
                    .claim("scope", List.of("openid", "profile", "app:use"))
                    .claim("tenant_id", TENANT_ID)));
    }

    @Test
    void testTokenSignedWithAnotherKeyIs401() {
        String forgedToken = otherKeyJwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256)
                    .build(),
                claims().build()))
            .getTokenValue();

        assertTokenInvalid(forgedToken);
    }

    @Test
    void testMalformedTokenIs401() {
        assertTokenInvalid("not-a-jwt");
    }

    @Test
    void testOtherIssuerIs401() {
        assertTokenInvalid(token(claims().issuer("https://idp.example.org")));
    }

    @Test
    void testOtherTenantTokenIs401() {
        assertTokenInvalid(token(claims().claim("tenant_id", "acme")));

        verifyNoInteractions(userService, permissionService);
    }

    @Test
    void testTokenWithoutTenantClaimIs401() {
        JwtClaimsSet.Builder claims = claims();

        claims.claims(claimMap -> claimMap.remove("tenant_id"));

        assertTokenInvalid(token(claims));
    }

    @Test
    void testTenantIsComparedBeforeAnyLookup() {
        assertTokenInvalid(token(claims().claim("tenant_id", "acme")));

        verify(revocationChecker, never()).isRevoked(anyString(), anyString());
        verifyNoInteractions(userService, authorityService, permissionService);
    }

    @Test
    void testTokenOfAnotherInstanceIs401() {
        assertTokenInvalid(token(claims().audience(List.of("app-5d1e9a77"))));
    }

    @Test
    void testTokenOfAFormerClientOfTheInstanceIs401() {
        String formerClientToken = token(claims());

        appInstance.setRegisteredClientId("app-replacement");

        assertTokenInvalid(formerClientToken);
    }

    @Test
    void testInstanceWithoutClientRefusesEveryToken() {
        appInstance.setRegisteredClientId(null);

        assertTokenInvalid(token(claims()));
    }

    @Test
    void testMcpTokenIs401() {
        String mcpToken = token(
            claims().audience(List.of(ISSUER + "/api/automation/tenant/mcp"))
                .claim("scope", List.of("mcp:automation")));

        assertTokenInvalid(mcpToken);
    }

    @Test
    void testTokenWithoutAppUseScopeIs401() {
        assertTokenInvalid(token(claims().claim("scope", List.of("openid", "profile"))));
    }

    @Test
    void testSpaceDelimitedScopeIsAccepted() {
        AppUser appUser = appUserTokenVerifier.authenticate(
            appInstance, TENANT_ID, token(claims().claim("scope", "openid profile app:use")));

        assertThat(appUser.id()).isEqualTo(USER_ID);
    }

    @Test
    void testRevokedTokenIs401() {
        String revokedToken = token(claims());

        when(revocationChecker.isRevoked(revokedToken, TENANT_ID)).thenReturn(true);

        assertTokenInvalid(revokedToken);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testTokenWithoutRevocationCheckerIs401() {
        AppUserTokenVerifier verifierWithoutChecker = createVerifier(jwtDecoder, null);

        assertThatThrownBy(() -> verifierWithoutChecker.authenticate(appInstance, TENANT_ID, token(claims())))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_TOKEN_INVALID));
    }

    @Test
    void testFailingRevocationCheckIsNotAnInvalidToken() {
        when(revocationChecker.isRevoked(anyString(), anyString())).thenThrow(new IllegalStateException("down"));

        String validToken = token(claims());

        assertThatThrownBy(() -> appUserTokenVerifier.authenticate(appInstance, TENANT_ID, validToken))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testAbsentAuthorizationServerMakesEveryTokenInvalid() {
        AppUserTokenVerifier verifierWithoutDecoder = createVerifier(null, revocationChecker);

        assertThatThrownBy(() -> verifierWithoutDecoder.authenticate(appInstance, TENANT_ID, token(claims())))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_TOKEN_INVALID));
        assertThat(verifierWithoutDecoder.getIssuer()).isEqualTo(ISSUER);
    }

    @Test
    void testMissingPublicUrlMakesEveryTokenInvalid() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        AppUserTokenVerifier verifierWithoutIssuer = new AppUserTokenVerifier(
            applicationProperties, authorityService, provider(jwtDecoder), permissionService,
            provider(revocationChecker), userService, provider(meterRegistry));

        assertThat(verifierWithoutIssuer.getIssuer()).isNull();

        String validToken = token(claims());

        assertThatThrownBy(() -> verifierWithoutIssuer.authenticate(appInstance, TENANT_ID, validToken))
            .isInstanceOf(AppUserAuthenticationException.class);
    }

    @Test
    void testUnknownUserIs403() {
        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.empty());

        assertForbidden(token(claims()));
    }

    @Test
    void testDeactivatedUserIs403() {
        String validToken = token(claims());

        assertThat(appUserTokenVerifier.authenticate(appInstance, TENANT_ID, validToken)).isNotNull();

        user.setActivated(false);

        assertForbidden(validToken);
    }

    @Test
    void testGrantRevokedBetweenCallsIs403() {
        String validToken = token(claims());

        assertThat(appUserTokenVerifier.authenticate(appInstance, TENANT_ID, validToken)).isNotNull();

        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenReturn(false);

        assertForbidden(validToken);
    }

    @Test
    void testRemovedWorkspaceMemberIs403() {
        AtomicBoolean member = new AtomicBoolean(true);

        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE"))
            .thenAnswer(invocation -> member.get() && LOGIN.equals(SecurityUtils.getCurrentUserLogin()));

        String validToken = token(claims());

        assertThat(appUserTokenVerifier.authenticate(appInstance, TENANT_ID, validToken)).isNotNull();

        member.set(false);

        assertForbidden(validToken);
    }

    @Test
    void testRoleChangedBetweenCallsIs403() {
        Authority adminAuthority = new Authority();

        adminAuthority.setId(2L);
        adminAuthority.setName("ROLE_ADMIN");

        when(authorityService.fetchAuthority(2L)).thenReturn(Optional.of(adminAuthority));
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE"))
            .thenAnswer(invocation -> SecurityUtils.fetchCurrentUserAuthorities()
                .contains("ROLE_ADMIN"));

        user.setAuthorityIds(List.of(1L, 2L));

        String validToken = token(claims());

        assertThat(appUserTokenVerifier.authenticate(appInstance, TENANT_ID, validToken)).isNotNull();

        user.setAuthorityIds(List.of(1L));

        assertForbidden(validToken);
    }

    @Test
    void testPermissionCheckLeavesTheCallerContextUntouched() {
        SecurityUtils.runAsSystem(() -> {
            appUserTokenVerifier.authenticate(appInstance, TENANT_ID, token(claims()));

            assertThat(SecurityUtils.getCurrentUserLogin()).isEqualTo(SecurityUtils.SYSTEM_LOGIN);

            return null;
        });
    }

    @Test
    void testFailureMessagesNeverCarryTheToken() {
        String revokedToken = token(claims());

        when(revocationChecker.isRevoked(revokedToken, TENANT_ID)).thenReturn(true);

        assertThatThrownBy(() -> appUserTokenVerifier.authenticate(appInstance, TENANT_ID, revokedToken))
            .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain(revokedToken));
    }

    @Test
    void testCallAsUserRunsWithTheUserAuthorities() {
        Set<String> authorities = appUserTokenVerifier.callAsUser(user, SecurityUtils::fetchCurrentUserAuthorities);

        assertThat(authorities).containsExactly("ROLE_USER");
        verify(authorityService).fetchAuthority(anyLong());
    }

    @Test
    void testAcceptedTokenCountsNoRejection() {
        appUserTokenVerifier.authenticate(appInstance, TENANT_ID, token(claims()));

        assertThat(totalRejected()).isZero();
    }

    @Test
    void testTokenRejectedByTheDecoderValidationCountsValidationFailed() {
        Instant issuedAt = Instant.now()
            .minusSeconds(3600);

        assertTokenInvalid(
            token(
                claims().issuedAt(issuedAt)
                    .expiresAt(issuedAt.plusSeconds(600))));

        assertThat(rejected("validation_failed")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testTokenWithoutExpiryCountsExpired() {
        assertTokenInvalid(
            token(
                JwtClaimsSet.builder()
                    .issuer(ISSUER)
                    .subject(LOGIN)
                    .audience(List.of(CLIENT_ID))
                    .claim("scope", List.of("openid", "profile", "app:use"))
                    .claim("tenant_id", TENANT_ID)));

        assertThat(rejected("expired")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testMalformedAndForgedTokensCountUndecodable() {
        String forgedToken = otherKeyJwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256)
                    .build(),
                claims().build()))
            .getTokenValue();

        assertTokenInvalid("not-a-jwt");
        assertTokenInvalid(forgedToken);

        assertThat(rejected("undecodable")).isEqualTo(2.0);
        assertThat(totalRejected()).isEqualTo(2.0);
    }

    @Test
    void testAbsentDecoderCountsDecoderUnavailable() {
        AppUserTokenVerifier verifierWithoutDecoder = createVerifier(null, revocationChecker);

        assertThatThrownBy(() -> verifierWithoutDecoder.authenticate(appInstance, TENANT_ID, token(claims())))
            .isInstanceOf(AppUserAuthenticationException.class);

        assertThat(rejected("decoder_unavailable")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testOtherIssuerCountsIssuer() {
        assertTokenInvalid(token(claims().issuer("https://idp.example.org")));

        assertThat(rejected("issuer")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testOtherTenantCountsTenant() {
        assertTokenInvalid(token(claims().claim("tenant_id", "acme")));

        assertThat(rejected("tenant")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testOtherAudienceCountsAudienceAndMissingClientCountsClientUnregistered() {
        assertTokenInvalid(token(claims().audience(List.of("app-5d1e9a77"))));

        assertThat(rejected("audience")).isEqualTo(1.0);

        appInstance.setRegisteredClientId(null);

        assertTokenInvalid(token(claims()));

        assertThat(rejected("client_unregistered")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(2.0);
    }

    @Test
    void testMissingAppUseScopeCountsScope() {
        assertTokenInvalid(token(claims().claim("scope", List.of("openid", "profile"))));

        assertThat(rejected("scope")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testRevokedTokenCountsRevoked() {
        String revokedToken = token(claims());

        when(revocationChecker.isRevoked(revokedToken, TENANT_ID)).thenReturn(true);

        assertTokenInvalid(revokedToken);

        assertThat(rejected("revoked")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testAbsentRevocationCheckerCountsRevocationUnavailable() {
        AppUserTokenVerifier verifierWithoutChecker = createVerifier(jwtDecoder, null);

        assertThatThrownBy(() -> verifierWithoutChecker.authenticate(appInstance, TENANT_ID, token(claims())))
            .isInstanceOf(AppUserAuthenticationException.class);

        assertThat(rejected("revocation_unavailable")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testUnknownAndDeactivatedUsersCountUserInactive() {
        String validToken = token(claims());

        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.empty());

        assertForbidden(validToken);

        when(userService.fetchUserByLogin(LOGIN)).thenReturn(Optional.of(user));

        user.setActivated(false);

        assertForbidden(validToken);

        assertThat(rejected("user_inactive")).isEqualTo(2.0);
        assertThat(totalRejected()).isEqualTo(2.0);
    }

    @Test
    void testMissingGrantCountsForbidden() {
        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenReturn(false);

        assertForbidden(token(claims()));

        assertThat(rejected("forbidden")).isEqualTo(1.0);
        assertThat(totalRejected()).isEqualTo(1.0);
    }

    @Test
    void testRejectionCounterCarriesOnlyTheReasonTag() {
        assertTokenInvalid("not-a-jwt");

        Counter counter = meterRegistry.get("bytechef_app_user_token_rejected")
            .counter();

        assertThat(counter.getId()
            .getTags()).extracting(tag -> tag.getKey())
                .containsExactly("reason");
    }

    @Test
    void testRejectionWithoutAMeterRegistryStillThrowsTheSameError() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setPublicUrl(ISSUER);

        AppUserTokenVerifier verifierWithoutRegistry = new AppUserTokenVerifier(
            applicationProperties, authorityService, provider(jwtDecoder), permissionService,
            provider(revocationChecker), userService, provider(null));

        assertThatThrownBy(() -> verifierWithoutRegistry.authenticate(appInstance, TENANT_ID, "not-a-jwt"))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_TOKEN_INVALID));
    }

    @Test
    void testAFailingMeterRegistryDoesNotReplaceTheRejection() {
        MeterRegistry throwingRegistry = new SimpleMeterRegistry() {

            @Override
            protected Counter newCounter(Meter.Id id) {
                throw new IllegalStateException("registration clash");
            }
        };

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setPublicUrl(ISSUER);

        AppUserTokenVerifier verifierWithThrowingRegistry = new AppUserTokenVerifier(
            applicationProperties, authorityService, provider(jwtDecoder), permissionService,
            provider(revocationChecker), userService, provider(throwingRegistry));

        assertThatThrownBy(() -> verifierWithThrowingRegistry.authenticate(appInstance, TENANT_ID, "not-a-jwt"))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_TOKEN_INVALID));

        when(permissionService.hasResourceScope(APP_INSTANCE_ID, "AppInstance", "APP_USE")).thenReturn(false);

        assertThatThrownBy(() -> verifierWithThrowingRegistry.authenticate(appInstance, TENANT_ID, token(claims())))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_FORBIDDEN));
    }

    private void assertForbidden(String userToken) {
        assertThatThrownBy(() -> appUserTokenVerifier.authenticate(appInstance, TENANT_ID, userToken))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_FORBIDDEN));
    }

    private void assertTokenInvalid(String userToken) {
        assertThatThrownBy(() -> appUserTokenVerifier.authenticate(appInstance, TENANT_ID, userToken))
            .isInstanceOfSatisfying(
                AppUserAuthenticationException.class,
                exception -> assertThat(exception.getErrorType()).isEqualTo(AppErrorType.APP_USER_TOKEN_INVALID));
    }

    private double rejected(String reason) {
        Counter counter = meterRegistry.find("bytechef_app_user_token_rejected")
            .tag("reason", reason)
            .counter();

        return counter == null ? 0.0 : counter.count();
    }

    private double totalRejected() {
        return meterRegistry.find("bytechef_app_user_token_rejected")
            .counters()
            .stream()
            .mapToDouble(Counter::count)
            .sum();
    }

    private AppUserTokenVerifier createVerifier(
        JwtDecoder decoder, SelfIssuedTokenRevocationChecker selfIssuedTokenRevocationChecker) {

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.setPublicUrl(ISSUER + "/");

        return new AppUserTokenVerifier(
            applicationProperties, authorityService, provider(decoder), permissionService,
            provider(selfIssuedTokenRevocationChecker), userService, provider(meterRegistry));
    }

    private String token(JwtClaimsSet.Builder claims) {
        return jwtEncoder.encode(
            JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256)
                    .build(),
                claims.build()))
            .getTokenValue();
    }

    private static JwtClaimsSet.Builder claims() {
        Instant now = Instant.now();

        return JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(LOGIN)
            .audience(List.of(CLIENT_ID))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300))
            .claim("scope", List.of("openid", "profile", "app:use"))
            .claim("tenant_id", TENANT_ID);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(bean);

        return objectProvider;
    }
}
