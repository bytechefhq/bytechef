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

package com.bytechef.platform.component.aspect;

import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.annotation.WithTokenRefresh;
import com.bytechef.platform.component.exception.ActionDefinitionErrorType;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Test service for exercising the TokenRefreshAspect. This service is used only in integration tests.
 *
 * @author Ivica Cardic
 */
@Service
public class TokenRefreshTestService {

    private final AtomicInteger callCount = new AtomicInteger(0);

    private Supplier<Object> behavior = () -> "success";

    /**
     * Sets the behavior for the next executeWithConnection call.
     *
     * @param behavior the behavior to execute
     */
    public void setBehavior(Supplier<Object> behavior) {
        this.behavior = behavior;
        this.callCount.set(0);
    }

    /**
     * Returns the number of times executeWithConnection was called.
     *
     * @return the call count
     */
    public int getCallCount() {
        return callCount.get();
    }

    /**
     * Resets the call count and behavior.
     */
    public void reset() {
        this.behavior = () -> "success";
        this.callCount.set(0);
    }

    /**
     * Test method with single ComponentConnection parameter.
     */
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_PERFORM")
    public Object executeWithConnection(
        @WithTokenRefresh.ComponentNameParam String componentName, int componentVersion,
        @WithTokenRefresh.ConnectionParam ComponentConnection componentConnection) {

        callCount.incrementAndGet();

        return behavior.get();
    }

    /**
     * Test method with Map of ComponentConnections parameter.
     */
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_PERFORM")
    public Object executeWithConnectionMap(
        @WithTokenRefresh.ComponentNameParam String componentName, int componentVersion,
        @WithTokenRefresh.ConnectionParam Map<String, ComponentConnection> componentConnections) {

        callCount.incrementAndGet();

        return behavior.get();
    }

    /**
     * Test method without connection parameter (should not trigger refresh).
     */
    @WithTokenRefresh(
        errorTypeClass = ActionDefinitionErrorType.class,
        errorTypeField = "EXECUTE_PERFORM")
    public Object executeWithoutConnection(
        @WithTokenRefresh.ComponentNameParam String componentName, int componentVersion) {
        callCount.incrementAndGet();

        return behavior.get();
    }
}
