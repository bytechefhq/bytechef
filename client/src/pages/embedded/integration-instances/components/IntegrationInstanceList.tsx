import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import {useIntegrationInstancesEnabledStore} from '@/pages/embedded/integration-instances/stores/useIntegrationInstancesEnabledStore';
import {IntegrationInstanceModel, IntegrationModel, TagModel} from 'middleware/embedded/configuration';

import IntegrationInstanceListItem from './IntegrationInstanceListItem';
import IntegrationInstanceWorkflowList from './IntegrationInstanceWorkflowList';

const IntegrationInstanceList = ({
    integration,
    integrationInstances,
    tags,
}: {
    integration: IntegrationModel;
    integrationInstances: IntegrationInstanceModel[];
    tags: TagModel[];
}) => {
    const integrationInstanceMap = useIntegrationInstancesEnabledStore(
        ({integrationInstanceMap}) => integrationInstanceMap
    );

    return (
        <>
            {integrationInstances.length > 0 && (
                <div className="flex w-full flex-col">
                    <h3 className="mb-1 px-2 text-lg font-semibold text-gray-500">{integration.componentName}</h3>

                    <div className="w-full divide-y divide-gray-100">
                        {integrationInstances.map((integrationInstance) => {
                            const integrationTagIds = integrationInstance.tags?.map((tag) => tag.id);

                            if (!integration.id) {
                                return;
                            }

                            return (
                                <Collapsible key={integrationInstance.id}>
                                    <IntegrationInstanceListItem
                                        integration={integration}
                                        integrationInstance={integrationInstance}
                                        key={integrationInstance.id}
                                        remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                                    />

                                    <CollapsibleContent>
                                        <IntegrationInstanceWorkflowList
                                            integrationId={integration.id}
                                            integrationInstanceEnabled={
                                                integrationInstanceMap.has(integrationInstance.id!)
                                                    ? integrationInstanceMap.get(integrationInstance.id!)!
                                                    : integrationInstance.enabled!
                                            }
                                            integrationInstanceId={integrationInstance.id!}
                                            integrationInstanceWorkflows={
                                                integrationInstance.integrationInstanceWorkflows
                                            }
                                        />
                                    </CollapsibleContent>
                                </Collapsible>
                            );
                        })}
                    </div>
                </div>
            )}
        </>
    );
};
export default IntegrationInstanceList;
