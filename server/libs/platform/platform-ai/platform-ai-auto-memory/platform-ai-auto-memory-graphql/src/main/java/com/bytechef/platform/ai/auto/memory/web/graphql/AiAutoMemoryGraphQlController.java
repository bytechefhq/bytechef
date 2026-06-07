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

package com.bytechef.platform.ai.auto.memory.web.graphql;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL surface for long-term auto-memory rows. Replaces the prior Command-Center-scoped controller — the underlying
 * {@link AiAutoMemoryService} contract is unchanged; only the location and naming differ. All operations are workspace
 * + user scoped (and, where the row's lookup needs it, environment scoped). Cross-workspace access is rejected with
 * Spring Security's {@link AccessDeniedException}; missing rows surface as the service-layer not-found exception.
 *
 * <p>
 * The class-level {@code @PreAuthorize("isAuthenticated()")} mirrors the prior controller; per-method authorization is
 * enforced inline by checking workspace membership against {@link WorkspaceFacade#getUserWorkspaces(long)}. In CE that
 * facade returns every workspace, so the gate is permissive there; in EE it returns only accessible workspaces, which
 * closes the cross-workspace privilege-escalation vector.
 *
 * @author Ivica Cardic
 */
@Controller
@PreAuthorize("isAuthenticated()")
public class AiAutoMemoryGraphQlController {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final UserService userService;
    private final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings("EI")
    public AiAutoMemoryGraphQlController(
        AiAutoMemoryService aiAutoMemoryService, UserService userService, WorkspaceFacade workspaceFacade) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.userService = userService;
        this.workspaceFacade = workspaceFacade;
    }

    @QueryMapping
    public List<AiAutoMemory> aiAutoMemories(
        @Argument long workspaceId, @Argument int environment, @Argument @Nullable AiAutoMemoryType memoryType) {

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        return aiAutoMemoryService.list(workspaceId, AiAutoMemoryPrincipalType.USER, userId, environment, memoryType);
    }

    @QueryMapping
    @Nullable
    public AiAutoMemory aiAutoMemory(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        Optional<AiAutoMemory> memory =
            aiAutoMemoryService.findById(workspaceId, AiAutoMemoryPrincipalType.USER, userId, id);

        return memory.orElse(null);
    }

    @MutationMapping
    public AiAutoMemory updateAiAutoMemory(@Argument UpdateAiAutoMemoryInput input) {
        if (input.title() == null && input.description() == null && input.memoryType() == null
            && input.content() == null) {
            throw new IllegalArgumentException(
                "UpdateAiAutoMemoryInput requires at least one of title, description, memoryType, content");
        }

        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, input.workspaceId());

        return aiAutoMemoryService.updateById(
            input.workspaceId(), AiAutoMemoryPrincipalType.USER, userId, input.id(),
            input.title(), input.description(), input.memoryType(), input.content());
    }

    @MutationMapping
    public boolean deleteAiAutoMemory(@Argument long workspaceId, @Argument long id) {
        long userId = userService.getCurrentUser()
            .getId();

        verifyUserCanAccessWorkspace(userId, workspaceId);

        aiAutoMemoryService.deleteById(workspaceId, AiAutoMemoryPrincipalType.USER, userId, id);

        return true;
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "memoryType")
    public String memoryType(AiAutoMemory memory) {
        return memory.getMemoryType()
            .name();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "createdAt")
    @Nullable
    public Long createdAt(AiAutoMemory memory) {
        return memory.getCreatedAt() == null ? null
            : memory.getCreatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    @SchemaMapping(typeName = "AiAutoMemory", field = "updatedAt")
    @Nullable
    public Long updatedAt(AiAutoMemory memory) {
        return memory.getUpdatedAt() == null ? null
            : memory.getUpdatedAt()
                .toInstant(java.time.ZoneOffset.UTC)
                .toEpochMilli();
    }

    private void verifyUserCanAccessWorkspace(long userId, long workspaceId) {
        boolean isMember = workspaceFacade.getUserWorkspaces(userId)
            .stream()
            .map(Workspace::getId)
            .anyMatch(id -> id != null && id == workspaceId);

        if (!isMember) {
            throw new AccessDeniedException("Workspace is not accessible to the current user");
        }
    }

    /**
     * Workspace, id, and the patch fields. Environment is intentionally absent — the row's environment is immutable
     * post-create, and the primary key already pins the partition.
     */
    public record UpdateAiAutoMemoryInput(
        long id, long workspaceId, @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, @Nullable String content) {
    }
}
