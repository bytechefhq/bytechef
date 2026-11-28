/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.contributor;

import static com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions.assertEveryContributorCallsConnectionScopeCheck;
import static com.bytechef.ee.automation.promotion.test.assertion.PromotionContributorAssertions.assertPreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentPromotionContributorAuthorizationTest {

    private static final String CONTRIBUTOR_PACKAGE = "com.bytechef.ee.automation.configuration.promotion.contributor";
    private static final Class<?>[] PREVIEW_PARAMETER_TYPES = {
        long.class, Environment.class, Environment.class
    };
    private static final Class<?>[] PROMOTE_PARAMETER_TYPES = {
        long.class, Environment.class, Environment.class, Map.class, String.class
    };

    @Test
    void testProjectDeploymentPromotionContributorPreviewRequiresProjectDeploymentPush() {
        assertPreAuthorize(
            ProjectDeploymentPromotionContributor.class, "preview", PREVIEW_PARAMETER_TYPES,
            "hasPermission(@projectDeploymentPromotionAuthorizer.projectIdOfProjectDeployment(#sourceId), " +
                "'Project', 'DEPLOYMENT_PUSH')");
    }

    @Test
    void testProjectDeploymentPromotionContributorPromoteRequiresProjectDeploymentPush() {
        assertPreAuthorize(
            ProjectDeploymentPromotionContributor.class, "promote", PROMOTE_PARAMETER_TYPES,
            "hasPermission(@projectDeploymentPromotionAuthorizer.projectIdOfProjectDeployment(#sourceId), " +
                "'Project', 'DEPLOYMENT_PUSH')");
    }

    @Test
    void testProjectDeploymentPromotionContributorPromoteCallsConnectionScopeCheck() {
        Set<Class<?>> contributorClasses = assertEveryContributorCallsConnectionScopeCheck(CONTRIBUTOR_PACKAGE);

        assertThat(contributorClasses).contains(ProjectDeploymentPromotionContributor.class);
    }
}
