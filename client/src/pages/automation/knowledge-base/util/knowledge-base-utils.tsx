import Badge from '@/components/Badge/Badge';
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

export const STATUS_UPLOADED = 0;
export const STATUS_PROCESSING = 1;
export const STATUS_READY = 2;
export const STATUS_ERROR = 3;

export const getStatusLabel = (status: number): string => {
    switch (status) {
        case STATUS_UPLOADED:
            return 'Uploaded';
        case STATUS_PROCESSING:
            return 'Processing';
        case STATUS_READY:
            return 'Ready';
        case STATUS_ERROR:
            return 'Error';
        default:
            return 'Unknown';
    }
};

export const getDocumentIcon = (extension?: string, mimeType?: string) => {
    const ext = extension?.toLowerCase();
    const mime = mimeType?.toLowerCase() || '';

    if (mime.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico'].includes(ext || '')) {
        return <FileImageIcon className="size-5 text-green-600" />;
    }

    if (mime === 'application/pdf' || ext === 'pdf') {
        return <FileTextIcon className="size-5 text-red-600" />;
    }

    if (mime.includes('msword') || mime.includes('wordprocessingml') || ['doc', 'docx', 'odt'].includes(ext || '')) {
        return <FileTypeIcon className="size-5 text-blue-600" />;
    }

    if (
        mime.includes('spreadsheetml') ||
        mime.includes('ms-excel') ||
        ['xls', 'xlsx', 'csv', 'ods'].includes(ext || '')
    ) {
        return <FileSpreadsheetIcon className="size-5 text-green-700" />;
    }

    if (
        mime.includes('presentationml') ||
        mime.includes('ms-powerpoint') ||
        ['ppt', 'pptx', 'odp'].includes(ext || '')
    ) {
        return <PresentationIcon className="size-5 text-orange-600" />;
    }

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

    if (mime.startsWith('text/') || ['txt', 'md', 'rtf'].includes(ext || '')) {
        return <FileTextIcon className="size-5 text-muted-foreground" />;
    }

    return <FileIcon className="size-5 text-muted-foreground" />;
};

export const getStatusBadge = (status: number) => {
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
