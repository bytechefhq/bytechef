import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

/**
 * Regression coverage for the cross-chat artifact bleed that motivated gating recording on
 * `tabsActiveChatId === chatId`. The scenario:
 *
 *   1. Tabs store starts mirrored to chat A with an open file tab.
 *   2. The hook is re-rendered with `chatId = B` — simulating the moment of a chat switch where the
 *      consumer (AiHub.tsx) has updated `currentChatId` but the separate `setActiveChatId` mirror
 *      effect on the tabs store has NOT yet run, so `tabsStore.activeChatId` is still A.
 *   3. Without the gate the hook would fire `recordReferencedAiHubChatArtifact` with `chatId = B` for
 *      every open tab, registering A's content under B in the persistent artifact log.
 *
 * The fix gates the recording loop on the tabs store's `activeChatId` matching the prop; this test
 * pins that gate against a recurrence.
 */

const {useRecordReferencedAiHubChatArtifactMutation: useGeneratedRecordMutation} = vi.hoisted(() => ({
    useRecordReferencedAiHubChatArtifactMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useRecordReferencedAiHubChatArtifactMutation: useGeneratedRecordMutation,
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
        activeChatId: undefined,
        activeTabId: undefined,
        openTabs: [],
        rightPanelOpen: false,
        snapshotsByChatId: {},
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

const aiAgentTab = {
    aiAgentId: 'agent-1',
    id: 'tab-agent-1',
    kind: 'aiAgent' as const,
    name: 'Support Agent',
};

describe('useRecordReferencedArtifacts', () => {
    it('records the open tabs when the tabs store is mirrored to the prop chatId', () => {
        // Baseline: tabs store has activeChatId = 10 and openTabs = [fileTab]. The hook is invoked with
        // chatId = 10. Both halves of the (chatId, tabsActiveChatId) gate match, so recording runs.
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'asset-7',
                artifactName: 'notes.md',
                chatId: '10',
                workspaceId: '1',
            }),
        });
    });

    it('records a codeWorkflow tab using projectId as the artifact id', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [codeWorkflowTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'proj-1',
                artifactName: 'My Code Workflow',
                chatId: '10',
                workspaceId: '1',
            }),
        });
    });

    it('records an aiAgent tab using aiAgentId as the artifact id', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [aiAgentTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'agent-1',
                artifactName: 'Support Agent',
                chatId: '10',
                kind: 'AI_AGENT_REFERENCED',
                workspaceId: '1',
            }),
        });
    });

    it('does NOT record when the tabs store is still mirrored to a different chat', () => {
        // The cross-chat-bleed scenario. Tabs store still says activeChatId=10 with file7 in tabs; the
        // consumer has updated to chat 20 but the mirror effect hasn't run yet. Recording must skip,
        // otherwise file7 ends up under chat 20 in the artifact log even though the user attached it to 10.
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).not.toHaveBeenCalled();
    });

    it('records once the tabs store mirror catches up to the new chat', () => {
        // Continuation of the previous scenario: after the mirror effect finally runs, activeChatId
        // flips to match the new prop chatId. The hook re-fires (tabsActiveChatId is in the dep array)
        // and now records for the correct chat.
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [fileTab]});

        const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(queryClient)});

        expect(mutateSpy).not.toHaveBeenCalled();

        // Simulate the mirror effect running: tabs store catches up to chat 20 and the openTabs reset
        // to whatever 20's snapshot had — empty in this test.
        act(() => {
            aiHubTabsStore.setState({activeChatId: 20, openTabs: []});
        });

        // No tabs to record now, but the gate would allow the loop to run if there were.
        expect(mutateSpy).not.toHaveBeenCalled();

        // Now chat 20 legitimately opens a tab — recording should fire under 20.
        act(() => {
            aiHubTabsStore.setState({
                activeChatId: 20,
                openTabs: [{...fileTab, fileId: 'asset-9', name: 'plan.md'}],
            });
        });

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({
                artifactId: 'asset-9',
                chatId: '20',
            }),
        });
    });
});
