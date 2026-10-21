import {CloudDownloadIcon, FolderSyncIcon, SendToBackIcon, Trash2Icon, UploadIcon} from 'lucide-react';
import {ReactElement} from 'react';

interface HttpMethodBadgeStylesI {
    icon?: ReactElement;
    textColor: string;
}

export const getHttpMethodBadgeStyles = (method?: string): HttpMethodBadgeStylesI => {
    switch (method) {
        case 'GET':
            return {icon: <CloudDownloadIcon className="size-3" />, textColor: 'text-content-brand-primary'};
        case 'POST':
            return {icon: <UploadIcon className="size-3" />, textColor: 'text-content-success-primary'};
        case 'PUT':
            return {icon: <SendToBackIcon className="size-3" />, textColor: 'text-content-warning-primary'};
        case 'PATCH':
            return {icon: <FolderSyncIcon className="size-3" />, textColor: 'text-orange-700 dark:text-orange-400'};
        case 'DELETE':
            return {icon: <Trash2Icon className="size-3" />, textColor: 'text-content-destructive-primary'};
        default:
            return {icon: undefined, textColor: ''};
    }
};
