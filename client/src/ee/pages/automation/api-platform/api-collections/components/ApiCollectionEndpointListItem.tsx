import '@/shared/styles/dropdownMenu.css';
import Badge from '@/components/Badge/Badge';
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import {ApiCollectionEndpoint} from '@/ee/shared/middleware/automation/api-platform';
import {useDeleteApiCollectionEndpointMutation} from '@/ee/shared/mutations/automation/apiCollectionEndpoints.mutations';
import {ApiCollectionKeys} from '@/ee/shared/mutations/automation/apiCollections.queries';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import useReadOnlyWorkflow from '@/shared/components/read-only-workflow-editor/hooks/useReadOnlyWorkflow';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {useQueryClient} from '@tanstack/react-query';
import {
    CloudDownloadIcon,
    EditIcon,
    EllipsisVerticalIcon,
    FolderSyncIcon,
    SendToBackIcon,
    Trash2Icon,
    UploadIcon,
} from 'lucide-react';
import {useMemo, useState} from 'react';

const ApiCollectionEndpointListItem = ({
    apiCollectionEndpoint,
    collectionVersion,
    contextPath,
    projectDeploymentId,
    projectDeploymentWorkflow,
    projectId,
    projectVersion,
    workflows,
}: {
    apiCollectionEndpoint: ApiCollectionEndpoint;
    collectionVersion: number;
    contextPath: string;
    projectId: number;
    projectDeploymentId: number;
    projectDeploymentWorkflow: ProjectDeploymentWorkflow;
    projectVersion: number;
    workflows: Workflow[];
}) => {
    const [showEditApiCollectionEndpointDialog, setShowEditApiCollectionEndpointDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {openReadOnlyWorkflowSheet} = useReadOnlyWorkflow();

    const workflow = workflows.filter((workflow) => workflow.workflowUuid === apiCollectionEndpoint.workflowUuid)[0];

    const queryClient = useQueryClient();

    const deleteApiCollectionEndpoint = useDeleteApiCollectionEndpointMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const enableProjectDeploymentWorkflowMutation = useEnableProjectDeploymentWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const handleApiCollectionEndpointEnable = () => {
        enableProjectDeploymentWorkflowMutation.mutate(
            {
                enable: !apiCollectionEndpoint.enabled,
                id: projectDeploymentId,
                workflowId: workflow.id!,
            },
            {
                onSuccess: () => {
                    apiCollectionEndpoint = {
                        ...apiCollectionEndpoint,
                        enabled: !apiCollectionEndpoint?.enabled,
                    };
                },
            }
        );
    };

    const handleWorkflowClick = () => {
        if (workflow) {
            openReadOnlyWorkflowSheet(workflow);
        }
    };

    const method = apiCollectionEndpoint.httpMethod;

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
                    icon: undefined,
                    textColor: '',
                };
        }
    }, [method]);

    const {icon, textColor} = httpMethodStyles;

    return (
        <>
            <div className="flex flex-1 items-center" onClick={handleWorkflowClick}>
                <Badge
                    className={`mr-4 w-20 ${textColor}`}
                    icon={icon}
                    label={method}
                    styleType="outline-outline"
                    weight="semibold"
                />

                <div className="flex flex-1 cursor-pointer items-center">
                    <div className="w-4/12 text-sm">{apiCollectionEndpoint.name}</div>

                    <div className="w-4/12 text-sm font-semibold">
                        {`/v${collectionVersion}/${contextPath}/${apiCollectionEndpoint.path}`}
                    </div>
                </div>
            </div>

            <div className="flex items-center justify-end gap-x-6">
                <Switch checked={apiCollectionEndpoint.enabled} onCheckedChange={handleApiCollectionEndpointEnable} />

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button icon={<EllipsisVerticalIcon />} size="icon" variant="ghost" />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end" className="p-0">
                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => setShowEditApiCollectionEndpointDialog(true)}
                        >
                            <EditIcon /> Edit Endpoint
                        </DropdownMenuItem>

                        <DropdownMenuItem
                            className="dropdown-menu-item"
                            onClick={() => setShowEditWorkflowDialog(true)}
                        >
                            <EditIcon /> Edit Workflow
                        </DropdownMenuItem>

                        <DropdownMenuSeparator className="m-0" />

                        <DropdownMenuItem
                            className="dropdown-menu-item-destructive"
                            onClick={() => setShowDeleteDialog(true)}
                        >
                            <Trash2Icon /> Delete
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showDeleteDialog && (
                <DeleteAlertDialog
                    onCancel={() => setShowDeleteDialog(false)}
                    onDelete={() => deleteApiCollectionEndpoint.mutate(apiCollectionEndpoint.id!)}
                    open={showDeleteDialog}
                />
            )}

            {showEditApiCollectionEndpointDialog && (
                <ApiCollectionEndpointDialog
                    apiCollectionId={apiCollectionEndpoint.apiCollectionId!}
                    apiEndpoint={apiCollectionEndpoint}
                    collectionVersion={collectionVersion}
                    contextPath={contextPath}
                    onClose={() => setShowEditApiCollectionEndpointDialog(false)}
                    projectId={projectId}
                    projectVersion={projectVersion}
                />
            )}

            {showEditWorkflowDialog && projectDeploymentWorkflow && (
                <ProjectDeploymentEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectDeploymentWorkflow={projectDeploymentWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default ApiCollectionEndpointListItem;
