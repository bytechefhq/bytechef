import {Button} from '@/components/ui/button';
import {DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ProjectInstanceWorkflowModel, WorkflowModel} from '@/middleware/automation/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/mutations/automation/projectInstanceWorkflows.mutations';
import ProjectInstanceEditWorkflowDialog from '@/pages/automation/project-instances/components/ProjectInstanceEditWorkflowDialog';
import {ProjectInstanceKeys} from '@/queries/automation/projectInstances.queries';
import {DotsVerticalIcon} from '@radix-ui/react-icons';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectInstanceWorkflowListItem = ({
    filteredDefinitionNames,
    projectId,
    projectInstanceEnabled,
    projectInstanceId,
    projectInstanceWorkflow,
    workflow,
    workflowComponentDefinitions,
    workflowTaskDispatcherDefinitions,
}: {
    filteredDefinitionNames?: string[];
    projectId: number;
    projectInstanceEnabled: boolean;
    projectInstanceId: number;
    projectInstanceWorkflow: ProjectInstanceWorkflowModel;
    workflow: WorkflowModel;
    workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
    workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    };
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const queryClient = useQueryClient();

    const enableProjectInstanceWorkflowMutation = useEnableProjectInstanceWorkflowMutation({
        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ProjectInstanceKeys.projectInstances,
            });
        },
    });

    return (
        <>
            <Link
                className="flex flex-1 items-center"
                to={`/automation/projects/${projectId}/workflows/${workflow.id}`}
            >
                <div className="w-6/12 text-sm font-semibold">{workflow.label}</div>

                <div className="ml-6 flex">
                    {filteredDefinitionNames?.map((name) => {
                        const componentDefinition = workflowComponentDefinitions[name];
                        const taskDispatcherDefinition = workflowTaskDispatcherDefinitions[name];

                        return (
                            <div className="mr-0.5 flex items-center justify-center rounded-full border p-1" key={name}>
                                <Tooltip>
                                    <TooltipTrigger>
                                        <InlineSVG
                                            className="h-5 w-5 flex-none"
                                            key={name}
                                            src={
                                                componentDefinition?.icon
                                                    ? componentDefinition?.icon
                                                    : taskDispatcherDefinition?.icon ?? ''
                                            }
                                        />
                                    </TooltipTrigger>

                                    <TooltipContent side="right">{componentDefinition?.title}</TooltipContent>
                                </Tooltip>
                            </div>
                        );
                    })}
                </div>
            </Link>

            <div className="flex items-center justify-end gap-x-6">
                {projectInstanceWorkflow?.lastExecutionDate ? (
                    <Tooltip>
                        <TooltipTrigger className="flex items-center text-sm text-gray-500">
                            <CalendarIcon aria-hidden="true" className="mr-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" />

                            <span>
                                {`Executed at ${projectInstanceWorkflow.lastExecutionDate?.toLocaleDateString()} ${projectInstanceWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                            </span>
                        </TooltipTrigger>

                        <TooltipContent>Last Execution Date</TooltipContent>
                    </Tooltip>
                ) : (
                    '-'
                )}

                {projectInstanceWorkflow && (
                    <Switch
                        checked={projectInstanceWorkflow.enabled}
                        disabled={projectInstanceEnabled}
                        onCheckedChange={(value) => {
                            enableProjectInstanceWorkflowMutation.mutate(
                                {
                                    enable: value,
                                    id: projectInstanceId,
                                    workflowId: workflow.id!,
                                },
                                {
                                    onSuccess: () => {
                                        projectInstanceWorkflow.enabled = !projectInstanceWorkflow?.enabled;
                                    },
                                }
                            );
                        }}
                    />
                )}

                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <Button size="icon" variant="ghost">
                            <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                        </Button>
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem onClick={() => setShowEditWorkflowDialog(true)}>Edit</DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </div>

            {showEditWorkflowDialog && projectInstanceWorkflow && (
                <ProjectInstanceEditWorkflowDialog
                    onClose={() => setShowEditWorkflowDialog(false)}
                    projectInstanceEnabled={projectInstanceEnabled}
                    projectInstanceWorkflow={projectInstanceWorkflow}
                    workflow={workflow}
                />
            )}
        </>
    );
};

export default ProjectInstanceWorkflowListItem;
