/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.licence.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.licence.web.graphql.config.LicenceGraphQlTestConfiguration;
import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.licence.LicenceStatus;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Slice tests for {@link LicenceGraphQlController}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    LicenceGraphQlTestConfiguration.class,
    LicenceGraphQlController.class
})
@GraphQlTest(
    controllers = LicenceGraphQlController.class,
    properties = {
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@MockitoBean(types = {
    LicenceManager.class, LicenceJobUsageService.class
})
public class LicenceGraphQlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private LicenceJobUsageService licenceJobUsageService;

    @Autowired
    private LicenceManager licenceManager;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testLicenceQueryReturnsHolderAndStatus() {
        Licence licence = new Licence(
            "lic-123", "Acme Corp", "admin@acme.com",
            Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
            Set.of(LicenceFeature.SSO, LicenceFeature.AUDIT_LOG), 1000L, 50);

        when(licenceManager.getLicence()).thenReturn(Optional.of(licence));
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.VALID);
        when(licenceJobUsageService.currentMonthUsage()).thenReturn(42L);

        this.graphQlTester
            .document("""
                query {
                    licence {
                        id
                        holderName
                        holderEmail
                        status
                        allowedJobs
                        maxUsers
                        currentMonthJobUsage
                        features
                    }
                }
                """)
            .execute()
            .path("licence.id")
            .entity(String.class)
            .isEqualTo("lic-123")
            .path("licence.holderName")
            .entity(String.class)
            .isEqualTo("Acme Corp")
            .path("licence.holderEmail")
            .entity(String.class)
            .isEqualTo("admin@acme.com")
            .path("licence.status")
            .entity(String.class)
            .isEqualTo("VALID")
            .path("licence.currentMonthJobUsage")
            .entity(Long.class)
            .isEqualTo(42L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testLicenceQueryReturnsMissingStatusWhenNoLicence() {
        when(licenceManager.getLicence()).thenReturn(Optional.empty());
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);
        when(licenceJobUsageService.currentMonthUsage()).thenReturn(0L);

        this.graphQlTester
            .document("""
                query {
                    licence {
                        id
                        status
                        currentMonthJobUsage
                    }
                }
                """)
            .execute()
            .path("licence.id")
            .valueIsNull()
            .path("licence.status")
            .entity(String.class)
            .isEqualTo("MISSING")
            .path("licence.currentMonthJobUsage")
            .entity(Long.class)
            .isEqualTo(0L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testUploadLicenceCallsManagerAndReturnsMappedFields() {
        Licence uploaded = new Licence(
            "lic-456", "Beta Inc", "beta@beta.com",
            Instant.parse("2025-06-01T00:00:00Z"), Instant.parse("2027-06-01T00:00:00Z"),
            Set.of(LicenceFeature.SSO), 500L, 20);

        when(licenceManager.upload(any(byte[].class))).thenReturn(uploaded);
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.VALID);
        when(licenceJobUsageService.currentMonthUsage()).thenReturn(10L);

        this.graphQlTester
            .document("""
                mutation {
                    uploadLicence(contents: "LICENCE_DATA") {
                        id
                        holderName
                        status
                    }
                }
                """)
            .execute()
            .path("uploadLicence.id")
            .entity(String.class)
            .isEqualTo("lic-456")
            .path("uploadLicence.holderName")
            .entity(String.class)
            .isEqualTo("Beta Inc")
            .path("uploadLicence.status")
            .entity(String.class)
            .isEqualTo("VALID");

        verify(licenceManager).upload(any(byte[].class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testDeleteLicenceCallsManagerAndReturnsTrue() {
        this.graphQlTester
            .document("""
                mutation {
                    deleteLicence
                }
                """)
            .execute()
            .path("deleteLicence")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(licenceManager).delete();
    }
}
