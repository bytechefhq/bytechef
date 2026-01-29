import {useKnowledgeBaseDocumentChunkDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkDeleteDialogStore';
import {useKnowledgeBaseDocumentChunkEditDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentChunkEditDialogStore';
import {KnowledgeBaseDocumentChunk} from '@/shared/middleware/graphql';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentChunkListItemDropdownMenuProps {
    chunk: KnowledgeBaseDocumentChunk;
}

export default function useKnowledgeBaseDocumentChunkListItemDropdownMenu({
    chunk,
}: UseKnowledgeBaseDocumentChunkListItemDropdownMenuProps) {
    const {setChunk} = useKnowledgeBaseDocumentChunkEditDialogStore(
        useShallow((state) => ({
            setChunk: state.setChunk,
        }))
    );

    const {setChunkIdsToDelete} = useKnowledgeBaseDocumentChunkDeleteDialogStore(
        useShallow((state) => ({
            setChunkIdsToDelete: state.setChunkIdsToDelete,
        }))
    );

    const handleEdit = () => {
        setChunk(chunk);
    };

    const handleDelete = () => {
        setChunkIdsToDelete([chunk.id]);
    };

    return {
        handleDelete,
        handleEdit,
    };
}
