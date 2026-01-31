import {STATUS_ERROR, STATUS_READY} from '@/pages/automation/knowledge-base/util/knowledge-base-utils';
import {useKnowledgeBaseDocumentStatusQuery} from '@/shared/middleware/graphql';
import {useEffect} from 'react';

interface UseKnowledgeBaseDocumentStatusPollingOptionsI {
    documentId?: string;
    enabled: boolean;
    onStatusChange?: (status: number, message?: string) => void;
    pollingInterval?: number;
}

export default function useKnowledgeBaseDocumentStatusPolling({
    documentId,
    enabled,
    onStatusChange,
    pollingInterval = 2000,
}: UseKnowledgeBaseDocumentStatusPollingOptionsI) {
    const {data, isLoading} = useKnowledgeBaseDocumentStatusQuery(
        {id: documentId || ''},
        {
            enabled: enabled && !!documentId,
            refetchInterval: (query) => {
                const status = query.state.data?.knowledgeBaseDocumentStatus?.status;

                if (status === undefined || status === STATUS_READY || status === STATUS_ERROR) {
                    return false;
                }

                return pollingInterval;
            },
            refetchIntervalInBackground: false,
        }
    );

    const status = data?.knowledgeBaseDocumentStatus?.status;
    const isPolling = status !== undefined && status !== STATUS_READY && status !== STATUS_ERROR;

    useEffect(() => {
        if (data?.knowledgeBaseDocumentStatus) {
            const {message, status: documentStatus} = data.knowledgeBaseDocumentStatus;

            if (onStatusChange) {
                onStatusChange(documentStatus, message || undefined);
            }
        }
    }, [data, onStatusChange]);

    return {
        isLoading,
        isPolling,
        message: data?.knowledgeBaseDocumentStatus?.message,
        status,
    };
}
