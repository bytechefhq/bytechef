import {McpServer} from '@/shared/middleware/graphql';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServerListItem from '../useMcpServerListItem';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    updateTagsOnSuccess: undefined as undefined | (() => void),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteWorkspaceMcpServerMutation: () => ({mutate: vi.fn()}),
    useUpdateMcpServerMutation: () => ({mutate: vi.fn()}),
    useUpdateMcpServerTagsMutation: ({onSuccess}: {onSuccess: () => void}) => {
        hoisted.updateTagsOnSuccess = onSuccess;

        return {mutate: vi.fn()};
    },
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

const invalidatedKeys = () => hoisted.invalidateQueries.mock.calls.map(([argument]) => argument.queryKey[0]);

describe('useMcpServerListItem', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockClear();
    });

    it('invalidates the workspace server list and tag queries after a tag update', () => {
        renderHook(() => useMcpServerListItem({id: '1', name: 'Server', tags: []} as unknown as McpServer));

        hoisted.updateTagsOnSuccess?.();

        expect(invalidatedKeys()).toEqual(['workspaceMcpServers', 'workspaceMcpServerTags']);
    });
});
