import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {useMcpIntegrationInstanceConfigurationsQuery} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

import IntegrationInstanceConfigurationWorkflowList from '../integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList';
import IntegrationInstanceConfigurationListItem from './IntegrationInstanceConfigurationListItem';

const IntegrationInstanceConfigurationList = ({
    componentDefinitions,
    integration,
    integrationInstanceConfigurations,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    integration: Integration;
    integrationInstanceConfigurations: IntegrationInstanceConfiguration[];
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const {data: mcpIntegrationInstanceConfigurationsData} = useMcpIntegrationInstanceConfigurationsQuery();

    const mcpWorkflowIdsByConfigurationId = useMemo(() => {
        const result = new Map<number, Set<string>>();

        const mcpConfigurations =
            mcpIntegrationInstanceConfigurationsData?.mcpIntegrationInstanceConfigurations?.filter(
                (configuration) => configuration != null
            ) || [];

        for (const mcpConfiguration of mcpConfigurations) {
            const configurationId = Number(mcpConfiguration.integrationInstanceConfigurationId);

            const workflowIds =
                mcpConfiguration.mcpIntegrationInstanceConfigurationWorkflows
                    ?.filter((mcpWorkflow) => mcpWorkflow?.integrationInstanceConfigurationWorkflow?.workflowId != null)
                    .map((mcpWorkflow) => mcpWorkflow!.integrationInstanceConfigurationWorkflow!.workflowId!) || [];

            const existingSet = result.get(configurationId) || new Set<string>();

            for (const workflowId of workflowIds) {
                existingSet.add(workflowId);
            }

            result.set(configurationId, existingSet);
        }

        return result;
    }, [mcpIntegrationInstanceConfigurationsData]);

    return (
        <>
            {integrationInstanceConfigurations.length > 0 && (
                <>
                    {integrationInstanceConfigurations.map((integrationInstanceConfiguration) => {
                        const integrationTagIds = integrationInstanceConfiguration.tags?.map((tag) => tag.id);

                        if (!integration || !integration.id) {
                            return;
                        }

                        return (
                            <Collapsible className="group" key={integrationInstanceConfiguration.id}>
                                <IntegrationInstanceConfigurationListItem
                                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                                    key={integrationInstanceConfiguration.id}
                                    mcpWorkflowIds={mcpWorkflowIdsByConfigurationId.get(
                                        integrationInstanceConfiguration.id!
                                    )}
                                    remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                                />

                                <CollapsibleContent>
                                    <IntegrationInstanceConfigurationWorkflowList
                                        componentDefinitions={componentDefinitions}
                                        componentName={integration.componentName}
                                        integrationId={integration.id}
                                        integrationInstanceConfigurationId={integrationInstanceConfiguration.id!}
                                        integrationInstanceConfigurationWorkflows={
                                            integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                        }
                                        integrationVersion={integrationInstanceConfiguration.integrationVersion!}
                                        mcpWorkflowIds={
                                            mcpWorkflowIdsByConfigurationId.get(integrationInstanceConfiguration.id!) ||
                                            new Set()
                                        }
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </CollapsibleContent>
                            </Collapsible>
                        );
                    })}
                </>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationList;
