import {ScrollArea} from '@/components/ui/scroll-area';
import {Skeleton} from '@/components/ui/skeleton';
import IntegrationSelect from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationSelect';
import IntegrationWorkflowsList from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsList';
import IntegrationWorkflowsListFilter from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListFilter';
import IntegrationWorkflowsListItem from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItem';
import IntegrationWorkflowsListSkeleton from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListSkeleton';
import {useIntegrationsLeftSidebar} from '@/ee/pages/embedded/integration/components/integrations-sidebar/hooks/useIntegrationsLeftSidebar';
import {WorkflowApi} from '@/ee/shared/middleware/embedded/configuration';
import {
    IntegrationWorkflowKeys,
    useGetIntegrationWorkflowsQuery,
} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useQueries} from '@tanstack/react-query';
import {useEffect, useMemo, useRef, useState} from 'react';

interface IntegrationsLeftSidebarProps {
    currentWorkflowId: string;
    integrationId: number;
    onIntegrationClick: (integrationId: number, integrationWorkflowId: number) => void;
}

const workflowApi = new WorkflowApi();

const IntegrationsLeftSidebar = ({
    currentWorkflowId,
    integrationId,
    onIntegrationClick,
}: IntegrationsLeftSidebarProps) => {
    const [selectedIntegrationId, setSelectedIntegrationId] = useState(!isNaN(integrationId) ? integrationId : 0);
    const [sortBy, setSortBy] = useState('last-edited');
    const [searchValue, setSearchValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const searchInputRef = useRef<HTMLInputElement>(null);

    const {
        data: integrations,
        isLoading: integrationsLoading,
        refetch: refetchIntegrations,
    } = useGetIntegrationsQuery();

    const {data: selectedIntegrationWorkflows, isLoading: integrationWorkflowsLoading} =
        useGetIntegrationWorkflowsQuery(selectedIntegrationId, selectedIntegrationId !== 0);

    const allIntegrationWorkflowQueries = useQueries({
        queries:
            selectedIntegrationId === 0 && integrations
                ? integrations.map((integration) => ({
                      queryFn: () => workflowApi.getIntegrationWorkflows({id: integration.id!}),
                      queryKey: IntegrationWorkflowKeys.integrationWorkflows(integration.id!),
                  }))
                : [],
    });

    const allIntegrationsWorkflows = useMemo(
        () =>
            selectedIntegrationId === 0
                ? allIntegrationWorkflowQueries.flatMap((query) => query.data || [])
                : undefined,
        [allIntegrationWorkflowQueries, selectedIntegrationId]
    );
    const allIntegrationsWorkflowsLoading = allIntegrationWorkflowQueries.some((query) => query.isLoading);

    const workflows = selectedIntegrationWorkflows || allIntegrationsWorkflows;

    const {calculateTimeDifference, getFilteredWorkflows, getWorkflowsIntegrationId} = useIntegrationsLeftSidebar();

    const findIntegrationIdByWorkflow = getWorkflowsIntegrationId(integrations || []);

    const selectedIntegration = integrations?.find((integration) => integration.id === selectedIntegrationId);

    const filteredWorkflowsList = useMemo(
        () => getFilteredWorkflows(workflows, sortBy, searchValue),
        [workflows, sortBy, searchValue, getFilteredWorkflows]
    );

    useEffect(() => {
        setIsLoading(integrationWorkflowsLoading || allIntegrationsWorkflowsLoading || integrationsLoading);
    }, [integrationWorkflowsLoading, allIntegrationsWorkflowsLoading, integrationsLoading]);

    useEffect(() => {
        if (selectedIntegrationId === 0) {
            refetchIntegrations();
        }
    }, [selectedIntegrationId, refetchIntegrations]);

    useEffect(() => {
        setSelectedIntegrationId(!isNaN(integrationId) ? integrationId : 0);
    }, [integrationId]);

    useEffect(() => {
        if (isLoading) {
            return;
        }

        const timeoutId = setTimeout(() => {
            searchInputRef.current?.focus();
        }, 50);

        return () => clearTimeout(timeoutId);
    }, [isLoading, selectedIntegrationId]);

    return (
        <aside className="flex h-full min-w-[355px] flex-col items-center gap-2 bg-surface-main px-4 pt-3">
            <div className="flex w-full flex-col gap-2">
                {integrationsLoading ? (
                    <Skeleton className="h-9 w-full rounded-md" />
                ) : (
                    integrations && (
                        <div className="flex items-center gap-2">
                            <IntegrationSelect
                                integrationId={integrationId}
                                integrations={integrations}
                                selectedIntegrationId={selectedIntegrationId}
                                setSelectedIntegrationId={setSelectedIntegrationId}
                            />
                        </div>
                    )
                )}

                <IntegrationWorkflowsListFilter
                    ref={searchInputRef}
                    searchValue={searchValue}
                    setSearchValue={setSearchValue}
                    setSortBy={setSortBy}
                    sortBy={sortBy}
                />
            </div>

            <ScrollArea className="mb-3 min-h-0 w-full flex-1 [&_[data-radix-scroll-area-viewport]>div]:block!">
                {isLoading && <IntegrationWorkflowsListSkeleton />}

                {!isLoading && (
                    <ul className="flex flex-col gap-4">
                        {selectedIntegrationId === 0 &&
                            (integrations ? (
                                integrations.map((integration) => (
                                    <IntegrationWorkflowsList
                                        calculateTimeDifference={calculateTimeDifference}
                                        currentWorkflowId={currentWorkflowId}
                                        filteredWorkflowsList={filteredWorkflowsList}
                                        findIntegrationIdByWorkflow={findIntegrationIdByWorkflow}
                                        integration={integration}
                                        key={integration.id}
                                        onIntegrationClick={onIntegrationClick}
                                        setSelectedIntegrationId={setSelectedIntegrationId}
                                    />
                                ))
                            ) : (
                                <span className="w-full py-2 text-sm text-muted-foreground">No workflows found</span>
                            ))}

                        {selectedIntegrationId !== 0 && filteredWorkflowsList.length > 0 ? (
                            filteredWorkflowsList.map((workflow) => (
                                <IntegrationWorkflowsListItem
                                    calculateTimeDifference={calculateTimeDifference}
                                    currentWorkflowId={currentWorkflowId}
                                    findIntegrationIdByWorkflow={findIntegrationIdByWorkflow}
                                    integration={selectedIntegration}
                                    key={workflow.id}
                                    onIntegrationClick={onIntegrationClick}
                                    setSelectedIntegrationId={setSelectedIntegrationId}
                                    workflow={workflow}
                                />
                            ))
                        ) : (
                            <span className="w-full py-2 text-sm text-muted-foreground">No workflows found</span>
                        )}
                    </ul>
                )}
            </ScrollArea>
        </aside>
    );
};

export default IntegrationsLeftSidebar;
