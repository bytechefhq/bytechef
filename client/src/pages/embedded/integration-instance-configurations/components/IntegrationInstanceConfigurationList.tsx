import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {IntegrationInstanceConfigurationModel, IntegrationModel, TagModel} from '@/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/middleware/platform/configuration';
import {useIntegrationInstanceConfigurationsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationsEnabledStore';

import IntegrationInstanceConfigurationListItem from './IntegrationInstanceConfigurationListItem';
import IntegrationInstanceConfigurationWorkflowList from './IntegrationInstanceConfigurationWorkflowList';

const IntegrationInstanceConfigurationList = ({
    componentDefinitions,
    integration,
    integrationInstanceConfigurations,
    tags,
}: {
    componentDefinitions: ComponentDefinitionBasicModel[];
    integration: IntegrationModel;
    integrationInstanceConfigurations: IntegrationInstanceConfigurationModel[];
    tags: TagModel[];
}) => {
    const integrationInstanceConfigurationMap = useIntegrationInstanceConfigurationsEnabledStore(
        ({integrationInstanceConfigurationMap}) => integrationInstanceConfigurationMap
    );

    return (
        <>
            {integrationInstanceConfigurations.length > 0 && (
                <div className="w-full divide-y divide-gray-100">
                    {integrationInstanceConfigurations.map((integrationInstanceConfiguration) => {
                        const integrationTagIds = integrationInstanceConfiguration.tags?.map((tag) => tag.id);

                        if (!integration.id) {
                            return;
                        }

                        return (
                            <Collapsible key={integrationInstanceConfiguration.id}>
                                <IntegrationInstanceConfigurationListItem
                                    componentDefinition={
                                        componentDefinitions.filter(
                                            (componentDefinition) =>
                                                componentDefinition.name === integration.componentName
                                        )[0]
                                    }
                                    integration={integration}
                                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                                    key={integrationInstanceConfiguration.id}
                                    remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                                />

                                <CollapsibleContent>
                                    <IntegrationInstanceConfigurationWorkflowList
                                        integrationId={integration.id}
                                        integrationInstanceConfigurationEnabled={
                                            integrationInstanceConfigurationMap.has(
                                                integrationInstanceConfiguration.id!
                                            )
                                                ? integrationInstanceConfigurationMap.get(
                                                      integrationInstanceConfiguration.id!
                                                  )!
                                                : integrationInstanceConfiguration.enabled!
                                        }
                                        integrationInstanceConfigurationId={integrationInstanceConfiguration.id!}
                                        integrationInstanceConfigurationWorkflows={
                                            integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows
                                        }
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
