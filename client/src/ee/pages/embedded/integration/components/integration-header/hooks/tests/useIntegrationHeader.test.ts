import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useIntegrationHeader} from '../useIntegrationHeader';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useLoaderData: () => undefined,
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [new URLSearchParams('')],
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {
        filteredIntegrations: (filters: object) => ['filteredIntegrations', filters],
        integration: (id: number) => ['integration', id],
    },
    useGetIntegrationQuery: () => ({data: {id: 5}}),
    useGetIntegrationsQuery: () => ({
        data: [
            {id: 5, integrationWorkflowIds: [7]},
            {id: 6, integrationWorkflowIds: [61, 62]},
            {id: 8, integrationWorkflowIds: []},
        ],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrations.mutations', () => ({
    usePublishIntegrationMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/shared/hooks/useWorkflowTestStream', () => ({
    useWorkflowTestStream: () => ({
        close: vi.fn(),
        error: null,
        getPersistedJobId: vi.fn(),
        persistJobId: vi.fn(),
        setStreamRequest: vi.fn(),
    }),
}));

vi.mock('@/shared/hooks/useAnalytics', () => ({
    useAnalytics: () => ({captureIntegrationPublished: vi.fn(), captureIntegrationWorkflowTested: vi.fn()}),
}));

describe('useIntegrationHeader', () => {
    beforeEach(() => {
        hoisted.navigate.mockReset();

        useWorkflowEditorStore.setState({
            workflowIsRunning: false,
            workflowTestExecution: undefined,
            workflowTestNodeStates: {},
        });
    });

    it('opens the selected integration workflow', () => {
        const {result} = renderHook(
            () => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationWorkflowValueChange(7);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/7?');
    });

    it('keeps the output and the node states of a running test run when switching to another workflow', () => {
        const runningExecution = {job: {id: 'job-1'}} as WorkflowTestExecution;
        const currentNode = {name: 'aiAgent_1'} as ReturnType<
            typeof useWorkflowNodeDetailsPanelStore.getState
        >['currentNode'];

        useWorkflowEditorStore.setState({
            workflowIsRunning: true,
            workflowTestExecution: runningExecution,
            workflowTestNodeStates: {aiAgent_1: {status: 'RUNNING'}},
        });
        useWorkflowNodeDetailsPanelStore.setState({currentNode});

        const {result} = renderHook(
            () => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationWorkflowValueChange(7);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/7?');
        expect(useWorkflowEditorStore.getState().workflowTestExecution).toBe(runningExecution);
        expect(useWorkflowEditorStore.getState().workflowTestNodeStates).toEqual({aiAgent_1: {status: 'RUNNING'}});
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBe(currentNode);
    });

    it('opens the first workflow of the selected integration', () => {
        const {result} = renderHook(
            () => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationValueChange(6);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/6/integration-workflows/61?');
    });

    it('stays on the current workflow when the current integration or one without workflows is selected', () => {
        const {result} = renderHook(
            () => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationValueChange(5);
            result.current.handleIntegrationValueChange(8);
        });

        expect(hoisted.navigate).not.toHaveBeenCalled();
    });
});
