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
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetQueryFacadeViewerRolePinTest {

    @Test
    void testMinimumViewerRoleMatchesEnum() throws Exception {
        Field field = AiEvalDatasetQueryFacadeImpl.class.getDeclaredField("MINIMUM_VIEWER_ROLE");

        field.setAccessible(true);

        Object value = field.get(null);

        assertThat(value)
            .as("MINIMUM_VIEWER_ROLE must mirror WorkspaceRole.VIEWER.name(); rename either side and update both")
            .isEqualTo(WorkspaceRole.VIEWER.name());
    }
}
