import {useKnowledgeBaseDocumentChunkSelectionStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore';
import {useShallow} from 'zustand/react/shallow';

export default function useKnowledgeBaseDocumentChunkList() {
    const {clearSelection, selectedChunks, toggleChunkSelection} = useKnowledgeBaseDocumentChunkSelectionStore(
        useShallow((state) => ({
            clearSelection: state.clearSelection,
            selectedChunks: state.selectedChunks,
            toggleChunkSelection: state.toggleChunkSelection,
        }))
    );

    return {
        handleClearSelection: clearSelection,
        handleSelectChunk: toggleChunkSelection,
        selectedChunks,
    };
}
