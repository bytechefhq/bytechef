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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * Service responsible for managing cache cleaning operations.
 *
 * @author Ivica Cardic
 */
@Component
public class WorkflowCacheManager {

    private final CacheManager cacheManager;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowCacheManager(CacheManager cacheManager, WorkflowService workflowService) {
        this.cacheManager = cacheManager;
        this.workflowService = workflowService;
    }

    /**
     * Clears cache entries for a specific workflow.
     *
     * @param workflowId the ID of the workflow
     * @param cacheName  the name of the cache
     */
    public void clearCacheForWorkflow(String workflowId, String cacheName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<String> workflowNodeNames = new ArrayList<>();

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            workflowNodeNames.add(workflowTrigger.getName());
        }

        List<WorkflowTask> workflowTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : workflowTasks) {
            workflowNodeNames.add(workflowTask.getName());
        }

        clearCache(cacheName, workflowId, workflowNodeNames);
    }

    /**
     * Clears specific cache entries.
     *
     * @param cacheName         the name of the cache
     * @param workflowId        the ID of the workflow
     * @param workflowNodeNames the list of workflow node names
     */
    private void clearCache(String cacheName, String workflowId, List<String> workflowNodeNames) {
        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            for (String workflowNodeName : workflowNodeNames) {
                cache.evict(TenantCacheKeyUtils.getKey(workflowId, workflowNodeName));
            }
        }
    }
}
