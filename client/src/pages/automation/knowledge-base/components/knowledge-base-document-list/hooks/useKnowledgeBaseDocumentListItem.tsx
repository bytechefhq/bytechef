import useKnowledgeBaseDocumentStatusPolling from '@/pages/automation/knowledge-base/components/knowledge-base-document-list/hooks/useKnowledgeBaseDocumentStatusPolling';
import {
    STATUS_ERROR,
    STATUS_READY,
    getDocumentIcon,
    getStatusBadge,
} from '@/pages/automation/knowledge-base/util/knowledge-base-utils';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import React, {useCallback, useRef} from 'react';

interface UseKnowledgeBaseDocumentListItemProps {
    document: KnowledgeBaseDocument;
}

export default function useKnowledgeBaseDocumentListItem({document}: UseKnowledgeBaseDocumentListItemProps) {
    const chunksCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);

    const queryClient = useQueryClient();

    const needsPolling = document.status !== STATUS_READY && document.status !== STATUS_ERROR;

    const handleStatusChange = useCallback(
        (status: number) => {
            if (status === STATUS_READY || status === STATUS_ERROR) {
                queryClient.invalidateQueries({queryKey: ['knowledgeBase']});
            }
        },
        [queryClient]
    );

    const {status: polledStatus} = useKnowledgeBaseDocumentStatusPolling({
        documentId: document.id,
        enabled: needsPolling,
        onStatusChange: handleStatusChange,
    });

    const currentStatus = needsPolling && polledStatus !== undefined ? polledStatus : document.status;
    const chunkCount = document.chunks?.length || 0;
    const documentIcon = getDocumentIcon(
        document.document?.extension ?? undefined,
        document.document?.mimeType ?? undefined
    );
    const displayName = document.document?.name || document.name;
    const statusBadge = getStatusBadge(currentStatus);

    const handleDocumentListItemClick = useCallback((event: React.MouseEvent<HTMLDivElement>) => {
        const target = event.target as HTMLElement;

        const interactiveSelectors = [
            '[data-interactive]',
            '.dropdown-menu-item',
            '[data-radix-dropdown-menu-item]',
            '[data-radix-dropdown-menu-trigger]',
            '[data-radix-collapsible-trigger]',
        ].join(', ');

        if (target.closest(interactiveSelectors)) {
            return;
        }

        if (chunksCollapsibleTriggerRef.current?.contains(target)) {
            return;
        }

        chunksCollapsibleTriggerRef.current?.click();
    }, []);

    const handleDocumentListItemKeyDown = useCallback((event: React.KeyboardEvent<HTMLDivElement>) => {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            chunksCollapsibleTriggerRef.current?.click();
        }
    }, []);

    const handleTagListClick = (event: React.MouseEvent<HTMLDivElement>) => {
        event.stopPropagation();
    };

    return {
        chunkCount,
        chunksCollapsibleTriggerRef,
        displayName,
        documentIcon,
        handleDocumentListItemClick,
        handleDocumentListItemKeyDown,
        handleTagListClick,
        statusBadge,
    };
}
