/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web.graphql;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller exposing admin licence management operations.
 *
 * <p>
 * All operations require {@code ROLE_ADMIN} authority enforced via {@code @PreAuthorize} on each mapping.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
public class LicenceGraphQlController {

    private final LicenceJobUsageService licenceJobUsageService;
    private final LicenceManager licenceManager;

    public LicenceGraphQlController(LicenceManager licenceManager, LicenceJobUsageService licenceJobUsageService) {
        this.licenceJobUsageService = licenceJobUsageService;
        this.licenceManager = licenceManager;
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LicenceType licence() {
        Optional<Licence> licenceOptional = licenceManager.getLicence();
        long currentMonthJobUsage = licenceJobUsageService.currentMonthUsage();

        return toLicenceType(licenceOptional, currentMonthJobUsage);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public LicenceType uploadLicence(@Argument String contents) {
        Licence licence = licenceManager.upload(contents.getBytes(StandardCharsets.UTF_8));
        long currentMonthJobUsage = licenceJobUsageService.currentMonthUsage();

        return toLicenceType(Optional.of(licence), currentMonthJobUsage);
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public boolean deleteLicence() {
        licenceManager.delete();

        return true;
    }

    private LicenceType toLicenceType(Optional<Licence> licenceOptional, long currentMonthJobUsage) {
        String status = licenceManager.getStatus()
            .name();

        if (licenceOptional.isEmpty()) {
            return new LicenceType(null, null, null, null, null, status, List.of(), 0L, null, currentMonthJobUsage);
        }

        Licence licence = licenceOptional.get();

        List<String> features = licence.features()
            .stream()
            .map(LicenceFeature::getKey)
            .sorted()
            .toList();

        return new LicenceType(
            licence.id(),
            licence.holderName(),
            licence.holderEmail(),
            toIsoString(licence.issuedAt()),
            toIsoString(licence.expiresAt()),
            status,
            features,
            licence.allowedJobs(),
            licence.maxUsers(),
            currentMonthJobUsage);
    }

    private static String toIsoString(Instant instant) {
        if (instant == null) {
            return null;
        }

        return instant.toString();
    }

    record LicenceType(
        String id, String holderName, String holderEmail, String issuedAt, String expiresAt, String status,
        List<String> features, long allowedJobs, Integer maxUsers, long currentMonthJobUsage) {
    }
}
