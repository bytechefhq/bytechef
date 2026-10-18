import {AiHubArtifactKindType} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {WorkflowIcon, WrenchIcon} from 'lucide-react';

import {isWorkflowArtifactKind} from './artifactOpen';

export function getArtifactIcon(kind: AiHubArtifactKindType) {
    if (isWorkflowArtifactKind(kind)) {
        return <WorkflowIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    return <WrenchIcon className="size-3.5 shrink-0 text-muted-foreground" />;
}
