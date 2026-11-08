import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {createTestQueryClientWrapper} from '@/shared/util/test-utils';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useAutomationWorkflowEditorHeader} from '../useAutomationWorkflowEditorHeader';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    publishMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAutomationWorkflowProjectsQuery: () => ({
        data: {automationWorkflowProjects: [{id: 'project-1', name: 'Mailing', workflowTemplates: []}]},
    }),
    usePublishAutomationWorkflowProjectMutation: () => ({isPending: false, mutate: hoisted.publishMutate}),
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
}));

describe('useAutomationWorkflowEditorHeader', () => {
    beforeEach(() => {
        hoisted.navigate.mockReset();
        hoisted.publishMutate.mockReset();

        useWorkflowEditorStore.setState({
            workflowIsRunning: false,
            workflowTestExecution: undefined,
            workflowTestNodeStates: {},
        });
    });

    it('publishes the project with the description entered in the publish popover', () => {
        const onSuccess = vi.fn();

        const {result} = renderHook(
            () => useAutomationWorkflowEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
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
            () => useAutomationWorkflowEditorHeader({bottomResizablePanelRef: {current: null}, projectId: 'project-1'}),
            {wrapper: createTestQueryClientWrapper()}
        );

        act(() => {
            result.current.handleWorkflowValueChange('workflow-2');
        });

        expect(hoisted.navigate).toHaveBeenCalledWith('/embedded/automation-workflows/workflow-2/editor');
        expect(useWorkflowEditorStore.getState().workflowTestExecution).toBe(runningExecution);
        expect(useWorkflowEditorStore.getState().workflowTestNodeStates).toEqual({aiAgent_1: {status: 'RUNNING'}});
        expect(useWorkflowNodeDetailsPanelStore.getState().currentNode).toBe(currentNode);
    });
});
