/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.security.constant.AuthorityConstants;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentComparisonFacadeAuthorizationTest {

    @Test
    void testExperimentComparisonIsAdminOnly() throws NoSuchMethodException {
        Method method =
            AiEvalExperimentComparisonFacadeImpl.class.getDeclaredMethod("experimentComparison", List.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("experimentComparison must be guarded by @PreAuthorize")
            .isNotNull();
        assertThat(preAuthorize.value())
            .as("@PreAuthorize must require ROLE_ADMIN")
            .contains(AuthorityConstants.ADMIN);
    }
}
