import {useKnowledgeBaseDocumentChunkSelectionStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkSelectionStore';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentChunkListItemHeaderProps {
    chunkId: string;
}

export default function useKnowledgeBaseDocumentChunkListItemHeader({
    chunkId,
}: UseKnowledgeBaseDocumentChunkListItemHeaderProps) {
    const {selectedChunks, toggleChunkSelection} = useKnowledgeBaseDocumentChunkSelectionStore(
        useShallow((state) => ({
            selectedChunks: state.selectedChunks,
            toggleChunkSelection: state.toggleChunkSelection,
        }))
    );

    const isSelected = selectedChunks.includes(chunkId);

    const handleSelectionChange = () => {
        toggleChunkSelection(chunkId);
    };

    return {
        handleSelectionChange,
        isSelected,
    };
}
