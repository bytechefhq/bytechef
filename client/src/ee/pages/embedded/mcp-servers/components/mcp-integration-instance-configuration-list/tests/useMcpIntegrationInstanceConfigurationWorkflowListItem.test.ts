import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {McpIntegrationInstanceConfigurationWorkflowItemType} from '../hooks/useMcpIntegrationInstanceConfigurationList';
import useMcpIntegrationInstanceConfigurationWorkflowListItem from '../hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    invalidateQueries: vi.fn(),
    updateMutate: vi.fn(),
    updateOptions: {} as {onSuccess?: () => void},
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteMcpIntegrationInstanceConfigurationWorkflowMutation: () => ({
        isPending: false,
        mutate: hoisted.deleteMutate,
    }),
    useUpdateMcpIntegrationInstanceConfigurationWorkflowMutation: (options: {onSuccess?: () => void}) => {
        hoisted.updateOptions = options;

        return {isPending: false, mutate: hoisted.updateMutate};
    },
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({
        data: {integrationInstanceConfigurationWorkflows: [{id: 4, workflowId: 'workflow-a'}]},
    }),
}));

vi.mock('@/ee/shared/queries/embedded/workflows.queries', () => ({
    useGetWorkflowQuery: (id: string) => ({data: {id, label: 'Sync contacts'}}),
}));

const mcpIntegrationInstanceConfigurationWorkflow = {
    id: '9',
    integrationInstanceConfigurationWorkflow: {integrationInstanceConfigurationId: '44'},
    integrationInstanceConfigurationWorkflowId: 4,
} as unknown as McpIntegrationInstanceConfigurationWorkflowItemType;

describe('useMcpIntegrationInstanceConfigurationWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.deleteMutate.mockReset();
        hoisted.invalidateQueries.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('switches the workflow tool through the update mutation and refreshes the server list', () => {
        const {result} = renderHook(() =>
            useMcpIntegrationInstanceConfigurationWorkflowListItem(mcpIntegrationInstanceConfigurationWorkflow)
        );

        act(() => result.current.handleEnabledChange(false));

        expect(hoisted.updateMutate).toHaveBeenCalledWith({id: '9', input: {enabled: false}});

        hoisted.updateOptions.onSuccess?.();

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({
            queryKey: ['mcpIntegrationInstanceConfigurationsByServerId'],
        });
        expect(result.current.isEnablePending).toBe(false);
    });

    it('resolves the deployment workflow and the workflow of the tool', () => {
        const {result} = renderHook(() =>
            useMcpIntegrationInstanceConfigurationWorkflowListItem(mcpIntegrationInstanceConfigurationWorkflow)
        );

        expect(result.current.integrationInstanceConfigurationWorkflow).toEqual({id: 4, workflowId: 'workflow-a'});
        expect(result.current.workflow).toEqual({id: 'workflow-a', label: 'Sync contacts'});
    });
});
