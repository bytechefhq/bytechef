import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/ee/shared/queries/embedded/workflowExecutions.queries', () => ({
    useGetIntegrationWorkflowExecutionQuery: executionQueryMock,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/stores/useWorkflowExecutionSheetStore', () => ({
    default: (selector: (state: unknown) => unknown) =>
        selector({
            setWorkflowExecutionSheetOpen: vi.fn(),
            workflowExecutionId: 501,
            workflowExecutionSheetOpen: true,
        }),
}));

vi.mock('@/shared/components/workflow-executions/util/workflowExecution-utils', () => ({
    getWorkflowStatusType: () => 'completed',
}));

const useWorkflowExecutionSheet = (await import('../useWorkflowExecutionSheet')).default;

const workflowExecutionMock = {
    id: 501,
    integration: {componentName: 'slack', multipleInstances: false},
    integrationInstance: {},
    integrationInstanceConfiguration: {environmentId: 7},
    job: {
        priority: 0,
        startDate: new Date(),
        status: 'COMPLETED',
        workflowId: 'wf-embedded-1',
    },
    workflow: {label: 'My workflow'},
};

describe('useWorkflowExecutionSheet', () => {
    beforeEach(() => {
        executionQueryMock.mockReset().mockReturnValue({data: workflowExecutionMock, isLoading: false});

        useWorkspaceStore.setState({currentWorkspaceId: 2024});

        applicationInfoStore.setState((state) => ({...state, ai: {...state.ai, copilot: {enabled: true}}}));

        useCopilotStore.setState({
            context: {
                mode: MODE.ASK,
                parameters: {},
                source: Source.WORKFLOW_EDITOR,
                workflowExecutionError: undefined,
            },
            conversationStack: [],
        });
    });

    it('exposes copilotEnabled from the application info store', () => {
        const {result} = renderHook(() => useWorkflowExecutionSheet());

        expect(result.current.copilotEnabled).toBe(true);
    });

    it('sets the copilot context with the embedded source and execution parameters on open', () => {
        const {result} = renderHook(() => useWorkflowExecutionSheet());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(result.current.copilotPanelOpen).toBe(true);
        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {
                environmentId: 7,
                workflowExecutionId: 501,
                workflowId: 'wf-embedded-1',
                workspaceId: 2024,
            },
            source: Source.WORKFLOW_EXECUTION_EMBEDDED,
            workflowExecutionError: undefined,
        });
    });

    it('restores the saved conversation and closes the panel on close', () => {
        const {result} = renderHook(() => useWorkflowExecutionSheet());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(result.current.copilotPanelOpen).toBe(true);

        act(() => {
            result.current.handleCopilotClose();
        });

        expect(result.current.copilotPanelOpen).toBe(false);
        expect(useCopilotStore.getState().context.source).toBe(Source.WORKFLOW_EDITOR);
    });
});
