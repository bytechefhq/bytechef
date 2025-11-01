import '@/shared/styles/dropdownMenu.css';
import Button from '@/components/Button/Button';
import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Badge} from '@/components/ui/badge';
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
import {EditIcon, EllipsisVerticalIcon, Trash2Icon} from 'lucide-react';
import {useState} from 'react';
import {twMerge} from 'tailwind-merge';

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

    return (
        <>
            <div className="flex flex-1 items-center" onClick={handleWorkflowClick}>
                <Badge
                    className={twMerge(
                        'mr-4 w-20 border-transparent',
                        apiCollectionEndpoint.httpMethod === 'DELETE' && 'bg-red-400',
                        apiCollectionEndpoint.httpMethod === 'GET' && 'bg-green-400',
                        apiCollectionEndpoint.httpMethod === 'POST' && 'bg-yellow-400',
                        apiCollectionEndpoint.httpMethod === 'PATCH' && 'bg-amber-400',
                        apiCollectionEndpoint.httpMethod === 'PUT' && 'bg-orange-400'
                    )}
                    variant="secondary"
                >
                    {apiCollectionEndpoint.httpMethod}
                </Badge>

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
