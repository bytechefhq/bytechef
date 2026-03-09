import {Collapsible, CollapsibleContent, CollapsibleTrigger} from '@/components/ui/collapsible';
import {Skeleton} from '@/components/ui/skeleton';
import ProjectDeploymentWorkflowListItem from '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItem';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useGetProjectVersionWorkflowsQuery} from '@/shared/queries/automation/projectWorkflows.queries';
import {ChevronDownIcon} from 'lucide-react';

const ProjectDeploymentWorkflowList = ({
    componentDefinitions,
    environmentId,
    projectDeploymentEnabled,
    projectDeploymentId,
    projectDeploymentWorkflows,
    projectId,
    projectVersion,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    environmentId: number;
    projectDeploymentEnabled: boolean;
    projectDeploymentId: number;
    projectDeploymentWorkflows: Array<ProjectDeploymentWorkflow>;
    projectId: number;
    projectVersion: number;
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
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

    if (!componentDefinitions || !taskDispatcherDefinitions || isProjectWorkflowsLoading) {
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

    const sortedWorkflows =
        workflows?.sort((firstWorkflow, secondWorkflow) => firstWorkflow.label!.localeCompare(secondWorkflow.label!)) ??
        [];

    const enabledWorkflows = sortedWorkflows
        .map((workflow) => {
            const projectDeploymentWorkflow = projectDeploymentWorkflows.find(
                (deploymentWorkflow) => deploymentWorkflow.workflowId === workflow?.id
            );

            return projectDeploymentWorkflow?.enabled ? {projectDeploymentWorkflow, workflow} : null;
        })
        .filter((data): data is NonNullable<typeof data> => data != null);

    const disabledWorkflows = sortedWorkflows
        .map((workflow) => {
            const projectDeploymentWorkflow = projectDeploymentWorkflows.find(
                (deploymentWorkflow) => deploymentWorkflow.workflowId === workflow?.id
            );

            return projectDeploymentWorkflow?.enabled ? null : {projectDeploymentWorkflow, workflow};
        })
        .filter(
            (data): data is {projectDeploymentWorkflow: ProjectDeploymentWorkflow; workflow: Workflow} =>
                data != null && data.projectDeploymentWorkflow != null
        );

    const allRenderedWorkflows = [...enabledWorkflows, ...disabledWorkflows];

    allRenderedWorkflows.forEach(({workflow}) => {
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
    });

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="heading-tertiary flex justify-start pl-2 text-sm">Workflows</h3>

            {enabledWorkflows.length === 0 ? (
                <p className="py-4 pl-2 text-sm text-muted-foreground">
                    No enabled workflows. Enable a workflow in the project to run it in this deployment.
                </p>
            ) : (
                <ul className="divide-y divide-gray-100">
                    {enabledWorkflows.map(({projectDeploymentWorkflow, workflow}) => {
                        const componentNames = [
                            ...(workflow.workflowTriggerComponentNames ?? []),
                            ...(workflow.workflowTaskComponentNames ?? []),
                        ];

                        const filteredComponentNames = componentNames?.filter(
                            (item, index) => componentNames?.indexOf(item) === index
                        );

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
            )}

            {disabledWorkflows.length > 0 && (
                <Collapsible className="group">
                    <CollapsibleTrigger className="flex w-full items-center space-x-2 rounded-md p-2 hover:bg-surface-neutral-primary-hover [&[data-state=open]>svg]:rotate-180">
                        <h3 className="flex justify-start text-sm text-muted-foreground">Disabled Workflows</h3>

                        <ChevronDownIcon className="size-4 shrink-0 transition-transform duration-300" />
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                        <ul className="divide-y divide-gray-100">
                            {disabledWorkflows.map(({projectDeploymentWorkflow, workflow}) => {
                                const componentNames = [
                                    ...(workflow.workflowTriggerComponentNames ?? []),
                                    ...(workflow.workflowTaskComponentNames ?? []),
                                ];

                                const filteredComponentNames = componentNames?.filter(
                                    (item, index) => componentNames?.indexOf(item) === index
                                );

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
                    </CollapsibleContent>
                </Collapsible>
            )}
        </div>
    );
};

export default ProjectDeploymentWorkflowList;
