import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {CloudDownloadIcon, FolderSyncIcon, InfoIcon, SendToBackIcon, Trash2Icon, UploadIcon} from 'lucide-react';
import {createElement, useMemo, useState} from 'react';

import {useEndpointDetailPanelStore} from '../stores/useEndpointDetailPanelStore';

interface UseApiConnectorEndpointListItemProps {
    apiConnectorEndpoint: ApiConnectorEndpoint;
    apiConnectorName: string;
    specification?: string;
}

const useApiConnectorEndpointListItem = ({
    apiConnectorEndpoint,
    apiConnectorName,
    specification,
}: UseApiConnectorEndpointListItemProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {openPanel} = useEndpointDetailPanelStore();

    const lastExecutionDate = useMemo(
        () => (apiConnectorEndpoint.lastExecutionDate ? new Date(apiConnectorEndpoint.lastExecutionDate) : undefined),
        [apiConnectorEndpoint.lastExecutionDate]
    );

    const method = apiConnectorEndpoint.httpMethod;

    const httpMethodStyles = useMemo(() => {
        const iconClassName = 'size-3';

        switch (method) {
            case 'GET':
                return {
                    icon: createElement(CloudDownloadIcon, {className: iconClassName}),
                    textColor: 'text-content-brand-primary',
                };
            case 'POST':
                return {
                    icon: createElement(UploadIcon, {className: iconClassName}),
                    textColor: 'text-content-success-primary',
                };
            case 'PUT':
                return {
                    icon: createElement(SendToBackIcon, {className: iconClassName}),
                    textColor: 'text-content-warning-primary',
                };
            case 'PATCH':
                return {
                    icon: createElement(FolderSyncIcon, {className: iconClassName}),
                    textColor: 'text-orange-700',
                };
            case 'DELETE':
                return {
                    icon: createElement(Trash2Icon, {className: iconClassName}),
                    textColor: 'text-content-destructive-primary',
                };
            default:
                return {
                    icon: createElement(InfoIcon, {className: iconClassName}),
                    textColor: 'text-gray-700',
                };
        }
    }, [method]);

    const {icon, textColor} = httpMethodStyles;

    const handleClick = () => {
        openPanel(apiConnectorEndpoint, apiConnectorName, specification);
    };

    return {
        handleClick,
        icon,
        lastExecutionDate,
        method,
        setShowEditWorkflowDialog,
        showEditWorkflowDialog,
        textColor,
    };
};

export default useApiConnectorEndpointListItem;
