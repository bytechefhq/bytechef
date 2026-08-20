/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Pins the facade's hard-coded {@code MINIMUM_VIEWER_ROLE} string against the typed {@link WorkspaceRole#VIEWER} enum
 * value. Adding a direct EE dep on the {@code automation-configuration-api} EE module from this facade's {@code main}
 * source set would cycle the GraphQL module on EE-only configuration, so the value is duplicated as a string literal at
 * the call site. Without this test, a future rename of {@code WorkspaceRole.VIEWER} would compile cleanly here and
 * silently de-permission every viewer.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalExperimentQueryFacadeViewerRolePinTest {

    @Test
    void testMinimumViewerRoleMatchesEnum() throws Exception {
        Field field = AiEvalExperimentQueryFacadeImpl.class.getDeclaredField("MINIMUM_VIEWER_ROLE");

        field.setAccessible(true);

        Object value = field.get(null);

        assertThat(value)
            .as("MINIMUM_VIEWER_ROLE must mirror WorkspaceRole.VIEWER.name(); rename either side and update both")
            .isEqualTo(WorkspaceRole.VIEWER.name());
    }
}
