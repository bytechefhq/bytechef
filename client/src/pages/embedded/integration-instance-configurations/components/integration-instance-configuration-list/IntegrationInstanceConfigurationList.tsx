import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {Integration, IntegrationInstanceConfiguration, Tag} from '@/shared/middleware/embedded/configuration';

import IntegrationInstanceConfigurationWorkflowList from '../integration-instance-configuration-workflow-list/IntegrationInstanceConfigurationWorkflowList';
import IntegrationInstanceConfigurationListItem from './IntegrationInstanceConfigurationListItem';

const IntegrationInstanceConfigurationList = ({
    integration,
    integrationInstanceConfigurations,
    tags,
}: {
    integration: Integration;
    integrationInstanceConfigurations: IntegrationInstanceConfiguration[];
    tags: Tag[];
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
                                        componentName={integration.componentName}
                                        integrationId={integration.id}
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
                </>
            )}
        </>
    );
};

export default IntegrationInstanceConfigurationList;
