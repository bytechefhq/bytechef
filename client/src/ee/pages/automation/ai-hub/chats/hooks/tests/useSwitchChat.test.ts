import {hasPendingToolApproval} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {getServerRowIndex} from '@/ee/pages/automation/ai-hub/stores/serverRowIndex';
import {aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    AiHubChatArtifactI,
    AiHubChatI,
    AiHubChatMessageI,
    AiHubToolApprovalI,
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
            {content: 'hi', metadata: {custom: {serverRowIndex: 0}}, role: 'user'},
            {content: 'hello', metadata: {custom: {serverRowIndex: 1}}, role: 'assistant'},
            {content: 'sys', metadata: {custom: {serverRowIndex: 2}}, role: 'system'},
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
            metadata: {custom: {serverRowIndex: 1}},
            role: 'assistant',
        });
    });

    it.each([
        ['"done"', 'done'],
        ['42', '42'],
        ['true', 'true'],
        ['null', 'null'],
    ])('restores the scalar tool result %s as text', async (response, expected) => {
        mockGetChatMessages.mockResolvedValue([
            buildMessage({content: 'count them', role: 'user'}),
            buildMessage({
                content: 'Counted.',
                role: 'assistant',
                toolEventsJson: JSON.stringify([
                    {arguments: '{}', id: 'call-1', kind: 'call', name: 'countWorkflows'},
                    {id: 'call-1', kind: 'result', name: 'countWorkflows', response},
                ]),
            }),
        ]);

        await switchTo(buildChat());

        const content = aiHubStore.getState().messages[1].content as unknown as Array<{result?: unknown; type: string}>;

        expect(content.find((part) => part.type === 'tool-call')?.result).toBe(expected);
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
                arguments: '{}',
                componentName: null,
                decidedByUserId: 1,
                executionError: null,
                expiresAt: null,
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

    describe('picker cards restored from persisted turn-ending results', () => {
        const pickerRow = (toolName: string, response: Record<string, unknown>) =>
            buildMessage({
                content: 'Please pick one:',
                role: 'ASSISTANT',
                toolEventsJson: JSON.stringify([
                    {arguments: '{}', id: `call-${toolName}`, kind: 'call', name: toolName},
                    {id: `call-${toolName}`, kind: 'result', name: toolName, response: JSON.stringify(response)},
                ]),
            });

        const findDataPart = (type: string) =>
            aiHubStore
                .getState()
                .messages.flatMap((message) =>
                    Array.isArray(message.content)
                        ? (message.content as unknown as Array<{data?: Record<string, unknown>; type?: string}>)
                        : []
                )
                .find((part) => part.type === type);

        it('restores an answered connection picker as picked, using the user pick that followed it', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind a Slack connection', role: 'USER'}),
                pickerRow('selectConnection', {
                    awaitingAnswer: true,
                    componentLabel: 'Slack',
                    componentName: 'slack',
                    kind: 'select-connection',
                }),
                buildMessage({content: 'User picked: Slack Engineering (ID: 1051)', role: 'USER'}),
                buildMessage({content: 'Bound.', role: 'ASSISTANT'}),
            ]);

            await switchTo(buildChat());

            expect(findDataPart('data-select-connection')?.data).toMatchObject({
                componentName: 'slack',
                pickedLabel: 'Slack Engineering',
            });
        });

        it('restores an answered property option picker as picked', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Pick the channel', role: 'USER'}),
                pickerRow('selectPropertyOption', {
                    awaitingAnswer: true,
                    componentName: 'slack',
                    kind: 'select-property-option',
                    options: [{label: 'general', value: 'C1'}],
                    propertyName: 'channel',
                }),
                buildMessage({content: 'User picked: general (value: C1)', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataPart('data-select-property-option')?.data).toMatchObject({pickedLabel: 'general'});
        });

        it('keeps the latest unanswered picker interactive', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind a Slack connection', role: 'USER'}),
                pickerRow('selectConnection', {
                    awaitingAnswer: true,
                    componentLabel: 'Slack',
                    componentName: 'slack',
                    kind: 'select-connection',
                }),
            ]);

            await switchTo(buildChat());

            const pickerData = findDataPart('data-select-connection')?.data;

            expect(pickerData).toMatchObject({componentName: 'slack'});
            expect(pickerData?.pickedLabel).toBeUndefined();
        });

        const SLACK_PICKER = {
            awaitingAnswer: true,
            componentLabel: 'Slack',
            componentName: 'slack',
            kind: 'select-connection',
        };

        const CHANNEL_PICKER = {
            awaitingAnswer: true,
            componentName: 'slack',
            kind: 'select-property-option',
            options: [{label: 'general', value: 'C1'}],
            propertyName: 'channel',
        };

        const findDataParts = (type: string) =>
            aiHubStore
                .getState()
                .messages.flatMap((message) =>
                    Array.isArray(message.content)
                        ? (message.content as unknown as Array<{data?: Record<string, unknown>; type?: string}>)
                        : []
                )
                .filter((part) => part.type === type);

        it('restores a property option picker whose value contains a closing parenthesis', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Pick the channel', role: 'USER'}),
                pickerRow('selectPropertyOption', CHANNEL_PICKER),
                buildMessage({content: 'User picked: general (value: f(x))', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataPart('data-select-property-option')?.data).toMatchObject({pickedLabel: 'general'});
        });

        it('drops an answered picker when the user replied with free text instead of a pick', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind a Slack connection', role: 'USER'}),
                pickerRow('selectConnection', SLACK_PICKER),
                buildMessage({content: 'Actually, use Gmail instead.', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataParts('data-select-connection')).toEqual([]);
        });

        it('does not label a stale picker from a following row that is not its kind of pick', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind a Slack connection', role: 'USER'}),
                pickerRow('selectConnection', SLACK_PICKER),
                buildMessage({content: 'User picked: general (value: C1)', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataParts('data-select-connection')).toEqual([]);
        });

        it('does not copy one pick onto two pickers of the same kind in one row', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind Slack and Gmail', role: 'USER'}),
                buildMessage({
                    content: 'Pick both:',
                    role: 'ASSISTANT',
                    toolEventsJson: JSON.stringify([
                        {arguments: '{}', id: 'call-slack', kind: 'call', name: 'selectConnection'},
                        {arguments: '{}', id: 'call-gmail', kind: 'call', name: 'selectConnection'},
                        {
                            id: 'call-slack',
                            kind: 'result',
                            name: 'selectConnection',
                            response: JSON.stringify(SLACK_PICKER),
                        },
                        {
                            id: 'call-gmail',
                            kind: 'result',
                            name: 'selectConnection',
                            response: JSON.stringify({
                                ...SLACK_PICKER,
                                componentLabel: 'Gmail',
                                componentName: 'gmail',
                            }),
                        },
                    ]),
                }),
                buildMessage({content: 'User picked: Slack Engineering (ID: 1)', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataParts('data-select-connection').filter((part) => part.data?.pickedLabel != null)).toEqual(
                []
            );
        });

        it('labels only the picker whose kind matches the pick when a row holds one of each', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind Slack and pick the channel', role: 'USER'}),
                buildMessage({
                    content: 'Pick both:',
                    role: 'ASSISTANT',
                    toolEventsJson: JSON.stringify([
                        {arguments: '{}', id: 'call-connection', kind: 'call', name: 'selectConnection'},
                        {arguments: '{}', id: 'call-channel', kind: 'call', name: 'selectPropertyOption'},
                        {
                            id: 'call-connection',
                            kind: 'result',
                            name: 'selectConnection',
                            response: JSON.stringify(SLACK_PICKER),
                        },
                        {
                            id: 'call-channel',
                            kind: 'result',
                            name: 'selectPropertyOption',
                            response: JSON.stringify(CHANNEL_PICKER),
                        },
                    ]),
                }),
                buildMessage({content: 'User picked: Slack Engineering (ID: 1)', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataParts('data-select-connection')).toEqual([
                expect.objectContaining({data: expect.objectContaining({pickedLabel: 'Slack Engineering'})}),
            ]);
            expect(findDataParts('data-select-property-option')).toEqual([]);
        });

        it('does not label a stale picker from an approval continuation row', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({content: 'Bind a Slack connection', role: 'USER'}),
                pickerRow('selectConnection', SLACK_PICKER),
                buildMessage({content: '[tool-approval #3 rejected]\nTool: deleteWorkflow', role: 'USER'}),
            ]);

            await switchTo(buildChat());

            expect(findDataParts('data-select-connection')).toEqual([]);
        });
    });

    describe('tool approval cards whose gated tool result was never persisted', () => {
        const DELETE_ARGUMENTS = '{"workflowId": "a5667d0f"}';

        const gatedCallToolEventsJson = JSON.stringify([
            {arguments: DELETE_ARGUMENTS, id: 'call-delete', kind: 'call', name: 'deleteWorkflow'},
        ]);

        const gatedCallRow = buildMessage({
            content: 'Deleting the workflow "H6 Check".',
            role: 'ASSISTANT',
            toolEventsJson: gatedCallToolEventsJson,
        });

        const gatedCallOnUserRow = buildMessage({
            content: 'Delete the workflow "H6 Check".',
            role: 'USER',
            toolEventsJson: gatedCallToolEventsJson,
        });

        const persistedEnvelopeRow = buildMessage({
            content: 'Delete the workflow "H6 Check".',
            role: 'USER',
            toolEventsJson: JSON.stringify([
                {arguments: DELETE_ARGUMENTS, id: 'call-delete', kind: 'call', name: 'deleteWorkflow'},
                {
                    id: 'call-delete',
                    kind: 'result',
                    name: 'deleteWorkflow',
                    response: JSON.stringify({
                        approvalId: 3,
                        arguments: {workflowId: 'a5667d0f'},
                        awaitingApproval: true,
                        kind: 'tool-approval-request',
                        toolName: 'deleteWorkflow',
                    }),
                },
            ]),
        });

        const buildApproval = (overrides: Partial<AiHubToolApprovalI> = {}): AiHubToolApprovalI => ({
            arguments: DELETE_ARGUMENTS,
            componentName: null,
            decidedByUserId: 1,
            executionError: null,
            expiresAt: null,
            id: 3,
            status: 'REJECTED',
            toolName: 'deleteWorkflow',
            ...overrides,
        });

        const findApprovalCards = () =>
            aiHubStore
                .getState()
                .messages.flatMap((message) =>
                    Array.isArray(message.content)
                        ? (message.content as Array<{data?: Record<string, unknown>; type?: string}>)
                        : []
                )
                .filter((part) => part.type === 'data-tool-approval-request');

        const findContinuationRows = () =>
            aiHubStore.getState().messages.filter((message) => {
                const text =
                    typeof message.content === 'string'
                        ? message.content
                        : (message.content as unknown as Array<{text?: string}>)
                              .map((part) => part.text ?? '')
                              .join('');

                return text.startsWith('[tool-approval #');
            });

        it.each([
            ['an assistant row', gatedCallRow],
            ['the user row it was attached to', gatedCallOnUserRow],
        ])(
            'rebuilds the resolved card in place of the continuation status row (call on %s)',
            async (_label, callRow) => {
                mockGetChatMessages.mockResolvedValue([
                    callRow,
                    buildMessage({content: '[tool-approval #3 rejected]\nTool: deleteWorkflow', role: 'USER'}),
                    buildMessage({content: 'The deletion was rejected.', role: 'ASSISTANT'}),
                ]);
                mockGetToolApprovals.mockResolvedValue([buildApproval()]);

                await switchTo(buildChat());

                expect(findContinuationRows()).toEqual([]);

                const cards = findApprovalCards();

                expect(cards).toHaveLength(1);
                expect(cards[0]!.data).toMatchObject({
                    approvalId: 3,
                    arguments: {workflowId: 'a5667d0f'},
                    kind: 'tool-approval-request',
                    resolvedBy: '1',
                    resolvedStatus: 'REJECTED',
                    toolName: 'deleteWorkflow',
                });

                const messages = aiHubStore.getState().messages;
                const cardMessageIndex = messages.findIndex(
                    (message) =>
                        Array.isArray(message.content) &&
                        (message.content as Array<{type?: string}>).some(
                            (part) => part.type === 'data-tool-approval-request'
                        )
                );

                expect(messages[cardMessageIndex]!.role).toBe('assistant');
                expect(messages[cardMessageIndex + 1]!.content).toBe('The deletion was rejected.');
            }
        );

        it('drops the continuation status row when the card was restored from the persisted tool result', async () => {
            mockGetChatMessages.mockResolvedValue([
                buildMessage({
                    content: '',
                    role: 'ASSISTANT',
                    toolEventsJson: JSON.stringify([
                        {arguments: DELETE_ARGUMENTS, id: 'call-delete', kind: 'call', name: 'deleteWorkflow'},
                        {
                            id: 'call-delete',
                            kind: 'result',
                            name: 'deleteWorkflow',
                            response: JSON.stringify({
                                approvalId: 3,
                                arguments: {workflowId: 'a5667d0f'},
                                awaitingApproval: true,
                                kind: 'tool-approval-request',
                                toolName: 'deleteWorkflow',
                            }),
                        },
                    ]),
                }),
                buildMessage({content: '[tool-approval #3 rejected]\nTool: deleteWorkflow', role: 'USER'}),
            ]);
            mockGetToolApprovals.mockResolvedValue([buildApproval()]);

            await switchTo(buildChat());

            expect(findContinuationRows()).toEqual([]);
            expect(findApprovalCards()).toHaveLength(1);
            expect(findApprovalCards()[0]!.data).toMatchObject({approvalId: 3, resolvedStatus: 'REJECTED'});
        });

        it.each(['SUPERSEDED', 'EXPIRED'] as const)(
            'renders a %s approval with no continuation row from the persisted request',
            async (status) => {
                mockGetChatMessages.mockResolvedValue([
                    persistedEnvelopeRow,
                    buildMessage({content: 'Never mind, keep it.', role: 'USER'}),
                ]);
                mockGetToolApprovals.mockResolvedValue([buildApproval({decidedByUserId: null, status})]);

                await switchTo(buildChat());

                const cards = findApprovalCards();

                expect(cards).toHaveLength(1);
                expect(cards[0]!.data).toMatchObject({approvalId: 3, resolvedStatus: status});
                expect(hasPendingToolApproval(aiHubStore.getState().messages)).toBe(false);
            }
        );

        it('treats a pending approval past its expiry as expired, not actionable', async () => {
            mockGetChatMessages.mockResolvedValue([persistedEnvelopeRow]);
            mockGetToolApprovals.mockResolvedValue([
                buildApproval({decidedByUserId: null, expiresAt: '2026-01-01T00:00:00.000Z', status: 'PENDING'}),
            ]);

            await switchTo(buildChat());

            expect(findApprovalCards()[0]!.data).toMatchObject({approvalId: 3, resolvedStatus: 'EXPIRED'});
            expect(hasPendingToolApproval(aiHubStore.getState().messages)).toBe(false);
        });

        it('does not append an actionable card for an expired pending approval with no persisted request', async () => {
            mockGetChatMessages.mockResolvedValue([gatedCallRow]);
            mockGetToolApprovals.mockResolvedValue([
                buildApproval({decidedByUserId: null, expiresAt: '2026-01-01T00:00:00.000Z', status: 'PENDING'}),
            ]);

            await switchTo(buildChat());

            expect(hasPendingToolApproval(aiHubStore.getState().messages)).toBe(false);
        });

        it('rebuilds a still-pending card at the end of the transcript so it stays answerable', async () => {
            mockGetChatMessages.mockResolvedValue([gatedCallRow]);
            mockGetToolApprovals.mockResolvedValue([buildApproval({decidedByUserId: null, status: 'PENDING'})]);

            await switchTo(buildChat());

            const messages = aiHubStore.getState().messages;
            const lastMessage = messages[messages.length - 1]!;
            const lastParts = lastMessage.content as Array<{data?: Record<string, unknown>; type?: string}>;

            expect(lastMessage.role).toBe('assistant');
            expect(lastParts[0]!.type).toBe('data-tool-approval-request');
            expect(lastParts[0]!.data).toMatchObject({approvalId: 3, awaitingApproval: true});
            expect(lastParts[0]!.data!.resolvedStatus).toBeUndefined();
        });

        it('keeps the continuation status row when the approvals lookup is unavailable', async () => {
            mockGetChatMessages.mockResolvedValue([
                gatedCallRow,
                buildMessage({content: '[tool-approval #3 rejected]\nTool: deleteWorkflow', role: 'USER'}),
            ]);
            mockGetToolApprovals.mockRejectedValue(new Error('unavailable'));

            await switchTo(buildChat());

            expect(findContinuationRows()).toHaveLength(1);
            expect(findApprovalCards()).toEqual([]);
        });
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

    it('loads nothing and reports failure while the workspace id is unset', async () => {
        vi.mocked(useWorkspaceStore).mockImplementation(((
            selector: (state: {currentWorkspaceId: number | undefined}) => unknown
        ) => selector({currentWorkspaceId: undefined})) as typeof useWorkspaceStore);

        const returned = await switchTo(buildChat());

        vi.mocked(useWorkspaceStore).mockImplementation(((
            selector: (state: {currentWorkspaceId: number | undefined}) => unknown
        ) => selector({currentWorkspaceId: 99})) as typeof useWorkspaceStore);

        expect(returned).toBe(false);
        expect(mockGetChatMessages).not.toHaveBeenCalled();
        expect(mockGetToolApprovals).not.toHaveBeenCalled();
    });

    it('marks each row with its server row index and leaves client-only rows unmarked', async () => {
        mockGetChatMessages.mockResolvedValue([
            buildMessage({
                content: 'Bind a Slack connection',
                role: 'USER',
                toolEventsJson: JSON.stringify([
                    {arguments: '{}', id: 'call-list', kind: 'call', name: 'listConnections'},
                    {id: 'call-list', kind: 'result', name: 'listConnections', response: '[]'},
                ]),
            }),
            buildMessage({content: 'Pick one:', role: 'ASSISTANT'}),
            buildMessage({content: 'User picked: Slack Engineering (ID: 1051)', role: 'USER'}),
            buildMessage({content: 'Bound.', role: 'ASSISTANT'}),
        ]);

        await switchTo(buildChat());

        const {messages, nextServerRowIndex} = aiHubStore.getState();

        expect(messages.map((message) => getServerRowIndex(message))).toEqual([0, undefined, 1, 2, 3]);
        expect(nextServerRowIndex).toBe(4);
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

            expect(aiHubStore.getState().messages).toEqual([
                {content: 'Opened.', metadata: {custom: {serverRowIndex: 0}}, role: 'assistant'},
            ]);
        });
    });
});
