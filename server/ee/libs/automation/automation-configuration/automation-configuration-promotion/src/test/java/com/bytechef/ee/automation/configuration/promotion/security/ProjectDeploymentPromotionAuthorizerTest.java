/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.promotion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentPromotionAuthorizerTest {

    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private final ProjectDeploymentPromotionAuthorizer projectDeploymentPromotionAuthorizer =
        new ProjectDeploymentPromotionAuthorizer(projectDeploymentService);

    @Test
    void testProjectIdOfProjectDeploymentIsResolvedThroughTheDeploymentItself() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(300L);
        projectDeployment.setProjectId(42L);

        when(projectDeploymentService.fetchProjectDeployment(300L)).thenReturn(Optional.of(projectDeployment));

        assertThat(projectDeploymentPromotionAuthorizer.projectIdOfProjectDeployment(300L)).isEqualTo(42L);
    }

    @Test
    void testUnknownProjectDeploymentIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(projectDeploymentService.fetchProjectDeployment(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectDeploymentPromotionAuthorizer.projectIdOfProjectDeployment(404L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
