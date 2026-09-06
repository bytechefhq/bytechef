import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpComponentDialog from '../useMcpComponentDialog';

const hoisted = vi.hoisted(() => ({
    createOnSuccess: undefined as undefined | (() => void),
    invalidateQueries: vi.fn(),
    updateOnSuccess: undefined as undefined | (() => void),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.createOnSuccess = onSuccess;

        return {mutate: vi.fn()};
    },
    useMcpToolsByComponentIdQuery: () => ({data: undefined}),
    useUpdateMcpComponentWithToolsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.updateOnSuccess = onSuccess;

        return {mutate: vi.fn()};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const invalidatedKeys = () => hoisted.invalidateQueries.mock.calls.map(([argument]) => argument.queryKey[0]);

describe('useMcpComponentDialog', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockClear();
    });

    // The server list is served by useWorkspaceMcpServersQuery and the per-server component count is read off that
    // payload, so a save that only invalidated 'mcpServers' left the count stale until a page reload.
    it('invalidates the workspace servers query after creating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.createOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });

    it('invalidates the workspace servers query after updating a component', () => {
        renderHook(() => useMcpComponentDialog({mcpServerId: '1', open: true}));

        hoisted.updateOnSuccess?.();

        expect(invalidatedKeys()).toContain('workspaceMcpServers');
    });
});
