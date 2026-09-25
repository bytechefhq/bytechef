import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useMcpIntegrationInstanceConfigurationsQuery} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

import IntegrationInstanceConfigurationWorkflowList from '../integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList';
import IntegrationInstanceConfigurationListItem from './IntegrationInstanceConfigurationListItem';

const IntegrationInstanceConfigurationList = ({
    componentDefinitions,
    divided = false,
    expandAll = false,
    integration,
    integrationInstanceConfigurations,
    showIntegrationIcon = true,
    showOpenInIntegration = false,
    showTags = true,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    divided?: boolean;
    expandAll?: boolean;
    integration: Integration;
    integrationInstanceConfigurations: IntegrationInstanceConfiguration[];
    showIntegrationIcon?: boolean;
    showOpenInIntegration?: boolean;
    showTags?: boolean;
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const isTenantAdmin = useIsTenantAdmin();

    const {data: mcpIntegrationInstanceConfigurationsData} = useMcpIntegrationInstanceConfigurationsQuery(undefined, {
        enabled: isTenantAdmin,
    });

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
                <div className={divided ? 'divide-y divide-stroke-neutral-primary' : undefined}>
                    {integrationInstanceConfigurations.map((integrationInstanceConfiguration) => {
                        const integrationTagIds = integrationInstanceConfiguration.tags?.map((tag) => tag.id);

                        if (!integration || !integration.id) {
                            return;
                        }

                        return (
                            <Collapsible
                                className={divided ? 'group' : 'group mb-2 rounded border border-border/50 last:mb-0'}
                                defaultOpen={expandAll}
                                key={integrationInstanceConfiguration.id}
                            >
                                <IntegrationInstanceConfigurationListItem
                                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                                    key={integrationInstanceConfiguration.id}
                                    remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                                    showIntegrationIcon={showIntegrationIcon}
                                    showTags={showTags}
                                />

                                <CollapsibleContent className="pl-8">
                                    <IntegrationInstanceConfigurationWorkflowList
                                        componentDefinitions={componentDefinitions}
                                        componentName={integration.componentName}
                                        integrationId={integration.id}
                                        integrationInstanceConfigurationId={integrationInstanceConfiguration.id!}
                                        integrationInstanceConfigurationName={integrationInstanceConfiguration.name}
                                        integrationInstanceConfigurationWorkflows={
                                            integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                        }
                                        integrationVersion={integrationInstanceConfiguration.integrationVersion!}
                                        mcpWorkflowIds={
                                            mcpWorkflowIdsByConfigurationId.get(integrationInstanceConfiguration.id!) ||
                                            new Set()
                                        }
                                        showOpenInIntegration={showOpenInIntegration}
                                        taskDispatcherDefinitions={taskDispatcherDefinitions}
                                    />
                                </CollapsibleContent>
                            </Collapsible>
                        );
                    })}
                </div>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationList;
