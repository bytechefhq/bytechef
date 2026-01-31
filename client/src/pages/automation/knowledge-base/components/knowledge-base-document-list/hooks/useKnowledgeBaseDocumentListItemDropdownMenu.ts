import {useKnowledgeBaseDocumentDeleteDialogStore} from '@/pages/automation/knowledge-base/stores/useKnowledgeBaseDocumentDeleteDialogStore';
import {useShallow} from 'zustand/react/shallow';

interface UseKnowledgeBaseDocumentListItemDropdownMenuProps {
    documentId: string;
}

export default function useKnowledgeBaseDocumentListItemDropdownMenu({
    documentId,
}: UseKnowledgeBaseDocumentListItemDropdownMenuProps) {
    const {setDocumentId} = useKnowledgeBaseDocumentDeleteDialogStore(
        useShallow((state) => ({
            setDocumentId: state.setDocumentId,
        }))
    );

    return {
        handleDelete: () => setDocumentId(documentId),
    };
}
