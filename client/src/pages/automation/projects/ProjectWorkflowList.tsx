import DropdownMenu from '@/components/DropdownMenu/DropdownMenu';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/taskDispatcherDefinitions.queries';
import {ProjectModel} from 'middleware/helios/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
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

    return (
        <div className="border-b border-b-gray-100 py-2">
            <div className="mb-1 flex items-center justify-between">
                <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                    Workflows
                </h3>
            </div>

            <ul>
                {workflows?.map((workflow) => {
                    const names = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    names?.map((name) => {
                        if (!workflowComponentDefinitions[name]) {
                            workflowComponentDefinitions[name] =
                                componentDefinitions?.find(
                                    (componentDefinition) =>
                                        componentDefinition.name === name
                                );
                        }

                        if (!workflowTaskDispatcherDefinitions[name]) {
                            workflowTaskDispatcherDefinitions[name] =
                                taskDispatcherDefinitions?.find(
                                    (taskDispatcherDefinition) =>
                                        taskDispatcherDefinition.name === name
                                );
                        }
                    });

                    const filteredNames = names?.filter(
                        (item, index) => names?.indexOf(item) === index
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
                                        {filteredNames?.map((name) => {
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
                                                    <TooltipProvider>
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
                                                    </TooltipProvider>
                                                </div>
                                            );
                                        })}
                                    </div>

                                    <div className="flex flex-1 justify-end text-sm">
                                        {workflow.lastModifiedDate?.toLocaleDateString()}
                                    </div>
                                </Link>
                            </div>

                            <DropdownMenu
                                id={project.id}
                                menuItems={[
                                    {
                                        label: 'Edit',
                                        onClick: () => {
                                            console.log('TODO');
                                        },
                                    },
                                    {
                                        label: 'Duplicate',
                                        onClick: () => {
                                            console.log('TODO');
                                        },
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
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
