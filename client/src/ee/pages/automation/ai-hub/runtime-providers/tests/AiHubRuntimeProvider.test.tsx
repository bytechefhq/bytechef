import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubRunStateStore} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {MODE, aiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import RunErrorMessage from '@/shared/components/ai-chat/messages/RunErrorMessage';
import {RunErrorDataI, toRunErrorData} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {readLastUsedModel, writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {EventType} from '@ag-ui/core';
import {DataMessagePartProps, ThreadMessageLike} from '@assistant-ui/react';
import {QueryClient} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    TURN_IN_FLIGHT_MESSAGE,
    bootstrapAiHubTurnRunState,
    buildAiHubSubscriber,
    buildStateToSend,
    getForbiddenTurnRunErrorCode,
    getLastUserMessageText,
    getOptimisticTurnMarker,
    hasLivePropertyOptionPicker,
    invalidateWorkflowQueries,
    isCachedWorkflowChat,
    isForbiddenTurnError,
    isTurnInFlightError,
    projectMessagesWithToolCalls,
    removeOptimisticTurnMessages,
    runPostTurnTelemetry,
    stampOptimisticTurnMarker,
} from '../AiHubRuntimeProvider';

import type {AgentSubscriber} from '@ag-ui/client';
import type {
    RunFinishedEvent,
    RunStartedEvent,
    ToolCallEndEvent,
    ToolCallResultEvent,
    ToolCallStartEvent,
} from '@ag-ui/core';

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/ee/pages/automation/ai-hub/chats/api/chats.api')>()),
    createAiHubChat: vi.fn(),
    generateAiHubChatTitle: vi.fn(),
    patchChat: vi.fn(),
}));

const {generateAiHubChatTitle, patchChat} = await import('@/ee/pages/automation/ai-hub/chats/api/chats.api');

const mockPatchChat = vi.mocked(patchChat);
const mockGenerateChatTitle = vi.mocked(generateAiHubChatTitle);

type SubscriberParamsBaseType = Parameters<NonNullable<AgentSubscriber['onRunFinalized']>>[0];

const baseSubscriberParams = {
    agent: {} as SubscriberParamsBaseType['agent'],
    input: {} as SubscriberParamsBaseType['input'],
    messages: [] as unknown as SubscriberParamsBaseType['messages'],
    state: {} as SubscriberParamsBaseType['state'],
} as SubscriberParamsBaseType;

const makeToolCallStartParams = (
    overrides: Pick<ToolCallStartEvent, 'toolCallId' | 'toolCallName'>
): {event: ToolCallStartEvent} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {rawEvent: undefined, timestamp: undefined, type: EventType.TOOL_CALL_START, ...overrides},
});

const makeToolCallEndParams = (
    overrides: Pick<ToolCallEndEvent, 'toolCallId'> & {toolCallArgs: Record<string, unknown>}
): {
    event: ToolCallEndEvent;
    toolCallArgs: Record<string, unknown>;
    toolCallName: string;
} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {rawEvent: undefined, timestamp: undefined, toolCallId: overrides.toolCallId, type: EventType.TOOL_CALL_END},
    toolCallArgs: overrides.toolCallArgs,
    toolCallName: 'unknown',
});

const makeToolCallResultParams = (
    overrides: Pick<ToolCallResultEvent, 'content' | 'toolCallId'>
): {event: ToolCallResultEvent} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        messageId: 'msg-' + overrides.toolCallId,
        rawEvent: undefined,
        role: 'tool',
        timestamp: undefined,
        type: EventType.TOOL_CALL_RESULT,
        ...overrides,
    },
});

const makeRunFinishedParams = (): {
    event: RunFinishedEvent;
    outcome: 'success';
    pendingToolCallIds: string[];
} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        rawEvent: undefined,
        result: undefined,
        runId: 'run-1',
        threadId: 'thread-1',
        timestamp: undefined,
        type: EventType.RUN_FINISHED,
    },
    outcome: 'success',
    pendingToolCallIds: [],
});

const makeRunStartedParams = (): {event: RunStartedEvent} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        rawEvent: undefined,
        runId: 'run-1',
        threadId: 'thread-1',
        timestamp: undefined,
        type: EventType.RUN_STARTED,
    },
});

type RunErrorParamsType = Parameters<NonNullable<AgentSubscriber['onRunErrorEvent']>>[0];

type CustomEventParamsType = Parameters<NonNullable<AgentSubscriber['onCustomEvent']>>[0];

const makeCustomEventParams = (name: string, value: unknown) =>
    ({...baseSubscriberParams, event: {name, type: EventType.CUSTOM, value}}) as CustomEventParamsType;

const makeRunErrorParams = (message: string) => ({event: {message}}) as RunErrorParamsType;

type TextMessageStartParamsType = Parameters<NonNullable<AgentSubscriber['onTextMessageStartEvent']>>[0];

type TextMessageContentParamsType = Parameters<NonNullable<AgentSubscriber['onTextMessageContentEvent']>>[0];

const makeTextMessageStartParams = () =>
    ({
        ...baseSubscriberParams,
        event: {messageId: 'message-1', role: 'assistant', type: EventType.TEXT_MESSAGE_START},
    }) as TextMessageStartParamsType;

const makeTextMessageContentParams = (delta: string) =>
    ({
        ...baseSubscriberParams,
        event: {delta, messageId: 'message-1', type: EventType.TEXT_MESSAGE_CONTENT},
        textMessageBuffer: '',
    }) as TextMessageContentParamsType;

function deliverToolResult(subscriber: AgentSubscriber, toolCallId: string, toolCallName: string, content: string) {
    subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId, toolCallName}));
    subscriber.onToolCallResultEvent!(makeToolCallResultParams({content, toolCallId}));
}

const askUserQuestionPayload = {
    awaitingAnswer: true,
    kind: 'ask-user-question',
    questions: [{multiSelect: false, options: [{label: 'Gmail'}, {label: 'Outlook'}], question: 'Which inbox?'}],
};

const toolApprovalPayload = {
    approvalId: 12,
    arguments: {workflowId: 'wf-1'},
    awaitingApproval: true,
    kind: 'tool-approval-request',
    toolName: 'deleteWorkflow',
};

describe('isTurnInFlightError', () => {
    it('recognises the 409 body as an HttpAgent payload', () => {
        expect(isTurnInFlightError({payload: {error: 'TURN_IN_FLIGHT'}})).toBe(true);
    });

    it('recognises the 409 body inside an `HTTP 409: <json>` message', () => {
        expect(isTurnInFlightError({message: 'HTTP 409: {"error":"TURN_IN_FLIGHT"}'})).toBe(true);
    });

    it('rejects a different error code', () => {
        expect(isTurnInFlightError({message: 'HTTP 409: {"error":"SOME_OTHER_CONFLICT"}'})).toBe(false);
        expect(isTurnInFlightError({payload: {error: 'OTHER'}})).toBe(false);
    });

    it('rejects a message without embedded JSON, invalid JSON and missing input', () => {
        expect(isTurnInFlightError({message: 'boom'})).toBe(false);
        expect(isTurnInFlightError({message: 'HTTP 409: {not json'})).toBe(false);
        expect(isTurnInFlightError(null)).toBe(false);
        expect(isTurnInFlightError(undefined)).toBe(false);
    });
});

describe('getForbiddenTurnRunErrorCode', () => {
    it('maps the workspace role refusal from the parsed payload', () => {
        expect(
            getForbiddenTurnRunErrorCode({
                payload: {error: 'Workspace is not accessible to the current user'},
                status: 403,
            })
        ).toBe('WORKSPACE_ACCESS_DENIED');
    });

    it('maps the chat refusal from the message when there is no payload', () => {
        expect(
            getForbiddenTurnRunErrorCode({
                message: 'HTTP 403: {"error":"AiHubChat is not accessible to the current user"}',
            })
        ).toBe('CHAT_ACCESS_DENIED');
    });

    it('falls back to the neutral code for any other refusal', () => {
        expect(getForbiddenTurnRunErrorCode({payload: {error: 'Access denied'}, status: 403})).toBe('ACCESS_DENIED');
        expect(getForbiddenTurnRunErrorCode({message: 'HTTP 403: ', status: 403})).toBe('ACCESS_DENIED');
    });
});

describe('isForbiddenTurnError', () => {
    it('recognises the HttpAgent rejection of a 403 by its status', () => {
        expect(isForbiddenTurnError({message: 'HTTP 403: {}', status: 403})).toBe(true);
    });

    it('recognises a 403 from its message when the status is missing', () => {
        expect(
            isForbiddenTurnError({message: 'HTTP 403: {"error":"Workspace is not accessible to the current user"}'})
        ).toBe(true);
    });

    it('does not match other failures', () => {
        expect(isForbiddenTurnError({message: 'HTTP 409: {"error":"TURN_IN_FLIGHT"}', status: 409})).toBe(false);
        expect(isForbiddenTurnError({message: 'HTTP 4031 is not a status'})).toBe(false);
        expect(isForbiddenTurnError(null)).toBe(false);
    });
});

describe('getLastUserMessageText', () => {
    it('returns the text of the latest user message, skipping later assistant rows', () => {
        expect(
            getLastUserMessageText([
                {content: 'First', role: 'user'},
                {content: 'Reply', role: 'assistant'},
                {content: [{text: 'Delete the Demo project', type: 'text'}], role: 'user'},
                {content: 'Approve it?', role: 'assistant'},
            ])
        ).toBe('Delete the Demo project');
    });

    it('returns an empty string for a transcript without a user message', () => {
        expect(getLastUserMessageText([{content: 'Hello', role: 'assistant'}])).toBe('');
    });
});

describe('stampOptimisticTurnMarker / getOptimisticTurnMarker', () => {
    it('round-trips the marker through metadata.custom', () => {
        const stamped = stampOptimisticTurnMarker({content: 'hi', role: 'user'}, 'marker-1');

        expect(getOptimisticTurnMarker(stamped)).toBe('marker-1');
    });

    it('returns undefined for a message that was never stamped', () => {
        expect(getOptimisticTurnMarker({content: 'hi', role: 'user'})).toBeUndefined();
    });

    it('preserves any existing metadata.custom fields on the message rather than replacing them', () => {
        const stamped = stampOptimisticTurnMarker(
            {content: 'hi', metadata: {custom: {source: 'composer'}}, role: 'user'},
            'marker-1'
        );

        expect(stamped.metadata?.custom).toEqual({optimisticTurnMarker: 'marker-1', source: 'composer'});
    });
});

describe('removeOptimisticTurnMessages', () => {
    it('removes exactly the messages carrying the given marker, leaving every other message untouched', () => {
        const serverMessage: ThreadMessageLike = {content: 'earlier reply', role: 'assistant'};

        aiHubStore.setState({
            messages: [
                serverMessage,
                stampOptimisticTurnMarker({content: 'hi', role: 'user'}, 'marker-1'),
                stampOptimisticTurnMarker({content: '', role: 'assistant'}, 'marker-1'),
            ],
        });

        removeOptimisticTurnMessages('marker-1');

        expect(aiHubStore.getState().messages).toEqual([serverMessage]);
    });

    it('is a no-op when nothing in the store carries the given marker', () => {
        const untaggedMessages: ThreadMessageLike[] = [
            {content: 'a', role: 'user'},
            {content: 'b', role: 'assistant'},
        ];

        aiHubStore.setState({messages: untaggedMessages});

        removeOptimisticTurnMessages('marker-that-does-not-exist');

        expect(aiHubStore.getState().messages).toEqual(untaggedMessages);
    });
});

describe('buildAiHubSubscriber', () => {
    beforeEach(() => {
        aiHubTabsStore.getState().reset();
        aiHubChatsStore.getState().reset();
        aiHubStore.setState({chatId: 'thread-1', messages: []});
        aiChatRetryableErrorStore.setState({currentError: undefined});
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
    });

    const createSubscriber = (overrides: Partial<Parameters<typeof buildAiHubSubscriber>[0]> = {}) =>
        buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            chatId: 'thread-1',
            getLastUserMessage: vi.fn().mockReturnValue(''),
            ...overrides,
        });

    it('attaches a tool call to its assistant message after the loaded history was prepended', () => {
        aiHubStore.setState({
            messages: [
                {content: 'sent while loading', id: 'user-new', role: 'user'},
                {content: '', id: 'assistant-new', role: 'assistant'},
            ],
        });

        const subscriber = createSubscriber({assistantMessageIndex: 1});

        aiHubStore.setState((state) => ({
            messages: [
                {content: 'earlier question', id: 'history-1', role: 'user'},
                {content: 'earlier answer', id: 'history-2', role: 'assistant'},
                ...state.messages,
            ],
        }));

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-1', toolCallName: 'listProjects'}));

        expect(aiChatToolCallStore.getState().toolCalls['call-1']?.messageIndex).toBe(3);
    });

    describe('openWorkflowTab', () => {
        const openWorkflowTabResult = JSON.stringify({
            name: 'Leads Sync',
            opened: true,
            projectId: 'proj-1',
            projectWorkflowId: 42,
            workflowId: 'wf-1',
        });

        it('opens a workflow tab when the openWorkflowTab tool result arrives', () => {
            deliverToolResult(createSubscriber(), 'call-wf', 'openWorkflowTab', openWorkflowTabResult);

            const state = aiHubTabsStore.getState();

            expect(state.openTabs).toHaveLength(1);
            expect(state.openTabs[0]).toMatchObject({
                kind: 'workflow',
                name: 'Leads Sync',
                projectId: 'proj-1',
                projectWorkflowId: 42,
                workflowId: 'wf-1',
            });
            expect(state.activeTabId).toBe(state.openTabs[0]!.id);
        });

        it('focuses the open tab when a second result for the same workflow arrives', () => {
            const subscriber = createSubscriber();

            deliverToolResult(subscriber, 'call-wf-1', 'openWorkflowTab', openWorkflowTabResult);

            const firstTabId = aiHubTabsStore.getState().openTabs[0]!.id;

            aiHubTabsStore.getState().openWorkflowTab({
                name: 'Other',
                projectId: 'proj-2',
                projectWorkflowId: 7,
                workflowId: 'wf-2',
            });

            expect(aiHubTabsStore.getState().activeTabId).not.toBe(firstTabId);

            deliverToolResult(subscriber, 'call-wf-2', 'openWorkflowTab', openWorkflowTabResult);

            const state = aiHubTabsStore.getState();

            expect(state.openTabs.filter((tab) => tab.workflowId === 'wf-1')).toHaveLength(1);
            expect(state.activeTabId).toBe(firstTabId);
        });

        it('surfaces a retryable error when the tool reports opened:false and the turn ends unrecovered', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-wf',
                'openWorkflowTab',
                JSON.stringify({error: 'Workflow not found', opened: false})
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(aiChatRetryableErrorStore.getState().currentError).toMatchObject({
                errorMessage: 'Workflow not found',
                toolName: 'openWorkflowTab',
            });
        });

        it('raises the retry banner for an unparseable result only once the run finishes', () => {
            vi.spyOn(console, 'warn').mockImplementation(() => undefined);

            const subscriber = createSubscriber();

            deliverToolResult(subscriber, 'call-wf', 'openWorkflowTab', 'not-json');

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError?.errorMessage).toBe('unparseable result');
        });

        it('raises no retry banner for an opened:false result when the run then fails', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-wf',
                'openWorkflowTab',
                JSON.stringify({error: 'Workflow not found', opened: false})
            );

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();

            subscriber.onRunErrorEvent!(makeRunErrorParams('Provider overloaded'));

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
        });

        it('raises the retry banner for a malformed picker payload only once the run finishes unrecovered', () => {
            const subscriber = createSubscriber();

            deliverToolResult(subscriber, 'call-pick', 'selectConnection', JSON.stringify({kind: 'select-connection'}));

            expect(aiChatToolCallStore.getState().toolCalls['call-pick']!.status).toBe('error');
            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toMatchObject({toolName: 'selectConnection'});
        });

        it('raises no retry banner for a malformed picker payload the model recovers from', () => {
            const subscriber = createSubscriber();

            deliverToolResult(subscriber, 'call-pick', 'selectConnection', JSON.stringify({kind: 'select-connection'}));
            deliverToolResult(
                subscriber,
                'call-pick-retry',
                'selectConnection',
                JSON.stringify({componentName: 'slack', kind: 'select-connection'})
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
        });

        it('ignores the same payload from a tool that is not openWorkflowTab', () => {
            deliverToolResult(createSubscriber(), 'call-other', 'someOtherTool', openWorkflowTabResult);

            expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
        });
    });

    describe('workflow-mutating tools', () => {
        it('notifies onWorkflowMutated with the tool result for buildWorkflow and importWorkflow', () => {
            const onWorkflowMutated = vi.fn();
            const subscriber = createSubscriber({onWorkflowMutated});

            deliverToolResult(subscriber, 'call-build', 'buildWorkflow', 'Workflow updated.');
            deliverToolResult(subscriber, 'call-import', 'importWorkflow', 'Imported.');
            deliverToolResult(subscriber, 'call-other', 'listProjects', '[]');

            expect(onWorkflowMutated).toHaveBeenCalledTimes(2);
            expect(onWorkflowMutated).toHaveBeenNthCalledWith(1, 'Workflow updated.');
        });

        it('notifies onWorkflowMutated when an approved call resumes in a continuation run', () => {
            const onWorkflowMutated = vi.fn();

            createSubscriber().onToolCallStartEvent!(
                makeToolCallStartParams({toolCallId: 'call-gated', toolCallName: 'buildWorkflow'})
            );

            createSubscriber({onWorkflowMutated}).onToolCallResultEvent!(
                makeToolCallResultParams({content: 'Workflow updated.', toolCallId: 'call-gated'})
            );

            expect(onWorkflowMutated).toHaveBeenCalledTimes(1);
        });

        it('does not warn about a prose tool result and keeps the prose as the result', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

            deliverToolResult(createSubscriber(), 'call-build', 'buildWorkflow', 'The workflow **H6 Check** is built.');

            expect(consoleWarnSpy).not.toHaveBeenCalled();
            expect(aiChatToolCallStore.getState().toolCalls['call-build']).toMatchObject({
                result: 'The workflow **H6 Check** is built.',
                status: 'success',
            });

            consoleWarnSpy.mockRestore();
        });

        it('parses a JSON string tool result to its text', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

            deliverToolResult(createSubscriber(), 'call-scalar', 'getValue', '"done"');

            expect(aiChatToolCallStore.getState().toolCalls['call-scalar']).toMatchObject({
                result: 'done',
                status: 'success',
            });
            expect(consoleWarnSpy).not.toHaveBeenCalled();

            consoleWarnSpy.mockRestore();
        });

        it.each(['42', '-1.5', 'true', 'false', 'null'])(
            'keeps the bare JSON literal tool result %s as text, silently',
            (content) => {
                const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

                deliverToolResult(createSubscriber(), 'call-scalar', 'getValue', content);

                expect(aiChatToolCallStore.getState().toolCalls['call-scalar']).toMatchObject({
                    result: content,
                    status: 'success',
                });
                expect(consoleWarnSpy).not.toHaveBeenCalled();

                consoleWarnSpy.mockRestore();
            }
        );

        it('keeps prose that starts like a JSON literal as text, silently', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

            deliverToolResult(createSubscriber(), 'call-prose', 'buildWorkflow', 'no workflows matched; true for now');

            expect(aiChatToolCallStore.getState().toolCalls['call-prose']).toMatchObject({
                result: 'no workflows matched; true for now',
                status: 'success',
            });
            expect(consoleWarnSpy).not.toHaveBeenCalled();

            consoleWarnSpy.mockRestore();
        });

        it('still parses a JSON array tool result', () => {
            deliverToolResult(createSubscriber(), 'call-list', 'listProjects', ' [{"id":1}]');

            expect(aiChatToolCallStore.getState().toolCalls['call-list']).toMatchObject({
                result: [{id: 1}],
                status: 'success',
            });
        });

        it('invalidates ProjectWorkflowKeys for the project of the built workflow', () => {
            const queryClient = new QueryClient();
            const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

            aiHubTabsStore
                .getState()
                .openWorkflowTab({name: 'Leads', projectId: '7', projectWorkflowId: 3, workflowId: 'wf-7'});

            const subscriber = createSubscriber({
                onWorkflowMutated: (toolResultContent) => invalidateWorkflowQueries(queryClient, toolResultContent),
            });

            deliverToolResult(subscriber, 'call-build', 'buildWorkflow', 'Workflow updated.');

            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectWorkflowKeys.projectWorkflows(7)});
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectWorkflowKeys.workflows});
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
        });

        it('notifies onWorkflowMutated for every workflow-mutating hub tool', () => {
            const onWorkflowMutated = vi.fn();
            const subscriber = createSubscriber({onWorkflowMutated});
            const workflowMutatingToolNames = [
                'createProjectWorkflow',
                'deleteProject',
                'deleteWorkflow',
                'publishProject',
                'saveWorkflowTestConnection',
            ];

            workflowMutatingToolNames.forEach((toolName) =>
                deliverToolResult(subscriber, `call-${toolName}`, toolName, 'Done.')
            );

            expect(onWorkflowMutated).toHaveBeenCalledTimes(workflowMutatingToolNames.length);
        });

        it('invalidates the workflow test configurations after a successful saveWorkflowTestConnection', () => {
            const queryClient = new QueryClient();
            const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

            aiHubTabsStore
                .getState()
                .openWorkflowTab({name: 'Leads', projectId: '7', projectWorkflowId: 3, workflowId: 'wf-7'});

            const subscriber = createSubscriber({
                onWorkflowMutated: (toolResultContent) => invalidateWorkflowQueries(queryClient, toolResultContent),
            });

            deliverToolResult(
                subscriber,
                'call-save-connection',
                'saveWorkflowTestConnection',
                'Test connection saved for node slack_1.'
            );

            expect(invalidateQueriesSpy).toHaveBeenCalledWith({
                queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations,
            });
            expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ProjectWorkflowKeys.projectWorkflows(7)});
        });

        it('does not invalidate anything when saveWorkflowTestConnection returns an error envelope', () => {
            const queryClient = new QueryClient();
            const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

            const subscriber = createSubscriber({
                onWorkflowMutated: (toolResultContent) => invalidateWorkflowQueries(queryClient, toolResultContent),
            });

            deliverToolResult(
                subscriber,
                'call-save-connection',
                'saveWorkflowTestConnection',
                JSON.stringify({error: 'Connection 12 is not accessible'})
            );

            expect(invalidateQueriesSpy).not.toHaveBeenCalled();
        });

        it('adds a data-ask-user-question part when buildWorkflow relays a question, and the run ends idle', () => {
            const addMessage = vi.fn();
            const onSettle = vi.fn(() => aiHubRunStateStore.getState().setChatRunning('thread-1', false));

            aiHubRunStateStore.getState().setChatRunning('thread-1', true);
            aiHubChatsStore.getState().setActivityState('thread-1', 'running');

            const subscriber = createSubscriber({addMessage, runLifecycle: {onSettle}});

            deliverToolResult(subscriber, 'call-build', 'buildWorkflow', JSON.stringify(askUserQuestionPayload));

            expect(addMessage).toHaveBeenCalledWith({
                content: [
                    {
                        data: {
                            awaitingAnswer: true,
                            kind: 'ask-user-question',
                            questions: askUserQuestionPayload.questions,
                        },
                        type: 'data-ask-user-question',
                    },
                ],
                role: 'assistant',
            });

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(onSettle).toHaveBeenCalledOnce();
            expect(aiHubRunStateStore.getState().runningByChat['thread-1']).toBe(false);
            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
        });
    });

    describe('ask-workflow-question', () => {
        const workflowQuestion = {
            header: 'Plan',
            options: [{description: 'Monthly billing', label: 'Monthly'}, {label: 'Yearly'}],
            question: 'Which plan?',
        };

        it('routes the question into the question card and keeps the chat paused for the answer', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            aiHubChatsStore.getState().setActivityState('thread-1', 'running');

            subscriber.onCustomEvent!(
                makeCustomEventParams('ask-workflow-question', {
                    questions: [workflowQuestion],
                    resumeUrl: 'http://localhost/resume/1',
                })
            );

            expect(addMessage).toHaveBeenCalledWith({
                content: [
                    {
                        data: {
                            answerFormat: 'plain',
                            awaitingAnswer: true,
                            kind: 'ask-user-question',
                            questions: [{...workflowQuestion, multiSelect: false}],
                        },
                        type: 'data-ask-user-question',
                    },
                ],
                role: 'assistant',
            });
            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBe('paused');

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBe('paused');
        });

        it('shows a question without a resume link but does not wait for an answer', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            aiHubChatsStore.getState().setActivityState('thread-1', 'running');

            subscriber.onCustomEvent!(
                makeCustomEventParams('ask-workflow-question', {
                    questions: [{...workflowQuestion, multiSelect: true}],
                })
            );

            expect(addMessage.mock.calls[0]![0].content[0]).toEqual({
                data: {
                    answerFormat: 'plain',
                    awaitingAnswer: false,
                    kind: 'ask-user-question',
                    questions: [{...workflowQuestion, multiSelect: true}],
                },
                type: 'data-ask-user-question',
            });

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
        });

        it('drops a malformed option but keeps the question and its valid options', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            subscriber.onCustomEvent!(
                makeCustomEventParams('ask-workflow-question', {
                    questions: [
                        {
                            options: [
                                {label: 'Monthly'},
                                {label: 42},
                                'Yearly',
                                null,
                                {description: 7, label: 'Weekly'},
                                {description: 'Pay once', label: 'Lifetime'},
                            ],
                            question: 'Which plan?',
                        },
                    ],
                })
            );

            expect(addMessage.mock.calls[0]![0].content[0].data.questions).toEqual([
                {
                    header: undefined,
                    multiSelect: false,
                    options: [{label: 'Monthly'}, {description: 'Pay once', label: 'Lifetime'}],
                    question: 'Which plan?',
                },
            ]);
        });

        it('drops a question without question text and keeps the valid ones', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            subscriber.onCustomEvent!(
                makeCustomEventParams('ask-workflow-question', {
                    questions: [{options: [{label: 'A'}]}, {options: [], question: 7}, 'bad', workflowQuestion],
                })
            );

            expect(addMessage.mock.calls[0]![0].content[0].data.questions).toEqual([
                {...workflowQuestion, multiSelect: false},
            ]);
        });

        it('renders no card when every question is malformed', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            subscriber.onCustomEvent!(
                makeCustomEventParams('ask-workflow-question', {
                    questions: [{options: [{label: 'A'}]}, {question: ''}, {question: '   '}, null],
                    resumeUrl: 'http://localhost/resume/1',
                })
            );

            expect(addMessage).not.toHaveBeenCalled();
            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
        });

        it('ignores a malformed question, another custom event, and a question for a chat no longer shown', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            subscriber.onCustomEvent!(makeCustomEventParams('ask-workflow-question', {questions: 'nope'}));
            subscriber.onCustomEvent!(makeCustomEventParams('workflow-event', {questions: [workflowQuestion]}));

            aiHubStore.setState({chatId: 'thread-2'});

            subscriber.onCustomEvent!(makeCustomEventParams('ask-workflow-question', {questions: [workflowQuestion]}));

            expect(addMessage).not.toHaveBeenCalled();
            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBeUndefined();
        });
    });

    describe('openWorkflowChatTab', () => {
        it('switches to the chat named by an openWorkflowChatTab result', () => {
            const onChatOpened = vi.fn();
            const subscriber = createSubscriber({onChatOpened});

            aiHubStore.setState({messages: [{content: 'Open the support chat', role: 'user'}]});

            deliverToolResult(
                subscriber,
                'call-open-chat',
                'openWorkflowChatTab',
                JSON.stringify({chatId: 77, opened: true, threadId: 'thread-77', title: 'Acme — Support'})
            );

            expect(aiHubStore.getState().chatId).toBe('thread-77');
            expect(aiHubStore.getState().messages).toEqual([]);
            expect(aiHubChatsStore.getState().currentChatId).toBe(77);
            expect(onChatOpened).toHaveBeenCalledOnce();
        });

        it('switches to the chat named by the exact JSON the server tool returns', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-open-chat',
                'openWorkflowChatTab',
                '{"opened":true,"chatId":101,"threadId":"00000000-0000-0000-0000-00000000abc3","title":"Support bot"}'
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubStore.getState().chatId).toBe('00000000-0000-0000-0000-00000000abc3');
            expect(aiHubChatsStore.getState().currentChatId).toBe(101);
            expect(aiChatRetryableErrorStore.getState().currentError).toBeUndefined();
        });

        it('stays in the chat and surfaces the refusal the server tool returns for an unknown thread', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-open-chat',
                'openWorkflowChatTab',
                '{"error":"No workflow chat with threadId 00000000-0000-0000-0000-00000000abc3. Pass the threadId returned from createWorkflowChat."}'
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubStore.getState().chatId).toBe('thread-1');
            expect(aiChatRetryableErrorStore.getState().currentError).toMatchObject({
                toolName: 'openWorkflowChatTab',
            });
        });

        it('raises the retry banner when openWorkflowChatTab reports a failure, and stays in the chat', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-open-chat',
                'openWorkflowChatTab',
                JSON.stringify({error: 'Workflow chat not found', opened: false})
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubStore.getState().chatId).toBe('thread-1');
            expect(aiChatRetryableErrorStore.getState().currentError).toMatchObject({
                errorMessage: 'Workflow chat not found',
                toolName: 'openWorkflowChatTab',
            });
        });
    });

    describe('tool approval requests', () => {
        it('adds a data-tool-approval-request part and marks the run paused-for-approval', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({addMessage});

            aiHubChatsStore.getState().setActivityState('thread-1', 'running');

            deliverToolResult(subscriber, 'call-gated', 'deleteWorkflow', JSON.stringify(toolApprovalPayload));

            expect(addMessage.mock.calls[0]![0].content[0].type).toBe('data-tool-approval-request');
            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBe('paused');

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiHubChatsStore.getState().chatActivity['thread-1']).toBe('paused');
        });
    });

    describe('run lifecycle', () => {
        it('onRunFinishedEvent fires runLifecycle.onSettle so the running flag is released even if runAgent hangs', () => {
            const onSettle = vi.fn();

            createSubscriber({runLifecycle: {onSettle}}).onRunFinishedEvent!(makeRunFinishedParams());

            expect(onSettle).toHaveBeenCalledOnce();
        });

        it('onRunFinishedEvent terminates orphan running tool calls of this chat as aborted', () => {
            const subscriber = createSubscriber();

            subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'orphan', toolCallName: 'x'}));
            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatToolCallStore.getState().toolCalls['orphan']!.status).toBe('aborted');
        });

        it('onRunErrorEvent fires runLifecycle.onSettle and appends a data-run-error message', () => {
            const addMessage = vi.fn();
            const onSettle = vi.fn();

            createSubscriber({addMessage, runLifecycle: {onSettle}}).onRunErrorEvent!(
                makeRunErrorParams('Anthropic 400: bad request')
            );

            expect(onSettle).toHaveBeenCalledOnce();
            expect(addMessage).toHaveBeenCalledWith({
                content: [{data: {message: 'Anthropic 400: bad request'}, type: 'data-run-error'}],
                role: 'assistant',
            });
        });

        it('onRunErrorEvent maps a coded NO_AI_PROVIDER error to the shared message and the AI Providers link', () => {
            const addMessage = vi.fn();

            createSubscriber({addMessage}).onRunErrorEvent!({
                event: {code: 'NO_AI_PROVIDER', message: 'No AI provider is enabled in environment 0.'},
            } as RunErrorParamsType);

            const runErrorPart = addMessage.mock.calls[0][0].content[0] as {
                data: RunErrorDataI;
                type: string;
            };

            expect(runErrorPart.type).toBe('data-run-error');
            expect(runErrorPart.data).toEqual({
                code: 'NO_AI_PROVIDER',
                message: toRunErrorData({code: 'NO_AI_PROVIDER'}).message,
            });

            render(
                <MemoryRouter>
                    <RunErrorMessage {...({data: runErrorPart.data} as DataMessagePartProps<RunErrorDataI>)} />
                </MemoryRouter>
            );

            expect(screen.getByRole('alert')).toHaveTextContent('No AI provider is set up for this environment.');
            expect(screen.getByRole('link', {name: 'Open AI Providers'})).toBeInTheDocument();
        });

        it('onRunErrorEvent strips the Java exception FQCN prefix when the payload is not JSON', () => {
            const addMessage = vi.fn();

            createSubscriber({addMessage}).onRunErrorEvent!(
                makeRunErrorParams('java.lang.IllegalStateException: Current user is not set!')
            );

            expect(addMessage).toHaveBeenCalledWith({
                content: [{data: {message: 'Current user is not set!'}, type: 'data-run-error'}],
                role: 'assistant',
            });
        });

        it('onRunErrorEvent falls back to a generic message when the event payload has no message', () => {
            const addMessage = vi.fn();

            createSubscriber({addMessage}).onRunErrorEvent!(makeRunErrorParams(''));

            expect(addMessage).toHaveBeenCalledWith({
                content: [
                    {data: {message: 'The agent run failed before completing this turn.'}, type: 'data-run-error'},
                ],
                role: 'assistant',
            });
        });

        it('onRunErrorEvent suppresses the red callout when the user aborted the turn (Stop button)', () => {
            const addMessage = vi.fn();
            const onSettle = vi.fn();
            const controller = new AbortController();

            controller.abort();

            createSubscriber({addMessage, runLifecycle: {onSettle}, turnAbortSignal: controller.signal})
                .onRunErrorEvent!(makeRunErrorParams('BodyStreamBuffer was aborted'));

            expect(addMessage).not.toHaveBeenCalled();
            expect(onSettle).toHaveBeenCalledOnce();
        });

        it('drops events from a previous chat after a chat switch', () => {
            const addMessage = vi.fn();
            const appendToLastAssistantMessage = vi.fn();
            const subscriber = createSubscriber({addMessage, appendToLastAssistantMessage});

            aiHubStore.setState({chatId: 'thread-2'});

            subscriber.onRunErrorEvent!(makeRunErrorParams('boom'));
            deliverToolResult(subscriber, 'call-late', 'deleteWorkflow', JSON.stringify(toolApprovalPayload));

            expect(addMessage).not.toHaveBeenCalled();
            expect(aiChatToolCallStore.getState().toolCalls['call-late']).toBeUndefined();
        });
    });

    describe('onRunStartedEvent', () => {
        it('tells the turn the server accepted it', () => {
            const onRunStarted = vi.fn();

            createSubscriber({onRunStarted}).onRunStartedEvent!(makeRunStartedParams());

            expect(onRunStarted).toHaveBeenCalledOnce();
        });

        it('stays quiet for a turn whose chat is no longer shown', () => {
            const onRunStarted = vi.fn();
            const subscriber = createSubscriber({onRunStarted});

            aiHubStore.setState({chatId: 'thread-2'});

            subscriber.onRunStartedEvent!(makeRunStartedParams());

            expect(onRunStarted).not.toHaveBeenCalled();
        });
    });

    describe('onRunErrorEvent for 409 TURN_IN_FLIGHT', () => {
        const turnInFlightMessage = 'HTTP 409: {"error":"TURN_IN_FLIGHT"}';

        it('invokes onTurnInFlight instead of appending the red error bubble, and still releases the run', () => {
            const addMessage = vi.fn();
            const onSettle = vi.fn();
            const onTurnInFlight = vi.fn();

            createSubscriber({addMessage, onTurnInFlight, runLifecycle: {onSettle}}).onRunErrorEvent!(
                makeRunErrorParams(turnInFlightMessage)
            );

            expect(addMessage).not.toHaveBeenCalled();
            expect(onTurnInFlight).toHaveBeenCalledWith('thread-1');
            expect(onSettle).toHaveBeenCalledOnce();
        });

        it('falls through to the ordinary red error bubble for a genuinely unrelated 409', () => {
            const addMessage = vi.fn();
            const onTurnInFlight = vi.fn();

            createSubscriber({addMessage, onTurnInFlight}).onRunErrorEvent!(
                makeRunErrorParams('HTTP 409: {"error":"SOME_OTHER_CONFLICT"}')
            );

            expect(onTurnInFlight).not.toHaveBeenCalled();
            expect(addMessage).toHaveBeenCalledWith({
                content: [{data: {message: 'HTTP 409: {"error":"SOME_OTHER_CONFLICT"}'}, type: 'data-run-error'}],
                role: 'assistant',
            });
        });
    });

    describe('interactive tool results', () => {
        it('collapses a duplicate selectPropertyOption picker for the same property', () => {
            const pickerPayload = {
                componentName: 'slack',
                kind: 'select-property-option',
                options: [{label: '#general', value: 'C1'}],
                propertyName: 'channel',
            };

            aiHubStore.setState({
                messages: [{content: [{data: pickerPayload, type: 'data-select-property-option'}], role: 'assistant'}],
            });

            const addMessage = vi.fn();

            deliverToolResult(
                createSubscriber({addMessage}),
                'call-spo',
                'selectPropertyOption',
                JSON.stringify(pickerPayload)
            );

            expect(addMessage).not.toHaveBeenCalled();
        });

        it('createConnection tool result calls addMessage with a data-create-connection part', () => {
            const addMessage = vi.fn();

            deliverToolResult(
                createSubscriber({addMessage}),
                'call-cc',
                'createConnection',
                JSON.stringify({componentLabel: 'Slack', componentName: 'slack', kind: 'create-connection'})
            );

            expect(addMessage.mock.calls[0]![0].content[0]).toMatchObject({
                data: {componentLabel: 'Slack', componentName: 'slack'},
                type: 'data-create-connection',
            });
        });

        it('raises the retry banner for a tool error still unrecovered when the turn ends', () => {
            const subscriber = createSubscriber({
                getLastUserMessage: vi.fn().mockReturnValue('Create a report for Q1'),
            });

            deliverToolResult(
                subscriber,
                'call-err',
                'createReport',
                JSON.stringify({error: 'Report service unavailable'})
            );

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
            expect(aiChatToolCallStore.getState().toolCalls['call-err']!.status).toBe('error');

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toEqual({
                errorMessage: 'Report service unavailable',
                lastUserMessage: 'Create a report for Q1',
                toolName: 'createReport',
            });
        });

        it('does not raise the retry banner for a tool error the model recovers from', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-err',
                'createReport',
                JSON.stringify({error: 'Report service unavailable'})
            );
            deliverToolResult(subscriber, 'call-retry', 'createReport', JSON.stringify({id: 7}));

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
            expect(aiChatToolCallStore.getState().toolCalls['call-err']!.status).toBe('error');
            expect(aiChatToolCallStore.getState().toolCalls['call-retry']!.status).toBe('success');
        });

        it('does not raise the retry banner for a tool error the model then explains in text', () => {
            const subscriber = createSubscriber({
                getLastUserMessage: vi.fn().mockReturnValue('Post signups to Slack'),
            });

            deliverToolResult(
                subscriber,
                'call-lookup',
                'lookupActionPropertyOptions',
                JSON.stringify({error: 'lookupActionPropertyOptions failed (ConfigurationException)'})
            );

            subscriber.onTextMessageStartEvent!(makeTextMessageStartParams());
            subscriber.onTextMessageContentEvent!(
                makeTextMessageContentParams('The channel lookup failed. Which Slack channel should I use?')
            );

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
        });

        it('does not raise the retry banner for a tool error followed by a question card', () => {
            const subscriber = createSubscriber({
                getLastUserMessage: vi.fn().mockReturnValue('Post signups to Slack'),
            });

            deliverToolResult(
                subscriber,
                'call-lookup',
                'lookupActionPropertyOptions',
                JSON.stringify({error: 'lookupActionPropertyOptions failed (ConfigurationException)'})
            );
            deliverToolResult(subscriber, 'call-ask', 'askUserQuestion', JSON.stringify(askUserQuestionPayload));

            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
            expect(aiChatToolCallStore.getState().toolCalls['call-lookup']!.status).toBe('error');
        });

        it('shows only the run error bubble, not the retry banner, when the run fails after a tool error', () => {
            const addMessage = vi.fn();
            const subscriber = createSubscriber({
                addMessage,
                getLastUserMessage: vi.fn().mockReturnValue('Create a report for Q1'),
            });

            deliverToolResult(
                subscriber,
                'call-err',
                'createReport',
                JSON.stringify({error: 'Report service unavailable'})
            );

            subscriber.onRunErrorEvent!(makeRunErrorParams('Provider overloaded'));

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
            expect(addMessage).toHaveBeenCalledWith(
                expect.objectContaining({content: [expect.objectContaining({type: 'data-run-error'})]})
            );
            expect(aiChatToolCallStore.getState().toolCalls['call-err']!.status).toBe('error');
        });

        it('raises neither the retry banner nor a bubble when the user stops a turn after a tool error', () => {
            const addMessage = vi.fn();
            const controller = new AbortController();
            const subscriber = createSubscriber({addMessage, turnAbortSignal: controller.signal});

            deliverToolResult(
                subscriber,
                'call-err',
                'createReport',
                JSON.stringify({error: 'Report service unavailable'})
            );

            controller.abort();

            subscriber.onRunErrorEvent!(makeRunErrorParams('BodyStreamBuffer was aborted'));

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
            expect(addMessage).not.toHaveBeenCalled();
        });

        it('does not carry a tool error from a failed run into the banner of a later finish', () => {
            const subscriber = createSubscriber();

            deliverToolResult(
                subscriber,
                'call-err',
                'createReport',
                JSON.stringify({error: 'Report service unavailable'})
            );

            subscriber.onRunErrorEvent!(makeRunErrorParams('Provider overloaded'));
            subscriber.onRunFinishedEvent!(makeRunFinishedParams());

            expect(aiChatRetryableErrorStore.getState().currentError).toBeFalsy();
        });
    });

    describe('tool-call store routing', () => {
        it('onToolCallStartEvent registers a running entry tagged with the assistant message index and chat', () => {
            createSubscriber({assistantMessageIndex: 4}).onToolCallStartEvent!(
                makeToolCallStartParams({toolCallId: 'call-x', toolCallName: 'listProjects'})
            );

            expect(aiChatToolCallStore.getState().toolCalls['call-x']).toMatchObject({
                chatId: 'thread-1',
                messageIndex: 4,
                status: 'running',
                toolName: 'listProjects',
            });
        });

        it('onToolCallEndEvent merges the args into the entry', () => {
            const subscriber = createSubscriber();

            subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-a', toolCallName: 'x'}));
            subscriber.onToolCallEndEvent!(makeToolCallEndParams({toolCallArgs: {name: 'p'}, toolCallId: 'call-a'}));

            expect(aiChatToolCallStore.getState().toolCalls['call-a']!.args).toEqual({name: 'p'});
        });

        it('onToolCallResultEvent flips the entry to success and stores the parsed result', () => {
            deliverToolResult(createSubscriber(), 'call-r', 'getProject', JSON.stringify({id: 1}));

            const entry = aiChatToolCallStore.getState().toolCalls['call-r']!;

            expect(entry.status).toBe('success');
            expect(entry.result).toEqual({id: 1});
        });
    });
});

describe('buildStateToSend', () => {
    beforeEach(() => {
        localStorage.clear();

        aiHubTabsStore.getState().reset();
        aiHubChatsStore.getState().reset();
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('includes the workspace, environment, mode and source', () => {
        environmentStore.setState({currentEnvironmentId: 3});

        const state = buildStateToSend({mode: MODE.ASK, providerCatalog: undefined, workspaceId: 7});

        expect(state).toEqual({
            activeTab: null,
            currentTabs: [],
            environmentId: '3',
            mode: 'ASK',
            parameters: {},
            source: 'AI_HUB',
            workspaceId: '7',
        });
    });

    it('includes the open workflow tabs and the active tab', () => {
        aiHubTabsStore
            .getState()
            .openWorkflowTab({name: 'A', projectId: '1', projectWorkflowId: 1, workflowId: 'wf-a'});
        aiHubTabsStore
            .getState()
            .openWorkflowTab({name: 'B', projectId: '2', projectWorkflowId: 2, workflowId: 'wf-b'});

        const state = buildStateToSend({mode: MODE.BUILD, providerCatalog: undefined, workspaceId: 1});

        expect(state.currentTabs).toEqual([
            {id: 'wf-a', kind: 'workflow', name: 'A'},
            {id: 'wf-b', kind: 'workflow', name: 'B'},
        ]);
        expect(state.activeTab).toEqual({id: 'wf-b', kind: 'workflow', name: 'B'});
    });

    it('includes the current chat LLM selection only when both halves are set', () => {
        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubChatsStore.getState().setChatLlmSelection(5, 'anthropic', 'claude');

        expect(buildStateToSend({mode: MODE.BUILD, providerCatalog: undefined, workspaceId: 1})).toMatchObject({
            userSelectedLlmModel: 'claude',
            userSelectedLlmProvider: 'anthropic',
        });

        aiHubChatsStore.getState().setChatLlmSelection(5, 'anthropic', null);

        expect(buildStateToSend({mode: MODE.BUILD, providerCatalog: undefined, workspaceId: 1})).not.toHaveProperty(
            'userSelectedLlmModel'
        );
    });
});

describe('buildStateToSend model selection', () => {
    const anthropicProvider = {
        enabled: true,
        icon: null,
        key: 'ai.provider.anthropic',
        models: [{label: 'Claude Sonnet 4.6', name: 'claude-sonnet-4-6'}],
        name: 'Anthropic',
        supportsModelById: false,
    };

    beforeEach(() => {
        localStorage.clear();

        aiHubTabsStore.getState().reset();
        aiHubChatsStore.getState().reset();
    });

    it('sends an available remembered model when nothing was picked for the chat', () => {
        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

        aiHubChatsStore.getState().setCurrentChatId(5);

        expect(
            buildStateToSend({mode: MODE.BUILD, providerCatalog: [anthropicProvider], workspaceId: 1})
        ).toMatchObject({
            userSelectedLlmModel: 'claude-sonnet-4-6',
            userSelectedLlmProvider: 'ai.provider.anthropic',
        });
        expect(readLastUsedModel(1)).not.toBeNull();
    });

    it('drops and clears a remembered model the catalog no longer offers, so no model is sent', () => {
        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-nonexistent-9');

        aiHubChatsStore.getState().setCurrentChatId(5);

        const state = buildStateToSend({mode: MODE.BUILD, providerCatalog: [anthropicProvider], workspaceId: 1});

        expect(state).not.toHaveProperty('userSelectedLlmModel');
        expect(state).not.toHaveProperty('userSelectedLlmProvider');
        expect(readLastUsedModel(1)).toBeNull();
    });

    it('drops and clears a remembered model whose provider is disabled', () => {
        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

        const state = buildStateToSend({
            mode: MODE.BUILD,
            providerCatalog: [{...anthropicProvider, enabled: false}],
            workspaceId: 1,
        });

        expect(state).not.toHaveProperty('userSelectedLlmModel');
        expect(readLastUsedModel(1)).toBeNull();
    });

    it('does not send the remembered model before the catalog has loaded', () => {
        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

        const state = buildStateToSend({mode: MODE.BUILD, providerCatalog: undefined, workspaceId: 1});

        expect(state).not.toHaveProperty('userSelectedLlmModel');
        expect(readLastUsedModel(1)).not.toBeNull();
    });

    it('sends an explicit pick for the chat as is, even when the catalog does not offer it', () => {
        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

        aiHubChatsStore.getState().setCurrentChatId(5);
        aiHubChatsStore.getState().setChatLlmSelection(5, 'ai.provider.openAi', 'gpt-5');

        expect(
            buildStateToSend({mode: MODE.BUILD, providerCatalog: [anthropicProvider], workspaceId: 1})
        ).toMatchObject({
            userSelectedLlmModel: 'gpt-5',
            userSelectedLlmProvider: 'ai.provider.openAi',
        });
    });

    it("does not read another workspace's remembered model", () => {
        writeLastUsedModel(2, 'ai.provider.anthropic', 'claude-sonnet-4-6');

        const state = buildStateToSend({mode: MODE.BUILD, providerCatalog: [anthropicProvider], workspaceId: 1});

        expect(state).not.toHaveProperty('userSelectedLlmModel');
        expect(readLastUsedModel(2)).not.toBeNull();
    });

    it('sends no model when nothing is remembered or picked, so the environment default applies', () => {
        aiHubChatsStore.getState().setCurrentChatId(5);

        const state = buildStateToSend({mode: MODE.BUILD, providerCatalog: [anthropicProvider], workspaceId: 1});

        expect(state).not.toHaveProperty('userSelectedLlmModel');
        expect(state).not.toHaveProperty('userSelectedLlmProvider');
    });
});

describe('runPostTurnTelemetry', () => {
    beforeEach(() => {
        mockPatchChat.mockReset();
        mockGenerateChatTitle.mockReset();

        const stubChat = {
            autoTitled: true,
            createdAt: '2026-01-01T00:00:00Z',
            environmentId: 0,
            id: 1,
            isOwner: true,
            kind: 'STANDARD' as const,
            lastPreview: null,
            messageCount: 0,
            ownerUserId: 1,
            projectDeploymentId: null,
            status: 'ACTIVE' as const,
            threadId: 'thread-1',
            title: null,
            updatedAt: '2026-01-01T00:00:00Z',
            userId: 1,
            workflowExecutionId: null,
            workspaceId: 1,
        };

        mockPatchChat.mockResolvedValue(stubChat);
        mockGenerateChatTitle.mockResolvedValue(stubChat);
    });

    it('calls patchChat with lastPreview and messageCount and skips title generation below two messages', async () => {
        const invalidateChats = vi.fn();

        runPostTurnTelemetry({chatId: 42, input: 'Hello world', invalidateChats, messageCount: 1, workspaceId: 7});

        await vi.waitFor(() => {
            expect(mockPatchChat).toHaveBeenCalledWith({
                chatId: 42,
                patch: {lastPreview: 'Hello world', messageCount: 1},
                workspaceId: 7,
            });
        });

        expect(mockGenerateChatTitle).not.toHaveBeenCalled();
        expect(invalidateChats).not.toHaveBeenCalled();
    });

    it('fires generateAiHubChatTitle and invalidates once messageCount reaches 2', async () => {
        const invalidateChats = vi.fn();

        runPostTurnTelemetry({chatId: 99, input: 'First turn', invalidateChats, messageCount: 2, workspaceId: 3});

        await vi.waitFor(() => {
            expect(mockGenerateChatTitle).toHaveBeenCalledWith({chatId: 99, workspaceId: 3});
            expect(invalidateChats).toHaveBeenCalledOnce();
        });
    });

    it('marks the title generation failure on the chats store, without a toast, when it fails', async () => {
        vi.spyOn(console, 'error').mockImplementation(() => undefined);

        const toastErrorSpy = vi.spyOn(toast, 'error');

        mockGenerateChatTitle.mockRejectedValue(new Error('model down'));

        runPostTurnTelemetry({chatId: 100, input: 'x', invalidateChats: vi.fn(), messageCount: 4, workspaceId: 3});

        await vi.waitFor(() => {
            expect(aiHubChatsStore.getState().titleGenerationFailures[100]).toBe('model down');
        });

        expect(toastErrorSpy).not.toHaveBeenCalled();
    });
});

describe('runPostTurnTelemetry for a workflow chat', () => {
    it('does not generate a title for a workflow chat', async () => {
        mockPatchChat.mockReset();
        mockGenerateChatTitle.mockReset();
        mockPatchChat.mockResolvedValue(undefined as never);

        runPostTurnTelemetry({
            chatId: 5,
            input: 'Hi',
            invalidateChats: vi.fn(),
            isWorkflowChat: true,
            messageCount: 4,
            workspaceId: 3,
        });

        await vi.waitFor(() => {
            expect(mockPatchChat).toHaveBeenCalledWith({
                chatId: 5,
                patch: {lastPreview: 'Hi', messageCount: 4},
                workspaceId: 3,
            });
        });

        expect(mockGenerateChatTitle).not.toHaveBeenCalled();
    });
});

describe('isCachedWorkflowChat', () => {
    it('finds the chat kind in any cached chat list', () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(AiHubChatsKeys.list(3, 1, 'ACTIVE'), [{id: 4, kind: 'STANDARD'}]);
        queryClient.setQueryData(AiHubChatsKeys.list(3, 1, 'ARCHIVED'), [{id: 5, kind: 'WORKFLOW_CHAT'}]);

        expect(isCachedWorkflowChat(queryClient, 5)).toBe(true);
        expect(isCachedWorkflowChat(queryClient, 4)).toBe(false);
        expect(isCachedWorkflowChat(queryClient, 6)).toBe(false);
    });
});

describe('AiHubChatsKeys.artifacts', () => {
    it('starts with the base all key so invalidating all also covers artifacts', () => {
        expect(AiHubChatsKeys.artifacts(42, 7)).toEqual(['aiHubChats', 'artifacts', 42, 7]);
    });
});

describe('hasLivePropertyOptionPicker', () => {
    const picker = (componentName: string, propertyName: string): ThreadMessageLike => ({
        content: [
            {
                data: {componentName, kind: 'select-property-option', options: [], propertyName},
                type: 'data-select-property-option',
            },
        ],
        role: 'assistant' as const,
    });

    it('returns true when a picker for the same component + property exists', () => {
        expect(hasLivePropertyOptionPicker([picker('slack', 'channel')], 'slack', 'channel')).toBe(true);
    });

    it('returns false when the property or component differs', () => {
        expect(hasLivePropertyOptionPicker([picker('slack', 'channel')], 'slack', 'username')).toBe(false);
        expect(hasLivePropertyOptionPicker([picker('slack', 'channel')], 'gmail', 'channel')).toBe(false);
    });
});

describe('projectMessagesWithToolCalls', () => {
    it('returns the messages unchanged when there are no tool-call entries', () => {
        const messages: ThreadMessageLike[] = [
            {content: 'Hello', role: 'user'},
            {content: 'Hi there', role: 'assistant'},
        ];

        expect(projectMessagesWithToolCalls(messages, [])).toBe(messages);
    });

    it('appends a tool-call part to the assistant message at the matching index', () => {
        const result = projectMessagesWithToolCalls(
            [
                {content: 'Find me a workflow', role: 'user'},
                {content: 'On it', role: 'assistant'},
            ],
            [
                {
                    args: {query: 'leads'},
                    messageIndex: 1,
                    progress: [],
                    progressiveOutput: '',
                    result: {ok: true},
                    status: 'success',
                    toolCallId: 'call-1',
                    toolName: 'searchWorkflows',
                },
            ]
        );

        const parts = result[1]!.content as ReadonlyArray<{toolName?: string; type: string}>;

        expect(parts.map((part) => part.type)).toEqual(['text', 'tool-call']);
        expect(parts[1]!.toolName).toBe('searchWorkflows');
    });

    it('does not attach tool-call parts to user messages even if their index matches', () => {
        const messages: ThreadMessageLike[] = [{content: 'User', role: 'user'}];

        const result = projectMessagesWithToolCalls(messages, [
            {messageIndex: 0, progress: [], progressiveOutput: '', status: 'success', toolCallId: 'c', toolName: 'n'},
        ]);

        expect(result[0]).toEqual(messages[0]);
    });

    it('collapses a running duplicate into its completed twin with the same name and args', () => {
        const result = projectMessagesWithToolCalls(
            [
                {content: 'q', role: 'user'},
                {content: '', role: 'assistant'},
            ],
            [
                {
                    args: {a: 1},
                    messageIndex: 1,
                    progress: [],
                    progressiveOutput: '',
                    status: 'running',
                    toolCallId: 'call-a',
                    toolName: 'same',
                },
                {
                    args: {a: 1},
                    messageIndex: 1,
                    progress: [],
                    progressiveOutput: '',
                    status: 'success',
                    toolCallId: 'call-b',
                    toolName: 'same',
                },
            ]
        );

        const parts = result[1]!.content as ReadonlyArray<{toolCallId?: string}>;

        expect(parts.map((part) => part.toolCallId)).toEqual(['call-b']);
    });

    it('strips leaked tool markup from assistant text', () => {
        const result = projectMessagesWithToolCalls(
            [{content: 'Done. <function_calls><invoke name="x"></invoke></function_calls>', role: 'assistant'}],
            []
        );

        expect(result[0]!.content).toBe('Done.');
    });
});

describe('bootstrapAiHubTurnRunState', () => {
    it('calls markRunning BEFORE the (slow) chat-create resolves', async () => {
        const calls: string[] = [];
        let resolveCreate: () => void = () => undefined;

        const createPromise = new Promise<void>((resolve) => {
            resolveCreate = () => {
                calls.push('create-resolved');
                resolve();
            };
        });

        const resultPromise = bootstrapAiHubTurnRunState({
            createChatIfNeeded: () => {
                calls.push('create-started');

                return createPromise;
            },
            markRunning: () => calls.push('mark-running'),
            revertOnFailure: () => calls.push('revert'),
        });

        expect(calls).toEqual(['mark-running', 'create-started']);

        resolveCreate();

        await expect(resultPromise).resolves.toBe(true);
    });

    it('reverts and returns false when the chat-create fails', async () => {
        const calls: string[] = [];

        const result = await bootstrapAiHubTurnRunState({
            createChatIfNeeded: () => {
                calls.push('create');

                return Promise.reject(new Error('boom'));
            },
            markRunning: () => calls.push('mark-running'),
            revertOnFailure: () => calls.push('revert'),
        });

        expect(result).toBe(false);
        expect(calls).toEqual(['mark-running', 'create', 'revert']);
    });
});

describe('TURN_IN_FLIGHT_MESSAGE', () => {
    it('tells the user a turn is already running in this chat', () => {
        expect(TURN_IN_FLIGHT_MESSAGE).toBe('A turn is already running in this chat.');
    });
});
