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

type DocumentType = 'image' | 'pdf' | 'word' | 'spreadsheet' | 'presentation' | 'code' | 'text' | 'unknown';

const getDocumentType = (extension?: string, mimeType?: string): DocumentType => {
    const ext = extension?.toLowerCase() || '';
    const mime = mimeType?.toLowerCase() || '';

    const isImage =
        mime.startsWith('image/') || ['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp', 'ico'].includes(ext);
    const isPdf = mime === 'application/pdf' || ext === 'pdf';
    const isWord = mime.includes('msword') || mime.includes('wordprocessingml') || ['doc', 'docx', 'odt'].includes(ext);
    const isSpreadsheet =
        mime.includes('spreadsheetml') || mime.includes('ms-excel') || ['xls', 'xlsx', 'csv', 'ods'].includes(ext);
    const isPresentation =
        mime.includes('presentationml') || mime.includes('ms-powerpoint') || ['ppt', 'pptx', 'odp'].includes(ext);
    const isCode =
        mime.includes('javascript') ||
        mime.includes('typescript') ||
        mime.includes('json') ||
        mime.includes('xml') ||
        mime.includes('html') ||
        ['js', 'ts', 'jsx', 'tsx', 'json', 'xml', 'html', 'css', 'py', 'java', 'rb', 'go', 'rs'].includes(ext);
    const isText = mime.startsWith('text/') || ['txt', 'md', 'rtf'].includes(ext);

    if (isImage) {
        return 'image';
    }

    if (isPdf) {
        return 'pdf';
    }

    if (isWord) {
        return 'word';
    }

    if (isSpreadsheet) {
        return 'spreadsheet';
    }

    if (isPresentation) {
        return 'presentation';
    }

    if (isCode) {
        return 'code';
    }

    if (isText) {
        return 'text';
    }

    return 'unknown';
};

export const getDocumentIcon = (extension?: string, mimeType?: string) => {
    const documentType = getDocumentType(extension, mimeType);

    switch (documentType) {
        case 'image':
            return <FileImageIcon className="size-5 text-content-success-primary" />;
        case 'pdf':
            return <FileTextIcon className="size-5 text-content-destructive-primary" />;
        case 'word':
            return <FileTypeIcon className="size-5 text-content-brand-primary" />;
        case 'spreadsheet':
            return <FileSpreadsheetIcon className="size-5 text-content-success-primary" />;
        case 'presentation':
            return <PresentationIcon className="size-5 text-content-warning-primary" />;
        case 'code':
            return <FileCodeIcon className="size-5 text-content-brand-secondary" />;
        case 'text':
            return <FileTextIcon className="size-5 text-muted-foreground" />;
        default:
            return <FileIcon className="size-5 text-muted-foreground" />;
    }
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
