import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// useWorkflowLayout.handleCopilotClick routes the Copilot panel to a coarse source based on the
// surface it is opened from: automation project editor vs. embedded integration editor, crossed
// with visual vs. code-backed workflows (Task 6, CC-B/C). Every dependency of the hook (stores,
// queries, the shared WorkflowEditorStateI context, and the route params) is mocked so the test can
// exercise handleCopilotClick in isolation without a QueryClient/Provider tree.

const {mockSetContext, mockSetCopilotPanelOpen, storeState, useParamsMock, useWorkflowEditorMock} = vi.hoisted(() => ({
    mockSetContext: vi.fn(),
    mockSetCopilotPanelOpen: vi.fn(),
    storeState: {
        context: {
            mode: 'ASK',
            parameters: {},
            source: 'WORKFLOW_EDITOR',
            workflowExecutionError: undefined,
        },
    },
    useParamsMock: vi.fn<() => Record<string, string | undefined>>(() => ({})),
    useWorkflowEditorMock: vi.fn(() => ({
        codeWorkflow: undefined as boolean | undefined,
        codeWorkflowLanguage: undefined as string | undefined,
        useGetComponentDefinitionsQuery: vi.fn(() => ({data: [], error: undefined, isLoading: false})),
    })),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useParams: useParamsMock,
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: useWorkflowEditorMock,
}));

vi.mock('@/pages/platform/workflow-editor/stores/useDataPillPanelStore', () => ({
    default: (selector: (state: {dataPillPanelOpen: boolean}) => unknown) => selector({dataPillPanelOpen: false}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useRightSidebarStore', () => ({
    default: (selector: (state: {rightSidebarOpen: boolean; setRightSidebarOpen: () => void}) => unknown) =>
        selector({rightSidebarOpen: false, setRightSidebarOpen: vi.fn()}),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => ({
    default: Object.assign(
        (selector: (state: {workflow: object; workflowNodes: unknown[]}) => unknown) =>
            selector({workflow: {id: 1, inputs: [], tasks: [], triggers: []}, workflowNodes: []}),
        {
            getState: () => ({
                setComponentDefinitions: vi.fn(),
                setTaskDispatcherDefinitions: vi.fn(),
            }),
        }
    ),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => ({
    default: (
        selector: (state: {
            rootClusterElementNodeData: undefined;
            setShowWorkflowCodeEditorSheet: () => void;
            setShowWorkflowInputsSheet: () => void;
            setShowWorkflowOutputsSheet: () => void;
        }) => unknown
    ) =>
        selector({
            rootClusterElementNodeData: undefined,
            setShowWorkflowCodeEditorSheet: vi.fn(),
            setShowWorkflowInputsSheet: vi.fn(),
            setShowWorkflowOutputsSheet: vi.fn(),
        }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => ({
    default: (
        selector: (state: {
            currentNode: undefined;
            setWorkflowNodeDetailsPanelOpen: () => void;
            workflowNodeDetailsPanelOpen: boolean;
        }) => unknown
    ) =>
        selector({
            currentNode: undefined,
            setWorkflowNodeDetailsPanelOpen: vi.fn(),
            workflowNodeDetailsPanelOpen: false,
        }),
}));

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => ({
    default: (selector: (state: {setWorkflowTestChatPanelOpen: () => void}) => unknown) =>
        selector({setWorkflowTestChatPanelOpen: vi.fn()}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotPanelStore', () => ({
    default: (selector: (state: {copilotPanelOpen: boolean; setCopilotPanelOpen: () => void}) => unknown) =>
        selector({copilotPanelOpen: false, setCopilotPanelOpen: mockSetCopilotPanelOpen}),
}));

vi.mock('@/shared/components/copilot/stores/useCopilotStore', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/components/copilot/stores/useCopilotStore')>();

    return {
        ...actual,
        useCopilotStore: Object.assign(
            (selector: (state: {setContext: typeof mockSetContext}) => unknown) =>
                selector({setContext: mockSetContext}),
            {getState: () => ({context: storeState.context})}
        ),
    };
});

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: [], error: undefined, isLoading: false}),
}));

vi.mock('@/shared/queries/platform/workflowNodeOutputs.queries', () => ({
    useGetPreviousWorkflowNodeOutputsQuery: () => ({data: undefined, isPending: false}),
}));

vi.mock('@/shared/queries/platform/workflowTestConfigurations.queries', () => ({
    useGetWorkflowTestConfigurationQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

type SurfaceType = {
    codeWorkflow?: boolean;
    codeWorkflowLanguage?: string;
    integrationId?: string;
};

const setSurface = ({codeWorkflow, codeWorkflowLanguage, integrationId}: SurfaceType) => {
    useParamsMock.mockReturnValue(integrationId ? {integrationId} : {});
    useWorkflowEditorMock.mockReturnValue({
        codeWorkflow,
        codeWorkflowLanguage,
        useGetComponentDefinitionsQuery: vi.fn(() => ({data: [], error: undefined, isLoading: false})),
    });
};

describe('useWorkflowLayout - handleCopilotClick', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('routes the automation visual editor to WORKFLOW_EDITOR', async () => {
        setSurface({});

        const {Source} = await import('@/shared/components/copilot/stores/useCopilotStore');
        const {useWorkflowLayout} = await import('../useWorkflowLayout');
        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(mockSetContext).toHaveBeenCalledWith({
            mode: 'ASK',
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
            workflowExecutionError: undefined,
        });
    });

    it('routes the automation code-backed workflow to CODE_WORKFLOW with the language param', async () => {
        setSurface({codeWorkflow: true, codeWorkflowLanguage: 'javascript'});

        const {Source} = await import('@/shared/components/copilot/stores/useCopilotStore');
        const {useWorkflowLayout} = await import('../useWorkflowLayout');
        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(mockSetContext).toHaveBeenCalledWith({
            mode: 'ASK',
            parameters: {language: 'javascript'},
            source: Source.CODE_WORKFLOW,
            workflowExecutionError: undefined,
        });
    });

    it('routes the embedded visual editor to WORKFLOW_EDITOR_EMBEDDED with the integrationId param', async () => {
        setSurface({integrationId: '789'});

        const {Source} = await import('@/shared/components/copilot/stores/useCopilotStore');
        const {useWorkflowLayout} = await import('../useWorkflowLayout');
        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(mockSetContext).toHaveBeenCalledWith({
            mode: 'ASK',
            parameters: {integrationId: '789'},
            source: Source.WORKFLOW_EDITOR_EMBEDDED,
            workflowExecutionError: undefined,
        });
    });

    it('routes the embedded code-backed workflow to CODE_WORKFLOW_EMBEDDED with both params', async () => {
        setSurface({codeWorkflow: true, codeWorkflowLanguage: 'python', integrationId: '789'});

        const {Source} = await import('@/shared/components/copilot/stores/useCopilotStore');
        const {useWorkflowLayout} = await import('../useWorkflowLayout');
        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(mockSetContext).toHaveBeenCalledWith({
            mode: 'ASK',
            parameters: {integrationId: '789', language: 'python'},
            source: Source.CODE_WORKFLOW_EMBEDDED,
            workflowExecutionError: undefined,
        });
    });

    it('opens the Copilot panel when it is closed', async () => {
        setSurface({});

        const {useWorkflowLayout} = await import('../useWorkflowLayout');
        const {result} = renderHook(() => useWorkflowLayout());

        act(() => {
            result.current.handleCopilotClick();
        });

        expect(mockSetCopilotPanelOpen).toHaveBeenCalledWith(true);
    });
});
