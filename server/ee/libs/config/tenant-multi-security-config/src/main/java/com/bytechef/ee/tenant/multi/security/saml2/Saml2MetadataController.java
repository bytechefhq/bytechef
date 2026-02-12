/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.security.saml2;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.tenant.annotation.ConditionalOnMultiTenant;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml5MetadataResolver;
import org.springframework.security.saml2.provider.service.metadata.Saml2MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes SAML 2.0 Service Provider (SP) metadata for each relying party registration. Identity providers use this
 * metadata to configure trust with ByteChef.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController
@RequestMapping("/saml2/metadata")
@ConditionalOnEEVersion
@ConditionalOnMultiTenant
@ConditionalOnProperty(prefix = "bytechef.security.sso", name = "enabled", havingValue = "true")
class Saml2MetadataController {

    private final Saml2MetadataResolver metadataResolver = new OpenSaml5MetadataResolver();
    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

    @SuppressFBWarnings("EI")
    Saml2MetadataController(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
    }

    @GetMapping("/{registrationId}")
    ResponseEntity<String> getMetadata(@PathVariable String registrationId) {
        RelyingPartyRegistration registration = relyingPartyRegistrationRepository.findByRegistrationId(registrationId);

        if (registration == null) {
            return ResponseEntity.notFound()
                .build();
        }

        String metadata = metadataResolver.resolve(registration);

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sp-metadata.xml")
            .body(metadata);
    }
}
