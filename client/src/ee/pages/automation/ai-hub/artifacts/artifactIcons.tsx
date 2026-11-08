import {AiHubArtifactKindType} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {
    BlocksIcon,
    BotIcon,
    CodeIcon,
    DatabaseIcon,
    FileTextIcon,
    HistoryIcon,
    LayersIcon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';

import {
    isAiAgentArtifactKind,
    isCodeWorkflowArtifactKind,
    isCustomComponentArtifactKind,
    isDataTableArtifactKind,
    isFileArtifactKind,
    isKnowledgeBaseArtifactKind,
    isWorkflowArtifactKind,
    isWorkflowExecutionArtifactKind,
} from './artifactOpen';

export function getArtifactIcon(kind: AiHubArtifactKindType) {
    if (isAiAgentArtifactKind(kind)) {
        return <BotIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isCustomComponentArtifactKind(kind)) {
        return <BlocksIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isCodeWorkflowArtifactKind(kind)) {
        return <CodeIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isDataTableArtifactKind(kind)) {
        return <DatabaseIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isKnowledgeBaseArtifactKind(kind)) {
        return <LayersIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isWorkflowExecutionArtifactKind(kind)) {
        return <HistoryIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isFileArtifactKind(kind)) {
        return <FileTextIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isWorkflowArtifactKind(kind)) {
        return <WorkflowIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    return <WrenchIcon className="size-3.5 shrink-0 text-muted-foreground" />;
}
