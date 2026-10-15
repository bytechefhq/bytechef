import Button from '@/components/Button/Button';
import useKnowledgeBaseDocumentChunkListSelectionBar from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentChunkListSelectionBar';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useHasWorkspaceScope} from '@/shared/hooks/useHasWorkspaceScope';
import {Trash2Icon} from 'lucide-react';

const KnowledgeBaseDocumentChunkListSelectionBar = () => {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {handleClearSelection, handleDeleteSelected, hasSelection, selectedCount} =
        useKnowledgeBaseDocumentChunkListSelectionBar();

    const canEditKnowledgeBase = useHasWorkspaceScope(currentWorkspaceId, 'KNOWLEDGE_BASE_EDIT');

    if (!hasSelection) {
        return null;
    }

    return (
        <div className="flex items-center justify-between rounded-lg border border-blue-200 bg-blue-50 p-3">
            <span className="text-sm font-medium text-blue-900">{selectedCount} chunk(s) selected</span>

            <div className="flex space-x-2">
                {canEditKnowledgeBase && (
                    <Button onClick={handleDeleteSelected} size="sm" variant="destructive">
                        <Trash2Icon className="mr-2 size-4" />
                        Delete Selected
                    </Button>
                )}

                <Button onClick={handleClearSelection} size="sm" variant="outline">
                    Clear Selection
                </Button>
            </div>
        </div>
    );
};

export default KnowledgeBaseDocumentChunkListSelectionBar;
