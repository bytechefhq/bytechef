import EmptyList from '@/components/EmptyList';
import {Button} from '@/components/ui/button';
import {Skeleton} from '@/components/ui/skeleton';
import IntegrationWorkflowListItem from '@/pages/embedded/integrations/components/IntegrationWorkflowListItem';
import WorkflowDialog from '@/pages/platform/workflow/components/WorkflowDialog';
import {useAnalytics} from '@/shared/hooks/useAnalytics';
import {Integration} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useCreateIntegrationWorkflowMutation} from '@/shared/mutations/embedded/workflows.mutations';
import {useGetWorkflowQuery} from '@/shared/queries/automation/workflows.queries';
import {useGetIntegrationWorkflowsQuery} from '@/shared/queries/embedded/integrationWorkflows.queries';
import {useGetComponentDefinitionsQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetTaskDispatcherDefinitionsQuery} from '@/shared/queries/platform/taskDispatcherDefinitions.queries';
import {WorkflowIcon} from 'lucide-react';
import {useNavigate} from 'react-router-dom';

const IntegrationWorkflowList = ({integration}: {integration: Integration}) => {
    const {data: componentDefinitions, isLoading: isComponentDefinitionsLoading} = useGetComponentDefinitionsQuery({
        actionDefinitions: true,
        triggerDefinitions: true,
    });

    const {captureIntegrationWorkflowCreated} = useAnalytics();

    const navigate = useNavigate();

    const {data: taskDispatcherDefinitions, isLoading: isTaskDispatcherDefinitionsLoading} =
        useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const {data: workflows, isLoading: isIntegrationWorkflowsLoading} = useGetIntegrationWorkflowsQuery(
        integration.id!
    );

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasic | undefined;
    } = {};

    const createIntegrationWorkflowMutation = useCreateIntegrationWorkflowMutation({
        onSuccess: (workflow) => {
            captureIntegrationWorkflowCreated();

            navigate(
                `/embedded/integrations/${integration.id}/integration-workflows/${workflow?.integrationWorkflowId}`
            );
        },
    });

    return isComponentDefinitionsLoading || isTaskDispatcherDefinitionsLoading || isIntegrationWorkflowsLoading ? (
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
            {workflows && workflows.length > 0 ? (
                <>
                    <div className="mb-1 flex items-center justify-between">
                        <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-500">
                            Workflows
                        </h3>
                    </div>

                    <ul className="divide-y divide-gray-100">
                        {workflows
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
                                        workflowTaskDispatcherDefinitions[definitionName] =
                                            taskDispatcherDefinitions?.find(
                                                (taskDispatcherDefinition) =>
                                                    taskDispatcherDefinition.name === definitionName
                                            );
                                    }
                                });

                                const filteredComponentNames = componentNames?.filter(
                                    (item, index) => componentNames?.indexOf(item) === index
                                );

                                return (
                                    <IntegrationWorkflowListItem
                                        filteredComponentNames={filteredComponentNames}
                                        integration={integration}
                                        key={workflow.id}
                                        workflow={workflow}
                                        workflowComponentDefinitions={workflowComponentDefinitions}
                                        workflowTaskDispatcherDefinitions={workflowTaskDispatcherDefinitions}
                                    />
                                );
                            })}
                    </ul>
                </>
            ) : (
                <div className="flex justify-center py-8">
                    <EmptyList
                        button={
                            <WorkflowDialog
                                createWorkflowMutation={createIntegrationWorkflowMutation}
                                parentId={integration.id}
                                triggerNode={<Button>Create Workflow</Button>}
                                useGetWorkflowQuery={useGetWorkflowQuery}
                            />
                        }
                        icon={<WorkflowIcon className="size-24 text-gray-300" />}
                        message="Get started by creating a new workflow."
                        title="No Workflows"
                    />
                </div>
            )}
        </div>
    );
};

export default IntegrationWorkflowList;
