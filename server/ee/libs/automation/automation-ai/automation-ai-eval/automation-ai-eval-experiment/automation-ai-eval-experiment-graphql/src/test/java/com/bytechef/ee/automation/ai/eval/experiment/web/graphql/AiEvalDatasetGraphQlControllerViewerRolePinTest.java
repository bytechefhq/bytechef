/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * Pins the controller's hard-coded {@code MINIMUM_VIEWER_ROLE} string against the typed {@link WorkspaceRole#VIEWER}
 * enum value. Mirrors {@code AiEvalExperimentGraphQlControllerViewerRolePinTest} — the same EE module-cycle constraint
 * applies to both controllers, so both duplicate the role name as a string literal at the call site rather than pulling
 * in an EE module dep on {@code automation-configuration-api} from the GraphQL module's {@code main} source set.
 *
 * <p>
 * Without this pin, a future rename of {@code WorkspaceRole.VIEWER} would compile cleanly here and silently
 * de-permission every dataset viewer in the operator console.
 *
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetGraphQlControllerViewerRolePinTest {

    @Test
    void testMinimumViewerRoleMatchesEnum() throws Exception {
        Field field = AiEvalDatasetGraphQlController.class.getDeclaredField("MINIMUM_VIEWER_ROLE");

        field.setAccessible(true);

        Object value = field.get(null);

        assertThat(value)
            .as("MINIMUM_VIEWER_ROLE must mirror WorkspaceRole.VIEWER.name(); rename either side and update both")
            .isEqualTo(WorkspaceRole.VIEWER.name());
    }
}
