import DropdownMenu from '@/components/DropdownMenu/DropdownMenu';
import {Switch} from '@/components/ui/switch';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {
    ProjectInstanceWorkflowModel,
    WorkflowModel,
} from '@/middleware/helios/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {useEnableProjectInstanceWorkflowMutation} from '@/mutations/projects.mutations';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {useQueryClient} from '@tanstack/react-query';
import {CalendarIcon} from 'lucide-react';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {
    ProjectKeys,
    useGetProjectWorkflowsQuery,
} from 'queries/projects.queries';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

import ProjectInstanceEditWorkflowDialog from './ProjectInstanceEditWorkflowDialog';

const ProjectInstanceWorkflowList = ({
    projectId,
    projectInstanceEnabled,
    projectInstanceId,
    projectInstanceWorkflows,
}: {
    projectId: number;
    projectInstanceId: number;
    projectInstanceEnabled: boolean;
    projectInstanceWorkflows?: Array<ProjectInstanceWorkflowModel>;
}) => {
    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const {data: taskDispatcherDefinitions} =
        useGetTaskDispatcherDefinitionsQuery();

    const [selectedWorkflow, setSelectedWorkflow] = useState<
        WorkflowModel | undefined
    >(undefined);

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const queryClient = useQueryClient();

    const enableProjectInstanceWorkflowMutation =
        useEnableProjectInstanceWorkflowMutation({
            onSuccess: () => {
                queryClient.invalidateQueries(ProjectKeys.projectInstances);
            },
        });

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">
                Workflows
            </h3>

            <ul>
                {workflows?.map((workflow) => {
                    const definitionNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    definitionNames?.forEach((definitionName) => {
                        if (!workflowComponentDefinitions[definitionName]) {
                            workflowComponentDefinitions[definitionName] =
                                componentDefinitions?.find(
                                    (componentDefinition) =>
                                        componentDefinition.name ===
                                        definitionName
                                );
                        }

                        if (
                            !workflowTaskDispatcherDefinitions[definitionName]
                        ) {
                            workflowTaskDispatcherDefinitions[definitionName] =
                                taskDispatcherDefinitions?.find(
                                    (taskDispatcherDefinition) =>
                                        taskDispatcherDefinition.name ===
                                        definitionName
                                );
                        }
                    });

                    const filteredDefinitionNames = definitionNames?.filter(
                        (item, index) =>
                            definitionNames?.indexOf(item) === index
                    );

                    const projectInstanceWorkflow =
                        projectInstanceWorkflows?.find(
                            (projectInstanceWorkflow) =>
                                projectInstanceWorkflow.workflowId ===
                                workflow?.id
                        );

                    const selectedProjectInstanceWorkflow =
                        projectInstanceWorkflows?.find(
                            (projectInstanceWorkflow) =>
                                projectInstanceWorkflow.workflowId ===
                                selectedWorkflow?.id
                        );

                    return (
                        <li
                            key={workflow.id}
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                        >
                            <div className="w-10/12">
                                <Link
                                    className="flex items-center"
                                    to={`/automation/projects/${projectId}/workflow/${workflow.id}`}
                                >
                                    <div className="w-6/12 text-sm font-semibold">
                                        {workflow.label}
                                    </div>

                                    <div className="ml-6 flex">
                                        {filteredDefinitionNames?.map(
                                            (name) => {
                                                const componentDefinition =
                                                    workflowComponentDefinitions[
                                                        name
                                                    ];
                                                const taskDispatcherDefinition =
                                                    workflowTaskDispatcherDefinitions[
                                                        name
                                                    ];

                                                return (
                                                    <div
                                                        key={name}
                                                        className="mr-0.5 flex items-center justify-center rounded-full border p-1"
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
                                                                {
                                                                    componentDefinition?.title
                                                                }
                                                            </TooltipContent>
                                                        </Tooltip>
                                                    </div>
                                                );
                                            }
                                        )}
                                    </div>

                                    <div className="flex flex-1 justify-end text-sm">
                                        {projectInstanceWorkflow?.lastExecutionDate ? (
                                            <Tooltip>
                                                <TooltipTrigger>
                                                    <div className="flex items-center text-sm text-gray-500">
                                                        <CalendarIcon
                                                            className="mr-1 h-4 w-4 shrink-0 text-gray-400"
                                                            aria-hidden="true"
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
                                        disabled={projectInstanceEnabled}
                                        checked={
                                            projectInstanceWorkflow.enabled
                                        }
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
                                <DropdownMenu
                                    id={projectInstanceId}
                                    menuItems={[
                                        {
                                            label: 'Edit',
                                            onClick: () => {
                                                setSelectedWorkflow(workflow);

                                                setShowEditWorkflowDialog(true);
                                            },
                                        },
                                    ]}
                                />
                            </div>

                            {showEditWorkflowDialog &&
                                projectInstanceWorkflow &&
                                selectedProjectInstanceWorkflow &&
                                selectedWorkflow && (
                                    <ProjectInstanceEditWorkflowDialog
                                        onClose={() =>
                                            setShowEditWorkflowDialog(false)
                                        }
                                        projectInstanceEnabled={
                                            projectInstanceEnabled
                                        }
                                        projectInstanceWorkflow={
                                            selectedProjectInstanceWorkflow
                                        }
                                        visible
                                        workflow={selectedWorkflow}
                                    />
                                )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
