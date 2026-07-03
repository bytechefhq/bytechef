/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentResourceRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.AiHubPersonalAgentToolRepository;
import com.bytechef.ee.ai.hub.personalagent.repository.WorkspaceAiHubPersonalAgentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests for {@link AiHubPersonalAgentServiceImpl}. Pin the contract surface other layers depend on:
 *
 * <ul>
 * <li>Slug normalisation — free text in, slug out — happens at the service layer (not the controller, not the LLM
 * tool). Without this, three callers might disagree on what counts as a valid name.</li>
 * <li>Ownership checks reject cross-user reads/writes even when the id is correct. The controller's
 * {@code @PreAuthorize} stops cross-workspace traffic; this is the second layer for cross-user-within-workspace.</li>
 * <li>The TOCTOU race on create surfaces as {@link ConflictException}, not as a leaked DB exception.</li>
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubPersonalAgentServiceTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long USER_ID = 10L;
    private static final long OTHER_USER_ID = 99L;
    private static final int ENVIRONMENT = 0;

    @Mock
    private AiHubPersonalAgentRepository aiHubPersonalAgentRepository;

    @Mock
    private AiHubPersonalAgentToolRepository aiHubPersonalAgentToolRepository;

    @Mock
    private AiHubPersonalAgentResourceRepository aiHubPersonalAgentResourceRepository;

    @Mock
    private WorkspaceAiHubPersonalAgentRepository workspaceAiHubPersonalAgentRepository;

    // Note: AiHubPersonalAgentServiceImpl uses Clock.systemUTC() internally (no Clock injection). The tests
    // below
    // only assert on identity and ownership semantics, not on persisted timestamps, so the wall clock is fine
    // here — a reading-the-clock-twice flake would only matter if a test pinned a specific value.

    @Test
    void testSlugifyHandlesFreeTextInput() {
        // Slugification is the canonical contract: any caller can hand in human-readable text and the service
        // produces a slug that matches the DB CHECK constraint. Pinning specific transformations because future
        // callers (REST endpoint, LLM tool) will rely on the same idempotent shape.
        assertThat(AiHubPersonalAgentServiceImpl.slugify("Research Bot")).isEqualTo("research-bot");
        assertThat(AiHubPersonalAgentServiceImpl.slugify("MIXED_Case-name")).isEqualTo("mixed_case-name");
        assertThat(AiHubPersonalAgentServiceImpl.slugify("dots.and!special&chars"))
            .isEqualTo("dots-and-special-chars");
        assertThat(AiHubPersonalAgentServiceImpl.slugify("   extra   spaces   ")).isEqualTo("extra-spaces");
    }

    @Test
    void testSlugifyRejectsBlank() {
        assertThatThrownBy(() -> AiHubPersonalAgentServiceImpl.slugify(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank");

        assertThatThrownBy(() -> AiHubPersonalAgentServiceImpl.slugify("   "))
            .isInstanceOf(IllegalArgumentException.class);

        // Pure-special-characters input that strips down to empty after slugification — must reject so a
        // controller / tool caller doesn't mint an unindexable empty-string row.
        assertThatThrownBy(() -> AiHubPersonalAgentServiceImpl.slugify("!@#$%"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty slug");
    }

    @Test
    void testCreatePersistsWithTimestamps() {
        AiHubPersonalAgentServiceImpl service = newService();

        when(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "research-bot"))
                .thenReturn(List.of());
        when(aiHubPersonalAgentRepository.save(any(AiHubPersonalAgent.class)))
            .thenAnswer(invocation -> {
                AiHubPersonalAgent arg = invocation.getArgument(0);

                arg.setId(42L);

                return arg;
            });

        AiHubPersonalAgent agent = service.create(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "Research Bot", "Research Assistant", "Helps with literature reviews",
            "Always cite sources.", null, null);

        assertThat(agent.getId()).isEqualTo(42L);
        assertThat(agent.getName()).isEqualTo("research-bot");
        assertThat(agent.getTitle()).isEqualTo("Research Assistant");
        assertThat(agent.getDescription()).isEqualTo("Helps with literature reviews");
        assertThat(agent.getInstructions()).isEqualTo("Always cite sources.");
        assertThat(agent.getCreatedAt()).isNotNull();
        assertThat(agent.getUpdatedAt()).isEqualTo(agent.getCreatedAt());
    }

    @Test
    void testCreateRejectsDuplicateNameWithConflictException() {
        AiHubPersonalAgentServiceImpl service = newService();

        // Pre-existing row with the same slug — service short-circuits with ConflictException rather than
        // letting the DB constraint surface as a runtime DataIntegrityViolation. The conflict shape is part
        // of the LLM tool contract: createAiHubPersonalAgent surfaces this typed message back to the model.
        when(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "research-bot"))
                .thenReturn(List.of(new AiHubPersonalAgent(USER_ID)));

        assertThatThrownBy(() -> service.create(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "research-bot", null, null, null, null, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(aiHubPersonalAgentRepository, never()).save(any(AiHubPersonalAgent.class));
    }

    @Test
    void testCreateMapsDataIntegrityViolationToConflictException() {
        AiHubPersonalAgentServiceImpl service = newService();

        // TOCTOU race: lookup says no row exists, but a concurrent insert wins between findByWorkspace... and
        // save(). The DB throws DataIntegrityViolationException; the service maps it to ConflictException so
        // the LLM tool sees a uniform error regardless of which path detected the duplicate.
        when(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironmentAndName(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "research-bot"))
                .thenReturn(List.of());
        when(aiHubPersonalAgentRepository.save(any(AiHubPersonalAgent.class)))
            .thenAnswer(invocation -> {
                AiHubPersonalAgent arg = invocation.getArgument(0);

                arg.setId(42L);

                return arg;
            });
        when(workspaceAiHubPersonalAgentRepository.save(any(WorkspaceAiHubPersonalAgent.class)))
            .thenThrow(new DataIntegrityViolationException("uk_workspace_ai_hub_personal_agent violated"));

        assertThatThrownBy(() -> service.create(
            WORKSPACE_ID, USER_ID, ENVIRONMENT, "research-bot", null, null, null, null, null))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testFindOwnedReturnsEmptyForCrossUserAccess() {
        AiHubPersonalAgentServiceImpl service = newService();

        // Agent exists, but it belongs to OTHER_USER_ID. Even though the requesting USER_ID has the right
        // workspace membership, the service must refuse — defense in depth above the controller's @PreAuthorize.
        AiHubPersonalAgent otherUsersAgent = new AiHubPersonalAgent(OTHER_USER_ID);

        otherUsersAgent.setId(7L);

        when(aiHubPersonalAgentRepository.findById(7L)).thenReturn(Optional.of(otherUsersAgent));

        assertThat(service.findOwned(7L, WORKSPACE_ID, USER_ID)).isEmpty();
    }

    @Test
    void testFindOwnedReturnsEmptyForCrossWorkspaceAccess() {
        AiHubPersonalAgentServiceImpl service = newService();

        // Agent in a different workspace. Mirrors the cross-user check — uniform-empty so a probe cannot
        // distinguish "missing" from "exists in another scope".
        AiHubPersonalAgent otherWorkspace = new AiHubPersonalAgent(USER_ID);

        otherWorkspace.setId(7L);

        when(aiHubPersonalAgentRepository.findById(7L)).thenReturn(Optional.of(otherWorkspace));

        assertThat(service.findOwned(7L, WORKSPACE_ID, USER_ID)).isEmpty();
    }

    @Test
    void testUpdateAppliesPatchedFields() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent existing = new AiHubPersonalAgent(USER_ID);

        existing.setId(42L);
        existing.setName("research-bot");
        existing.setTitle("Old Title");
        existing.setDescription("Old description");
        existing.setInstructions("Old instructions");

        when(aiHubPersonalAgentRepository.findById(42L)).thenReturn(Optional.of(existing));
        when(workspaceAiHubPersonalAgentRepository.findByWorkspaceIdAndAiHubPersonalAgentId(
            WORKSPACE_ID, 42L))
                .thenReturn(Optional.of(new WorkspaceAiHubPersonalAgent(WORKSPACE_ID, 42L)));
        when(aiHubPersonalAgentRepository.save(any(AiHubPersonalAgent.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Null fields stay untouched — partial update semantics. The controller relies on this to allow
        // separate "change just title" and "change just instructions" round-trips without forcing the client
        // to read-modify-write the whole row.
        AiHubPersonalAgent updated =
            service.update(42L, WORKSPACE_ID, USER_ID, "New Title", null, "New instructions", null, null);

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getDescription()).isEqualTo("Old description");
        assertThat(updated.getInstructions()).isEqualTo("New instructions");
    }

    @Test
    void testUpdateRejectsCrossUserAccess() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent otherUsersAgent = new AiHubPersonalAgent(OTHER_USER_ID);

        otherUsersAgent.setId(7L);
        otherUsersAgent.setName("research-bot");

        when(aiHubPersonalAgentRepository.findById(7L)).thenReturn(Optional.of(otherUsersAgent));

        assertThatThrownBy(() -> service.update(7L, WORKSPACE_ID, USER_ID, "Hacked", null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");

        verify(aiHubPersonalAgentRepository, never()).save(any(AiHubPersonalAgent.class));
    }

    @Test
    void testListDelegatesToRepository() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(USER_ID);

        agent.setId(1L);

        when(aiHubPersonalAgentRepository.findAllByWorkspaceUserEnvironment(WORKSPACE_ID, USER_ID, ENVIRONMENT))
            .thenReturn(List.of(agent));

        List<AiHubPersonalAgent> result = service.list(WORKSPACE_ID, USER_ID, ENVIRONMENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)
            .getId()).isEqualTo(1L);
    }

    @Test
    void testDeleteRefusesCrossUserAccess() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent otherUsersAgent = new AiHubPersonalAgent(OTHER_USER_ID);

        otherUsersAgent.setId(7L);
        otherUsersAgent.setName("research-bot");

        when(aiHubPersonalAgentRepository.findById(7L)).thenReturn(Optional.of(otherUsersAgent));

        assertThatThrownBy(() -> service.delete(7L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");

        verify(aiHubPersonalAgentRepository, never()).delete(any(AiHubPersonalAgent.class));
    }

    @Test
    void testAddResourceReturnsExistingRowWhenAlreadyPresent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(USER_ID);

        agent.setId(5L);

        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));
        when(workspaceAiHubPersonalAgentRepository
            .findByWorkspaceIdAndAiHubPersonalAgentId(WORKSPACE_ID, 5L))
                .thenReturn(Optional.of(new WorkspaceAiHubPersonalAgent(WORKSPACE_ID, 5L)));

        AiHubPersonalAgentResource existing = new AiHubPersonalAgentResource(
            5L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");

        when(aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdAndKindAndResourceId(
            5L, AiHubPersonalAgentResourceKind.WORKFLOW.ordinal(), "wf-1"))
                .thenReturn(Optional.of(existing));

        AiHubPersonalAgentResource result = service.addResource(
            5L, WORKSPACE_ID, USER_ID, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");

        assertThat(result).isSameAs(existing);
        verify(aiHubPersonalAgentResourceRepository, never()).save(any());
    }

    @Test
    void testAddResourceRejectsCrossUserAgent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(OTHER_USER_ID);

        agent.setId(5L);

        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.addResource(
            5L, WORKSPACE_ID, USER_ID, AiHubPersonalAgentResourceKind.FILE, "file-1", "notes.md"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(aiHubPersonalAgentResourceRepository, never()).save(any());
    }

    @Test
    void testRemoveResourceIsNoOpWhenResourceMissing() {
        AiHubPersonalAgentServiceImpl service = newService();

        when(aiHubPersonalAgentResourceRepository.findById(404L)).thenReturn(Optional.empty());

        service.removeResource(404L, WORKSPACE_ID, USER_ID);

        verify(aiHubPersonalAgentResourceRepository, never()).delete(any());
    }

    @Test
    void testRemoveResourceRejectsCrossUserAgent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgentResource resource = new AiHubPersonalAgentResource(
            5L, AiHubPersonalAgentResourceKind.FILE, "file-1", "notes.md");

        resource.setId(7L);

        AiHubPersonalAgent agent = new AiHubPersonalAgent(OTHER_USER_ID);

        agent.setId(5L);

        when(aiHubPersonalAgentResourceRepository.findById(7L)).thenReturn(Optional.of(resource));
        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.removeResource(7L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalArgumentException.class);

        verify(aiHubPersonalAgentResourceRepository, never()).delete(any());
    }

    @Test
    void testAddResourcePersistsNewRowWhenAbsent() {
        AiHubPersonalAgentServiceImpl service = newService();

        AiHubPersonalAgent agent = new AiHubPersonalAgent(USER_ID);

        agent.setId(5L);

        when(aiHubPersonalAgentRepository.findById(5L)).thenReturn(Optional.of(agent));
        when(workspaceAiHubPersonalAgentRepository
            .findByWorkspaceIdAndAiHubPersonalAgentId(WORKSPACE_ID, 5L))
                .thenReturn(Optional.of(new WorkspaceAiHubPersonalAgent(WORKSPACE_ID, 5L)));

        // No existing row — addResource must call save.
        when(aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdAndKindAndResourceId(
            5L, AiHubPersonalAgentResourceKind.FILE.ordinal(), "file-7"))
                .thenReturn(Optional.empty());

        AiHubPersonalAgentResource saved = new AiHubPersonalAgentResource(
            5L, AiHubPersonalAgentResourceKind.FILE, "file-7", "notes.md");

        saved.setId(99L);

        when(aiHubPersonalAgentResourceRepository.save(any(AiHubPersonalAgentResource.class)))
            .thenReturn(saved);

        AiHubPersonalAgentResource result = service.addResource(
            5L, WORKSPACE_ID, USER_ID, AiHubPersonalAgentResourceKind.FILE, "file-7", "notes.md");

        assertThat(result).isSameAs(saved);

        // Verify save was actually called and the row passed to save had createdAt set.
        ArgumentCaptor<AiHubPersonalAgentResource> captor =
            ArgumentCaptor.forClass(AiHubPersonalAgentResource.class);

        verify(aiHubPersonalAgentResourceRepository).save(captor.capture());

        assertThat(captor.getValue()
            .getCreatedAt())
                .as("impl sets createdAt before saving")
                .isNotNull();
    }

    @Test
    void testListResourcesDelegatesToRepository() {
        AiHubPersonalAgentServiceImpl service = newService();

        long agentId = 5L;

        AiHubPersonalAgentResource first = new AiHubPersonalAgentResource(
            agentId, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");
        AiHubPersonalAgentResource second = new AiHubPersonalAgentResource(
            agentId, AiHubPersonalAgentResourceKind.FILE, "file-9", "notes.md");

        when(aiHubPersonalAgentResourceRepository.findByAiHubPersonalAgentIdOrderByCreatedAtAsc(agentId))
            .thenReturn(List.of(first, second));

        List<AiHubPersonalAgentResource> result = service.listResources(agentId);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(first, second);
    }

    private AiHubPersonalAgentServiceImpl newService() {
        // Tests don't exercise the gateway validator path; provide an ObjectProvider that returns null so the
        // save-time (provider, model) availability check is a no-op. The both-set-or-both-null guard still runs
        // (it's local to the service) which is what the existing tests cover. Lenient because most tests in this
        // class skip the LLM override path entirely (null/null), so the stubbing is genuinely unused in many cases.
        ObjectProvider<PersonalAgentSaveValidator> emptyValidatorProvider = org.mockito.Mockito.mock(
            ObjectProvider.class);

        org.mockito.Mockito.lenient()
            .when(emptyValidatorProvider.getIfAvailable())
            .thenReturn(null);

        return new AiHubPersonalAgentServiceImpl(
            aiHubPersonalAgentRepository, aiHubPersonalAgentToolRepository, aiHubPersonalAgentResourceRepository,
            workspaceAiHubPersonalAgentRepository, emptyValidatorProvider);
    }
}
