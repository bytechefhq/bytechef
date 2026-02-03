import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    CalendarIcon,
    CloudDownloadIcon,
    EllipsisVerticalIcon,
    FolderSyncIcon,
    SendToBackIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

// TODO: Uncomment when api-connector middleware is implemented
// import {ApiConnectorEndpoint} from '@/ee/shared/middleware/platform/api-connector';
import {type ApiConnectorEndpointI} from './ApiConnectorEndpointList';

const ApiConnectorEndpointListItem = ({apiConnectorEndpoint}: {apiConnectorEndpoint: ApiConnectorEndpointI}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);
    const projectDeploymentWorkflow = undefined;

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
                    icon: null,
                    textColor: '',
                };
        }
    }, [method]);

    const {icon, textColor} = httpMethodStyles;

    return (
        <>
            <Link className="flex flex-1 items-center" to={`/automation/projects/${1}/workflows/${1}`}>
                <div className="flex flex-1 items-center">
                    {method &&
                        (icon ? (
                            <Badge
                                className={twMerge('mr-4 w-20', textColor)}
                                icon={icon}
                                label={method}
                                styleType="outline-outline"
                                weight="semibold"
                            />
                        ) : (
                            <Badge
                                className={twMerge('mr-4 w-20', textColor)}
                                label={method}
                                styleType="outline-outline"
                                weight="semibold"
                            />
                        ))}

                    <div className="w-2/6 text-sm font-semibold">{apiConnectorEndpoint.name}</div>

                    <div className="text-xs text-gray-500">{apiConnectorEndpoint.path}</div>
                </div>
            </Link>

            <div className="flex items-center justify-end gap-x-6">
                {apiConnectorEndpoint?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <CalendarIcon aria-hidden="true" className="mr-0.5 size-3.5 shrink-0 text-gray-400" />

                            <span className="text-xs">
                                {`Executed at ${apiConnectorEndpoint.lastExecutionDate?.toLocaleDateString()} ${apiConnectorEndpoint.lastExecutionDate?.toLocaleTimeString()}`}
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
