import {IntegrationInstanceWorkflowModel} from '@/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import IntegrationInstanceWorkflowListItem from '@/pages/embedded/integration-instances/components/IntegrationInstanceWorkflowListItem';
import {useGetTaskDispatcherDefinitionsQuery} from '@/queries/platform/taskDispatcherDefinitions.queries';
import {useGetIntegrationWorkflowsQuery} from 'queries/embedded/workflows.queries';
import {useGetComponentDefinitionsQuery} from 'queries/platform/componentDefinitions.queries';

const IntegrationInstanceWorkflowList = ({
    integrationId,
    integrationInstanceEnabled,
    integrationInstanceId,
    integrationInstanceWorkflows,
}: {
    integrationId: number;
    integrationInstanceId: number;
    integrationInstanceEnabled: boolean;
    integrationInstanceWorkflows?: Array<IntegrationInstanceWorkflowModel>;
}) => {
    const {data: workflows} = useGetIntegrationWorkflowsQuery(integrationId);

    const {data: componentDefinitions} = useGetComponentDefinitionsQuery();

    const {data: taskDispatcherDefinitions} = useGetTaskDispatcherDefinitionsQuery();

    const workflowComponentDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    const workflowTaskDispatcherDefinitions: {
        [key: string]: ComponentDefinitionBasicModel | undefined;
    } = {};

    return (
        <div className="border-b border-b-gray-100 py-3 pl-4">
            <h3 className="flex justify-start pl-2 text-sm font-semibold uppercase text-gray-400">Workflows</h3>

            <ul>
                {workflows?.map((workflow) => {
                    const componentNames = [
                        ...(workflow.workflowTriggerComponentNames ?? []),
                        ...(workflow.workflowTaskComponentNames ?? []),
                    ];

                    componentNames?.forEach((definitionName) => {
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

                    const integrationInstanceWorkflow = integrationInstanceWorkflows?.find(
                        (integrationInstanceWorkflow) => integrationInstanceWorkflow.workflowId === workflow?.id
                    );

                    return (
                        <li
                            className="flex items-center justify-between rounded-md p-2 hover:bg-gray-50"
                            key={workflow.id}
                        >
                            {integrationInstanceWorkflow && (
                                <IntegrationInstanceWorkflowListItem
                                    filteredComponentNames={filteredComponentNames}
                                    integrationId={integrationId}
                                    integrationInstanceEnabled={integrationInstanceEnabled}
                                    integrationInstanceId={integrationInstanceId}
                                    integrationInstanceWorkflow={integrationInstanceWorkflow}
                                    key={workflow.id}
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

export default IntegrationInstanceWorkflowList;
