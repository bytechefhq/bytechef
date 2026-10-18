import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    AiHubChatArtifactI,
    AiHubChatI,
    AiHubChatMessageI,
    getChatArtifacts,
    getChatMessages,
    getToolApprovals,
} from '../../api/chats.api';

const {navigateMock} = vi.hoisted(() => ({navigateMock: vi.fn()}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 99})
    ),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => navigateMock,
}));

vi.mock('../../api/chats.api', async () => {
    const actual = await vi.importActual<typeof import('../../api/chats.api')>('../../api/chats.api');

    return {
        ...actual,
        getChatArtifacts: vi.fn(),
        getChatMessages: vi.fn(),
        getToolApprovals: vi.fn(),
    };
});

vi.mock('@/shared/error/useReportQueryError', () => ({
    reportMutationError: vi.fn(),
    useReportQueryError: vi.fn(),
}));

const {useSwitchChat} = await import('../useSwitchChat');
const {reportMutationError} = await import('@/shared/error/useReportQueryError');

const mockGetChatArtifacts = vi.mocked(getChatArtifacts);
const mockGetChatMessages = vi.mocked(getChatMessages);
const mockGetToolApprovals = vi.mocked(getToolApprovals);
const mockReportMutationError = vi.mocked(reportMutationError);

const buildChat = (overrides: Partial<AiHubChatI> = {}): AiHubChatI => ({
    autoTitled: false,
    createdAt: '2026-04-01T00:00:00Z',
    environmentId: 0,
    id: 7,
    isOwner: true,
    lastPreview: null,
    messageCount: 0,
    ownerUserId: 1,
    status: 'ACTIVE',
    threadId: 'thread-target',
    title: 'Target',
    updatedAt: '2026-04-01T00:00:00Z',
    userId: 1,
    workspaceId: 99,
    ...overrides,
});

const buildMessage = (overrides: Partial<AiHubChatMessageI>): AiHubChatMessageI => ({
    content: 'hi',
    role: 'user',
    timestamp: '2026-04-01T00:00:00Z',
    toolEventsJson: null,
    ...overrides,
});

const buildArtifact = (overrides: Partial<AiHubChatArtifactI> = {}): AiHubChatArtifactI => ({
    artifactId: 'wf-1',
    artifactName: 'Lead Scoring',
    chatId: 7,
    createdAt: '2026-04-01T00:00:00Z',
    id: 1,
    kind: 'WORKFLOW_CREATED',
    metadataJson: JSON.stringify({projectId: '5', projectWorkflowId: 12}),
    status: 'APPLIED',
    ...overrides,
});

const switchTo = async (chat: AiHubChatI): Promise<boolean | undefined> => {
    const {result} = renderHook(() => useSwitchChat());

    let returned: boolean | undefined;

    await act(async () => {
        returned = await result.current(chat);
    });

    return returned;
};

describe('useSwitchChat', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        mockGetChatArtifacts.mockResolvedValue([]);
        mockGetToolApprovals.mockResolvedValue([]);

        aiHubStore.setState({chatId: 'thread-source', messages: []});
        aiHubChatsStore.setState({currentChatId: 1});
    });

    it('updates the hub store before the chats store and navigates to the chat', async () => {
        const observedThreadIdAtChatsFlip: Array<string | undefined> = [];

        const originalSetId = aiHubChatsStore.getState().setCurrentChatId;

        aiHubChatsStore.setState({
            setCurrentChatId: (id) => {
                observedThreadIdAtChatsFlip.push(aiHubStore.getState().chatId);

                originalSetId(id);
            },
        });

        mockGetChatMessages.mockResolvedValue([buildMessage({})]);

        await switchTo(buildChat());

        expect(observedThreadIdAtChatsFlip).toEqual(['thread-target']);
        expect(aiHubChatsStore.getState().currentChatId).toBe(7);
        expect(navigateMock).toHaveBeenCalledWith('/automation/ai-hub/chats/7');
        expect(mockReportMutationError).not.toHaveBeenCalled();

        aiHubChatsStore.setState({setCurrentChatId: originalSetId});
    });

    it('maps user/assistant/system roles and filters tool', async () => {
        mockGetChatMessages.mockResolvedValue([
            buildMessage({content: 'hi', role: 'user'}),
            buildMessage({content: 'hello', role: 'assistant'}),
            buildMessage({content: 'sys', role: 'system'}),
            buildMessage({content: 'tool-payload', role: 'tool'}),
        ]);

        await switchTo(buildChat());

        expect(aiHubStore.getState().chatId).toBe('thread-target');
        expect(aiHubStore.getState().messages).toEqual([
            {content: 'hi', role: 'user'},
            {content: 'hello', role: 'assistant'},
            {content: 'sys', role: 'system'},
        ]);
        expect(aiHubStore.getState().messagesLoading).toBe(false);
    });

    it('drops messages with unknown server roles instead of routing them', async () => {
        const consoleSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        mockGetChatMessages.mockResolvedValue([
            buildMessage({content: 'kept', role: 'user'}),
            buildMessage({content: 'dropped', role: 'TOOL_USE'}),
        ]);

        await switchTo(buildChat());

        const stored = aiHubStore.getState().messages;

        expect(stored).toHaveLength(1);
        expect(stored[0]).toMatchObject({content: 'kept', role: 'user'});
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Unknown message role'));

        consoleSpy.mockRestore();
    });

    it('restores persisted tool calls as tool-call parts on the assistant row', async () => {
        mockGetChatMessages.mockResolvedValue([
            buildMessage({content: 'build it', role: 'user'}),
            buildMessage({
                content: 'Done.',
                role: 'assistant',
                toolEventsJson: JSON.stringify([
                    {arguments: '{"projectId":5}', id: 'call-1', kind: 'call', name: 'listWorkflows'},
                    {id: 'call-1', kind: 'result', name: 'listWorkflows', response: '[]'},
                ]),
            }),
        ]);

        await switchTo(buildChat());

        expect(aiHubStore.getState().messages[1]).toEqual({
            content: [
                {text: 'Done.', type: 'text'},
                {
                    args: {projectId: 5},
                    argsText: '{"projectId":5}',
                    result: [],
                    toolCallId: 'call-1',
                    toolName: 'listWorkflows',
                    type: 'tool-call',
                },
            ],
            role: 'assistant',
        });
    });

    it('stitches a settled approval status onto its restored approval card', async () => {
        mockGetChatMessages.mockResolvedValue([
            buildMessage({
                content: '',
                role: 'assistant',
                toolEventsJson: JSON.stringify([
                    {arguments: '{}', id: 'call-1', kind: 'call', name: 'deleteWorkflow'},
                    {
                        id: 'call-1',
                        kind: 'result',
                        name: 'deleteWorkflow',
                        response: JSON.stringify({
                            approvalId: 4,
                            awaitingApproval: true,
                            kind: 'tool-approval-request',
                            toolName: 'deleteWorkflow',
                        }),
                    },
                ]),
            }),
        ]);

        mockGetToolApprovals.mockResolvedValue([
            {
                componentName: null,
                decidedByUserId: 1,
                executionError: null,
                id: 4,
                status: 'APPROVED',
                toolName: 'deleteWorkflow',
            },
        ]);

        await switchTo(buildChat());

        const content = aiHubStore.getState().messages[0]!.content as unknown as Array<{
            data?: Record<string, unknown>;
        }>;

        const approvalPart = content.find((part) => part.data?.approvalId === 4);

        expect(approvalPart?.data).toMatchObject({resolvedBy: '1', resolvedStatus: 'APPROVED'});
    });

    it('switches optimistically and reports the failure when getChatMessages rejects', async () => {
        const failure = new Error('network down');

        mockGetChatMessages.mockRejectedValue(failure);

        const returned = await switchTo(buildChat());

        expect(returned).toBe(false);
        expect(mockReportMutationError).toHaveBeenCalledWith('Switch chat', failure);
        expect(aiHubStore.getState().chatId).toBe('thread-target');
        expect(aiHubStore.getState().messages).toEqual([]);
        expect(aiHubStore.getState().messagesLoading).toBe(false);
        expect(aiHubChatsStore.getState().currentChatId).toBe(7);
    });

    it('returns true on success', async () => {
        mockGetChatMessages.mockResolvedValue([buildMessage({})]);

        expect(await switchTo(buildChat())).toBe(true);
    });

    describe('artifact link card rehydration', () => {
        beforeEach(() => {
            mockGetChatMessages.mockResolvedValue([]);
        });

        it.each(['WORKFLOW_CREATED', 'WORKFLOW_UPDATED', 'WORKFLOW_REFERENCED'] as const)(
            'rehydrates a %s artifact as an openWorkflowTab tool-call card',
            async (kind) => {
                mockGetChatArtifacts.mockResolvedValue([buildArtifact({kind})]);

                await switchTo(buildChat());

                const messages = aiHubStore.getState().messages;

                expect(messages).toHaveLength(1);
                expect(messages[0]!.content).toEqual([
                    expect.objectContaining({
                        args: {name: 'Lead Scoring', projectId: '5', projectWorkflowId: 12, workflowId: 'wf-1'},
                        toolCallId: 'rehydrated-artifact-1',
                        toolName: 'openWorkflowTab',
                        type: 'tool-call',
                    }),
                ]);
            }
        );

        it('skips non-workflow artifacts', async () => {
            mockGetChatArtifacts.mockResolvedValue([buildArtifact({kind: 'FILE_CREATED'})]);

            await switchTo(buildChat());

            expect(aiHubStore.getState().messages).toEqual([]);
        });

        it('rebuilds one card per workflow even when it was recorded twice', async () => {
            mockGetChatArtifacts.mockResolvedValue([
                buildArtifact({id: 1, kind: 'WORKFLOW_CREATED'}),
                buildArtifact({id: 2, kind: 'WORKFLOW_REFERENCED'}),
            ]);

            await switchTo(buildChat());

            expect(aiHubStore.getState().messages[0]!.content).toHaveLength(1);
        });

        it('does not restore a persisted openWorkflowTab call twice', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({
                    content: 'Opened.',
                    role: 'assistant',
                    toolEventsJson: JSON.stringify([
                        {arguments: '{}', id: 'call-1', kind: 'call', name: 'openWorkflowTab'},
                    ]),
                }),
            ]);

            await switchTo(buildChat());

            expect(aiHubStore.getState().messages).toEqual([{content: 'Opened.', role: 'assistant'}]);
        });
    });
});
