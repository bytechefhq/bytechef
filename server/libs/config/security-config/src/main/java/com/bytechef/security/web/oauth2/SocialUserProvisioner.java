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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.SignUpEmailDomainValidator;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

/**
 * Signs a Google or GitHub identity in, registering it when email/password sign-up would accept the same person.
 *
 * <p>
 * Registration mirrors {@code AccountController.registerAccount} so social login is never a wider door than the sign-up
 * form: in single-tenant mode only the first user of an empty instance registers (and becomes its admin); in
 * multi-tenant mode registration requires {@code bytechef.sign-up.enabled} and creates a new tenant whose only member
 * is the new user. An identity whose email already has an account signs in to it, in the tenant that holds it.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnExpression("${bytechef.security.social-login.enabled:false} or ${bytechef.security.sso.enabled:false}")
public class SocialUserProvisioner {

    static final String INVALID_EMAIL_DOMAIN_ERROR_CODE = "invalid_email_domain";
    static final String SIGN_UP_DISABLED_ERROR_CODE = "sign_up_disabled";
    static final String SSO_REQUIRED_ERROR_CODE = "sso_required";

    private static final Logger log = LoggerFactory.getLogger(SocialUserProvisioner.class);

    private final ApplicationProperties applicationProperties;
    private final AuthorityService authorityService;
    private final ObjectProvider<SignUpEmailDomainValidator> signUpEmailDomainValidatorProvider;
    private final ObjectProvider<SsoEnforcementChecker> ssoEnforcementCheckerProvider;
    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public SocialUserProvisioner(
        ApplicationProperties applicationProperties, AuthorityService authorityService,
        ObjectProvider<SignUpEmailDomainValidator> signUpEmailDomainValidatorProvider,
        ObjectProvider<SsoEnforcementChecker> ssoEnforcementCheckerProvider, TenantService tenantService,
        UserService userService) {

        this.applicationProperties = applicationProperties;
        this.authorityService = authorityService;
        this.signUpEmailDomainValidatorProvider = signUpEmailDomainValidatorProvider;
        this.ssoEnforcementCheckerProvider = ssoEnforcementCheckerProvider;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    public ProvisionedUser provision(SocialIdentity socialIdentity) {
        SsoEnforcementChecker ssoEnforcementChecker = ssoEnforcementCheckerProvider.getIfAvailable();

        if (ssoEnforcementChecker != null && ssoEnforcementChecker.isSsoEnforced(socialIdentity.email())) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(SSO_REQUIRED_ERROR_CODE, "Your organization requires SSO login", null));
        }

        if (tenantService.isMultiTenantEnabled()) {
            return provisionMultiTenant(socialIdentity);
        }

        boolean register = userService.countActiveUsers() == 0;

        if (register) {
            validateEmailDomain(socialIdentity.email());
        }

        return resolve(socialIdentity, register);
    }

    private ProvisionedUser provisionMultiTenant(SocialIdentity socialIdentity) {
        List<String> tenantIds = tenantService.getTenantIdsByUserEmail(socialIdentity.email());

        if (!tenantIds.isEmpty()) {
            return withTenantId(tenantIds.getFirst(), socialIdentity, false);
        }

        if (!applicationProperties.getSignUp()
            .isEnabled()) {

            throw new OAuth2AuthenticationException(
                new OAuth2Error(SIGN_UP_DISABLED_ERROR_CODE, "Sign-up is disabled", null));
        }

        validateEmailDomain(socialIdentity.email());

        String tenantId = tenantService.createTenant();

        try {
            return withTenantId(tenantId, socialIdentity, true);
        } catch (RuntimeException runtimeException) {
            log.error("Social sign-up failed, removing tenant {}", tenantId, runtimeException);

            tenantService.deleteTenant(tenantId);

            throw runtimeException;
        }
    }

    private ProvisionedUser resolve(SocialIdentity socialIdentity, boolean register) {
        User user = SocialUserResolver.resolveUser(
            userService, socialIdentity.email(), socialIdentity.firstName(), socialIdentity.lastName(),
            socialIdentity.imageUrl(), socialIdentity.authProvider(), socialIdentity.providerId(), register,
            AuthorityConstants.ADMIN);

        List<GrantedAuthority> grantedAuthorities = user.getAuthorityIds()
            .stream()
            .map(authorityId -> authorityService.fetchAuthority(authorityId)
                .orElseThrow(() -> new OAuth2AuthenticationException(
                    "Unknown authority id " + authorityId + " for user " + user.getLogin())))
            .map(Authority::getName)
            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
            .toList();

        return new ProvisionedUser(user.getLogin(), grantedAuthorities);
    }

    private void validateEmailDomain(String email) {
        SignUpEmailDomainValidator signUpEmailDomainValidator = signUpEmailDomainValidatorProvider.getIfAvailable();

        if (signUpEmailDomainValidator != null && !signUpEmailDomainValidator.isEmailDomainValid(email)) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(INVALID_EMAIL_DOMAIN_ERROR_CODE, "Email domain is not allowed to sign up", null));
        }
    }

    private ProvisionedUser withTenantId(String tenantId, SocialIdentity socialIdentity, boolean register) {
        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            return resolve(socialIdentity, register);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    public record ProvisionedUser(String login, List<GrantedAuthority> authorities) {

        public ProvisionedUser {
            authorities = List.copyOf(authorities);
        }
    }

    public record SocialIdentity(
        String authProvider, String providerId, String email, String firstName, String lastName, String imageUrl) {
    }
}
