import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {capturedOptions, useGeneratedRecordMutation} = vi.hoisted(() => ({
    capturedOptions: {current: undefined as {onSuccess?: () => void} | undefined},
    useGeneratedRecordMutation: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useRecordReferencedAiHubChatArtifactMutation: useGeneratedRecordMutation,
    };
});

const {default: useRecordReferencedArtifacts} = await import('../useRecordReferencedArtifacts');

const mutateSpy = vi.fn();

const workflowTab = {
    id: 'tab-1',
    kind: 'workflow' as const,
    name: 'Lead Scoring',
    projectId: '5',
    projectWorkflowId: 12,
    workflowId: 'wf-1',
};

const wrap = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

beforeEach(() => {
    mutateSpy.mockReset();
    useGeneratedRecordMutation.mockReset();

    useGeneratedRecordMutation.mockImplementation((options: {onSuccess?: () => void}) => {
        capturedOptions.current = options;

        return {
            isError: false,
            isIdle: true,
            isPending: false,
            isSuccess: false,
            mutate: mutateSpy,
            mutateAsync: vi.fn(),
            reset: vi.fn(),
            status: 'idle',
        };
    });

    aiHubTabsStore.setState({
        activeChatId: undefined,
        activeTabId: undefined,
        openTabs: [],
        rightPanelOpen: false,
        snapshotsByChatId: {},
    });
});

describe('useRecordReferencedArtifacts', () => {
    it('records an open workflow tab as WORKFLOW_REFERENCED with its project metadata', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [workflowTab]});

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(new QueryClient())});

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: {
                artifactId: 'wf-1',
                artifactName: 'Lead Scoring',
                chatId: '10',
                kind: 'WORKFLOW_REFERENCED',
                metadataJson: JSON.stringify({projectId: '5', projectWorkflowId: 12}),
                workspaceId: '1',
            },
        });
    });

    it('records a tab once across re-renders', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [workflowTab]});

        const {rerender} = renderHook(() => useRecordReferencedArtifacts(10, 1), {
            wrapper: wrap(new QueryClient()),
        });

        act(() => {
            aiHubTabsStore.setState({openTabs: [{...workflowTab}]});
        });

        rerender();

        expect(mutateSpy).toHaveBeenCalledTimes(1);
    });

    it('does not record on the home view', () => {
        aiHubTabsStore.setState({activeChatId: undefined, openTabs: [workflowTab]});

        renderHook(() => useRecordReferencedArtifacts(undefined, 1), {wrapper: wrap(new QueryClient())});

        expect(mutateSpy).not.toHaveBeenCalled();
    });

    it('does NOT record when the tabs store is still mirrored to a different chat', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [workflowTab]});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(new QueryClient())});

        expect(mutateSpy).not.toHaveBeenCalled();
    });

    it('records once the tabs store mirror catches up to the new chat', () => {
        aiHubTabsStore.setState({activeChatId: 10, openTabs: [workflowTab]});

        renderHook(() => useRecordReferencedArtifacts(20, 1), {wrapper: wrap(new QueryClient())});

        act(() => {
            aiHubTabsStore.setState({activeChatId: 20, openTabs: []});
        });

        expect(mutateSpy).not.toHaveBeenCalled();

        act(() => {
            aiHubTabsStore.setState({
                activeChatId: 20,
                openTabs: [{...workflowTab, name: 'Other', projectId: '6', workflowId: 'wf-9'}],
            });
        });

        expect(mutateSpy).toHaveBeenCalledTimes(1);
        expect(mutateSpy).toHaveBeenCalledWith({
            input: expect.objectContaining({artifactId: 'wf-9', chatId: '20'}),
        });
    });

    it('invalidates the artifact queries after a successful record', () => {
        const queryClient = new QueryClient();

        const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderHook(() => useRecordReferencedArtifacts(10, 1), {wrapper: wrap(queryClient)});

        act(() => {
            capturedOptions.current?.onSuccess?.();
        });

        expect(invalidateSpy).toHaveBeenCalledWith({queryKey: ['aiHubChats', 'artifacts']});
    });
});
