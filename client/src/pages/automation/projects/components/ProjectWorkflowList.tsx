import {Skeleton} from '@/components/ui/skeleton';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/ProjectWorkflowListItem';
import {useGetProjectWorkflowsQuery} from '@/queries/automation/workflows.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/platform/taskDispatcherDefinitions.queries';
import {ProjectModel} from 'middleware/automation/configuration';
import {useGetComponentDefinitionsQuery} from 'queries/platform/componentDefinitions.queries';

const ProjectWorkflowList = ({project}: {project: ProjectModel}) => {
    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectWorkflowsQuery(project.id!);

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    return isComponentDefinitionsLoading || isTaskDispatcherDefinitionsLoading || isProjectWorkflowsLoading ? (
        <div className="space-y-3 py-2">
            <Skeleton className="h-5 w-40" />

            {[1, 2].map((value) => (
                <div className="flex items-center space-x-4" key={value}>
                    <Skeleton className="h-4 w-80" />

                    <div className="flex w-60 items-center space-x-1">
                        <Skeleton className="h-6 w-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />

                        <Skeleton className="size-7 rounded-full" />
                    </div>

                    <Skeleton className="h-4 flex-1" />
                </div>
            ))}
        </div>
    ) : (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <div className="mb-1 flex items-center justify-between">
                <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">Workflows</h3>
            </div>

            <ul>
                {workflows?.map((workflow) => {
                    const componentNames = [
                        ...(workflow.workflowTriggerComponentNames ?? []),
                        ...(workflow.workflowTaskComponentNames ?? []),
                    ];

                    componentNames?.map((definitionName) => {
                        if (!workflowComponentDefinitions[definitionName]) {
                            workflowComponentDefinitions[definitionName] = componentDefinitions?.find(
                                (componentDefinition) => componentDefinition.name === definitionName
                            );
                        }

                        if (!workflowTaskDispatcherDefinitions[definitionName]) {
                            workflowTaskDispatcherDefinitions[definitionName] = taskDispatcherDefinitions?.find(
                                (taskDispatcherDefinition) => taskDispatcherDefinition.name === definitionName
                            );
                        }
                    });

                    const filteredComponentNames = componentNames?.filter(
                        (item, index) => componentNames?.indexOf(item) === index
                    );

                    return (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            <ProjectWorkflowListItem
                                filteredComponentNames={filteredComponentNames}
                                key={workflow.id}
                                project={project}
                                workflow={workflow}
                                workflowComponentDefinitions={workflowComponentDefinitions}
                                workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                            />
                        </li>
                    );
                })}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
