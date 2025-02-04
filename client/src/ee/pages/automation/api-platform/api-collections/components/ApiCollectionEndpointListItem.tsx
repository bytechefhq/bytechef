import DeleteAlertDialog from '@/components/DeleteAlertDialog';
import {Badge} from '@/components/ui/badge';
import {Button} from '@/components/ui/button';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {useDeleteApiCollectionEndpointMutation} from '@/ee/mutations/apiCollectionEndpoints.mutations';
import ApiCollectionEndpointDialog from '@/ee/pages/automation/api-platform/api-collections/components/ApiCollectionEndpointDialog';
import {ApiCollectionKeys} from '@/ee/queries/apiCollections.queries';
import {ApiCollectionEndpoint} from '@/ee/shared/middleware/automation/api-platform';
import ProjectDeploymentEditWorkflowDialog from '@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog';
import useReadOnlyWorkflowEditorSheetStore from '@/shared/components/read-only-workflow-editor/stores/useReadOnlyWorkflowEditorSheetStore';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {useEnableProjectDeploymentWorkflowMutation} from '@/shared/mutations/automation/projectDeploymentWorkflows.mutations';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
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

    const {setReadOnlyWorkflowEditorSheetOpen, setWorkflowId} = useReadOnlyWorkflowEditorSheetStore();

    const workflow = workflows.filter(
        (workflow) => workflow.workflowReferenceCode === apiCollectionEndpoint.workflowReferenceCode
    )[0];

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
        setWorkflowId(workflow.id!);
        setReadOnlyWorkflowEditorSheetOpen(true);
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
                        <Button size="icon" variant="ghost">
                            <DotsVerticalIcon className="size-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setShowEditApiCollectionEndpointDialog(true)}>
                            Edit Endpoint
                        </DropdownMenuItem>

                        <DropdownMenuItem onClick={() => setShowEditWorkflowDialog(true)}>
                            Edit Workflow
                        </DropdownMenuItem>

                        <DropdownMenuSeparator />

                        <DropdownMenuItem className="text-red-600" onClick={() => setShowDeleteDialog(true)}>
                            Delete
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
