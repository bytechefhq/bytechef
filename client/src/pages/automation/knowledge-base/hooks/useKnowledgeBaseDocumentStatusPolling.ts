import {STATUS_ERROR, STATUS_READY} from '@/pages/automation/knowledge-base/util/knowledge-base-utils';
import {useKnowledgeBaseDocumentStatusQuery} from '@/shared/middleware/graphql';
import {useEffect, useState} from 'react';

interface UseKnowledgeBaseDocumentStatusPollingOptionsI {
    documentId?: string;
    enabled: boolean;
    onStatusChange?: (status: number, message?: string) => void;
    pollingInterval?: number;
}

export const useKnowledgeBaseDocumentStatusPolling = ({
    documentId,
    enabled,
    onStatusChange,
    pollingInterval = 2000,
}: UseKnowledgeBaseDocumentStatusPollingOptionsI) => {
    const [isPolling, setIsPolling] = useState(false);

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

    useEffect(() => {
        if (data?.knowledgeBaseDocumentStatus) {
            const {message, status} = data.knowledgeBaseDocumentStatus;

            onStatusChange?.(status, message || undefined);

            if (status === STATUS_READY || status === STATUS_ERROR) {
                setIsPolling(false);
            } else {
                setIsPolling(true);
            }
        }
    }, [data, onStatusChange]);

    return {
        isLoading,
        isPolling,
        message: data?.knowledgeBaseDocumentStatus?.message,
        status: data?.knowledgeBaseDocumentStatus?.status,
    };
};
