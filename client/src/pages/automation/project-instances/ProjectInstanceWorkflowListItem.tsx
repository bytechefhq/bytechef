import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ProjectInstanceWorkflowModel,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/mutations/projectInstanceWorkflows.mutations';
import ProjectInstanceEditWorkflowDialog from '@/pages/automation/project-instances/ProjectInstanceEditWorkflowDialog';
import {ProjectInstanceKeys} from '@/queries/projectInstances.queries';
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

    const enableProjectInstanceWorkflowMutation =
        useEnableProjectInstanceWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries({
                    queryKey: ProjectInstanceKeys.projectInstances,
                });
            },
        });

    return (
        <>
            <div className="w-10/12">
                <Link
                    className="flex items-center"
                    to={`/automation/projects/${projectId}/workflows/${workflow.id}`}
                >
                    <div className="w-6/12 text-sm font-semibold">
                        {workflow.label}
                    </div>

                    <div className="ml-6 flex">
                        {filteredDefinitionNames?.map((name) => {
                            const componentDefinition =
                                workflowComponentDefinitions[name];
                            const taskDispatcherDefinition =
                                workflowTaskDispatcherDefinitions[name];

                            return (
                                <div
                                    className="mr-0.5 flex items-center justify-center rounded-full border p-1"
                                    key={name}
                                >
                                    <Tooltip>
                                        <TooltipTrigger>
                                            <InlineSVG
                                                className="h-5 w-5 flex-none"
                                                key={name}
                                                src={
                                                    componentDefinition?.icon
                                                        ? componentDefinition?.icon
                                                        : taskDispatcherDefinition?.icon ??
                                                          ''
                                                }
                                            />
                                        </TooltipTrigger>

                                        <TooltipContent side="right">
                                            {componentDefinition?.title}
                                        </TooltipContent>
                                    </Tooltip>
                                </div>
                            );
                        })}
                    </div>

                    <div className="flex flex-1 justify-end text-sm">
                        {projectInstanceWorkflow?.lastExecutionDate ? (
                            <Tooltip>
                                <TooltipTrigger>
                                    <div className="flex items-center text-sm text-gray-500">
                                        <CalendarIcon
                                            aria-hidden="true"
                                            className="mr-0.5 h-4 w-4 shrink-0 text-gray-400"
                                        />

                                        <span>
                                            {`${projectInstanceWorkflow.lastExecutionDate?.toLocaleDateString()} ${projectInstanceWorkflow.lastExecutionDate?.toLocaleTimeString()}`}
                                        </span>
                                    </div>
                                </TooltipTrigger>

                                <TooltipContent>
                                    Last Execution Date
                                </TooltipContent>
                            </Tooltip>
                        ) : (
                            '-'
                        )}
                    </div>
                </Link>
            </div>

            <div className="flex w-1/12 items-center justify-end">
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
                                        projectInstanceWorkflow.enabled =
                                            !projectInstanceWorkflow?.enabled;
                                    },
                                }
                            );
                        }}
                    />
                )}
            </div>

            <div className="flex w-1/12 justify-end">
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <DotsVerticalIcon className="h-4 w-4 hover:cursor-pointer" />
                    </DropdownMenuTrigger>

                    <DropdownMenuContent align="end">
                        <DropdownMenuItem
                            onClick={() => setShowEditWorkflowDialog(true)}
                        >
                            Edit
                        </DropdownMenuItem>
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
