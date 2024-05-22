import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useIntegrationInstanceConfigurationsEnabledStore} from '@/pages/embedded/integration-instance-configurations/stores/useIntegrationInstanceConfigurationsEnabledStore';
import {
    IntegrationInstanceConfigurationModel,
    IntegrationModel,
    TagModel,
} from '@/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasicModel} from '@/shared/middleware/platform/configuration';

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
