import {useKnowledgeBaseDocumentChunkDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore';
import {useKnowledgeBaseDocumentChunkSelectionStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore';
import {useShallow} from 'zustand/react/shallow';

export default function useKnowledgeBaseDocumentChunkListSelectionBar() {
    const {clearSelection, selectedChunks} = useKnowledgeBaseDocumentChunkSelectionStore(
        useShallow((state) => ({
            clearSelection: state.clearSelection,
            selectedChunks: state.selectedChunks,
        }))
    );

    const {setChunkIdsToDelete} = useKnowledgeBaseDocumentChunkDeleteDialogStore(
        useShallow((state) => ({
            setChunkIdsToDelete: state.setChunkIdsToDelete,
        }))
    );

    const handleClearSelection = () => {
        clearSelection();
    };

    const handleDeleteSelected = () => {
        setChunkIdsToDelete(selectedChunks);
    };

    const selectedCount = selectedChunks.length;
    const hasSelection = selectedCount > 0;

    return {
        handleClearSelection,
        handleDeleteSelected,
        hasSelection,
        selectedCount,
    };
}
