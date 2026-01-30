import Badge from '@/components/Badge/Badge';
import {useKnowledgeBaseDocumentStatusPolling} from '@/pages/automation/knowledge-base/hooks/useKnowledgeBaseDocumentStatusPolling';
import {
    STATUS_ERROR,
    STATUS_PROCESSING,
    STATUS_READY,
    STATUS_UPLOADED,
    getStatusLabel,
} from '@/pages/automation/knowledge-base/util/knowledge-base-utils';
import {KnowledgeBaseDocument} from '@/shared/middleware/graphql';
import {useQueryClient} from '@tanstack/react-query';
import {
    FileCodeIcon,
    FileIcon,
    FileImageIcon,
    FileSpreadsheetIcon,
    FileTextIcon,
    FileTypeIcon,
    LoaderCircleIcon,
    PresentationIcon,
} from 'lucide-react';
import {useCallback, useRef, useState} from 'react';

interface UseKnowledgeBaseDocumentListItemProps {
    document: KnowledgeBaseDocument;
}

const getDocumentIcon = (extension?: string, mimeType?: string) => {
    const ext = extension?.toLowerCase();
    const mime = mimeType?.toLowerCase() || '';

    // Image files
    if (mime.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico'].includes(ext || '')) {
        return <FileImageIcon className="size-5 text-green-600" />;
    }

    // PDF files
    if (mime === 'application/pdf' || ext === 'pdf') {
        return <FileTextIcon className="size-5 text-red-600" />;
    }

    // Word documents
    if (mime.includes('msword') || mime.includes('wordprocessingml') || ['doc', 'docx', 'odt'].includes(ext || '')) {
        return <FileTypeIcon className="size-5 text-blue-600" />;
    }

    // Excel/Spreadsheet files
    if (
        mime.includes('spreadsheetml') ||
        mime.includes('ms-excel') ||
        ['xls', 'xlsx', 'csv', 'ods'].includes(ext || '')
    ) {
        return <FileSpreadsheetIcon className="size-5 text-green-700" />;
    }

    // PowerPoint/Presentation files
    if (
        mime.includes('presentationml') ||
        mime.includes('ms-powerpoint') ||
        ['ppt', 'pptx', 'odp'].includes(ext || '')
    ) {
        return <PresentationIcon className="size-5 text-orange-600" />;
    }

    // Code files
    if (
        mime.includes('javascript') ||
        mime.includes('typescript') ||
        mime.includes('json') ||
        mime.includes('xml') ||
        mime.includes('html') ||
        ['js', 'ts', 'jsx', 'tsx', 'json', 'xml', 'html', 'css', 'py', 'java', 'rb', 'go', 'rs'].includes(ext || '')
    ) {
        return <FileCodeIcon className="size-5 text-purple-600" />;
    }

    // Text files
    if (mime.startsWith('text/') || ['txt', 'md', 'rtf'].includes(ext || '')) {
        return <FileTextIcon className="size-5 text-muted-foreground" />;
    }

    // Default file icon
    return <FileIcon className="size-5 text-muted-foreground" />;
};

export default function useKnowledgeBaseDocumentListItem({document}: UseKnowledgeBaseDocumentListItemProps) {
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);

    const chunksCollapsibleTriggerRef = useRef<HTMLButtonElement | null>(null);
    const queryClient = useQueryClient();

    const needsPolling = document.status !== STATUS_READY && document.status !== STATUS_ERROR;

    const {status: polledStatus} = useKnowledgeBaseDocumentStatusPolling({
        documentId: document.id,
        enabled: needsPolling,
        onStatusChange: (status) => {
            if (status === STATUS_READY || status === STATUS_ERROR) {
                queryClient.invalidateQueries({queryKey: ['knowledgeBase']});
            }
        },
    });

    const currentStatus = needsPolling && polledStatus !== undefined ? polledStatus : document.status;

    const handleDocumentListItemClick = useCallback((event: React.MouseEvent) => {
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

    const handleShowDeleteDialog = () => {
        setShowDeleteDialog(true);
    };

    const handleCloseDeleteDialog = () => {
        setShowDeleteDialog(false);
    };

    const getStatusBadge = (status: number) => {
        const statusLabel = getStatusLabel(status);

        switch (status) {
            case STATUS_READY:
                return (
                    <Badge styleType="success-outline" weight="semibold">
                        {statusLabel}
                    </Badge>
                );
            case STATUS_UPLOADED:
            case STATUS_PROCESSING:
                return (
                    <Badge styleType="secondary-filled" weight="semibold">
                        <LoaderCircleIcon className="size-3 animate-spin" />

                        {statusLabel}
                    </Badge>
                );
            case STATUS_ERROR:
                return (
                    <Badge styleType="destructive-outline" weight="semibold">
                        {statusLabel}
                    </Badge>
                );
            default:
                return (
                    <Badge styleType="secondary-filled" weight="semibold">
                        {statusLabel}
                    </Badge>
                );
        }
    };

    const chunkCount = document.chunks?.length || 0;
    const documentIcon = getDocumentIcon(
        document.document?.extension ?? undefined,
        document.document?.mimeType ?? undefined
    );
    const displayName = document.document?.name || document.name;

    return {
        chunkCount,
        chunksCollapsibleTriggerRef,
        currentStatus,
        displayName,
        documentIcon,
        getStatusBadge,
        handleCloseDeleteDialog,
        handleDocumentListItemClick,
        handleShowDeleteDialog,
        showDeleteDialog,
    };
}
