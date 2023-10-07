import DropdownMenu from '@/components/DropdownMenu/DropdownMenu';
import WorkflowDialog from '@/components/WorkflowDialog/WorkflowDialog';
import {Tooltip, TooltipContent, TooltipTrigger} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {CalendarIcon} from '@heroicons/react/24/outline';
import {ProjectModel, WorkflowModel} from 'middleware/helios/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import {useState} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(project.id!);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const {data: taskDispatcherDefinitions} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const [selectedWorkflow, setSelectedWorkflow] = useState<
        WorkflowModel | undefined
    >(undefined);

    const [showEditWorkflowDialog, setShowEditWorkflowDialog] = useState(false);

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <div className="mb-1 flex items-center justify-between">
                <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                    Workflows
                </h3>
            </div>

            <ul>
                {workflows?.map((workflow) => {
                    const definitionNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    definitionNames?.map((definitionName) => {
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

                    return (
                        <li
                            key={workflow.id}
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                        >
                            <div className="w-10/12">
                                <Link
                                    className="flex items-center"
                                    to={`/automation/projects/${project.id}/workflow/${workflow.id}`}
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

                                    <div className="flex flex-1 justify-end">
                                        <Tooltip>
                                            <TooltipTrigger>
                                                <div className="flex text-sm text-gray-500">
                                                    <CalendarIcon
                                                        className="mr-1 h-5 w-5 shrink-0 text-gray-400"
                                                        aria-hidden="true"
                                                    />

                                                    <span>{`${workflow.lastModifiedDate?.toLocaleDateString()} ${workflow.lastModifiedDate?.toLocaleTimeString()}`}</span>
                                                </div>
                                            </TooltipTrigger>

                                            <TooltipContent>
                                                Last Modified Date
                                            </TooltipContent>
                                        </Tooltip>
                                    </div>
                                </Link>
                            </div>

                            <div className="flex w-2/12 justify-end">
                                <DropdownMenu
                                    id={project.id}
                                    menuItems={[
                                        {
                                            label: 'Edit',
                                            onClick: () => {
                                                setSelectedWorkflow(workflow);

                                                setShowEditWorkflowDialog(true);
                                            },
                                        },
                                        {
                                            label: 'Duplicate',
                                            onClick: () => console.log('TODO'),
                                        },
                                        {
                                            separator: true,
                                        },
                                        {
                                            danger: true,
                                            label: 'Delete',
                                            onClick: () => {
                                                console.log('TODO');
                                            },
                                        },
                                    ]}
                                />
                            </div>

                            {showEditWorkflowDialog && (
                                <WorkflowDialog
                                    showTrigger={false}
                                    visible
                                    workflow={selectedWorkflow!}
                                    onClose={() =>
                                        setShowEditWorkflowDialog(false)
                                    }
                                />
                            )}
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
