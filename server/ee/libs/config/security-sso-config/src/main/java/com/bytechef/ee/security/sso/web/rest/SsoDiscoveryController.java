/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.web.rest;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.constant.UserConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for SSO discovery. Given an email address or company name, returns the redirect URL for the matching
 * identity provider (if one is configured for the email's domain or company name).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/api")
@ConditionalOnEEVersion
final class SsoDiscoveryController {

    private final IdentityProviderService identityProviderService;

    @SuppressFBWarnings("EI")
    SsoDiscoveryController(ObjectProvider<IdentityProviderService> identityProviderServiceProvider) {
        this.identityProviderService = identityProviderServiceProvider.getIfAvailable();
    }

    @PostMapping("/sso/discover")
    ResponseEntity<SsoDiscoveryResponse> discover(@RequestBody SsoDiscoveryRequest request) {
        if (identityProviderService == null) {
            return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
        }

        String email = request.email();

        if (email == null || !email.contains("@")) {
            return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
        }

        String domain = email.substring(email.lastIndexOf('@') + 1);

        Optional<IdentityProvider> identityProvider = identityProviderService.fetchByDomain(domain);

        if (identityProvider.isPresent() && identityProvider.get()
            .isEnabled()) {
            IdentityProvider idp = identityProvider.get();

            String redirectUrl;

            if (UserConstants.AUTH_PROVIDER_SAML.equals(idp.getType())) {
                redirectUrl = "/saml2/authenticate/saml-" + idp.getId();
            } else {
                redirectUrl = "/oauth2/authorization/sso-" + idp.getId();
            }

            return ResponseEntity.ok(new SsoDiscoveryResponse(redirectUrl, idp.getName()));
        }

        return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
    }

    @GetMapping("/sso/discover-by-name")
    ResponseEntity<SsoDiscoveryResponse> discoverByName(@RequestParam String company) {
        if (identityProviderService == null) {
            return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
        }

        if (company == null || company.isBlank()) {
            return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
        }

        Optional<IdentityProvider> identityProviderOptional = identityProviderService.fetchByName(company.trim());

        if (identityProviderOptional.isPresent()) {
            IdentityProvider identityProvider = identityProviderOptional.get();
            if (identityProvider.isEnabled()) {
                String redirectUrl;

                if (UserConstants.AUTH_PROVIDER_SAML.equals(identityProvider.getType())) {
                    redirectUrl = "/saml2/authenticate/saml-" + identityProvider.getId();
                } else {
                    redirectUrl = "/oauth2/authorization/sso-" + identityProvider.getId();
                }

                return ResponseEntity.ok(new SsoDiscoveryResponse(redirectUrl, identityProvider.getName()));
            }
        }

        return ResponseEntity.ok(new SsoDiscoveryResponse(null, null));
    }

    record SsoDiscoveryRequest(String email) {
    }

    record SsoDiscoveryResponse(String redirectUrl, String providerName) {
    }
}
