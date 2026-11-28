import WorkflowBuilder from '@/ee/pages/embedded/workflow-builder/WorkflowBuilder';
import {ConnectedUserProjectWorkflowKeys} from '@/ee/shared/queries/embedded/connectedUserProjectWorkflows.queries';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {copilotPanelMock, editorConnectedUserWorkflowUuidMock, editorIncludeComponentsMock, workflowEditorLayoutMock} =
    vi.hoisted(() => ({
        copilotPanelMock: vi.fn(),
        editorConnectedUserWorkflowUuidMock: vi.fn(),
        editorIncludeComponentsMock: vi.fn(),
        workflowEditorLayoutMock: vi.fn(),
    }));

vi.mock('@/shared/components/copilot/CopilotPanel', () => ({
    default: (props: {open: boolean}) => {
        copilotPanelMock(props);

        return <div data-testid="copilot-panel" />;
    },
}));

vi.mock('@/ee/pages/embedded/workflow-builder/hooks/useWorkflowBuilder', () => ({
    useWorkflowBuilder: () => ({
        bottomResizablePanelRef: {current: null},
        cancelWorkflowQueries: vi.fn(),
        connectedUserProjectWorkflow: {connectedUserId: 1, workflowVersion: 1},
        includeComponents: ['slack'],
        initialized: true,
        invalidateWorkflowQueries: vi.fn(),
        projectId: 2,
    }),
}));

vi.mock('@/pages/platform/workflow-editor/hooks/useRun', () => ({
    useRun: () => ({runDisabled: false, runDisabledReasons: []}),
}));

vi.mock('@/ee/pages/embedded/workflow-builder/components/workflow-builder-header/WorkflowBuilderHeader', () => ({
    default: () => null,
}));

vi.mock('@/ee/shared/mutations/embedded/connections.mutations', () => ({
    getCreateConnectedUserConnection: () => vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connections.queries', () => ({
    ConnectionKeys: {},
    getConnectedUserConnectionsQuery: () => vi.fn(),
    useGetConnectionTagsQuery: vi.fn(),
}));

vi.mock('@/components/ui/resizable', () => ({
    ResizableHandle: () => null,
    ResizablePanel: ({children}: {children?: ReactNode}) => <div>{children}</div>,
    ResizablePanelGroup: ({children}: {children?: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/pages/platform/workflow-editor/WorkflowEditorLayout', async () => {
    const {useWorkflowEditor} = await import('@/pages/platform/workflow-editor/providers/workflowEditorProvider');

    const MockWorkflowEditorLayout = (props: Record<string, unknown>) => {
        const {connectedUserWorkflowUuid, includeComponents} = useWorkflowEditor();

        editorConnectedUserWorkflowUuidMock(connectedUserWorkflowUuid);
        editorIncludeComponentsMock(includeComponents);
        workflowEditorLayoutMock(props);

        return <div data-testid="workflow-editor-layout" />;
    };

    return {default: MockWorkflowEditorLayout};
});

const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

const renderWorkflowBuilder = () =>
    render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter initialEntries={['/workflows/workflow-uuid-1']}>
                <Routes>
                    <Route element={<WorkflowBuilder />} path="/workflows/:workflowUuid" />
                </Routes>
            </MemoryRouter>
        </QueryClientProvider>
    );

const setCopilotEnabled = (enabled: boolean) =>
    applicationInfoStore.setState((state) => ({ai: {...state.ai, copilot: {enabled}}}));

describe('WorkflowBuilder', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        setCopilotEnabled(true);

        useCopilotPanelStore.setState({copilotPanelOpen: false});
        useCopilotPostTurnRegistry.setState({callbacks: {}});
    });

    it('hides the workflow inputs panel from the connected user', () => {
        renderWorkflowBuilder();

        expect(screen.getByTestId('workflow-editor-layout')).toBeInTheDocument();
        expect(workflowEditorLayoutMock).toHaveBeenCalledWith(expect.objectContaining({showWorkflowInputs: false}));
    });

    it('offers the AI Copilot for the open workflow when the Copilot is enabled', () => {
        useCopilotPanelStore.setState({copilotPanelOpen: true});

        renderWorkflowBuilder();

        expect(workflowEditorLayoutMock).toHaveBeenCalledWith(expect.objectContaining({showCopilot: true}));
        expect(editorConnectedUserWorkflowUuidMock).toHaveBeenCalledWith('workflow-uuid-1');
        expect(copilotPanelMock).toHaveBeenCalledWith({open: true});
    });

    it('hides the AI Copilot when the Copilot is disabled', () => {
        setCopilotEnabled(false);

        renderWorkflowBuilder();

        expect(workflowEditorLayoutMock).toHaveBeenCalledWith(expect.objectContaining({showCopilot: false}));
        expect(screen.queryByTestId('copilot-panel')).not.toBeInTheDocument();
    });

    it('refreshes the workflow after each Copilot turn', () => {
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderWorkflowBuilder();

        useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_BUILDER_EMBEDDED);

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({
            queryKey: ConnectedUserProjectWorkflowKeys.connectedUserProjectWorkflow('workflow-uuid-1'),
        });
    });

    it('points an open connected-user Copilot chat at the workflow in the builder', () => {
        useCopilotStore.setState({
            context: {
                mode: MODE.BUILD,
                parameters: {workflowUuid: 'another-workflow-uuid'},
                source: Source.WORKFLOW_BUILDER_EMBEDDED,
            },
        });

        renderWorkflowBuilder();

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.BUILD,
            parameters: {workflowUuid: 'workflow-uuid-1'},
            source: Source.WORKFLOW_BUILDER_EMBEDDED,
        });
    });

    it('leaves a Copilot chat of another source alone', () => {
        useCopilotStore.setState({context: {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR}});

        renderWorkflowBuilder();

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
    });

    it('hands the host component filter to the workflow editor', () => {
        renderWorkflowBuilder();

        expect(editorIncludeComponentsMock).toHaveBeenCalledWith(['slack']);
    });
});
