import {Checkbox} from '@/components/ui/checkbox';
import useKnowledgeBaseDocumentChunkListItemHeader from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkListItemHeader';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';

interface KnowledgeBaseDocumentChunkListItemHeaderProps {
    chunkId: string;
    chunkIndex: number;
    documentName: string;
}

const KnowledgeBaseDocumentChunkListItemHeader = ({
    chunkId,
    chunkIndex,
    documentName,
}: KnowledgeBaseDocumentChunkListItemHeaderProps) => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleSelectionChange, isSelected} = useKnowledgeBaseDocumentChunkListItemHeader({chunkId});

    const canEditKnowledgeBase = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_EDIT');

    return (
        <div className="flex items-center space-x-3">
            <Checkbox
                aria-label={`Select chunk ${chunkIndex + 1}`}
                checked={isSelected}
                disabled={!canEditKnowledgeBase}
                onCheckedChange={handleSelectionChange}
            />

            <div className="flex items-center space-x-2 text-sm text-content-neutral-secondary">
                <span className="font-medium">{documentName}</span>

                <span>•</span>

                <span>Chunk {chunkIndex + 1}</span>
            </div>
        </div>
    );
};

export default KnowledgeBaseDocumentChunkListItemHeader;
