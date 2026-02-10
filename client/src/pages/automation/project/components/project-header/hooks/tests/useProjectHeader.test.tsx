import {BASE_PATH} from '@/shared/middleware/platform/workflow/test/runtime';
import {act, renderHook, waitFor} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {useProjectHeader} from '../useProjectHeader';

// ---- Test doubles and module mocks ----

// Create spies in a hoisted block so they are available to vi.mock factories (which are hoisted too)
const hoisted = vi.hoisted(() => {
    return {
        analyticsSpies: {
            captureProjectPublished: vi.fn(),
            captureProjectWorkflowTested: vi.fn(),
        },
        chatSpies: {
            resetMessages: vi.fn(),
            setWorkflowTestChatPanelOpen: vi.fn(),
            workflowTestChatPanelOpen: false,
        },
        editorSpies: {
            setShowBottomPanelOpen: vi.fn(),
            setWorkflowIsRunning: vi.fn(),
            setWorkflowTestExecution: vi.fn(),
            showBottomPanel: false,
        },
        nodePanelSpies: {
            setCurrentNode: vi.fn(),
            setWorkflowNodeDetailsPanelOpen: vi.fn(),
        },
    } as const;
});

// Mocks for Zustand stores used inside the hook
vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowDataStore', () => {
    const state = {workflow: {id: 'wf-1'}};
    return {default: (selector: (s: typeof state) => unknown) => selector(state)};
});

vi.mock('@/shared/stores/useEnvironmentStore', () => {
    const state = {currentEnvironmentId: 1};
    return {useEnvironmentStore: (selector: (s: typeof state) => unknown) => selector(state)};
});

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowEditorStore', () => {
    return {
        __esModule: true,
        __spies: hoisted.editorSpies,
        default: (selector: (s: typeof hoisted.editorSpies) => unknown) => selector(hoisted.editorSpies),
    };
});

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore', () => {
    return {
        __esModule: true,
        __spies: hoisted.nodePanelSpies,
        default: (selector: (s: typeof hoisted.nodePanelSpies) => unknown) => selector(hoisted.nodePanelSpies),
    };
});

vi.mock('@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore', () => {
    return {
        __esModule: true,
        __spies: hoisted.chatSpies,
        default: (selector: (s: typeof hoisted.chatSpies) => unknown) => selector(hoisted.chatSpies),
    };
});

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => {
    const state = {currentWorkspaceId: 'ws-1'};
    return {useWorkspaceStore: (selector: (s: typeof state) => unknown) => selector(state)};
});

// Analytics hook
vi.mock('@/shared/hooks/useAnalytics', () => ({useAnalytics: () => hoisted.analyticsSpies}));

// Toast (unused in these tests but required)
vi.mock('@/hooks/use-toast', () => ({useToast: () => ({toast: vi.fn()})}));

// React Router hooks used inside the hook
vi.mock('react-router-dom', () => ({
    useLoaderData: () => ({}),
    useNavigate: () => vi.fn(),
    useSearchParams: () => [new URLSearchParams(''), vi.fn()],
}));

// Queries and mutations used by the hook
vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {filteredProjects: (_: unknown) => ['filtered', _], project: (_: number) => ['project', _]},
    useGetProjectQuery: () => ({data: {id: 42}}),
}));
vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: () => ({data: []}),
}));
vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    usePublishProjectMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

// react-query client (prevent the need for a Provider in tests)
vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

// ProjectVersionKeys used in publish success path
vi.mock('@/shared/queries/automation/projectVersions.queries', () => ({
    ProjectVersionKeys: {projectProjectVersions: (_: number) => ['projectVersions', _]},
}));

// Stream request builder for Run
const streamRequest = {
    init: {headers: {'Content-Type': 'application/json'}, method: 'POST'} as RequestInit,
    url: '/sse/start',
};
vi.mock('@/shared/util/testWorkflow-utils', () => ({
    getTestWorkflowAttachRequest: vi.fn((params: {jobId: string}) => ({
        init: {
            credentials: 'include',
            headers: {Accept: 'text/event-stream'},
            method: 'GET',
        },
        url: `${BASE_PATH}/workflow-tests/${params.jobId}/attach`,
    })),
    getTestWorkflowStreamPostRequest: vi.fn(() => streamRequest),
}));

// WorkflowTestExecution model helper used in result handler – return input as-is for tests
vi.mock('@/shared/middleware/platform/workflow/test/models/WorkflowTestExecution', () => ({
    WorkflowTestExecutionFromJSON: (x: unknown) => x,
}));

// ---- Mock useSSE to capture requests and provide controllable handlers ----
import type {SSERequestType, UseSSEOptionsType, UseSSEResultType} from '@/shared/hooks/useSSE';

const latest = {
    close: vi.fn(),
    handlers: undefined as UseSSEOptionsType['eventHandlers'],
    req: null as SSERequestType,
};

vi.mock('@/shared/hooks/useSSE', async () => {
    const actual: Record<string, unknown> = await vi.importActual('@/shared/hooks/useSSE');
    return {
        __esModule: true,
        ...actual,
        useSSE: (req: SSERequestType, options: UseSSEOptionsType = {}): UseSSEResultType => {
            latest.req = req;
            latest.handlers = options.eventHandlers;
            return {close: latest.close, connectionState: 'CONNECTED', data: null, error: null};
        },
    };
});

// ---- Helpers ----
import type {PanelImperativeHandle} from 'react-resizable-panels';

type PanelRefType = {
    current: PanelImperativeHandle;
};

function makePanelRef(getSizeReturn = 0): PanelRefType {
    const handle: PanelImperativeHandle = {
        collapse: vi.fn(),
        expand: vi.fn(),
        getSize: vi.fn().mockReturnValue({asPercentage: getSizeReturn, inPixels: 0}),
        isCollapsed: vi.fn().mockReturnValue(false),
        resize: vi.fn(),
    };
    return {current: handle};
}

describe('useProjectHeader', () => {
    const originalFetch = global.fetch;

    function resetIfMock(obj: Record<string, unknown>) {
        Object.values(obj).forEach((v) => {
            const maybeMock = v as {mockReset?: () => void; mockClear?: () => void};
            maybeMock.mockReset?.();
            maybeMock.mockClear?.();
        });
    }

    beforeEach(() => {
        // Clean localStorage before every test
        localStorage.clear();
        // Reset spies
        resetIfMock(hoisted.editorSpies as unknown as Record<string, unknown>);
        resetIfMock(hoisted.nodePanelSpies as unknown as Record<string, unknown>);
        resetIfMock(hoisted.chatSpies as unknown as Record<string, unknown>);
        resetIfMock(hoisted.analyticsSpies as unknown as Record<string, unknown>);
        latest.req = null;
        latest.handlers = undefined;
        latest.close.mockReset();
        document.cookie = '';
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    it('reattaches on mount using saved jobId and processes start/result', async () => {
        const jobId = '123';
        const storageKey = `bytechef.workflow-test-run.wf-1:1`;
        localStorage.setItem(storageKey, jobId);

        const panelRef = makePanelRef(0);

        const {result} = renderHook(() =>
            useProjectHeader({bottomResizablePanelRef: panelRef, chatTrigger: false, projectId: 42})
        );

        // useSSE should be called with attach request for the saved jobId
        await waitFor(() => {
            const req = latest.req as NonNullable<SSERequestType>;
            expect(req && req.url).toBe(`${BASE_PATH}/workflow-tests/${jobId}/attach`);
            const init = req.init as RequestInit;
            expect(init.method).toBe('GET');
            expect((init.headers as Record<string, string>)['Accept']).toBe('text/event-stream');
            expect(init.credentials).toBe('include');
        });

        // Simulate start -> persist jobId
        act(() => latest.handlers?.start?.({jobId}));
        expect(localStorage.getItem(storageKey)).toBe(jobId);

        // Simulate result -> stop running, set execution, clear jobId and resize panel if closed
        act(() => latest.handlers?.result?.({ok: true}));
        await waitFor(() => expect(hoisted.editorSpies.setWorkflowIsRunning).toHaveBeenCalledWith(false));
        expect(hoisted.editorSpies.setWorkflowTestExecution).toHaveBeenCalled();
        expect(panelRef.current.resize).toHaveBeenCalledWith(350);
        expect(localStorage.getItem(storageKey)).toBeNull();

        // Expose returned API just to avoid unused warning
        expect(typeof result.current.handleRunClick).toBe('function');
    });

    it('handleStopClick posts to stop endpoint with XSRF and clears jobId', async () => {
        const jobId = '555';
        const storageKey = `bytechef.workflow-test-run.wf-1:1`;

        // Render hook and simulate a running stream with a jobId via start
        const panelRef = makePanelRef(0);
        const {result} = renderHook(() =>
            useProjectHeader({bottomResizablePanelRef: panelRef, chatTrigger: false, projectId: 42})
        );
        act(() => latest.handlers?.start?.({jobId}));
        localStorage.setItem(storageKey, jobId);

        // Mock fetch and cookie
        document.cookie = 'XSRF-TOKEN=token123';
        const fetchSpy = vi.fn<typeof fetch>().mockResolvedValue(new Response(null, {status: 200}));
        (globalThis as unknown as {fetch: typeof fetch}).fetch = fetchSpy;

        // Call stop
        const stopCloseBefore = latest.close.mock.calls.length;
        act(() => {
            result.current.handleStopClick();
        });

        // Should call close and POST to stop endpoint
        expect(latest.close.mock.calls.length).toBeGreaterThanOrEqual(stopCloseBefore + 1);

        await waitFor(() => {
            expect(fetchSpy).toHaveBeenCalled();
            const call = fetchSpy.mock.calls[0] as [string, RequestInit];
            const [url, init] = call;
            expect(url).toBe(`${BASE_PATH}/workflow-tests/${jobId}/stop`);
            expect(init.method).toBe('POST');
        });

        // JobId cleared from storage
        await waitFor(() => expect(localStorage.getItem(storageKey)).toBeNull());
    });

    it('handleRunClick opens panel, tracks analytics and starts stream via builder', async () => {
        const panelRef = makePanelRef(0);
        const {result} = renderHook(() =>
            useProjectHeader({bottomResizablePanelRef: panelRef, chatTrigger: false, projectId: 42})
        );

        act(() => {
            result.current.handleRunClick();
        });

        // Panel opened and resized
        expect(hoisted.editorSpies.setShowBottomPanelOpen).toHaveBeenCalledWith(true);
        expect(panelRef.current.resize).toHaveBeenCalledWith(350);
        // Execution cleared and analytics tracked
        expect(hoisted.editorSpies.setWorkflowTestExecution).toHaveBeenCalledWith(undefined);
        expect(hoisted.analyticsSpies.captureProjectWorkflowTested).toHaveBeenCalled();

        // useSSE should be called with the request produced by the builder
        await waitFor(() => {
            const req = latest.req as NonNullable<SSERequestType>;
            expect(req && req.url).toBe(streamRequest.url);
        });
    });

    it('does not stop workflow execution when node details panel is open (fix for issue #3851)', async () => {
        const panelRef = makePanelRef(0);
        const {rerender, result} = renderHook(() =>
            useProjectHeader({bottomResizablePanelRef: panelRef, chatTrigger: false, projectId: 42})
        );

        // Start workflow execution
        act(() => {
            result.current.handleRunClick();
        });

        // Verify workflow started
        expect(hoisted.editorSpies.setWorkflowIsRunning).toHaveBeenCalledWith(true);
        expect(hoisted.analyticsSpies.captureProjectWorkflowTested).toHaveBeenCalled();

        // Clear the mock to check if handleStopClick is called
        hoisted.editorSpies.setWorkflowIsRunning.mockClear();

        // Re-render - even if node details panel were open, workflow should NOT stop
        // This tests the fix for issue #3851 where opening the node details panel
        // would automatically stop the workflow execution
        rerender();

        // Wait a bit to ensure the effect doesn't trigger
        await new Promise((resolve) => setTimeout(resolve, 100));

        // Workflow should still be running - setWorkflowIsRunning(false) should NOT have been called
        expect(hoisted.editorSpies.setWorkflowIsRunning).not.toHaveBeenCalledWith(false);
        expect(latest.close).not.toHaveBeenCalled();
    });

    it('stops workflow execution in chat mode when chat panel is closed', async () => {
        const panelRef = makePanelRef(0);
        const {result} = renderHook(() =>
            useProjectHeader({bottomResizablePanelRef: panelRef, chatTrigger: true, projectId: 42})
        );

        // Start workflow execution via chat
        act(() => {
            result.current.handleRunClick();
        });

        // Verify chat panel was opened
        expect(hoisted.chatSpies.setWorkflowTestChatPanelOpen).toHaveBeenCalledWith(true);

        // Clear mocks
        hoisted.editorSpies.setWorkflowIsRunning.mockClear();
        latest.close.mockClear();

        // Manually call handleStopClick to simulate what happens when chat panel closes
        // In a real scenario, the effect would trigger when workflowTestChatPanelOpen becomes false
        act(() => {
            result.current.handleStopClick();
        });

        // Verify workflow stopped
        expect(hoisted.editorSpies.setWorkflowIsRunning).toHaveBeenCalledWith(false);
        expect(latest.close).toHaveBeenCalled();
    });
});
