import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAutomationEditorHeader} from '../useAutomationEditorHeader';

const hoisted = vi.hoisted(() => ({
    draftWorkflowChangesQuery: {data: undefined, refetch: vi.fn()} as {
        data: {automationProjectVersionWorkflowChanges: {workflows: object[]}} | undefined;
        refetch: ReturnType<typeof vi.fn>;
    },
    draftWorkflowChangesQueryArguments: [] as unknown[],
    isMutating: vi.fn(() => 0),
    navigate: vi.fn(),
    publishMutate: vi.fn(),
    searchParams: new URLSearchParams(''),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useIsMutating: () => hoisted.isMutating(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationProjectVersionWorkflowChangesQuery: (...queryArguments: unknown[]) => {
        hoisted.draftWorkflowChangesQueryArguments = queryArguments;

        return hoisted.draftWorkflowChangesQuery;
    },
    useAutomationProjectsQuery: () => ({
        data: {
            automationProjects: [
                {id: 'project-1', name: 'Mailing', workflowTemplates: [{workflowUuid: 'workflow-1'}]},
                {id: 'project-4', name: 'Published', published: true, version: 3, workflowTemplates: []},
                {
                    id: 'project-2',
                    name: 'Billing',
                    workflowTemplates: [{workflowUuid: 'workflow-21'}, {workflowUuid: 'workflow-22'}],
                },
                {id: 'project-3', name: 'Empty', workflowTemplates: []},
            ],
        },
    }),
    usePublishAutomationProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutate}),
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
    useAnalytics: () => ({captureProjectWorkflowTested: vi.fn()}),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => hoisted.navigate,
    useSearchParams: () => [hoisted.searchParams],
}));

describe('useAutomationEditorHeader', () => {
    beforeEach(() => {
        hoisted.draftWorkflowChangesQuery = {data: undefined, refetch: vi.fn()};
        hoisted.draftWorkflowChangesQueryArguments = [];
        hoisted.isMutating.mockReturnValue(0);
        hoisted.navigate.mockReset();
        hoisted.publishMutate.mockReset();
        hoisted.searchParams = new URLSearchParams('');

        useWorkflowEditorStore.setState({
            showBottomPanel: false,
            workflowIsRunning: false,
            workflowTestExecution: undefined,
            workflowTestNodeStates: {},
        });
    });

    it('publishes the project with the description entered in the publish popover', () => {
        const onSuccess = vi.fn();

        const {result} = renderHook(
            () => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        result.current.handlePublishProjectSubmit({description: 'Adds greeting', onSuccess});

        expect(hoisted.publishMutate).toHaveBeenCalledWith(
            {description: 'Adds greeting', id: 'project-1'},
            {onSuccess}
        );
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
            () => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleWorkflowValueChange('workflow-2');
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
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
                useAutomationEditorHeader({
                    bottomResizablePanelRef: {current: {resize: bottomResizablePanelResize} as never},
                    projectId: 'project-1',
                }),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleWorkflowValueChange('workflow-2');
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor');
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
            () => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleWorkflowValueChange('workflow-2');
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-2/editor?environment=2');
    });

    it('opens the first workflow of the selected project', () => {
        const {result} = renderHook(
            () => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleProjectValueChange('project-2');
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automations/workflow-21/editor');
    });

    it('stays on the current workflow when the current project or a project without workflows is selected', () => {
        const {result} = renderHook(
            () => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleProjectValueChange('project-1');
            result.current.handleProjectValueChange('project-3');
        });

        expect(hoisted.navigate).not.toHaveBeenCalled();
    });
    const renderAutomationEditorHeaderHook = (projectId: string) =>
        renderHook(() => useAutomationEditorHeader({bottomResizablePanelRef: {current: null}, projectId}), {
            wrapper: createTestQueryClientWrapper(),
        });

    it('reports unpublished changes and skips the draft changes query when the project was never published', () => {
        const {result} = renderAutomationEditorHeaderHook('project-1');

        expect(result.current.hasUnpublishedChanges).toBe(true);
        expect(hoisted.draftWorkflowChangesQueryArguments[1]).toEqual({enabled: false});
    });

    it('asks for the workflow changes of the draft version of a published project', () => {
        renderAutomationEditorHeaderHook('project-4');

        expect(hoisted.draftWorkflowChangesQueryArguments).toEqual([
            {id: 'project-4', projectVersion: 3},
            {enabled: true},
        ]);
    });

    it('reports no unpublished changes when the draft has no workflow changes', () => {
        hoisted.draftWorkflowChangesQuery = {
            data: {automationProjectVersionWorkflowChanges: {workflows: []}},
            refetch: vi.fn(),
        };

        const {result} = renderAutomationEditorHeaderHook('project-4');

        expect(result.current.hasUnpublishedChanges).toBe(false);
    });

    it('reports unpublished changes when the draft has workflow changes', () => {
        hoisted.draftWorkflowChangesQuery = {
            data: {automationProjectVersionWorkflowChanges: {workflows: [{changeType: 'MODIFIED'}]}},
            refetch: vi.fn(),
        };

        const {result} = renderAutomationEditorHeaderHook('project-4');

        expect(result.current.hasUnpublishedChanges).toBe(true);
    });

    it('checks the draft for changes again once pending saves finish', () => {
        hoisted.isMutating.mockReturnValue(1);

        const {rerender} = renderAutomationEditorHeaderHook('project-4');

        expect(hoisted.draftWorkflowChangesQuery.refetch).not.toHaveBeenCalled();

        hoisted.isMutating.mockReturnValue(0);

        rerender();

        expect(hoisted.draftWorkflowChangesQuery.refetch).toHaveBeenCalledTimes(1);
    });
});
