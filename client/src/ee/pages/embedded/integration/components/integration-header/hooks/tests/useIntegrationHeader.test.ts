import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useIntegrationHeader} from '../useIntegrationHeader';

const hoisted = vi.hoisted(() => ({
    integrationQuery: {data: {id: 5}} as {data: object},
    integrationVersionWorkflowsQuery: {data: undefined} as {data: object[] | undefined},
    integrationVersionWorkflowsQueryArguments: [] as unknown[],
    integrationWorkflowsQuery: {data: []} as {data: object[]},
    navigate: vi.fn(),
    searchParams: new URLSearchParams(''),
}));

vi.mock('react-router-dom', () => ({
    useLoaderData: () => undefined,
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [hoisted.searchParams],
}));

vi.mock('@/ee/shared/queries/embedded/integrations.queries', () => ({
    IntegrationKeys: {
        filteredIntegrations: (filters: object) => ['filteredIntegrations', filters],
        integration: (id: number) => ['integration', id],
    },
    useGetIntegrationQuery: () => hoisted.integrationQuery,
    useGetIntegrationsQuery: () => ({
        data: [
            {id: 5, integrationWorkflowIds: [7]},
            {id: 6, integrationWorkflowIds: [61, 62]},
            {id: 8, integrationWorkflowIds: []},
        ],
    }),
}));

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: (...queryArguments: unknown[]) => {
        hoisted.integrationVersionWorkflowsQueryArguments = queryArguments;

        return hoisted.integrationVersionWorkflowsQuery;
    },
    useGetIntegrationWorkflowsQuery: () => hoisted.integrationWorkflowsQuery,
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
        hoisted.integrationQuery = {data: {id: 5}};
        hoisted.integrationVersionWorkflowsQuery = {data: undefined};
        hoisted.integrationVersionWorkflowsQueryArguments = [];
        hoisted.integrationWorkflowsQuery = {data: []};
        hoisted.navigate.mockReset();
        hoisted.searchParams = new URLSearchParams('');

        useWorkflowEditorStore.setState({
            showBottomPanel: false,
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

    it('clears the test output, the node states, the selected node and the bottom panel when switching to another workflow', () => {
        const bottomResizablePanelResize = vi.fn();
        const finishedExecution = {job: {id: 'job-1'}} as WorkflowTestExecution;

        useWorkflowEditorStore.setState({
            showBottomPanel: true,
            workflowTestExecution: finishedExecution,
            workflowTestNodeStates: {aiAgent_1: {status: 'COMPLETED'}},
        });
        useWorkflowNodeDetailsPanelStore.setState({
            currentNode: {name: 'aiAgent_1'} as ReturnType<
                typeof useWorkflowNodeDetailsPanelStore.getState
            >['currentNode'],
        });

        const {result} = renderHook(
            () =>
                useIntegrationHeader({
                    bottomResizablePanelRef: {current: {resize: bottomResizablePanelResize} as never},
                    integrationId: 5,
                }),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationWorkflowValueChange(7);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/integrations/5/integration-workflows/7?');
        expect(useWorkflowEditorStore.getState().workflowTestExecution).toBeUndefined();
        expect(useWorkflowEditorStore.getState().workflowTestNodeStates).toEqual({});
        expect(useWorkflowEditorStore.getState().showBottomPanel).toBe(false);
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBeUndefined();
        expect(bottomResizablePanelResize).toHaveBeenCalledWith(0);
    });

    it('keeps other search params but drops the sub-workflow ones when switching to another workflow', () => {
        hoisted.searchParams = new URLSearchParams(
            'environment=2&fromSubflow=true&parentChain=a&parentProjectWorkflowId=3&restoreExecutionPanel=true'
        );

        const {result} = renderHook(
            () => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleIntegrationValueChange(6);
        });

        expect(hoisted.navigate).toHaveBeenCalledWith(
            '/embedded/integrations/6/integration-workflows/61?environment=2'
        );
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
    const renderIntegrationHeaderHook = () =>
        renderHook(() => useIntegrationHeader({bottomResizablePanelRef: {current: null}, integrationId: 5}), {
            wrapper: createTestQueryClientWrapper(),
        });

    it('reports unpublished changes when the integration has never been published', () => {
        hoisted.integrationWorkflowsQuery = {data: [{version: 1}]};

        const {result} = renderIntegrationHeaderHook();

        expect(result.current.hasUnpublishedChanges).toBe(true);
    });

    it('reports no unpublished changes when every workflow is still at the version set by publishing', () => {
        hoisted.integrationQuery = {data: {id: 5, lastPublishedDate: new Date()}};
        hoisted.integrationWorkflowsQuery = {data: [{version: 1}, {version: 1}]};

        const {result} = renderIntegrationHeaderHook();

        expect(result.current.hasUnpublishedChanges).toBe(false);
    });

    it('reports unpublished changes when any workflow was edited after publishing', () => {
        hoisted.integrationQuery = {data: {id: 5, lastPublishedDate: new Date()}};
        hoisted.integrationWorkflowsQuery = {data: [{version: 1}, {version: 2}]};

        const {result} = renderIntegrationHeaderHook();

        expect(result.current.hasUnpublishedChanges).toBe(true);
    });

    it('requests the workflows of the version before the draft once the integration has been published', () => {
        hoisted.integrationQuery = {data: {id: 5, lastIntegrationVersion: 4, lastPublishedDate: new Date()}};

        renderIntegrationHeaderHook();

        expect(hoisted.integrationVersionWorkflowsQueryArguments).toEqual([5, 3, false, true]);
    });

    it('reports no unpublished changes when the draft has the same workflows as the last published version', () => {
        hoisted.integrationQuery = {data: {id: 5, lastIntegrationVersion: 4, lastPublishedDate: new Date()}};
        hoisted.integrationVersionWorkflowsQuery = {data: [{workflowUuid: 'a'}, {workflowUuid: 'b'}]};
        hoisted.integrationWorkflowsQuery = {
            data: [
                {version: 1, workflowUuid: 'b'},
                {version: 1, workflowUuid: 'a'},
            ],
        };

        const {result} = renderIntegrationHeaderHook();

        expect(result.current.hasUnpublishedChanges).toBe(false);
    });

    it('reports unpublished changes when a workflow was deleted after publishing', () => {
        hoisted.integrationQuery = {data: {id: 5, lastIntegrationVersion: 4, lastPublishedDate: new Date()}};
        hoisted.integrationVersionWorkflowsQuery = {data: [{workflowUuid: 'a'}, {workflowUuid: 'b'}]};
        hoisted.integrationWorkflowsQuery = {data: [{version: 1, workflowUuid: 'a'}]};

        const {result} = renderIntegrationHeaderHook();

        expect(result.current.hasUnpublishedChanges).toBe(true);
    });
});
