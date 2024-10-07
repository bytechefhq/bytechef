import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useIntegrationInstanceConfigurationsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationsEnabledStore';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';

import IntegrationInstanceConfigurationWorkflowList from '../integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList';
import IntegrationInstanceConfigurationListItem from './IntegrationInstanceConfigurationListItem';

const IntegrationInstanceConfigurationList = ({
    componentDefinitions,
    integration,
    integrationInstanceConfigurations,
    tags,
}: {
    componentDefinitions: ComponentDefinitionBasic[];
    integration: Integration;
    integrationInstanceConfigurations: IntegrationInstanceConfiguration[];
    tags: Tag[];
}) => {
    const integrationInstanceConfigurationMap = useIntegrationInstanceConfigurationsEnabledStore(
        ({integrationInstanceConfigurationMap}) => integrationInstanceConfigurationMap
    );

    return (
        <>
            {integrationInstanceConfigurations.length > 0 && (
                <div className="w-full">
                    {integrationInstanceConfigurations.map((integrationInstanceConfiguration) => {
                        const integrationTagIds = integrationInstanceConfiguration.tags?.map((tag) => tag.id);

                        if (!integration || !integration.id) {
                            return;
                        }

                        return (
                            <Collapsible className="group" key={integrationInstanceConfiguration.id}>
                                <IntegrationInstanceConfigurationListItem
                                    componentDefinition={
                                        componentDefinitions.filter(
                                            (componentDefinition) =>
                                                componentDefinition.name === integration.componentName
                                        )[0]
                                    }
                                    integrationInstanceConfiguration={integrationInstanceConfiguration}
                                    key={integrationInstanceConfiguration.id}
                                    remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                                />

                                <CollapsibleContent>
                                    <IntegrationInstanceConfigurationWorkflowList
                                        componentName={integration.componentName}
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
                                        integrationVersion={integrationInstanceConfiguration.integrationVersion!}
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
