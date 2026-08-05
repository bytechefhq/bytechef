import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Regression coverage for the cross-task artifact bleed that motivated gating recording on
 * `tabsActiveTaskId === taskId`. The scenario:
 *
 *   1. Tabs store starts mirrored to task A with an open file tab.
 *   2. The hook is re-rendered with `taskId = B` — simulating the moment of a task switch where the
 *      consumer (AiHub.tsx) has updated `currentTaskId` but the separate `setActiveTaskId` mirror
 *      effect on the tabs store has NOT yet run, so `tabsStore.activeTaskId` is still A.
 *   3. Without the gate the hook would fire `recordReferencedAiHubTaskArtifact` with `taskId = B` for
 *      every open tab, registering A's content under B in the persistent artifact log.
 *
 * The fix gates the recording loop on the tabs store's `activeTaskId` matching the prop; this test
 * pins that gate against a recurrence.
 */

const {useRecordReferencedAiHubTaskArtifactMutation: useGeneratedRecordMutation} = vi.hoisted(() => ({
    useRecordReferencedAiHubTaskArtifactMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useRecordReferencedAiHubTaskArtifactMutation: useGeneratedRecordMutation,
    };
});

const useRecordReferencedArtifacts = (await import('../useRecordReferencedArtifacts')).default;

const mutateSpy = vi.fn();

beforeEach(() => {
    mutateSpy.mockReset();
    useGeneratedRecordMutation.mockReset();

    // The mock returns the same surface for every test. Tests below assert on `mutate` invocations
    // rather than firing onSuccess, so the options arg passes through unread.
    useGeneratedRecordMutation.mockImplementation(() => {
        return {
            isError: false,
            isIdle: true,
            isPending: false,
            isSuccess: false,
            mutate: mutateSpy,
            mutateAsync: vi.fn(),
            reset: vi.fn(),
            status: 'idle',
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any;
    });

    aiHubTabsStore.setState({
        activeTabId: undefined,
        activeTaskId: undefined,
        openTabs: [],
        rightPanelOpen: false,
        snapshotsByTaskId: {},
    });
});

const wrap = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

const fileTab = {
    fileId: 'asset-7',
    id: 'tab-1',
    kind: 'file' as const,
    name: 'notes.md',
    viewMode: 'editor' as const,
};

const codeWorkflowTab = {
    id: 'codeWorkflow-proj-1',
    kind: 'codeWorkflow' as const,
    language: 'java',
    name: 'My Code Workflow',
    projectId: 'proj-1',
};

describe('useRecordReferencedArtifacts', () => {
    it('records the open tabs when the tabs store is mirrored to the prop taskId', () => {
        // Baseline: tabs store has activeTaskId = 10 and openTabs = [fileTab]. The hook is invoked with
        // taskId = 10. Both halves of the (taskId, tabsActiveTaskId) gate match, so recording runs.
        aiHubTabsStore.setState({activeTaskId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'asset-7',
                artifactName: 'notes.md',
                taskId: '10',
                workspaceId: '1',
            }),
        });
    });

    it('records a codeWorkflow tab using projectId as the artifact id', () => {
        aiHubTabsStore.setState({activeTaskId: 10, openTabs: [codeWorkflowTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'proj-1',
                artifactName: 'My Code Workflow',
                taskId: '10',
                workspaceId: '1',
            }),
        });
    });

    it('does NOT record when the tabs store is still mirrored to a different task', () => {
        // The cross-task-bleed scenario. Tabs store still says activeTaskId=10 with file7 in tabs; the
        // consumer has updated to task 20 but the mirror effect hasn't run yet. Recording must skip,
        // otherwise file7 ends up under task 20 in the artifact log even though the user attached it to 10.
        aiHubTabsStore.setState({activeTaskId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).not.toHaveBeenCalled();
    });

    it('records once the tabs store mirror catches up to the new task', () => {
        // Continuation of the previous scenario: after the mirror effect finally runs, activeTaskId
        // flips to match the new prop taskId. The hook re-fires (tabsActiveTaskId is in the dep array)
        // and now records for the correct task.
        aiHubTabsStore.setState({activeTaskId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).not.toHaveBeenCalled();

        // Simulate the mirror effect running: tabs store catches up to task 20 and the openTabs reset
        // to whatever 20's snapshot had — empty in this test.
        act(() => {
            aiHubTabsStore.setState({activeTaskId: 20, openTabs: []});
        });

        // No tabs to record now, but the gate would allow the loop to run if there were.
        expect(mutateSpy).not.toHaveBeenCalled();

        // Now task 20 legitimately opens a tab — recording should fire under 20.
        act(() => {
            aiHubTabsStore.setState({
                activeTaskId: 20,
                openTabs: [{...fileTab, fileId: 'asset-9', name: 'plan.md'}],
            });
        });

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'asset-9',
                taskId: '20',
            }),
        });
    });
});
