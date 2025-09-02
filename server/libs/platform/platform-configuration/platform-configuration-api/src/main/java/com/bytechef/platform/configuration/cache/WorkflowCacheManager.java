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

/**
 * Service responsible for managing cache cleaning operations.
 *
 * @author Ivica Cardic
 */
public interface WorkflowCacheManager {

    /**
     * Clears cache entries for a specific workflow.
     *
     * @param workflowId    the ID of the workflow
     * @param cacheName     the name of the cache
     * @param environmentId the environment id
     */
    void clearCacheForWorkflow(String workflowId, String cacheName, long environmentId);
}
