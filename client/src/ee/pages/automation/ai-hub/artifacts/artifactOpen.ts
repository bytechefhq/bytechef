import {AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {toast} from 'sonner';

function parseMetadataJson(metadataJson: string | null): Record<string, string> {
    if (!metadataJson) {
        return {};
    }

    try {
        return JSON.parse(metadataJson) as Record<string, string>;
    } catch (parseError) {
        // A server-side serialization bug would otherwise make every artifact non-clickable with no diagnostic
        // trail. Surface a console warning so devtools makes the failure visible; we still fall back to {} to keep
        // the rest of the sidebar rendering.
        console.warn('[artifactOpen] Failed to parse artifact metadataJson; quick-open will be disabled', {
            parseError,
            raw: metadataJson,
        });

        return {};
    }
}

/*
 * Quick-open affordance:
 * - FILE_CREATED / BINARY_FILE_CREATED → opens file tab
 * - WORKFLOW_EXECUTION_STARTED → opens the execution as a right-panel tab
 * - WORKFLOW_CREATED / WORKFLOW_UPDATED → opens workflow tab if metadataJson.projectId is present;
 *   otherwise no quick-open (projectId needed for tab routing)
 * - DATA_TABLE_* → opens data table tab using metadataJson.dataTableId if present;
 *   DATA_TABLE_ROW_* artifactId is a row id which doesn't open cleanly, so we fall back to metadataJson
 * - KB_DOCUMENT_* → opens knowledge-base tab using metadataJson.knowledgeBaseId if present
 * - SKILL_REFERENCED → opens skill tab using artifactId directly (the artifact is the skill itself,
 *   same shape as DATA_TABLE_REFERENCED / KB_REFERENCED)
 * - CUSTOM_COMPONENT_REFERENCED → opens custom-component tab using artifactId directly, same shape as
 *   SKILL_REFERENCED
 * - CODE_WORKFLOW_REFERENCED → artifactId IS the projectId (same shape as CUSTOM_COMPONENT_REFERENCED),
 *   but openCodeWorkflowTab also needs a `language`, which the artifact row doesn't carry (the recorder
 *   only stashes artifactId + name). We fetch the project via ProjectApi().getProject on click and read
 *   its `codeWorkflowLanguage`; if the fetch fails or the project is no longer code-backed, we surface a
 *   toast instead of opening a tab.
 * - AI_AGENT_REFERENCED → opens an AI Agent tab using artifactId directly (the artifact is the agent
 *   itself), same shape as SKILL_REFERENCED / CUSTOM_COMPONENT_REFERENCED
 *
 * Limitation: if metadataJson doesn't carry the parent entity id (projectId / dataTableId /
 * knowledgeBaseId), the artifact row is rendered as non-clickable (icon + name + timestamp only).
 */
/**
 * CODE_WORKFLOW_REFERENCED's artifact row only carries artifactId (= projectId) + name — the language
 * `openCodeWorkflowTab` needs was never stashed on the artifact (see {@link OpenCodeWorkflowTabToolCallback}
 * on the server, which only records projectId + name). Fetch the project and read its
 * `codeWorkflowLanguage` (added alongside code workflows) rather than inventing a value. A missing
 * language means the project is no longer code-backed (e.g. converted back to a visual workflow) — surface
 * that as a toast instead of opening a tab with a bogus language.
 */
async function openCodeWorkflowArtifact(artifact: AiHubChatArtifactI): Promise<void> {
    try {
        const project = await new ProjectApi().getProject({id: Number(artifact.artifactId)});

        if (!project.codeWorkflowLanguage) {
            toast.error(`"${artifact.artifactName}" is no longer a code workflow.`);

            return;
        }

        aiHubTabsStore
            .getState()
            .openCodeWorkflowTab(artifact.artifactId, project.codeWorkflowLanguage, artifact.artifactName);
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);

        toast.error(`Failed to open "${artifact.artifactName}": ${message}`);
    }
}

export async function handleArtifactQuickOpen(artifact: AiHubChatArtifactI): Promise<void> {
    const metadata = parseMetadataJson(artifact.metadataJson);

    // FILE_REFERENCED carries the same artifactId shape as FILE_CREATED (asset_file id), so a single
    // openFileTab call covers both quick-open paths. Same applies to the *_REFERENCED variants below for
    // workflow / data-table / knowledge-base — they were missing here, so the rows in the sidebar artifact
    // list looked clickable-shaped (wrench icon, hover) but had no handler firing on click.
    if (
        artifact.kind === 'FILE_CREATED' ||
        artifact.kind === 'FILE_UPDATED' ||
        artifact.kind === 'BINARY_FILE_CREATED' ||
        artifact.kind === 'FILE_REFERENCED'
    ) {
        aiHubTabsStore.getState().openFileTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        // Opens the execution as a right-panel tab instead of a new browser tab. artifactId is the
        // workflow execution id; openWorkflowExecutionTab takes a number.
        aiHubTabsStore.getState().openWorkflowExecutionTab(Number(artifact.artifactId), artifact.artifactName);

        return;
    }

    if (
        artifact.kind === 'WORKFLOW_CREATED' ||
        artifact.kind === 'WORKFLOW_UPDATED' ||
        artifact.kind === 'WORKFLOW_REFERENCED'
    ) {
        const projectId = metadata['projectId'];
        const projectWorkflowId = Number(metadata['projectWorkflowId'] ?? 0);

        if (projectId) {
            aiHubTabsStore
                .getState()
                .openWorkflowTab(artifact.artifactId, projectId, projectWorkflowId, artifact.artifactName);
        }

        return;
    }

    if (
        artifact.kind === 'DATA_TABLE_ROW_ADDED' ||
        artifact.kind === 'DATA_TABLE_ROW_UPDATED' ||
        artifact.kind === 'DATA_TABLE_ROW_DELETED' ||
        artifact.kind === 'DATA_TABLE_COLUMN_ADDED' ||
        artifact.kind === 'DATA_TABLE_REFERENCED'
    ) {
        // DATA_TABLE_REFERENCED stores the dataTableId directly as artifactId (the artifact is the table
        // itself). The DATA_TABLE_ROW_* and DATA_TABLE_COLUMN_ADDED variants store a row id / column id as
        // artifactId and stash the parent dataTableId in metadata — fall back to artifactId for the
        // referenced case.
        const dataTableId = metadata['dataTableId'] ?? artifact.artifactId;

        if (dataTableId) {
            aiHubTabsStore.getState().openDataTableTab(dataTableId, metadata['name'] ?? artifact.artifactName);
        }

        return;
    }

    if (
        artifact.kind === 'KB_DOCUMENT_ADDED' ||
        artifact.kind === 'KB_DOCUMENT_DELETED' ||
        artifact.kind === 'KB_REFERENCED'
    ) {
        // KB_REFERENCED stores the knowledgeBaseId directly as artifactId. Document-add/delete variants stash
        // it in metadata — same fallback logic as the data-table case above.
        const knowledgeBaseId = metadata['knowledgeBaseId'] ?? artifact.artifactId;

        if (knowledgeBaseId) {
            aiHubTabsStore.getState().openKnowledgeBaseTab(knowledgeBaseId, metadata['name'] ?? artifact.artifactName);
        }

        return;
    }

    if (artifact.kind === 'SKILL_REFERENCED') {
        aiHubTabsStore.getState().openSkillTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'CUSTOM_COMPONENT_REFERENCED') {
        aiHubTabsStore.getState().openCustomComponentTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (artifact.kind === 'CODE_WORKFLOW_REFERENCED') {
        await openCodeWorkflowArtifact(artifact);

        return;
    }

    if (artifact.kind === 'AI_AGENT_REFERENCED') {
        aiHubTabsStore.getState().openAiAgentTab(artifact.artifactId, artifact.artifactName);

        return;
    }
}

// Reference-kind artifacts are user-attached and removable; agent-driven audit rows
// (FILE_CREATED, WORKFLOW_EXECUTION_STARTED, MEMORY_*, etc.) are immutable history and must NOT
// expose a remove affordance — deleting them would corrupt the workspace-wide audit listing.
export function isArtifactRemovable(artifact: AiHubChatArtifactI): boolean {
    return (
        artifact.kind === 'FILE_REFERENCED' ||
        artifact.kind === 'WORKFLOW_REFERENCED' ||
        artifact.kind === 'DATA_TABLE_REFERENCED' ||
        artifact.kind === 'KB_REFERENCED' ||
        artifact.kind === 'SKILL_REFERENCED' ||
        artifact.kind === 'CUSTOM_COMPONENT_REFERENCED' ||
        artifact.kind === 'CODE_WORKFLOW_REFERENCED' ||
        artifact.kind === 'AI_AGENT_REFERENCED'
    );
}

export function isArtifactClickable(artifact: AiHubChatArtifactI): boolean {
    if (
        artifact.kind === 'FILE_CREATED' ||
        artifact.kind === 'FILE_UPDATED' ||
        artifact.kind === 'BINARY_FILE_CREATED' ||
        artifact.kind === 'FILE_REFERENCED'
    ) {
        return true;
    }

    if (artifact.kind === 'WORKFLOW_EXECUTION_STARTED') {
        return true;
    }

    const metadata = parseMetadataJson(artifact.metadataJson);

    if (artifact.kind === 'WORKFLOW_CREATED' || artifact.kind === 'WORKFLOW_UPDATED') {
        return !!metadata['projectId'];
    }

    if (artifact.kind === 'WORKFLOW_REFERENCED') {
        // Referenced-workflow rows came from a tab open — they may not have projectId in metadata yet
        // (the recordReference mutation doesn't currently stash it). Fall back to artifactId being non-empty
        // so the click attempt at least dispatches; if projectId is truly missing, openWorkflowTab no-ops
        // without surfacing an error.
        return !!artifact.artifactId;
    }

    if (
        artifact.kind === 'DATA_TABLE_ROW_ADDED' ||
        artifact.kind === 'DATA_TABLE_ROW_UPDATED' ||
        artifact.kind === 'DATA_TABLE_ROW_DELETED' ||
        artifact.kind === 'DATA_TABLE_COLUMN_ADDED'
    ) {
        return !!metadata['dataTableId'];
    }

    if (artifact.kind === 'DATA_TABLE_REFERENCED') {
        // For referenced tables, artifactId IS the dataTableId — no metadata lookup needed.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'KB_DOCUMENT_ADDED' || artifact.kind === 'KB_DOCUMENT_DELETED') {
        return !!metadata['knowledgeBaseId'];
    }

    if (artifact.kind === 'KB_REFERENCED') {
        // Same logic as DATA_TABLE_REFERENCED — artifactId IS the knowledgeBaseId.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'SKILL_REFERENCED') {
        // Same logic as DATA_TABLE_REFERENCED / KB_REFERENCED — artifactId IS the skillId.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'CUSTOM_COMPONENT_REFERENCED') {
        // Same logic as SKILL_REFERENCED — artifactId IS the custom component id.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'CODE_WORKFLOW_REFERENCED') {
        // Same logic as CUSTOM_COMPONENT_REFERENCED — artifactId IS the projectId. The language fetch
        // that quick-open needs happens on click (see openCodeWorkflowArtifact), not here.
        return !!artifact.artifactId;
    }

    if (artifact.kind === 'AI_AGENT_REFERENCED') {
        // Same logic as SKILL_REFERENCED / CUSTOM_COMPONENT_REFERENCED — artifactId IS the agent id.
        return !!artifact.artifactId;
    }

    return false;
}
