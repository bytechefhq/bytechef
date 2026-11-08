import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useIntegration} from '../useIntegration';

const hoisted = vi.hoisted(() => ({
    integrationWorkflowId: '7',
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
    useParams: () => ({integrationId: '5', integrationWorkflowId: hoisted.integrationWorkflowId}),
    useSearchParams: () => [new URLSearchParams('')],
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    IntegrationWorkflowKeys: {integrationWorkflows: (id: number) => ['integrationWorkflows', id]},
    useGetIntegrationWorkflowQuery: () => ({data: undefined, isLoading: true}),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    useGetConnectionsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/mutations/embedded/workflows.mutations', () => ({
    useUpdateWorkflowMutation: () => ({}),
}));

vi.mock('@/shared/mutations/platform/workflowNodeParameters.mutations', () => ({
    useDeleteClusterElementParameterMutation: () => ({}),
    useDeleteWorkflowNodeParameterMutation: () => ({}),
    useUpdateClusterElementParameterMutation: () => ({}),
    useUpdateWorkflowNodeParameterMutation: () => ({}),
}));

vi.mock('@/shared/mutations/platform/workflows.mutations', () => ({
    default: () => ({}),
}));

describe('useIntegration', () => {
    beforeEach(() => {
        hoisted.integrationWorkflowId = '7';

        useWorkflowEditorStore.setState({
            workflowIsRunning: false,
            workflowTestExecution: undefined,
            workflowTestNodeStates: {},
        });
    });

    it('clears the test output and the node states of the previous workflow once another workflow opens', () => {
        const {rerender} = renderHook(() => useIntegration(), {wrapper: createTestQueryClientWrapper()});

        act(() => {
            useWorkflowEditorStore.setState({
                workflowTestExecution: {job: {id: 'job-1'}} as WorkflowTestExecution,
                workflowTestNodeStates: {aiAgent_1: {status: 'COMPLETED'}},
            });
        });

        hoisted.integrationWorkflowId = '8';

        rerender();

        expect(useWorkflowEditorStore.getState().workflowTestExecution).toBeUndefined();
        expect(useWorkflowEditorStore.getState().workflowTestNodeStates).toEqual({});
    });
});
