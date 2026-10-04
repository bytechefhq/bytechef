import {TooltipProvider} from '@/components/ui/tooltip';
import IntegrationsLeftSidebar from '@/ee/pages/embedded/integration/components/integrations-sidebar/IntegrationsLeftSidebar';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import {ReactElement} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    getIntegrationWorkflows: vi.fn(),
    getIntegrationWorkflowsQuery: vi.fn(),
    getIntegrationsQuery: vi.fn(),
}));

vi.mock('@/components/ui/scroll-area', () => ({
    ScrollArea: ({children}: {children: React.ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationSelect', () => ({
    default: ({selectedIntegrationId}: {selectedIntegrationId: number}) => (
        <div data-testid="integration-select">IntegrationSelect:{selectedIntegrationId}</div>
    ),
}));

vi.mock('@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsList', () => ({
    default: ({integration}: {integration: {id: number}}) => (
        <li data-testid="integration-workflows-list">Integration:{integration.id}</li>
    ),
}));

vi.mock(
    '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListFilter',
    () => ({
        default: ({sortBy}: {sortBy: string}) => <div data-testid="workflows-list-filter">Sort:{sortBy}</div>,
    })
);

vi.mock(
    '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListItem',
    () => ({
        default: ({workflow}: {workflow: {id: string}}) => <li data-testid="workflow-item">Workflow:{workflow.id}</li>,
    })
);

vi.mock(
    '@/ee/pages/embedded/integration/components/integrations-sidebar/components/IntegrationWorkflowsListSkeleton',
    () => ({
        default: () => <div data-testid="skeleton">Loading...</div>,
    })
);

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    useGetIntegrationsQuery: () => hoisted.getIntegrationsQuery(),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    IntegrationWorkflowKeys: {
        integrationWorkflows: (id: number) => ['integrationWorkflows', id],
    },
    useGetIntegrationWorkflowsQuery: (integrationId: number, enabled: boolean) =>
        hoisted.getIntegrationWorkflowsQuery(integrationId, enabled),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', () => ({
    WorkflowApi: class {
        getIntegrationWorkflows(request: {id: number}) {
            return hoisted.getIntegrationWorkflows(request);
        }
    },
}));

const createTestQueryClient = () =>
    new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    });

let queryClient: QueryClient;

const setupQueries = ({
    integrations = [{id: 1}, {id: 2}],
    integrationsLoading = false,
    workflows = [{id: 'w1'}, {id: 'w2'}],
    workflowsLoading = false,
}: {
    integrations?: {id: number}[];
    integrationsLoading?: boolean;
    workflows?: {id: string}[];
    workflowsLoading?: boolean;
}) => {
    hoisted.getIntegrationsQuery.mockReturnValue({
        data: integrationsLoading ? undefined : integrations,
        isLoading: integrationsLoading,
        refetch: vi.fn(),
    });

    hoisted.getIntegrationWorkflowsQuery.mockImplementation((integrationId: number, enabled: boolean) => ({
        data: enabled ? workflows : undefined,
        isLoading: enabled && workflowsLoading,
    }));

    hoisted.getIntegrationWorkflows.mockResolvedValue(workflows);
};

const renderWithProviders = (ui: ReactElement) =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );

const baseProps = {
    currentWorkflowId: 'w1',
    onIntegrationClick: vi.fn(),
};

describe('IntegrationsLeftSidebar', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        queryClient = createTestQueryClient();
    });

    afterEach(() => {
        queryClient.clear();
    });

    it('shows a select skeleton while the integrations are loading', () => {
        setupQueries({integrationsLoading: true});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={5} />);

        expect(screen.queryByTestId('integration-select')).not.toBeInTheDocument();
    });

    it('shows the list skeleton while the workflows are loading', async () => {
        setupQueries({workflowsLoading: true});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={5} />);

        await waitFor(() => expect(screen.getByTestId('skeleton')).toBeInTheDocument());
    });

    it('renders an item per workflow of the selected integration', async () => {
        setupQueries({workflows: [{id: 'wa'}, {id: 'wb'}, {id: 'wc'}]});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={7} />);

        expect(await screen.findAllByTestId('workflow-item')).toHaveLength(3);
        expect(screen.getByTestId('integration-select')).toHaveTextContent('IntegrationSelect:7');
        expect(hoisted.getIntegrationWorkflowsQuery).toHaveBeenCalledWith(7, true);
    });

    it('shows an empty state when the selected integration has no workflows', async () => {
        setupQueries({workflows: []});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={7} />);

        expect(await screen.findByText('No workflows found')).toBeInTheDocument();
    });

    it('groups the workflows by integration when every integration is selected', async () => {
        setupQueries({integrations: [{id: 11}, {id: 22}, {id: 33}]});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={NaN} />);

        expect(await screen.findAllByTestId('integration-workflows-list')).toHaveLength(3);
        expect(hoisted.getIntegrationWorkflowsQuery).toHaveBeenCalledWith(0, false);

        await waitFor(() => expect(hoisted.getIntegrationWorkflows).toHaveBeenCalledWith({id: 11}));
    });

    it('leaves integration and workflow creation to the settings menu and the Integrations page', async () => {
        setupQueries({});

        renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={9} />);

        await screen.findAllByTestId('workflow-item');

        expect(screen.queryByLabelText('New integration')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Workflow'})).not.toBeInTheDocument();
    });

    it('follows the integrationId prop when it changes', async () => {
        setupQueries({});

        const {rerender} = renderWithProviders(<IntegrationsLeftSidebar {...baseProps} integrationId={5} />);

        await waitFor(() => expect(hoisted.getIntegrationWorkflowsQuery).toHaveBeenCalledWith(5, true));

        rerender(
            <QueryClientProvider client={queryClient}>
                <TooltipProvider>
                    <IntegrationsLeftSidebar {...baseProps} integrationId={8} />
                </TooltipProvider>
            </QueryClientProvider>
        );

        await waitFor(() => expect(hoisted.getIntegrationWorkflowsQuery).toHaveBeenCalledWith(8, true));
    });
});
