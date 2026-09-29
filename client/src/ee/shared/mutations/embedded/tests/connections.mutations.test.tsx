import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {getCreateConnectedUserConnection} from '../connections.mutations';

const {createConnectedUserConnectionMock} = vi.hoisted(() => ({
    createConnectedUserConnectionMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/configuration', () => ({
    ConnectionApi: class {
        createConnectedUserConnection = createConnectedUserConnectionMock;
    },
}));

describe('connections mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it("leaves the connected user's connection environment to the server", async () => {
        createConnectedUserConnectionMock.mockResolvedValue(5);

        const useCreateConnectedUserConnectionMutation = getCreateConnectedUserConnection(7);

        const {result} = renderHook(() => useCreateConnectedUserConnectionMutation(), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({
            componentName: 'slack',
            connectionVersion: 1,
            environmentId: 0,
            name: 'Slack',
            parameters: {},
        });

        await waitFor(() =>
            expect(createConnectedUserConnectionMock).toHaveBeenCalledWith({
                connectedUserId: 7,
                connection: {
                    componentName: 'slack',
                    connectionVersion: 1,
                    environmentId: undefined,
                    name: 'Slack',
                    parameters: {},
                },
            })
        );
    });
});
