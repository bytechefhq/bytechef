/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.security.sso.web.rest;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.IdentityProviderRegistrationId;
import com.bytechef.ee.security.sso.IdentityProviderLocator;
import com.bytechef.ee.security.sso.IdentityProviderLocator.LocatedIdentityProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
final class SsoDiscoveryController {

    private final IdentityProviderLocator identityProviderLocator;

    @SuppressFBWarnings("EI")
    SsoDiscoveryController(IdentityProviderLocator identityProviderLocator) {
        this.identityProviderLocator = identityProviderLocator;
    }

    @PostMapping("/sso/discover")
    ResponseEntity<SsoDiscoveryResponse> discover(@RequestBody SsoDiscoveryRequest request) {
        String email = request.email();

        if (email == null || !email.contains("@")) {
            return ResponseEntity.ok(SsoDiscoveryResponse.NONE);
        }

        String domain = email.substring(email.lastIndexOf('@') + 1);

        return ResponseEntity.ok(toResponse(identityProviderLocator.findByDomain(domain)));
    }

    @GetMapping("/sso/discover-by-name")
    ResponseEntity<SsoDiscoveryResponse> discoverByName(@RequestParam String company) {
        if (company == null || company.isBlank()) {
            return ResponseEntity.ok(SsoDiscoveryResponse.NONE);
        }

        return ResponseEntity.ok(toResponse(identityProviderLocator.findByName(company.trim())));
    }

    private static SsoDiscoveryResponse toResponse(Optional<LocatedIdentityProvider> locatedIdentityProvider) {
        if (locatedIdentityProvider.isEmpty() || !locatedIdentityProvider.get()
            .identityProvider()
            .isEnabled()) {

            return SsoDiscoveryResponse.NONE;
        }

        IdentityProvider identityProvider = locatedIdentityProvider.get()
            .identityProvider();
        IdentityProviderRegistrationId registrationId = locatedIdentityProvider.get()
            .registrationId();

        String redirectUrl = registrationId.isSaml()
            ? "/saml2/authenticate/" + registrationId.value()
            : "/oauth2/authorization/" + registrationId.value();

        return new SsoDiscoveryResponse(redirectUrl, identityProvider.getName());
    }

    record SsoDiscoveryRequest(String email) {
    }

    record SsoDiscoveryResponse(String redirectUrl, String providerName) {

        static final SsoDiscoveryResponse NONE = new SsoDiscoveryResponse(null, null);
    }
}
