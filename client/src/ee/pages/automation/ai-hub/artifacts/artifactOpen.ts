import {AiHubArtifactKindType, AiHubChatArtifactI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {ProjectApi} from '@/shared/middleware/automation/configuration';
import {toast} from 'sonner';

const WORKFLOW_ARTIFACT_KINDS: ReadonlySet<AiHubArtifactKindType> = new Set([
    'WORKFLOW_CREATED',
    'WORKFLOW_REFERENCED',
    'WORKFLOW_UPDATED',
]);

export function isAiAgentArtifactKind(kind: AiHubArtifactKindType): boolean {
    return kind === 'AI_AGENT_REFERENCED';
}

export function isFileArtifactKind(kind: AiHubArtifactKindType): boolean {
    return kind === 'FILE_REFERENCED';
}

export function isCodeWorkflowArtifactKind(kind: AiHubArtifactKindType): boolean {
    return kind === 'CODE_WORKFLOW_REFERENCED';
}

export function isCustomComponentArtifactKind(kind: AiHubArtifactKindType): boolean {
    return kind === 'CUSTOM_COMPONENT_REFERENCED';
}

export async function openCodeWorkflowArtifactTab(projectId: string, name: string, language?: string): Promise<void> {
    if (language) {
        aiHubTabsStore.getState().openCodeWorkflowTab({language, name, projectId});

        return;
    }

    try {
        const project = await new ProjectApi().getProject({id: Number(projectId)});

        if (!project.codeWorkflowLanguage) {
            toast.error(`"${name}" is no longer a code workflow.`);

            return;
        }

        aiHubTabsStore.getState().openCodeWorkflowTab({language: project.codeWorkflowLanguage, name, projectId});
    } catch (error) {
        const message = error instanceof Error ? error.message : String(error);

        toast.error(`Failed to open "${name}": ${message}`);
    }
}

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

export function handleArtifactQuickOpen(artifact: AiHubChatArtifactI): void {
    if (isAiAgentArtifactKind(artifact.kind)) {
        aiHubTabsStore.getState().openAiAgentTab({aiAgentId: artifact.artifactId, name: artifact.artifactName});

        return;
    }

    if (isFileArtifactKind(artifact.kind)) {
        aiHubTabsStore.getState().openFileTab(artifact.artifactId, artifact.artifactName);

        return;
    }

    if (isCustomComponentArtifactKind(artifact.kind)) {
        aiHubTabsStore
            .getState()
            .openCustomComponentTab({customComponentId: artifact.artifactId, name: artifact.artifactName});

        return;
    }

    if (isCodeWorkflowArtifactKind(artifact.kind)) {
        void openCodeWorkflowArtifactTab(artifact.artifactId, artifact.artifactName);

        return;
    }

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

const REMOVABLE_ARTIFACT_KINDS: AiHubArtifactKindType[] = [
    'AI_AGENT_REFERENCED',
    'CODE_WORKFLOW_REFERENCED',
    'CUSTOM_COMPONENT_REFERENCED',
    'FILE_REFERENCED',
    'WORKFLOW_REFERENCED',
];

export function isArtifactRemovable(artifact: AiHubChatArtifactI): boolean {
    return REMOVABLE_ARTIFACT_KINDS.includes(artifact.kind);
}

export function isArtifactClickable(artifact: AiHubChatArtifactI): boolean {
    if (
        isAiAgentArtifactKind(artifact.kind) ||
        isCodeWorkflowArtifactKind(artifact.kind) ||
        isCustomComponentArtifactKind(artifact.kind) ||
        isFileArtifactKind(artifact.kind)
    ) {
        return true;
    }

    if (!isWorkflowArtifactKind(artifact.kind)) {
        return false;
    }

    return !!parseArtifactMetadata(artifact.metadataJson)['projectId'];
}
