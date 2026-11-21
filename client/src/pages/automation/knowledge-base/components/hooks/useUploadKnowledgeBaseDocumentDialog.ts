import useFileUploadDialog from '@/shared/hooks/useFileUploadDialog';
import {useMemo} from 'react';

interface UseUploadKnowledgeBaseDocumentDialogProps {
    knowledgeBaseId: string;
}

export default function useUploadKnowledgeBaseDocumentDialog({
    knowledgeBaseId,
}: UseUploadKnowledgeBaseDocumentDialogProps) {
    const invalidateQueryKey = useMemo(() => ['knowledgeBase', {id: knowledgeBaseId}], [knowledgeBaseId]);

    return useFileUploadDialog({
        fieldName: 'file',
        invalidateQueryKey,
        url: `/api/automation/internal/knowledge-bases/${knowledgeBaseId}/documents`,
    });
}
