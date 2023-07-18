import {Switch} from '@/components/ui/switch';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip';
import {ComponentDefinitionBasicModel} from '@/middleware/hermes/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/componentDefinitions.queries';
import {useGetProjectWorkflowsQuery} from 'queries/projects.queries';
import InlineSVG from 'react-inlinesvg';
import {Link} from 'react-router-dom';

const ProjectInstanceWorkflowList = ({
    projectId,
    projectInstanceEnabled,
}: {
    projectId: number;
    projectInstanceEnabled?: boolean;
}) => {
    const {data: workflows} = useGetProjectWorkflowsQuery(projectId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    return (
        <div className="border-b border-b-gray-100 py-2">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                Workflows
            </h3>

            <ul>
                {workflows?.map((workflow) => {
                    const componentNames = workflow.tasks?.map(
                        (task) => task.type.split('/')[0]
                    );

                    componentNames?.forEach((componentName) => {
                        if (!workflowComponentDefinitions[componentName]) {
                            workflowComponentDefinitions[componentName] =
                                componentDefinitions?.find(
                                    (componentDefinition) =>
                                        componentDefinition.name ===
                                        componentName
                                );
                        }
                    });

                    const filteredComponentNames = componentNames?.filter(
                        (item, index) => componentNames?.indexOf(item) === index
                    );

                    return (
                        <li
                            key={workflow.id}
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                        >
                            <div className="w-9/12">
                                <Link
                                    className="flex items-center"
                                    to={`/automation/projects/${projectId}/workflow/${workflow.id}`}
                                >
                                    <div className="w-6/12 text-sm font-semibold">
                                        {workflow.label}
                                    </div>

                                    <div className="ml-6 flex">
                                        {filteredComponentNames?.map(
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
                                </Link>
                            </div>

                            <div className="flex w-2/12 items-center justify-center">
                                <Switch disabled={projectInstanceEnabled} />
                            </div>

                            <div className="flex w-1/12"></div>
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectInstanceWorkflowList;
