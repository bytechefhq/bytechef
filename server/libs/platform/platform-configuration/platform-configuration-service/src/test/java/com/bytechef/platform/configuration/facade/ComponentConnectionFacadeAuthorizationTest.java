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

package com.bytechef.platform.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close the component-connection IDOR (ticket 1051):
 * {@code ComponentConnectionGraphQlController#clusterElementComponentConnections} and
 * {@code #workflowNodeComponentConnections} accepted a client-supplied {@code workflowId} with no authorization check
 * anywhere on the path, letting any authenticated principal read connection shape for any workflow in the tenant. Both
 * operations key on their {@code workflowId} and resolve the owning project's workspace via
 * {@code hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')}. These two facade methods are invoked only by the
 * workflow-editor GraphQL controller (the EE distributed remote-client stub throws
 * {@code UnsupportedOperationException} for both, and every other internal caller of {@link ComponentConnectionFacade}
 * uses a different overload), so a per-workflow gate here is safe.
 *
 * <p>
 * This gate binds ordinary automation users only: it is inert for connected/embedded users while
 * {@code AutomationAuthorizationContext} still returns true for them before this check runs (embedded skip mode). A
 * parallel branch narrows that separately.
 *
 * @author Ivica Cardic
 */
class ComponentConnectionFacadeAuthorizationTest {

    @Test
    void testGetClusterElementComponentConnectionsRequiresView() {
        assertExpression("getClusterElementComponentConnections");
    }

    @Test
    void testGetWorkflowNodeComponentConnectionsRequiresView() {
        assertExpression("getWorkflowNodeComponentConnections");
    }

    private static void assertExpression(String methodName) {
        Method match = null;

        for (Method candidate : ComponentConnectionFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s", methodName)
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')");
    }
}
