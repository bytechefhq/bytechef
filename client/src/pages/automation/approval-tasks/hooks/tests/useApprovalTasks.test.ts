import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useApprovalTaskDetail} from '../../components/hooks/useApprovalTaskDetail';
import {useApprovalTasksStore} from '../../stores/useApprovalTasksStore';
import {useApprovalTasks} from '../useApprovalTasks';

const hoisted = vi.hoisted(() => ({
    account: {activated: true, email: 'jane@example.com', firstName: 'Jane', id: 7, lastName: 'Doe'},
    approvalTasksData: {
        approvalTasks: [{assigneeId: '7', id: '1', name: 'Approve invoice', priority: 'HIGH', status: 'OPEN'}],
    },
    isTenantAdmin: true,
    useUsersQuery: vi.fn(),
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: (selector: (state: {account: typeof hoisted.account}) => unknown) =>
        selector({account: hoisted.account}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useApprovalTasksQuery: () => ({data: hoisted.approvalTasksData}),
    useUpdateApprovalTaskMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUsersQuery: hoisted.useUsersQuery,
}));

const usersData = {
    users: {
        content: [
            {activated: true, email: 'listed@example.com', firstName: 'Listed', id: '7', lastName: 'User'},
            {activated: true, email: 'other@example.com', firstName: 'Other', id: '8', lastName: 'User'},
        ],
    },
};

describe('useApprovalTasks', () => {
    beforeEach(() => {
        hoisted.isTenantAdmin = true;

        hoisted.useUsersQuery.mockReset();
        hoisted.useUsersQuery.mockImplementation((_variables: unknown, options?: {enabled?: boolean}) => ({
            data: options?.enabled === false ? undefined : usersData,
        }));

        useApprovalTasksStore.setState({approvalTasks: [], selectedApprovalTaskId: null});
    });

    it('resolves assignees from every user for a tenant admin', () => {
        const {result} = renderHook(() => useApprovalTasks(), {wrapper: createTestQueryClientWrapper()});

        expect(useApprovalTasksStore.getState().approvalTasks[0].assignee).toBe('Listed User');
        expect(result.current.availableAssigneeOptions).toEqual([
            {id: '7', name: 'Listed User'},
            {id: '8', name: 'Other User'},
        ]);
    });

    it('shows the current user as the assignee without reading every user for a user who is not a tenant admin', () => {
        hoisted.isTenantAdmin = false;

        const {result} = renderHook(() => useApprovalTasks(), {wrapper: createTestQueryClientWrapper()});

        expect(hoisted.useUsersQuery).not.toHaveBeenCalledWith(undefined, {enabled: true});
        expect(hoisted.useUsersQuery).toHaveBeenCalledWith(undefined, {enabled: false});
        expect(useApprovalTasksStore.getState().approvalTasks[0].assignee).toBe('Jane Doe');
        expect(result.current.availableAssignees).toEqual(['Jane Doe']);
        expect(result.current.availableAssigneeOptions).toEqual([{id: '7', name: 'Jane Doe'}]);
    });

    it('offers only the current user as an assignee in the task detail for a user who is not a tenant admin', () => {
        hoisted.isTenantAdmin = false;

        renderHook(() => useApprovalTasks(), {wrapper: createTestQueryClientWrapper()});

        act(() => {
            useApprovalTasksStore.getState().setSelectedApprovalTaskId('1');
        });

        const {result} = renderHook(() => useApprovalTaskDetail(), {wrapper: createTestQueryClientWrapper()});

        expect(hoisted.useUsersQuery).toHaveBeenCalledWith(undefined, {enabled: false});
        expect(result.current.availableAssigneeOptions).toEqual([{id: '7', name: 'Jane Doe'}]);
    });
});
