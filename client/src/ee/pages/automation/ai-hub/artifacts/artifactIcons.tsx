import {AiHubArtifactKindType} from '@/ee/pages/automation/ai-hub/tasks/api/tasks.api';
import {
    BlocksIcon,
    BrainIcon,
    CodeIcon,
    DatabaseIcon,
    FileTextIcon,
    HexagonIcon,
    ImageIcon,
    PlayIcon,
    WorkflowIcon,
    WrenchIcon,
} from 'lucide-react';

export function getArtifactIcon(kind: AiHubArtifactKindType) {
    if (kind === 'FILE_CREATED' || kind === 'FILE_UPDATED' || kind === 'FILE_REFERENCED') {
        return <FileTextIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'BINARY_FILE_CREATED') {
        return <ImageIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'WORKFLOW_EXECUTION_STARTED') {
        return <PlayIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'WORKFLOW_CREATED' || kind === 'WORKFLOW_UPDATED' || kind === 'WORKFLOW_REFERENCED') {
        return <WorkflowIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (
        kind === 'DATA_TABLE_ROW_ADDED' ||
        kind === 'DATA_TABLE_ROW_UPDATED' ||
        kind === 'DATA_TABLE_ROW_DELETED' ||
        kind === 'DATA_TABLE_COLUMN_ADDED' ||
        kind === 'DATA_TABLE_REFERENCED'
    ) {
        return <DatabaseIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'KB_DOCUMENT_ADDED' || kind === 'KB_DOCUMENT_DELETED' || kind === 'KB_REFERENCED') {
        return <BrainIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'SKILL_REFERENCED') {
        return <HexagonIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'CUSTOM_COMPONENT_REFERENCED') {
        return <BlocksIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    if (kind === 'CODE_WORKFLOW_REFERENCED') {
        return <CodeIcon className="size-3.5 shrink-0 text-muted-foreground" />;
    }

    return <WrenchIcon className="size-3.5 shrink-0 text-muted-foreground" />;
}
