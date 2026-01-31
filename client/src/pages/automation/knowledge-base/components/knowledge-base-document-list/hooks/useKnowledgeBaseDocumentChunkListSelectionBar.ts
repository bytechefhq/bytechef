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

    return {
        handleClearSelection: clearSelection,
        handleDeleteSelected: () => setChunkIdsToDelete(selectedChunks),
        hasSelection: selectedChunks.length > 0,
        selectedCount: selectedChunks.length,
    };
}
