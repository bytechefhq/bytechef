import {Collapsible} from '@/components/ui/collapsible';
import IntegrationInstanceConfigurationsPanel from '@/ee/pages/embedded/integration-instance-configurations/components/IntegrationInstanceConfigurationsPanel';
import IntegrationInstanceConfigurationWorkflowExecutionsSheet from '@/ee/pages/embedded/integration-instance-configurations/components/integration-instance-configuration-workflow-executions-sheet/IntegrationInstanceConfigurationWorkflowExecutionsSheet';
import IntegrationListItem from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItem';
import {IntegrationListItemTabType} from '@/ee/pages/embedded/integrations/components/integration-list/IntegrationListItemTabs';
import {Integration, Tag} from '@/ee/shared/middleware/embedded/configuration';
import {useGetComponentDefinitionsQuery} from '@/ee/shared/queries/embedded/componentDefinitions.queries';
import {WorkflowReadOnlyProvider} from '@/pages/platform/workflow-editor/providers/workflowEditorProvider';
import {ComponentDefinitionBasic, TaskDispatcherDefinition} from '@/shared/middleware/platform/configuration';
import {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {useSearchParams} from 'react-router-dom';

import IntegrationWorkflowList from '../integration-workflow-list/IntegrationWorkflowList';

const getInitialOpenIntegrations = (searchParams: URLSearchParams) => {
    const openIntegrations = new Map<number, IntegrationListItemTabType>();

    const integrationId = Number(searchParams.get('integrationId'));

    if (integrationId && searchParams.get('tab') === 'configurations') {
        openIntegrations.set(integrationId, 'configurations');
    }

    return openIntegrations;
};

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
    const [searchParams, setSearchParams] = useSearchParams();

    const [openIntegrations, setOpenIntegrations] = useState<Map<number, IntegrationListItemTabType>>(() =>
        getInitialOpenIntegrations(searchParams)
    );

    const scrollToIntegrationIdRef = useRef<number | undefined>(openIntegrations.keys().next().value);

    const hasOpenConfigurationsTab = useMemo(
        () => Array.from(openIntegrations.values()).includes('configurations'),
        [openIntegrations]
    );

    const handleTabChange = useCallback((integrationId: number, tab?: IntegrationListItemTabType) => {
        setOpenIntegrations((previousOpenIntegrations) => {
            const newOpenIntegrations = new Map(previousOpenIntegrations);

            if (tab) {
                newOpenIntegrations.set(integrationId, tab);
            } else {
                newOpenIntegrations.delete(integrationId);
            }

            return newOpenIntegrations;
        });
    }, []);

    useEffect(() => {
        if (!searchParams.has('integrationId') && !searchParams.has('tab')) {
            return;
        }

        const remainingSearchParams = new URLSearchParams(searchParams);

        remainingSearchParams.delete('integrationId');
        remainingSearchParams.delete('tab');

        setSearchParams(remainingSearchParams, {replace: true});
    }, [searchParams, setSearchParams]);

    useEffect(() => {
        const scrollToIntegrationId = scrollToIntegrationIdRef.current;

        if (
            scrollToIntegrationId === undefined ||
            !integrations.some((integration) => integration.id === scrollToIntegrationId)
        ) {
            return;
        }

        scrollToIntegrationIdRef.current = undefined;

        document.querySelector(`[data-integration-id="${scrollToIntegrationId}"]`)?.scrollIntoView({block: 'start'});
    }, [integrations]);

    useEffect(() => {
        if (newlyCreatedIntegrationId) {
            handleTabChange(newlyCreatedIntegrationId, 'workflows');
        }
    }, [handleTabChange, newlyCreatedIntegrationId]);

    return (
        <div className="w-full self-start p-4 pt-0 3xl:mx-auto 3xl:w-4/5">
            {integrations.map((integration) => {
                const activeTab = openIntegrations.get(integration.id!);
                const integrationTagIds = integration.tags?.map((tag) => tag.id);

                return (
                    <Collapsible
                        className="group/integration mb-2 rounded border border-border/50"
                        data-integration-id={integration.id}
                        key={integration.id}
                        onOpenChange={(open) => handleTabChange(integration.id!, open ? 'workflows' : undefined)}
                        open={!!activeTab}
                    >
                        <IntegrationListItem
                            activeTab={activeTab}
                            configurationsContent={
                                <IntegrationInstanceConfigurationsPanel enabled integration={integration} />
                            }
                            integration={integration}
                            key={integration.id}
                            onTabChange={(tab) => handleTabChange(integration.id!, tab)}
                            remainingTags={tags?.filter((tag) => !integrationTagIds?.includes(tag.id))}
                            workflowsContent={
                                <IntegrationWorkflowList
                                    componentDefinitions={componentDefinitions}
                                    integration={integration}
                                    taskDispatcherDefinitions={taskDispatcherDefinitions}
                                />
                            }
                        />
                    </Collapsible>
                );
            })}

            {hasOpenConfigurationsTab && (
                <WorkflowReadOnlyProvider
                    value={{
                        useGetComponentDefinitionsQuery: useGetComponentDefinitionsQuery,
                    }}
                >
                    <IntegrationInstanceConfigurationWorkflowExecutionsSheet />
                </WorkflowReadOnlyProvider>
            )}
        </div>
    );
};

export default IntegrationList;
