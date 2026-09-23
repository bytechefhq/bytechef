import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useGetComponentConnectionsQuery} from '../queries/automationHub.queries';

const {getFrontendConnectionsMock} = vi.hoisted(() => ({getFrontendConnectionsMock: vi.fn()}));

vi.mock('@/ee/shared/middleware/embedded/public', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/ee/shared/middleware/embedded/public')>();

    return {
        ...actual,
        ConnectionApi: vi.fn(function ConnectionApiMock() {
            return {getFrontendConnections: getFrontendConnectionsMock};
        }),
    };
});

const renderConnectionsHook = (componentName: string) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return renderHook(() => useGetComponentConnectionsQuery(componentName), {wrapper: Wrapper});
};

describe('useGetComponentConnectionsQuery', () => {
    beforeEach(() => {
        getFrontendConnectionsMock.mockReset();
        getFrontendConnectionsMock.mockResolvedValue([]);

        useAutomationHubStore.setState({
            connectionDialogAllowed: true,
            includeComponents: undefined,
            initialized: true,
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });
    });

    it('asks for the connections of the given component only', async () => {
        renderConnectionsHook('slack');

        await waitFor(() => expect(getFrontendConnectionsMock).toHaveBeenCalledWith({componentName: 'slack'}));
    });
});
