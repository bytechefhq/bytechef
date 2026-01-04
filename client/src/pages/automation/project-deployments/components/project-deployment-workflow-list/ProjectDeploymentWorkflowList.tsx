import {Skeleton} from '@/components/ui/skeleton';
import ProjectDeploymentWorkflowListItem from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItem';
import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/automation/componentDefinitions.queries';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';

const ProjectDeploymentWorkflowList = ({
    environmentId,
    projectDeploymentEnabled,
    projectDeploymentId,
    projectDeploymentWorkflows,
    projectId,
    projectVersion,
}: {
    environmentId: number;
    projectId: number;
    projectDeploymentId: number;
    projectDeploymentEnabled: boolean;
    projectDeploymentWorkflows: Array<ProjectDeploymentWorkflow>;
    projectVersion: number;
}) => {
    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const {data: workflows, isLoading: isProjectWorkflowsLoading} = useGetProjectVersionWorkflowsQuery(
        projectId,
        projectVersion
    );

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    if (isComponentDefinitionsLoading || isTaskDispatcherDefinitionsLoading || isProjectWorkflowsLoading) {
        return (
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
        );
    }

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Workflows</h3>

            <ul className="divide-y divide-gray-100">
                {workflows &&
                    workflows
                        .sort((a, b) => a.label!.localeCompare(b.label!))
                        .map((workflow) => {
                            const componentNames = [
                                ...(workflow.workflowTriggerComponentNames ?? []),
                                ...(workflow.workflowTaskComponentNames ?? []),
                            ];

                            componentNames?.forEach((componentName) => {
                                if (!workflowComponentDefinitions[componentName]) {
                                    workflowComponentDefinitions[componentName] = componentDefinitions?.find(
                                        (componentDefinition) => componentDefinition.name === componentName
                                    );
                                }

                                if (!workflowTaskDispatcherDefinitions[componentName]) {
                                    workflowTaskDispatcherDefinitions[componentName] = taskDispatcherDefinitions?.find(
                                        (taskDispatcherDefinition) => taskDispatcherDefinition.name === componentName
                                    );
                                }
                            });

                            const filteredComponentNames = componentNames?.filter(
                                (item, index) => componentNames?.indexOf(item) === index
                            );

                            const projectDeploymentWorkflow = projectDeploymentWorkflows.find(
                                (projectDeploymentWorkflow) => projectDeploymentWorkflow.workflowId === workflow?.id
                            );

                            if (!projectDeploymentWorkflow) {
                                return <></>;
                            }

                            return (
                                <ProjectDeploymentWorkflowListItem
                                    environmentId={environmentId}
                                    filteredComponentNames={filteredComponentNames}
                                    key={workflow.id}
                                    projectDeploymentEnabled={projectDeploymentEnabled}
                                    projectDeploymentId={projectDeploymentId}
                                    projectDeploymentWorkflow={projectDeploymentWorkflow}
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

export default ProjectDeploymentWorkflowList;
