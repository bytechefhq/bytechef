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

package com.bytechef.automation.configuration.audit;

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditMapper;
import com.bytechef.platform.audit.AuditOutcome;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class WorkspaceApiKeyAuditMapper implements AuditMapper {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceApiKeyAuditMapper(WorkspaceApiKeyRepository workspaceApiKeyRepository) {
        this.workspaceApiKeyRepository = workspaceApiKeyRepository;
    }

    @Override
    public @Nullable Object capture(AuditInvocation auditInvocation) {
        String event = auditInvocation.event();

        if (event.equals(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED)) {
            return null;
        }

        Optional<WorkspaceApiKey> workspaceApiKeyOptional;

        if (auditInvocation.argument("id") instanceof Long workspaceApiKeyId) {
            workspaceApiKeyOptional = workspaceApiKeyRepository.findById(workspaceApiKeyId);
        } else {
            long apiKeyId = (Long) auditInvocation.requireArgument("apiKeyId");

            workspaceApiKeyOptional = workspaceApiKeyRepository.findByApiKeyId(apiKeyId);
        }

        return workspaceApiKeyOptional.orElse(null);
    }

    @Override
    public Map<String, String> map(AuditInvocation auditInvocation) {
        Map<String, String> data = new LinkedHashMap<>();

        if (auditInvocation.result() instanceof WorkspaceApiKey workspaceApiKey) {
            putWorkspaceApiKey(data, workspaceApiKey);
        } else if (auditInvocation.captured() instanceof WorkspaceApiKey workspaceApiKey) {
            putWorkspaceApiKey(data, workspaceApiKey);
        } else {
            putArgument(data, auditInvocation, "id", "workspaceApiKeyId");
            putArgument(data, auditInvocation, "apiKeyId", "apiKeyId");
            putArgument(data, auditInvocation, "workspaceId", "workspaceId");
        }

        return data;
    }

    @Override
    public boolean shouldRecord(AuditInvocation auditInvocation) {
        String event = auditInvocation.event();

        if (event.equals(WorkspaceApiKeyAuditEvents.WORKSPACE_API_KEY_CREATED)) {
            return true;
        }

        return auditInvocation.outcome() != AuditOutcome.SUCCESS || auditInvocation.captured() != null;
    }

    private static void putArgument(
        Map<String, String> data, AuditInvocation auditInvocation, String argumentName, String key) {

        Object value = auditInvocation.argument(argumentName);

        if (value != null) {
            data.put(key, String.valueOf(value));
        }
    }

    private static void putWorkspaceApiKey(Map<String, String> data, WorkspaceApiKey workspaceApiKey) {
        data.put("workspaceApiKeyId", String.valueOf(workspaceApiKey.getId()));
        data.put("apiKeyId", String.valueOf(workspaceApiKey.getApiKeyId()));
        data.put("workspaceId", String.valueOf(workspaceApiKey.getWorkspaceId()));
    }
}
