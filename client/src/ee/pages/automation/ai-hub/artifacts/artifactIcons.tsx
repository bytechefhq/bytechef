import {AiHubArtifactKindType} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {BotIcon, WorkflowIcon, WrenchIcon} from 'lucide-react';

import {isAiAgentArtifactKind, isWorkflowArtifactKind} from './artifactOpen';

export function getArtifactIcon(kind: AiHubArtifactKindType) {
    if (isAiAgentArtifactKind(kind)) {
        return <BotIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (isWorkflowArtifactKind(kind)) {
        return <WorkflowIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    return <WrenchIcon className="size-3.5 shrink-0 text-muted-foreground" />;
}
