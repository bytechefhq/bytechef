import {AiHubArtifactKindType, AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';

const WORKFLOW_ARTIFACT_KINDS: ReadonlySet<AiHubArtifactKindType> = new Set([
    'WORKFLOW_CREATED',
    'WORKFLOW_REFERENCED',
    'WORKFLOW_UPDATED',
]);

export function isWorkflowArtifactKind(kind: AiHubArtifactKindType): boolean {
    return WORKFLOW_ARTIFACT_KINDS.has(kind);
}

export function parseArtifactMetadata(metadataJson: string | null): Record<string, string> {
    if (!metadataJson) {
        return {};
    }

    try {
        const parsed = JSON.parse(metadataJson);

        return parsed != null && typeof parsed === 'object' ? (parsed as Record<string, string>) : {};
    } catch (parseError) {
        console.warn('[artifactOpen] Failed to parse artifact metadataJson; quick-open will be disabled', {
            parseError,
            raw: metadataJson,
        });

        return {};
    }
}

/**
 * Opens a workflow artifact as a workflow tab. Every workflow kind carries the parent {@code projectId} and
 * {@code projectWorkflowId} in its metadata, which the tab needs because it routes by project; without them there is
 * nothing to open.
 */
export function handleArtifactQuickOpen(artifact: AiHubChatArtifactI): void {
    if (!isWorkflowArtifactKind(artifact.kind)) {
        return;
    }

    const metadata = parseArtifactMetadata(artifact.metadataJson);

    const projectId = metadata['projectId'];

    if (!projectId) {
        return;
    }

    aiHubTabsStore.getState().openWorkflowTab({
        name: artifact.artifactName,
        projectId: String(projectId),
        projectWorkflowId: Number(metadata['projectWorkflowId'] ?? 0),
        workflowId: artifact.artifactId,
    });
}

/**
 * Only a referenced workflow is user-attached and removable; the created and updated rows are the agent's history.
 */
export function isArtifactRemovable(artifact: AiHubChatArtifactI): boolean {
    return artifact.kind === 'WORKFLOW_REFERENCED';
}

export function isArtifactClickable(artifact: AiHubChatArtifactI): boolean {
    if (!isWorkflowArtifactKind(artifact.kind)) {
        return false;
    }

    return !!parseArtifactMetadata(artifact.metadataJson)['projectId'];
}
