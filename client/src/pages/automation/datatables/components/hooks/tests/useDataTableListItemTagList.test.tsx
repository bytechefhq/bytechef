import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useDataTableListItemTagList from '../useDataTableListItemTagList';

interface TagsByTableI {
    dataTableTagsByTable: {tableId: string; tags: {id?: string; name: string}[]}[];
}

interface MutationCallbacksI {
    onError: (error: Error, variables: unknown, context: {previous?: TagsByTableI}) => void;
    onMutate: (variables: {
        input: {tableId: string; tags: {id?: string; name: string}[]};
    }) => Promise<{previous?: TagsByTableI}>;
}

const {useUpdateDataTableTagsMutationMock} = vi.hoisted(() => ({
    useUpdateDataTableTagsMutationMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useUpdateDataTableTagsMutation: useUpdateDataTableTagsMutationMock,
}));

const CURRENT_WORKSPACE_KEY = ['dataTableTagsByTable', {workspaceId: '7'}];
const OTHER_WORKSPACE_KEY = ['dataTableTagsByTable', {workspaceId: '8'}];

let queryClient: QueryClient;

function Wrapper({children}: {children: ReactNode}) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}

function renderMutationCallbacks(tableId: string): MutationCallbacksI {
    renderHook(() => useDataTableListItemTagList({tableId}), {wrapper: Wrapper});

    return useUpdateDataTableTagsMutationMock.mock.calls[0][0] as MutationCallbacksI;
}

describe('useDataTableListItemTagList', () => {
    beforeEach(() => {
        useUpdateDataTableTagsMutationMock.mockReset();

        queryClient = new QueryClient();

        useWorkspaceStore.setState({currentWorkspaceId: 7});
    });

    it('replaces the table tags in the current workspace cache entry only', async () => {
        const otherWorkspaceTagsByTable = {dataTableTagsByTable: [{tableId: 'orders', tags: [{id: '9', name: 'x'}]}]};

        queryClient.setQueryData(CURRENT_WORKSPACE_KEY, {
            dataTableTagsByTable: [
                {tableId: 'orders', tags: [{id: '1', name: 'old'}]},
                {tableId: 'invoices', tags: [{id: '3', name: 'finance'}]},
            ],
        });
        queryClient.setQueryData(OTHER_WORKSPACE_KEY, otherWorkspaceTagsByTable);

        const mutationCallbacks = renderMutationCallbacks('orders');

        const context = await mutationCallbacks.onMutate({
            input: {tableId: 'orders', tags: [{id: '2', name: 'billing'}]},
        });

        expect(queryClient.getQueryData(CURRENT_WORKSPACE_KEY)).toEqual({
            dataTableTagsByTable: [
                {tableId: 'orders', tags: [{id: '2', name: 'billing'}]},
                {tableId: 'invoices', tags: [{id: '3', name: 'finance'}]},
            ],
        });
        expect(queryClient.getQueryData(OTHER_WORKSPACE_KEY)).toEqual(otherWorkspaceTagsByTable);
        expect(context.previous?.dataTableTagsByTable[0].tags).toEqual([{id: '1', name: 'old'}]);
    });

    it('adds an entry for a table that had no tags', async () => {
        queryClient.setQueryData(CURRENT_WORKSPACE_KEY, {dataTableTagsByTable: []});

        const mutationCallbacks = renderMutationCallbacks('orders');

        await mutationCallbacks.onMutate({input: {tableId: 'orders', tags: [{id: '2', name: 'billing'}]}});

        expect(queryClient.getQueryData(CURRENT_WORKSPACE_KEY)).toEqual({
            dataTableTagsByTable: [{tableId: 'orders', tags: [{id: '2', name: 'billing'}]}],
        });
    });

    it('restores the current workspace cache entry when the update fails', async () => {
        const previous = {dataTableTagsByTable: [{tableId: 'orders', tags: [{id: '1', name: 'old'}]}]};

        queryClient.setQueryData(CURRENT_WORKSPACE_KEY, previous);

        const mutationCallbacks = renderMutationCallbacks('orders');

        const context = await mutationCallbacks.onMutate({
            input: {tableId: 'orders', tags: [{id: '2', name: 'billing'}]},
        });

        mutationCallbacks.onError(new Error('failed'), undefined, context);

        expect(queryClient.getQueryData(CURRENT_WORKSPACE_KEY)).toEqual(previous);
    });
});
