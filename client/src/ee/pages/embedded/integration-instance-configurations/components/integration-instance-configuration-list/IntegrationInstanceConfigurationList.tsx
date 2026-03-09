import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';

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
