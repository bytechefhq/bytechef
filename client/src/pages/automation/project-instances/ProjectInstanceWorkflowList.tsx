import {Switch} from '@/components/ui/switch';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/core/workflow/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectInstanceWorkflowList = ({projectId}: {projectId: number}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    return (
        <div className="border-b border-b-gray-100 py-2">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                Workflows
            </h3>

            <ul className="space-y-2">
                {workflows?.map((workflow) => {
                    const workflowComponentDefinitions: {
                        [key: string]:
                            | ComponentDefinitionBasicModel
                            | undefined;
                    } = {};

                    const componentNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    componentNames?.map((componentName) => {
                        if (!workflowComponentDefinitions[componentName]) {
                            workflowComponentDefinitions[componentName] =
                                componentDefinitions?.find(
                                    (componentDefinition) =>
                                        componentDefinition.name ===
                                        componentName
                                );
                        }
                    });

                    const componentNamesFiltered = componentNames?.filter(
                        (item, index) => componentNames?.indexOf(item) === index
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
                                        {componentNamesFiltered?.map(
                                            (componentName) => {
                                                const componentDefinition =
                                                    workflowComponentDefinitions[
                                                        componentName
                                                    ];

                                                return (
                                                    <div
                                                        key={componentName}
                                                        className="mr-0.5 flex items-center justify-center rounded-full border p-1"
                                                    >
                                                        <TooltipProvider>
                                                            <Tooltip>
                                                                <TooltipTrigger>
                                                                    <InlineSVG
                                                                        className="h-5 w-5 flex-none"
                                                                        key={
                                                                            componentName
                                                                        }
                                                                        src={
                                                                            componentDefinition?.icon ??
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
                                            }
                                        )}
                                    </div>

                                    <div className="flex flex-1 justify-end text-sm">
                                        <Switch />
                                    </div>
                                </Link>
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
