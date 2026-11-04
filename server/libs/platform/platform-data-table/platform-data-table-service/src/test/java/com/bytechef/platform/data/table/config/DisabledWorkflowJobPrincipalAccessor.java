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

package com.bytechef.platform.data.table.config;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reports every workflow as enabled except those disabled through {@link #disable(String)}.
 */
public class DisabledWorkflowJobPrincipalAccessor implements JobPrincipalAccessor {

    private final Set<String> disabledWorkflowUuids = ConcurrentHashMap.newKeySet();

    public void clear() {
        disabledWorkflowUuids.clear();
    }

    public void disable(String workflowUuid) {
        disabledWorkflowUuids.add(workflowUuid);
    }

    @Override
    public boolean isConnectionUsed(long connectionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWorkflowEnabled(long jobPrincipalId, String workflowUuid) {
        return !disabledWorkflowUuids.contains(workflowUuid);
    }

    @Override
    public long getEnvironmentId(long jobPrincipalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ?> getInputMap(long jobPrincipalId, String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ?> getMetadataMap(long jobPrincipalId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public String getWorkflowId(long jobPrincipalId, String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getLastWorkflowId(String workflowUuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getWorkflowUuid(String workflowId) {
        throw new UnsupportedOperationException();
    }
}
