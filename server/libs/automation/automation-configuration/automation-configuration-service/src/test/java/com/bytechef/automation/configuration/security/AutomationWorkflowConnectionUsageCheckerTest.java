/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * @author Ivica Cardic
 */
class AutomationWorkflowConnectionUsageCheckerTest {

    private final PermissionService permissionService = mock(PermissionService.class);
    private final AutomationWorkflowConnectionUsageChecker checker =
        new AutomationWorkflowConnectionUsageChecker(permissionService);

    @Test
    void testCheckConnectionUsagePassesWhenTheBindingIsAllowed() {
        when(permissionService.canUseConnectionInWorkflow(9L, "workflow-1", Environment.PRODUCTION)).thenReturn(true);

        assertThatCode(() -> checker.checkConnectionUsage("workflow-1", 9L, Environment.PRODUCTION.ordinal()))
            .doesNotThrowAnyException();
    }

    @Test
    void testCheckConnectionUsageDeniesWhenTheBindingIsRefused() {
        assertThatThrownBy(() -> checker.checkConnectionUsage("workflow-1", 9L, Environment.PRODUCTION.ordinal()))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testCheckConnectionUsageDeniesAnEnvironmentOutsideTheEnum() {
        assertThatThrownBy(() -> checker.checkConnectionUsage("workflow-1", 9L, 99L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
