import {useAutomationHubStore} from '@/ee/pages/embedded/automation-hub/stores/useAutomationHubStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useGetComponentConnectionsQuery} from '../queries/automationHub.queries';

// vi.mock factories hoist above module-scope `const`s, so refs they close over must come from
// vi.hoisted — see CLAUDE.md's Vitest mock factory hoisting note.
const {getFrontendConnectionsMock} = vi.hoisted(() => ({getFrontendConnectionsMock: vi.fn()}));

vi.mock('@/ee/shared/middleware/embedded/public', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/ee/shared/middleware/embedded/public')>();

    // An arrow function is not a constructor, and the generated client is instantiated with
    // `new` — hence the function expression.
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
            sharedConnectionIds: [],
            tabs: {automations: true, connections: true, newWorkflow: true},
            theme: {},
        });
    });

    it('forwards the vendor-shared connection ids so shared connections are selectable', async () => {
        useAutomationHubStore.setState({sharedConnectionIds: [11, 12]});

        renderConnectionsHook('slack');

        // The server unions these onto the connected user's own connections and then filters by
        // component, so a shared connection is only ever offered when the ids ride along.
        await waitFor(() =>
            expect(getFrontendConnectionsMock).toHaveBeenCalledWith({componentName: 'slack', connectionIds: [11, 12]})
        );
    });

    it('omits connectionIds entirely when the vendor shared none', async () => {
        renderConnectionsHook('slack');

        await waitFor(() => expect(getFrontendConnectionsMock).toHaveBeenCalledTimes(1));

        // Not an empty array: the generated client emits any non-null value as a query parameter,
        // so `[]` would put a meaningless `connectionIds=` on every request.
        const [request] = getFrontendConnectionsMock.mock.calls[0];

        expect(request.connectionIds).toBeUndefined();
    });
});
