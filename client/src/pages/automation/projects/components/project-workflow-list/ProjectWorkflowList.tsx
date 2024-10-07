import {Skeleton} from '@/components/ui/skeleton';
import ProjectWorkflowListItem from '@/pages/automation/projects/components/project-workflow-list/ProjectWorkflowListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetProjectWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';

const ProjectWorkflowList = ({project}: {project: Project}) => {
    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectWorkflowsQuery(
        project.id!,
        !!project.id
    );

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
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
                <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Workflows</h3>
            </div>

            <ul className="divide-y divide-gray-100">
                {workflows &&
                    workflows
                        .sort((a, b) => a.label!.localeCompare(b.label!))
                        .map((workflow) => {
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
                                <ProjectWorkflowListItem
                                    filteredComponentNames={filteredComponentNames}
                                    key={workflow.id}
                                    project={project}
                                    workflow={workflow}
                                    workflowComponentDefinitions={workflowComponentDefinitions}
                                    workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                />
                            );
                        })}
            </ul>
        </div>
    );
};

export default ProjectWorkflowList;
