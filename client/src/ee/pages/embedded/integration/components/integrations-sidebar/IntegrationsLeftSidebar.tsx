import Button from '@/components/Button/Button';
import {ScrollArea} from '@/components/ui/scroll-area';
import {Skeleton} from '@/components/ui/skeleton';
import IntegrationSelect from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationSelect';
import IntegrationWorkflowsList from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsList';
import IntegrationWorkflowsListFilter from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListFilter';
import IntegrationWorkflowsListItem from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItem';
import IntegrationWorkflowsListSkeleton from '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListSkeleton';
import {useIntegrationsLeftSidebar} from '@/ee/pages/embedded/integration/components/integrations-sidebar/hooks/useIntegrationsLeftSidebar';
import IntegrationDialog from '@/ee/pages/embedded/integrations/components/IntegrationDialog';
import {WorkflowApi} from '@/ee/shared/middleware/embedded/configuration';
import {
    IntegrationWorkflowKeys,
    useGetIntegrationWorkflowsQuery,
} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useGetIntegrationsQuery} from '@/ee/shared/queries/embedded/integrations.queries';
import {useGetWorkflowQuery} from '@/ee/shared/queries/embedded/workflows.queries';
import WorkflowDialog from '@/shared/components/workflow/WorkflowDialog';
import {useQueries} from '@tanstack/react-query';
import {PlusIcon} from 'lucide-react';
import {RefObject, useEffect, useMemo, useRef, useState} from 'react';
import {PanelImperativeHandle} from 'react-resizable-panels';

interface IntegrationsLeftSidebarProps {
    bottomResizablePanelRef: RefObject<PanelImperativeHandle | null>;
    currentWorkflowId: string;
    integrationId: number;
    onIntegrationClick: (integrationId: number, integrationWorkflowId: number) => void;
}

const workflowApi = new WorkflowApi();

const IntegrationsLeftSidebar = ({
    bottomResizablePanelRef,
    currentWorkflowId,
    integrationId,
    onIntegrationClick,
}: IntegrationsLeftSidebarProps) => {
    const [selectedIntegrationId, setSelectedIntegrationId] = useState(!isNaN(integrationId) ? integrationId : 0);
    const [sortBy, setSortBy] = useState('last-edited');
    const [searchValue, setSearchValue] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [showWorkflowDialog, setShowWorkflowDialog] = useState(false);

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

    const {
        calculateTimeDifference,
        createIntegrationWorkflowMutation,
        getFilteredWorkflows,
        getWorkflowsIntegrationId,
    } = useIntegrationsLeftSidebar({
        bottomResizablePanelRef,
        integrationId: selectedIntegrationId === 0 ? integrationId : selectedIntegrationId,
    });

    const findIntegrationIdByWorkflow = getWorkflowsIntegrationId(integrations || []);

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
                    <div className="flex items-center gap-2">
                        <Skeleton className="h-9 flex-1 rounded-md" />

                        <Skeleton className="size-9 rounded-md" />
                    </div>
                ) : (
                    integrations && (
                        <div className="flex items-center gap-2">
                            <IntegrationSelect
                                integrationId={integrationId}
                                integrations={integrations}
                                selectedIntegrationId={selectedIntegrationId}
                                setSelectedIntegrationId={setSelectedIntegrationId}
                            />

                            <IntegrationDialog
                                integration={undefined}
                                triggerNode={
                                    <Button
                                        aria-label="New integration"
                                        className="data-[state=open]:border-stroke-brand-secondary data-[state=open]:bg-surface-brand-secondary data-[state=open]:text-content-brand-primary"
                                        icon={<PlusIcon />}
                                        size="icon"
                                        variant="outline"
                                    />
                                }
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

                <Button
                    className="w-full [&_svg]:size-5"
                    icon={<PlusIcon />}
                    label="Workflow"
                    onClick={() => setShowWorkflowDialog(true)}
                    variant="secondary"
                />
            </div>

            <ScrollArea className="mb-3 h-screen w-full overflow-y-auto">
                {isLoading && <IntegrationWorkflowsListSkeleton />}

                {!isLoading && (
                    <ul className="flex flex-col items-center gap-4">
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
                                <span className="text-sm text-muted-foreground">No workflows found</span>
                            ))}

                        {selectedIntegrationId !== 0 && filteredWorkflowsList.length > 0 ? (
                            filteredWorkflowsList.map((workflow) => (
                                <IntegrationWorkflowsListItem
                                    calculateTimeDifference={calculateTimeDifference}
                                    currentWorkflowId={currentWorkflowId}
                                    findIntegrationIdByWorkflow={findIntegrationIdByWorkflow}
                                    key={workflow.id}
                                    onIntegrationClick={onIntegrationClick}
                                    setSelectedIntegrationId={setSelectedIntegrationId}
                                    workflow={workflow}
                                />
                            ))
                        ) : (
                            <span className="text-sm text-muted-foreground">No workflows found</span>
                        )}
                    </ul>
                )}
            </ScrollArea>

            {showWorkflowDialog && (
                <WorkflowDialog
                    createWorkflowMutation={createIntegrationWorkflowMutation}
                    integrationId={selectedIntegrationId === 0 ? integrationId : selectedIntegrationId}
                    onClose={() => setShowWorkflowDialog(false)}
                    useGetWorkflowQuery={useGetWorkflowQuery}
                />
            )}
        </aside>
    );
};

export default IntegrationsLeftSidebar;
