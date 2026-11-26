/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.promotion.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionPromotionAuthorizerTest {

    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);

    private final ApiCollectionPromotionAuthorizer apiCollectionPromotionAuthorizer =
        new ApiCollectionPromotionAuthorizer(apiCollectionService, projectDeploymentService);

    @Test
    void testProjectIdOfApiCollectionIsResolvedThroughItsDeployment() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(100L);
        apiCollection.setProjectDeploymentId(300L);

        when(apiCollectionService.fetchApiCollection(100L)).thenReturn(Optional.of(apiCollection));

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(300L);
        projectDeployment.setProjectId(42L);

        when(projectDeploymentService.getProjectDeployment(300L)).thenReturn(projectDeployment);

        assertThat(apiCollectionPromotionAuthorizer.projectIdOfApiCollection(100L)).isEqualTo(42L);
    }

    @Test
    void testUnknownApiCollectionIsRejectedAsAccessDeniedRatherThanAsNotFound() {
        when(apiCollectionService.fetchApiCollection(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiCollectionPromotionAuthorizer.projectIdOfApiCollection(404L))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(projectDeploymentService);
    }
}
