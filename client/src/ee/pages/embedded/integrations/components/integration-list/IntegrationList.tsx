import {Collapsible, CollapsibleContent} from '@/components/ui/collapsible';
import IntegrationListItem from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useEffect, useState} from 'react';

import IntegrationWorkflowList from '../integration-workflow-list/IntegrationWorkflowList';

const IntegrationList = ({
    componentDefinitions,
    integrations,
    newlyCreatedIntegrationId,
    tags,
    taskDispatcherDefinitions,
}: {
    componentDefinitions?: ComponentDefinitionBasic[];
    integrations: Integration[];
    newlyCreatedIntegrationId?: number;
    tags: Tag[];
    taskDispatcherDefinitions?: TaskDispatcherDefinition[];
}) => {
    const [openCollapsibles, setOpenCollapsibles] = useState<Set<number>>(new Set());

    useEffect(() => {
        if (newlyCreatedIntegrationId) {
            setOpenCollapsibles((previousOpenCollapsibles) => {
                const nextOpenCollapsibles = new Set(previousOpenCollapsibles);

                nextOpenCollapsibles.add(newlyCreatedIntegrationId);

                return nextOpenCollapsibles;
            });
        }
    }, [newlyCreatedIntegrationId]);

    return (
        <div className="w-full px-4 3xl:mx-auto 3xl:w-4/5">
            {integrations.map((integration) => {
                const integrationTagIds = integration.tags?.map((tag) => tag.id);

                return (
                    <Collapsible
                        className="group mb-2 rounded border border-border/50"
                        key={integration.id}
                        onOpenChange={(open) => {
                            setOpenCollapsibles((previousOpenCollapsibles) => {
                                const openCollapsibles = new Set(previousOpenCollapsibles);

                                if (open) {
                                    openCollapsibles.add(integration.id!);
                                } else {
                                    openCollapsibles.delete(integration.id!);
                                }

                                return openCollapsibles;
                            });
                        }}
                        open={openCollapsibles.has(integration.id!)}
                    >
                        <IntegrationListItem
                            integration={integration}
                            key={integration.id}
                            remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                        />

                        <CollapsibleContent>
                            <IntegrationWorkflowList
                                componentDefinitions={componentDefinitions}
                                integration={integration}
                                taskDispatcherDefinitions={taskDispatcherDefinitions}
                            />
                        </CollapsibleContent>
                    </Collapsible>
                );
            })}
        </div>
    );
};
export default IntegrationList;
