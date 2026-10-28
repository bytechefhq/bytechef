import {
    AutomationHubKeys,
    useFetchWorkflow,
    useGetAutomationsQuery,
    useGetComponentConnectionsQuery,
    useGetConnectionsQuery,
    useGetTemplateProjectsQuery,
    useGetWorkflowQuery,
} from '@/ee/pages/embedded/automation-hub/queries/automationHub.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {automationWorkflowProjectApiMock, connectedUserProjectWorkflowApiMock, connectionApiMock} = vi.hoisted(() => ({
    automationWorkflowProjectApiMock: {getFrontendProjects: vi.fn()},
    connectedUserProjectWorkflowApiMock: {
        getFrontendProjectWorkflow: vi.fn(),
        getFrontendProjectWorkflows: vi.fn(),
    },
    connectionApiMock: {
        getAllFrontendConnections: vi.fn(),
        getFrontendConnections: vi.fn(),
    },
}));

vi.mock('@/ee/shared/middleware/embedded/public', () => ({
    AutomationWorkflowProjectApi: class {
        constructor() {
            return automationWorkflowProjectApiMock;
        }
    },
    ConnectedUserProjectWorkflowApi: class {
        constructor() {
            return connectedUserProjectWorkflowApiMock;
        }
    },
    ConnectionApi: class {
        constructor() {
            return connectionApiMock;
        }
    },
}));

const renderQueryHook = <T,>(useHook: () => T) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return {queryClient, ...renderHook(useHook, {wrapper})};
};

describe('automationHub.queries', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('builds the query keys under the automationHub namespace', () => {
        expect(AutomationHubKeys.connectionsByComponent('slack')).toEqual(['automationHub', 'connections', 'slack']);
        expect(AutomationHubKeys.workflow('workflow-uuid')).toEqual(['automationHub', 'workflow', 'workflow-uuid']);
    });

    it('loads the template projects', async () => {
        automationWorkflowProjectApiMock.getFrontendProjects.mockResolvedValue([{id: 1}]);

        const {result} = renderQueryHook(useGetTemplateProjectsQuery);

        await waitFor(() => expect(result.current.data).toEqual([{id: 1}]));
        expect(automationWorkflowProjectApiMock.getFrontendProjects).toHaveBeenCalledWith({});
    });

    it('leaves projects hidden from the hub out of the template projects', async () => {
        automationWorkflowProjectApiMock.getFrontendProjects.mockResolvedValue([
            {automationHubVisible: true, id: 1},
            {automationHubVisible: false, id: 2},
            {id: 3},
        ]);

        const {result} = renderQueryHook(useGetTemplateProjectsQuery);

        await waitFor(() => expect(result.current.data).toEqual([{automationHubVisible: true, id: 1}, {id: 3}]));
    });

    it('loads the automations', async () => {
        connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflows.mockResolvedValue([{workflowUuid: 'a'}]);

        const {result} = renderQueryHook(useGetAutomationsQuery);

        await waitFor(() => expect(result.current.data).toEqual([{workflowUuid: 'a'}]));
        expect(connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflows).toHaveBeenCalledWith({});
    });

    it('loads all connections', async () => {
        connectionApiMock.getAllFrontendConnections.mockResolvedValue([{id: 5}]);

        const {result} = renderQueryHook(useGetConnectionsQuery);

        await waitFor(() => expect(result.current.data).toEqual([{id: 5}]));
        expect(connectionApiMock.getAllFrontendConnections).toHaveBeenCalledWith({});
    });

    it('loads the connections of one component', async () => {
        connectionApiMock.getFrontendConnections.mockResolvedValue([{id: 6}]);

        const {result} = renderQueryHook(() => useGetComponentConnectionsQuery('slack'));

        await waitFor(() => expect(result.current.data).toEqual([{id: 6}]));
        expect(connectionApiMock.getFrontendConnections).toHaveBeenCalledWith({componentName: 'slack'});
    });

    it('does not load component connections while disabled', async () => {
        const {result} = renderQueryHook(() => useGetComponentConnectionsQuery('slack', false));

        await new Promise((resolve) => setTimeout(resolve, 20));

        expect(result.current.fetchStatus).toBe('idle');
        expect(connectionApiMock.getFrontendConnections).not.toHaveBeenCalled();
    });

    it('loads a workflow by its uuid', async () => {
        connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflow.mockResolvedValue({workflowUuid: 'b'});

        const {result} = renderQueryHook(() => useGetWorkflowQuery('b'));

        await waitFor(() => expect(result.current.data).toEqual({workflowUuid: 'b'}));
        expect(connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflow).toHaveBeenCalledWith({
            workflowUuid: 'b',
        });
    });

    it('does not load a workflow without a uuid', async () => {
        renderQueryHook(() => useGetWorkflowQuery(undefined));

        await new Promise((resolve) => setTimeout(resolve, 20));

        expect(connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflow).not.toHaveBeenCalled();
    });

    it('fetches a workflow on demand and caches it under its workflow key', async () => {
        connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflow.mockResolvedValue({workflowUuid: 'c'});

        const {queryClient, result} = renderQueryHook(useFetchWorkflow);

        let fetchedWorkflow: unknown;

        await act(async () => {
            fetchedWorkflow = await result.current('c');
        });

        expect(fetchedWorkflow).toEqual({workflowUuid: 'c'});
        expect(connectedUserProjectWorkflowApiMock.getFrontendProjectWorkflow).toHaveBeenCalledWith({
            workflowUuid: 'c',
        });
        expect(queryClient.getQueryData(AutomationHubKeys.workflow('c'))).toEqual({workflowUuid: 'c'});
    });
});
