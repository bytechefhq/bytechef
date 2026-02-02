import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ApiConnectorEndpoint} from '@/shared/middleware/graphql';
import {
    CalendarIcon,
    CloudDownloadIcon,
    EllipsisVerticalIcon,
    FolderSyncIcon,
    InfoIcon,
    SendToBackIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import {useEndpointDetailPanelStore} from '../stores/useEndpointDetailPanelStore';

interface ApiConnectorEndpointListItemProps {
    apiConnectorEndpoint: ApiConnectorEndpoint;
    apiConnectorName: string;
    specification?: string;
}

const ApiConnectorEndpointListItem = ({
    apiConnectorEndpoint,
    apiConnectorName,
    specification,
}: ApiConnectorEndpointListItemProps) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const projectDeploymentWorkflow = undefined;

    const {openPanel} = useEndpointDetailPanelStore();

    const method = apiConnectorEndpoint.httpMethod;

    const httpMethodStyles = useMemo(() => {
        switch (method) {
            case 'GET':
                return {
                    icon: <CloudDownloadIcon className="size-3" />,
                    textColor: 'text-content-brand-primary',
                };
            case 'POST':
                return {
                    icon: <UploadIcon className="size-3" />,
                    textColor: 'text-content-success-primary',
                };
            case 'PUT':
                return {
                    icon: <SendToBackIcon className="size-3" />,
                    textColor: 'text-content-warning-primary',
                };
            case 'PATCH':
                return {
                    icon: <FolderSyncIcon className="size-3" />,
                    textColor: 'text-orange-700',
                };
            case 'DELETE':
                return {
                    icon: <Trash2Icon className="size-3" />,
                    textColor: 'text-content-destructive-primary',
                };
            default:
                return {
                    icon: <InfoIcon className="size-3" />,
                    textColor: 'text-gray-700',
                };
        }
    }, [method]);

    const {icon, textColor} = httpMethodStyles;

    const handleClick = () => {
        openPanel(apiConnectorEndpoint, apiConnectorName, specification);
    };

    return (
        <>
            <button className="flex flex-1 items-center text-left" onClick={handleClick} type="button">
                <div className="flex flex-1 items-center">
                    {method && (
                        <Badge
                            className={twMerge('mr-4 w-20', textColor)}
                            icon={icon}
                            label={method}
                            styleType="outline-outline"
                            weight="semibold"
                        />
                    )}

                    <div className="w-2/6 text-sm font-semibold">{apiConnectorEndpoint.name}</div>

                    <div className="text-xs text-gray-500">{apiConnectorEndpoint.path}</div>
                </div>
            </button>

            <div className="flex items-center justify-end gap-x-6">
                {apiConnectorEndpoint?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <CalendarIcon aria-hidden="true" className="mr-0.5 size-3.5 shrink-0 text-gray-400" />

                            <span className="text-xs">
                                {`Executed at ${new Date(apiConnectorEndpoint.lastExecutionDate).toLocaleDateString()} ${new Date(apiConnectorEndpoint.lastExecutionDate).toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    <span className="text-xs">No executions</span>
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button
                            icon={<EllipsisVerticalIcon className="size-4 hover:cursor-pointer" />}
                            size="icon"
                            variant="ghost"
                        />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setShowEditWorkflowDialog(true)}>Edit</DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showEditWorkflowDialog && projectDeploymentWorkflow && (
                // <ProjectDeploymentEditWorkflowDialog
                //     onClose={() => setShowEditWorkflowDialog(false)}
                //     projectDeploymentEnabled={projectDeploymentEnabled}
                //     projectDeploymentWorkflow={projectDeploymentWorkflow}
                //     workflow={workflow}
                // />
                <>TODO</>
            )}
        </>
    );
};

export default ApiConnectorEndpointListItem;
