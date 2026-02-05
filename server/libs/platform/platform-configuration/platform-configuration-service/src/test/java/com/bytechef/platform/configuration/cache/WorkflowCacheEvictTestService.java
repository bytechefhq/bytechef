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

package com.bytechef.platform.configuration.cache;

import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Test service for exercising the WorkflowCacheEvictAspect. This service is used only in integration tests.
 *
 * @author Ivica Cardic
 */
@Service
class WorkflowCacheEvictTestService {

    private final AtomicInteger callCount = new AtomicInteger(0);

    private Supplier<Object> behavior = () -> "success";

    /**
     * Sets the behavior for the next method call.
     *
     * @param behavior the behavior to execute
     */
    public void setBehavior(Supplier<Object> behavior) {
        this.behavior = behavior;
        this.callCount.set(0);
    }

    /**
     * Returns the number of times a test method was called.
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
     * Test method with single cache name.
     */
    @WorkflowCacheEvict(cacheNames = "testCache")
    public Object evictSingleCache(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

        callCount.incrementAndGet();

        return behavior.get();
    }

    /**
     * Test method with multiple cache names.
     */
    @WorkflowCacheEvict(cacheNames = {
        "cache1", "cache2", "cache3"
    })
    public Object evictMultipleCaches(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

        callCount.incrementAndGet();

        return behavior.get();
    }

    /**
     * Test method with parameters in different order.
     */
    @WorkflowCacheEvict(cacheNames = "reversedCache")
    public Object evictWithReversedParameters(
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId, String otherParam,
        @WorkflowCacheEvict.WorkflowIdParam String workflowId) {

        callCount.incrementAndGet();

        return behavior.get();
    }
}
