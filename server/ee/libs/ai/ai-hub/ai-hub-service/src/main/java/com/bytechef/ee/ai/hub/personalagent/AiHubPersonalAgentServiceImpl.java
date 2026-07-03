/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.audit.AiHubAuditEvent;
import com.bytechef.ee.ai.hub.audit.AuditAiHub;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentResourceRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentToolRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.WorkspaceAiHubPersonalAgentRepository;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data JDBC backed {@link AiHubPersonalAgentService}.
 *
 * <p>
 * <b>Workspace association via the relation table.</b> The {@code ai_hub_personal_agent} entity is workspace-agnostic;
 * {@code workspace_ai_hub_personal_agent} carries the (workspace, agent) link. Every {@code create}, {@code findOwned},
 * and listing path goes through that table — the entity-side queries only see the (user, environment) dimension.
 * Mirrors {@code WorkspaceMcpServer} / {@code WorkspaceConnection} / {@code WorkspaceAssetFile}.
 * </p>
 *
 * <p>
 * <b>Slug normalisation happens here</b> — see {@link #slugify}. The LLM tool callback can pass either a pre-slugified
 * name or a free-text title; this service decides what makes it onto the row. Centralising the slug logic prevents
 * drift between the LLM tool, a future REST controller, and the DB CHECK constraint.
 * </p>
 *
 * <p>
 * <b>Why per-(workspace, user, env, name) duplicate detection lives in the service</b> — the DB no longer enforces
 * (user, env, name) unique. The service-layer duplicate check is the only gate; under heavy concurrency two creates
 * against the same name CAN both succeed, producing two rows. The race is acceptable because the LLM rarely creates
 * agents concurrently and the user can delete the redundant row from the UI.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class AiHubPersonalAgentServiceImpl implements AiHubPersonalAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiHubPersonalAgentServiceImpl.class);

    private final AiHubPersonalAgentRepository aiHubPersonalAgentRepository;
    private final AiHubPersonalAgentToolRepository aiHubPersonalAgentToolRepository;
    private final AiHubPersonalAgentResourceRepository aiHubPersonalAgentResourceRepository;
    private final WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository;
    private final ObjectProvider<PersonalAgentSaveValidator> saveValidatorProvider;
    private final Clock clock;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubPersonalAgentServiceImpl(
        AiHubPersonalAgentRepository aiHubPersonalAgentRepository,
        AiHubPersonalAgentToolRepository aiHubPersonalAgentToolRepository,
        AiHubPersonalAgentResourceRepository aiHubPersonalAgentResourceRepository,
        WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository,
        ObjectProvider<PersonalAgentSaveValidator> saveValidatorProvider) {

        this.aiHubPersonalAgentRepository = aiHubPersonalAgentRepository;
        this.aiHubPersonalAgentToolRepository = aiHubPersonalAgentToolRepository;
        this.aiHubPersonalAgentResourceRepository = aiHubPersonalAgentResourceRepository;
        this.workspaceAiHubPersonalAgentRepository = workspaceAiHubPersonalAgentRepository;
        this.saveValidatorProvider = saveValidatorProvider;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubPersonalAgent> list(long workspaceId, long userId, int environment) {
        return aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(workspaceId, userId, environment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubPersonalAgent> findOwned(long agentId, long workspaceId, long userId) {
        return aiHubPersonalAgentRepository.findById(agentId)
            .filter(agent -> agent.getUserId() == userId)
            .filter(agent -> workspaceAiHubPersonalAgentRepository
                .findByWorkspaceIdAndAiHubPersonalAgentId(workspaceId, agent.getId())
                .isPresent());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubPersonalAgent>
        findByName(long workspaceId, long userId, int environment, String name) {
        // The lookup expects an already-slugified name, but we normalise defensively so a caller that didn't go
        // through create() (e.g. a future REST endpoint) doesn't silently miss a row that was written via slug.
        // Multiple matches are possible since (user, env, name) is no longer DB-unique; return the first row to keep
        // the LLM create-or-resolve flow deterministic — the service-layer duplicate check in create() prevents new
        // duplicates, so a second row only exists from a pre-existing TOCTOU race that the user can manually clean
        // up.
        List<AiHubPersonalAgent> matches =
            aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
                workspaceId, userId, environment, slugify(name));

        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.get(0));
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_CREATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#result.id"),
            @AuditAiHub.AuditData(key = "name", value = "#result.name"),
            @AuditAiHub.AuditData(key = "environment", value = "#environment")
        })
    @Override
    @Transactional
    public AiHubPersonalAgent create(
        long workspaceId, long userId, int environment, String name, @Nullable String title,
        @Nullable String description, @Nullable String instructions, @Nullable String llmProvider,
        @Nullable String llmModel) {

        validateLlmOverridePair(llmProvider, llmModel);
        validateLlmOverrideAvailability(workspaceId, llmProvider, llmModel);

        String slug = slugify(name);

        // Pre-flight duplicate check so the LLM tool gets a typed error rather than the DB integrity exception
        // wrapped in a runtime failure. Unlike the prior schema, the entity table no longer enforces (user, env,
        // name) uniqueness — this check is the only gate, so a concurrent create against the same name CAN slip
        // through and produce two rows. We accept the race; agents are user-driven, not high-throughput.
        List<AiHubPersonalAgent> existing =
            aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
                workspaceId, userId, environment, slug);

        if (!existing.isEmpty()) {
            throw new ConflictException("Personal agent with name '" + slug + "' already exists");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AiHubPersonalAgent agent = new AiHubPersonalAgent(userId);

        agent.setName(slug);
        agent.setTitle(title);
        agent.setDescription(description);
        agent.setInstructions(instructions);
        agent.setEnvironment(Environment.values()[environment]);
        agent.setLlmProvider(llmProvider);
        agent.setLlmModel(llmModel);
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        AiHubPersonalAgent saved = aiHubPersonalAgentRepository.save(agent);

        try {
            workspaceAiHubPersonalAgentRepository.save(
                new WorkspaceAiHubPersonalAgent(workspaceId, saved.getId()));
        } catch (DataIntegrityViolationException exception) {
            // The unique constraint on (workspace_id, ai_hub_personal_agent_id) is the only DB-level
            // duplicate gate now. A concurrent create CAN race here (two requests slip past the service-layer
            // duplicate check and both insert the agent row, but only one wins the membership). Surface as
            // ConflictException for uniform error handling.
            throw new ConflictException("Personal agent with name '" + slug + "' already exists", exception);
        }

        return saved;
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
        })
    @Override
    @Transactional
    public AiHubPersonalAgent update(
        long agentId, long workspaceId, long userId, @Nullable String title, @Nullable String description,
        @Nullable String instructions, @Nullable String llmProvider, @Nullable String llmModel) {

        AiHubPersonalAgent agent = findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        // Patch semantics: null means "don't touch". Distinguish from "set to empty" by passing an empty string;
        // the entity's setters allow null for nullable columns and we want to mirror that.
        if (title != null) {
            agent.setTitle(title);
        }

        if (description != null) {
            agent.setDescription(description);
        }

        if (instructions != null) {
            agent.setInstructions(instructions);
        }

        // Tri-state on the (llmProvider, llmModel) pair:
        // - both null on the input: leave existing fields untouched.
        // - both empty: clear the override (revert to workspace default).
        // - both non-empty: apply the override after the both-set-or-both-null guard.
        // - only one set: validateLlmOverridePair throws.
        if (llmProvider != null || llmModel != null) {
            if (llmProvider != null && llmProvider.isEmpty() && llmModel != null && llmModel.isEmpty()) {
                agent.setLlmProvider(null);
                agent.setLlmModel(null);
            } else {
                validateLlmOverridePair(llmProvider, llmModel);
                validateLlmOverrideAvailability(workspaceId, llmProvider, llmModel);

                agent.setLlmProvider(llmProvider);
                agent.setLlmModel(llmModel);
            }
        }

        agent.setUpdatedAt(LocalDateTime.now(clock));

        return aiHubPersonalAgentRepository.save(agent);
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_DELETED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId")
        })
    @Override
    @Transactional
    public void delete(long agentId, long workspaceId, long userId) {
        AiHubPersonalAgent agent = findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        // Delete-and-orphan-tasks: we leave the task rows intact (they hold valuable chat history)
        // but their ai_hub_personal_agent_id FK becomes a dangling reference. The runtime treats a dangling
        // id as "agent deleted, fall back to plain LLM system prompt" rather than refusing the task, so the user
        // can still read past messages even after deletion. A future cleanup job can archive the tasks explicitly.
        // The workspace_ai_hub_personal_agent membership row clears via ON DELETE CASCADE.
        aiHubPersonalAgentRepository.delete(agent);

        if (log.isDebugEnabled()) {
            log.debug(
                "Deleted personal agent {} (name={}) for workspace {} user {}",
                agentId, agent.getName(), workspaceId, userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubPersonalAgentTool> listTools(long agentId) {
        return aiHubPersonalAgentToolRepository.findByAiHubPersonalAgentIdOrderByCreatedAtAsc(agentId);
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_ADDED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId"),
            @AuditAiHub.AuditData(key = "componentName", value = "#componentName"),
            @AuditAiHub.AuditData(key = "componentVersion", value = "#componentVersion"),
            @AuditAiHub.AuditData(key = "operationName", value = "#operationName")
        })
    @Override
    @Transactional
    public AiHubPersonalAgentTool addTool(
        long agentId, long workspaceId, long userId, String componentName, int componentVersion,
        String operationName) {

        // Defense in depth: verify the agent belongs to (workspaceId, userId) before mutating its tool list. The
        // controller already gates by workspace via @PreAuthorize, but a forged agent id from the client could
        // otherwise reach a different user's tool template.
        findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        if (componentName == null || componentName.isBlank()) {
            throw new IllegalArgumentException("componentName must not be blank");
        }

        if (operationName == null || operationName.isBlank()) {
            throw new IllegalArgumentException("operationName must not be blank");
        }

        // Pre-flight idempotency check so the LLM tool callback / dialog returns a typed "already added" rather
        // than a 500 from the DB integrity exception. The unique constraint catches a TOCTOU race between this
        // lookup and the save below.
        Optional<AiHubPersonalAgentTool> existing = aiHubPersonalAgentToolRepository
            .findByAiHubPersonalAgentIdAndComponentNameAndComponentVersionAndOperationName(
                agentId, componentName, componentVersion, operationName);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubPersonalAgentTool tool =
            new AiHubPersonalAgentTool(agentId, componentName, componentVersion, operationName);

        tool.setCreatedAt(LocalDateTime.now(clock));

        try {
            return aiHubPersonalAgentToolRepository.save(tool);
        } catch (DataIntegrityViolationException exception) {
            // TOCTOU race: another concurrent request inserted the same tuple. Re-fetch and return the existing
            // row so the caller gets the same idempotent answer either way.
            return aiHubPersonalAgentToolRepository
                .findByAiHubPersonalAgentIdAndComponentNameAndComponentVersionAndOperationName(
                    agentId, componentName, componentVersion, operationName)
                .orElseThrow(() -> new ConflictException(
                    "Personal agent tool conflict for agent " + agentId, exception));
        }
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_REMOVED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "toolId", value = "#toolId")
        })
    @Override
    @Transactional
    public void removeTool(long toolId, long workspaceId, long userId) {
        Optional<AiHubPersonalAgentTool> toolOptional =
            aiHubPersonalAgentToolRepository.findById(toolId);

        if (toolOptional.isEmpty()) {
            // Idempotent: deleting an already-deleted tool is a no-op rather than a 404. The dialog might fire a
            // remove for a row that just disappeared from a concurrent session — we don't want that to error.
            return;
        }

        AiHubPersonalAgentTool tool = toolOptional.get();

        // Verify the parent agent belongs to (workspaceId, userId) before deleting the tool. Without this check, a
        // forged tool id could reach a tool row whose parent agent is owned by another user.
        findOwned(tool.getAiHubPersonalAgentId(), workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent tool " + toolId + " not found for workspace " + workspaceId + " user " + userId));

        aiHubPersonalAgentToolRepository.delete(tool);
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#result.aiHubPersonalAgentId"),
            @AuditAiHub.AuditData(key = "toolId", value = "#toolId"),
            @AuditAiHub.AuditData(key = "connectionId", value = "#connectionId"),
            @AuditAiHub.AuditData(
                key = "parameterKeys", value = "#parameters == null ? '[]' : #parameters.keySet().toString()")
        })
    @Override
    @Transactional
    public AiHubPersonalAgentTool updateToolConfig(
        long toolId, long workspaceId, long userId, @Nullable Long connectionId,
        @Nullable Map<String, ?> parameters) {

        AiHubPersonalAgentTool tool = aiHubPersonalAgentToolRepository.findById(toolId)
            .orElseThrow(() -> new NotFoundException(
                "Personal agent tool %d not found for workspace %d user %d".formatted(
                    toolId, workspaceId, userId)));

        // Defense-in-depth ownership check: a forged id from the client must not reach a tool row whose parent
        // agent is owned by another user. The findOwned probe also returns NotFoundException-shaped Optional.empty
        // when the parent doesn't belong to the caller, so the message stays uniform between the two failure modes
        // — a probing client can't distinguish "tool exists, agent owned by someone else" from "tool doesn't
        // exist", same defense the rest of this service applies.
        findOwned(tool.getAiHubPersonalAgentId(), workspaceId, userId)
            .orElseThrow(() -> new NotFoundException(
                "Personal agent tool %d not found for workspace %d user %d".formatted(
                    toolId, workspaceId, userId)));

        // connectionId: explicit set (including null to clear). The schema's ON DELETE SET NULL handles the
        // connection-deletion-after-pin case; the explicit null path here covers the user actively unbinding a
        // connection from the configure dialog.
        tool.setConnectionId(connectionId);

        // parameters: null means "preserve existing", explicit empty map means "reset to no pre-set". Without the
        // null check, a partial update payload (connection-only) would unintentionally clear the parameters map.
        // Mirrors the pattern in update(...) for the agent's editable fields.
        if (parameters != null) {
            tool.setParameters(parameters);
        }

        return aiHubPersonalAgentToolRepository.save(tool);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubPersonalAgentResource> listResources(long agentId) {
        return aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdOrderByCreatedAtAsc(agentId);
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_RESOURCE_ADDED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "agentId", value = "#agentId"),
            @AuditAiHub.AuditData(key = "kind", value = "#kind"),
            @AuditAiHub.AuditData(key = "resourceId", value = "#resourceId")
        })
    @Override
    @Transactional
    public AiHubPersonalAgentResource addResource(
        long agentId, long workspaceId, long userId, AiHubPersonalAgentResourceKind kind, String resourceId,
        String resourceName) {

        // Defense in depth: verify the agent belongs to (workspaceId, userId) before mutating its resource list.
        findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("resourceId must not be blank");
        }

        if (resourceName == null || resourceName.isBlank()) {
            throw new IllegalArgumentException("resourceName must not be blank");
        }

        // Pre-flight idempotency check so the caller gets a typed "already added" rather than a 500 from the DB
        // integrity exception. The unique constraint catches a TOCTOU race between this lookup and the save.
        Optional<AiHubPersonalAgentResource> existing = aiHubPersonalAgentResourceRepository
            .findByAiHubPersonalAgentIdAndKindAndResourceId(agentId, kind.ordinal(), resourceId);

        if (existing.isPresent()) {
            return existing.get();
        }

        AiHubPersonalAgentResource resource =
            new AiHubPersonalAgentResource(agentId, kind, resourceId, resourceName);

        resource.setCreatedAt(LocalDateTime.now(clock));

        try {
            return aiHubPersonalAgentResourceRepository.save(resource);
        } catch (DataIntegrityViolationException exception) {
            // TOCTOU race: a concurrent request inserted the same tuple. Re-fetch and return the existing row.
            return aiHubPersonalAgentResourceRepository
                .findByAiHubPersonalAgentIdAndKindAndResourceId(agentId, kind.ordinal(), resourceId)
                .orElseThrow(() -> new ConflictException(
                    "Personal agent resource conflict for agent " + agentId, exception));
        }
    }

    @AuditAiHub(
        event = AiHubAuditEvent.AI_HUB_PERSONAL_AGENT_RESOURCE_REMOVED,
        data = {
            @AuditAiHub.AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditAiHub.AuditData(key = "personalAgentResourceId", value = "#personalAgentResourceId")
        })
    @Override
    @Transactional
    public void removeResource(long personalAgentResourceId, long workspaceId, long userId) {
        Optional<AiHubPersonalAgentResource> resourceOptional =
            aiHubPersonalAgentResourceRepository.findById(personalAgentResourceId);

        if (resourceOptional.isEmpty()) {
            // Idempotent: removing an already-deleted resource is a no-op rather than a 404.
            return;
        }

        AiHubPersonalAgentResource resource = resourceOptional.get();

        // Verify the parent agent belongs to (workspaceId, userId) before deleting — a forged id from the client
        // must not reach a resource row whose parent agent is owned by another user.
        findOwned(resource.getAiHubPersonalAgentId(), workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent resource " + personalAgentResourceId + " not found for workspace " + workspaceId
                    + " user " + userId));

        aiHubPersonalAgentResourceRepository.delete(resource);
    }

    @Override
    @Transactional
    public AiHubPersonalAgent cloneToEnvironment(
        long agentId, long workspaceId, long userId, int targetEnvironment, @Nullable String newName) {

        // Defense-in-depth: verify the source agent belongs to (workspaceId, userId) before cloning. Mirrors the
        // ownership check in update / addTool — a forged id from the client could otherwise reach an agent owned
        // by another user, and the clone would be a workspace-cross leakage.
        AiHubPersonalAgent source = findOwned(agentId, workspaceId, userId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Personal agent " + agentId + " not found for workspace " + workspaceId + " user " + userId));

        // Resolve the clone's slug. Prefer the caller-supplied newName so the LLM can disambiguate when a target
        // env already has an agent of the same name; fall back to the source's slug for the common case (clone
        // from DEV to PROD where neither has the slug yet).
        String slug = slugify(newName != null && !newName.isBlank() ? newName : source.getName());

        List<AiHubPersonalAgent> existing =
            aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
                workspaceId, userId, targetEnvironment, slug);

        if (!existing.isEmpty()) {
            throw new ConflictException(
                "Personal agent with name '" + slug + "' already exists in target environment");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AiHubPersonalAgent clone = new AiHubPersonalAgent(userId);

        clone.setName(slug);
        clone.setTitle(source.getTitle());
        clone.setDescription(source.getDescription());
        clone.setInstructions(source.getInstructions());
        clone.setEnvironment(Environment.values()[targetEnvironment]);
        clone.setCreatedAt(now);
        clone.setUpdatedAt(now);

        AiHubPersonalAgent saved = aiHubPersonalAgentRepository.save(clone);

        try {
            workspaceAiHubPersonalAgentRepository.save(
                new WorkspaceAiHubPersonalAgent(workspaceId, saved.getId()));
        } catch (DataIntegrityViolationException exception) {
            // TOCTOU race — another concurrent clone inserted the same slug between the pre-flight check and save.
            // Surface as the same ConflictException for uniform error handling on the caller side.
            throw new ConflictException(
                "Personal agent with name '" + slug + "' already exists in target environment", exception);
        }

        // Deep-clone the source agent's tool template entries. Tools are tiny (a triple per row) so deep-cloning
        // is cheap; cloning without them defeats the point of "promote my agent" (the LLM-visible tool surface
        // would silently shrink in the target env). Each cloned tool gets a fresh id + the new agent's id; the
        // source rows are untouched.
        List<AiHubPersonalAgentTool> sourceTools = aiHubPersonalAgentToolRepository
            .findByAiHubPersonalAgentIdOrderByCreatedAtAsc(source.getId());

        for (AiHubPersonalAgentTool sourceTool : sourceTools) {
            AiHubPersonalAgentTool toolClone = new AiHubPersonalAgentTool(
                saved.getId(), sourceTool.getComponentName(), sourceTool.getComponentVersion(),
                sourceTool.getOperationName());

            toolClone.setCreatedAt(now);

            // Carry parameters through the clone so a promote preserves the user's pre-set defaults; do NOT
            // carry connectionId. Connections are workspace+environment-scoped, so pinning the source's DEV
            // connection on the clone (which sits in PROD, or even the same env in a different agent name) would
            // either break at first invocation (connection id from another env doesn't resolve) or cross
            // environment boundaries silently. Forcing the user to re-pick the connection on the clone is the
            // safer default; parameters are connection-agnostic and carry through verbatim.
            toolClone.setParameters(sourceTool.getParameters());

            try {
                aiHubPersonalAgentToolRepository.save(toolClone);
            } catch (DataIntegrityViolationException exception) {
                // The (agent, component, version, operation) unique constraint should never trip during a clone
                // since the destination agent is fresh — but log + skip rather than fail the whole clone if
                // somehow it does. The clone agent is still usable; the user can re-add the missing tool.
                log.warn(
                    "Skipping duplicate tool clone for agent {} ({}:{}:{}); destination already has it.",
                    saved.getId(), sourceTool.getComponentName(), sourceTool.getComponentVersion(),
                    sourceTool.getOperationName(), exception);
            }
        }

        return saved;
    }

    /**
     * Normalises a user-supplied name into the slug form the DB CHECK constraint accepts. Lowercases, replaces runs of
     * whitespace / punctuation with a single hyphen, strips characters outside {@code [a-z0-9_-]}, and truncates to 64
     * characters. Throws when the result is empty (e.g. a name that's all whitespace).
     */
    static String slugify(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        String lower = input.toLowerCase(Locale.ROOT);
        String collapsed = lower.replaceAll("[^a-z0-9_-]+", "-");
        String trimmed = collapsed.replaceAll("^-+|-+$", "");

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                "name '" + input + "' produces an empty slug — use letters, digits, hyphens, or underscores");
        }

        if (trimmed.length() > 64) {
            trimmed = trimmed.substring(0, 64)
                .replaceAll("-+$", "");
        }

        return trimmed;
    }

    /**
     * Save-time check that the (provider, model) pair actually exists in the workspace's enabled AI Gateway providers +
     * models. Throws an {@link IllegalArgumentException} on miss so admins get the error at save time rather than
     * discovering at the next voice/chat turn that the override silently fell back to the workspace default.
     *
     * <p>
     * Skipped (no-op) when both fields are null/empty (no override) OR when the gateway resolver bean is absent
     * (gateway disabled in this build/profile). The empty-check is defensive — callers typically pass either both
     * non-empty or both null, but the update path uses empty strings to encode "clear the override."
     */
    private void validateLlmOverrideAvailability(
        long workspaceId, @Nullable String llmProvider, @Nullable String llmModel) {

        if (llmProvider == null || llmProvider.isEmpty() || llmModel == null || llmModel.isEmpty()) {
            return;
        }

        PersonalAgentSaveValidator validator = saveValidatorProvider.getIfAvailable();

        if (validator == null) {
            // Gateway disabled — there's nothing to validate against. Trust the persisted values; the runtime
            // resolver's warn-and-fallback path handles any mismatch when voice / chat eventually runs.
            return;
        }

        validator.validate(workspaceId, llmProvider, llmModel);
    }

    /**
     * Enforces the both-set-or-both-null invariant on the per-agent LLM override pair. Setting only one is invalid —
     * the {@link AiHubPersonalAgent#hasLlmOverride()} helper assumes the invariant when reading, and the routing
     * agent's overlay relies on it for clean state-key injection.
     */
    private static void validateLlmOverridePair(@Nullable String llmProvider, @Nullable String llmModel) {
        boolean providerPresent = llmProvider != null && !llmProvider.isEmpty();
        boolean modelPresent = llmModel != null && !llmModel.isEmpty();

        if (providerPresent != modelPresent) {
            throw new IllegalArgumentException(
                "llmProvider and llmModel must both be set or both null/empty; got llmProvider="
                    + llmProvider + ", llmModel=" + llmModel);
        }
    }
}
