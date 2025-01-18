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
import ProjectInstanceEditWorkflowDialog from '@/pages/automation/project-instances/components/ProjectInstanceEditWorkflowDialog';
import {ProjectInstanceWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/shared/mutations/automation/projectInstanceWorkflows.mutations';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {useState} from 'react';
import {Link} from 'react-router-dom';
import {twMerge} from 'tailwind-merge';

const ApiCollectionEndpointListItem = ({
    apiCollectionEndpoint,
    projectId,
    projectInstanceId,
    projectInstanceWorkflow,
    projectVersion,
    workflows,
}: {
    apiCollectionEndpoint: ApiCollectionEndpoint;
    projectId: number;
    projectInstanceId: number;
    projectInstanceWorkflow: ProjectInstanceWorkflow;
    projectVersion: number;
    workflows: Workflow[];
}) => {
    const [showEditApiCollectionEndpointDialog, setShowEditApiCollectionEndpointDialog] = useState(false);
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

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

    const enableProjectInstanceWorkflowMutation = useEnableProjectInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ApiCollectionKeys.apiCollections,
            });
        },
    });

    const handleApiCollectionEndpointEnable = () => {
        enableProjectInstanceWorkflowMutation.mutate(
            {
                enable: !apiCollectionEndpoint.enabled,
                id: projectInstanceId,
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

    return (
        <>
            <div className="flex flex-1 items-center">
                <Badge
                    className={twMerge(
                        'mr-4 w-20 border-transparent',
                        apiCollectionEndpoint.httpMethod === 'DELETE' && 'bg-red-400',
                        apiCollectionEndpoint.httpMethod === 'GET' && 'bg-blue-400',
                        apiCollectionEndpoint.httpMethod === 'POST' && 'bg-green-400',
                        apiCollectionEndpoint.httpMethod === 'PUT' && 'bg-amber-400'
                    )}
                    variant="secondary"
                >
                    {apiCollectionEndpoint.httpMethod}
                </Badge>

                <div className="flex flex-1 items-center">
                    <Link
                        className="w-4/12 text-sm"
                        to={`/automation/projects/${1}/project-workflows/${workflow.projectWorkflowId}`}
                    >
                        {apiCollectionEndpoint.name}
                    </Link>

                    <div className="w-4/12 text-sm font-semibold">{apiCollectionEndpoint.path}</div>
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
                    onClose={() => setShowEditApiCollectionEndpointDialog(false)}
                    projectId={projectId}
                    projectVersion={projectVersion}
                />
            )}

            {showEditWorkflowDialog && projectInstanceWorkflow && (
                <ProjectInstanceEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectInstanceWorkflow={projectInstanceWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default ApiCollectionEndpointListItem;
