import {Skeleton} from '@/components/ui/skeleton';
import {ProjectInstanceWorkflowModel} from '@/middleware/automation/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import ProjectInstanceWorkflowListItem from '@/pages/automation/project-instances/components/ProjectInstanceWorkflowListItem';
import {useGetProjectWorkflowsQuery} from '@/queries/automation/workflows.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/platform/taskDispatcherDefinitions.queries';
import {useGetComponentDefinitionsQuery} from 'queries/platform/componentDefinitions.queries';

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
    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectWorkflowsQuery(projectId);

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

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
        <div className="border-b border-b-gray-100 pl-4">
            <h3 className="flex justify-start px-2 text-sm font-semibold uppercase text-gray-400">Workflows</h3>

            <ul>
                {workflows?.map((workflow) => {
                    const definitionNames = workflow.tasks?.map((task) => task.type.split('/')[0]);

                    definitionNames?.forEach((definitionName) => {
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

                    const filteredDefinitionNames = definitionNames?.filter(
                        (item, index) => definitionNames?.indexOf(item) === index
                    );

                    const projectInstanceWorkflow = projectInstanceWorkflows?.find(
                        (projectInstanceWorkflow) => projectInstanceWorkflow.workflowId === workflow?.id
                    );

                    return (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            {projectInstanceWorkflow && (
                                <ProjectInstanceWorkflowListItem
                                    filteredDefinitionNames={filteredDefinitionNames}
                                    key={workflow.id}
                                    projectId={projectId}
                                    projectInstanceEnabled={projectInstanceEnabled}
                                    projectInstanceId={projectInstanceId}
                                    projectInstanceWorkflow={projectInstanceWorkflow}
                                    workflow={workflow}
                                    workflowComponentDefinitions={workflowComponentDefinitions}
                                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
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
