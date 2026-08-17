import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';

/* eslint-disable sort-keys */
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {aiHubProgressStore} from '@/ee/pages/automation/ai-hub/progress/stores/useAiHubProgressStore';
import {MODE} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {aiChatToolCallStore} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {EventType} from '@ag-ui/core';
import {ThreadMessageLike} from '@assistant-ui/react';

vi.mock('@/ee/pages/automation/ai-hub/chats/api/chats.api', () => ({
    generateAiHubChatTitle: vi.fn(),
    patchChat: vi.fn(),
}));

vi.mock('../workflowStreamHandler', () => ({
    fetchWorkflowResponse: vi.fn(),
    openWorkflowSseStream: vi.fn(),
}));

const {generateAiHubChatTitle, patchChat} = await import('@/ee/pages/automation/ai-hub/chats/api/chats.api');

const {fetchWorkflowResponse, openWorkflowSseStream} = await import('../workflowStreamHandler');

const mockPatchChat = vi.mocked(patchChat);
const mockGenerateChatTitle = vi.mocked(generateAiHubChatTitle);
const mockOpenWorkflowSseStream = vi.mocked(openWorkflowSseStream);
const mockFetchWorkflowResponse = vi.mocked(fetchWorkflowResponse);

import {beforeEach, describe, expect, it, vi} from 'vitest';

import {
    bootstrapAiHubTurnRunState,
    buildAiHubSubscriber,
    buildStateToSend,
    cleanupForChatChange,
    hasLivePropertyOptionPicker,
    projectMessagesWithToolCalls,
    runPostTurnTelemetry,
} from '../AiHubRuntimeProvider';

import type {AgentSubscriber} from '@ag-ui/client';
import type {
    CustomEvent as AgUiCustomEvent,
    RunFinishedEvent,
    ToolCallEndEvent,
    ToolCallResultEvent,
    ToolCallStartEvent,
} from '@ag-ui/core';

/**
 * Typed event factories. Tests previously cast every event payload via `as never`, which silently dropped
 * any AG-UI rename or shape change. These factories use the real AG-UI event interfaces, so renaming a field
 * in @ag-ui/core (e.g. `toolCallId` → `toolCallID`) breaks tests at compile time instead of slipping through.
 *
 * Each factory accepts only the fields the runtime provider actually reads, applying sensible defaults for
 * the rest. They return the full `params` object the AG-UI subscriber expects, including the
 * `AgentSubscriberParams` baseline that the runtime provider does not consume but the SDK contract requires.
 */
type SubscriberParamsBaseType = Parameters<NonNullable<AgentSubscriber['onRunFinalized']>>[0];
type SubscriberAgentType = SubscriberParamsBaseType['agent'];
type SubscriberInputType = SubscriberParamsBaseType['input'];
type SubscriberMessagesType = SubscriberParamsBaseType['messages'];
type SubscriberStateType = SubscriberParamsBaseType['state'];

// Build empty placeholders shaped to the actual AG-UI types instead of `as never`. This way a future
// AG-UI rename or signature change breaks compilation here rather than silently slipping through.
const baseSubscriberParams: SubscriberParamsBaseType = {
    agent: {} as SubscriberAgentType,
    input: {} as SubscriberInputType,
    messages: [] as unknown as SubscriberMessagesType,
    state: {} as SubscriberStateType,
} as SubscriberParamsBaseType;

const makeToolCallStartParams = (
    overrides: Pick<ToolCallStartEvent, 'toolCallId' | 'toolCallName'>
): {event: ToolCallStartEvent} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        rawEvent: undefined,
        timestamp: undefined,
        type: EventType.TOOL_CALL_START,
        ...overrides,
    },
});

const makeToolCallEndParams = (
    overrides: Pick<ToolCallEndEvent, 'toolCallId'> & {
        toolCallArgs: Record<string, unknown>;
        toolCallName?: string;
    }
): {
    event: ToolCallEndEvent;
    toolCallArgs: Record<string, unknown>;
    toolCallName: string;
} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        rawEvent: undefined,
        timestamp: undefined,
        toolCallId: overrides.toolCallId,
        type: EventType.TOOL_CALL_END,
    },
    toolCallArgs: overrides.toolCallArgs,
    toolCallName: overrides.toolCallName ?? 'unknown',
});

const makeToolCallResultParams = (
    overrides: Pick<ToolCallResultEvent, 'toolCallId' | 'content'>
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

const makeCustomEventParams = (name: string, value: unknown): {event: AgUiCustomEvent} & SubscriberParamsBaseType => ({
    ...baseSubscriberParams,
    event: {
        name,
        rawEvent: undefined,
        timestamp: undefined,
        type: EventType.CUSTOM,
        value,
    },
});

const makeRunFinishedParams = (): {
    event: RunFinishedEvent;
    outcome: 'success';
    result?: unknown;
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
});

describe('buildAiHubSubscriber', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });

        aiHubProgressStore.setState({progressText: null});
        aiChatRetryableErrorStore.setState({currentError: undefined});
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        mockOpenWorkflowSseStream.mockReset();
        mockFetchWorkflowResponse.mockReset();
    });

    it('opens a file tab when the openFileTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-1', toolCallName: 'openFileTab'}));

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({opened: true, fileId: '42', name: 'spec.md'}),
                toolCallId: 'call-1',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('spec.md');
        expect(tab.kind).toBe('file');

        if (tab.kind === 'file') {
            expect(tab.fileId).toBe('42');
        }
    });

    it('opens a file tab when an openResourceTab tool result with type FILE arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rt-1', toolCallName: 'openResourceTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({fileId: '42', name: 'spec.md', opened: true, type: 'FILE'}),
                toolCallId: 'call-rt-1',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('spec.md');
        expect(tab.kind).toBe('file');

        if (tab.kind === 'file') {
            expect(tab.fileId).toBe('42');
        }
    });

    it('opens a workflow tab when an openResourceTab tool result with type WORKFLOW arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rt-2', toolCallName: 'openResourceTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    name: 'Sync',
                    opened: true,
                    projectId: '12',
                    projectWorkflowId: 34,
                    type: 'WORKFLOW',
                    workflowId: 'w-1',
                }),
                toolCallId: 'call-rt-2',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.kind).toBe('workflow');

        if (tab.kind === 'workflow') {
            expect(tab.workflowId).toBe('w-1');
            expect(tab.projectWorkflowId).toBe(34);
        }
    });

    it('opens an AI Agent tab when an openResourceTab tool result with type AI_AGENT arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rt-agent', toolCallName: 'openResourceTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({aiAgentId: 'agent-1', name: 'Support Agent', opened: true, type: 'AI_AGENT'}),
                toolCallId: 'call-rt-agent',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Support Agent');
        expect(tab.kind).toBe('aiAgent');

        if (tab.kind === 'aiAgent') {
            expect(tab.aiAgentId).toBe('agent-1');
        }
    });

    it('surfaces a failure when an openResourceTab tool result carries an unknown type', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rt-3', toolCallName: 'openResourceTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({name: 'Mystery', opened: true, type: 'WIDGET'}),
                toolCallId: 'call-rt-3',
            })
        );

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('opens a workflow tab when the openWorkflowTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-wf', toolCallName: 'openWorkflowTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    opened: true,
                    workflowId: 'wf-1',
                    projectId: 'proj-1',
                    projectWorkflowId: 42,
                    name: 'Leads Sync',
                }),
                toolCallId: 'call-wf',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Leads Sync');
        expect(tab.kind).toBe('workflow');

        if (tab.kind === 'workflow') {
            expect(tab.workflowId).toBe('wf-1');
            expect(tab.projectId).toBe('proj-1');
            expect(tab.projectWorkflowId).toBe(42);
        }
    });

    it('opens a data-table tab when the openDataTableTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-dt', toolCallName: 'openDataTableTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({opened: true, dataTableId: 'dt-1', name: 'Contacts'}),
                toolCallId: 'call-dt',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Contacts');
        expect(tab.kind).toBe('dataTable');

        if (tab.kind === 'dataTable') {
            expect(tab.dataTableId).toBe('dt-1');
        }
    });

    it('opens a knowledge-base tab when the openKnowledgeBaseTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-kb', toolCallName: 'openKnowledgeBaseTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({opened: true, knowledgeBaseId: 'kb-1', name: 'Support Docs'}),
                toolCallId: 'call-kb',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Support Docs');
        expect(tab.kind).toBe('knowledgeBase');

        if (tab.kind === 'knowledgeBase') {
            expect(tab.knowledgeBaseId).toBe('kb-1');
        }
    });

    it('opens an AI Agent tab when the openAiAgentTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-agent', toolCallName: 'openAiAgentTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({aiAgentId: 'agent-1', name: 'Support Agent', opened: true}),
                toolCallId: 'call-agent',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Support Agent');
        expect(tab.kind).toBe('aiAgent');

        if (tab.kind === 'aiAgent') {
            expect(tab.aiAgentId).toBe('agent-1');
        }
    });

    it('opens a skill tab when the openSkillTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-skill', toolCallName: 'openSkillTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({name: 'Lead Scoring', opened: true, skillId: 'skill-1'}),
                toolCallId: 'call-skill',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('Lead Scoring');
        expect(tab.kind).toBe('skill');

        if (tab.kind === 'skill') {
            expect(tab.skillId).toBe('skill-1');
        }
    });

    it('opens a custom component tab when the openCustomComponentTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-cc', toolCallName: 'openCustomComponentTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({customComponentId: 'cc-1', name: 'My Component', opened: true}),
                toolCallId: 'call-cc',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('My Component');
        expect(tab.kind).toBe('customComponent');

        if (tab.kind === 'customComponent') {
            expect(tab.customComponentId).toBe('cc-1');
        }
    });

    it('opens a code workflow tab when the openCodeWorkflowTab tool result arrives', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-cw', toolCallName: 'openCodeWorkflowTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    language: 'java',
                    name: 'My Code Workflow',
                    opened: true,
                    projectId: 'proj-1',
                }),
                toolCallId: 'call-cw',
            })
        );

        const state = aiHubTabsStore.getState();
        const tab = state.openTabs[0]!;

        expect(state.openTabs).toHaveLength(1);
        expect(tab.name).toBe('My Code Workflow');
        expect(tab.kind).toBe('codeWorkflow');

        if (tab.kind === 'codeWorkflow') {
            expect(tab.projectId).toBe('proj-1');
            expect(tab.language).toBe('java');
        }
    });

    it('switches chat when openAiHubTaskTab tool result arrives', async () => {
        // The task + workflow-chat tab tools share an output shape and a behaviour: the server has already done
        // create-or-restore on the chat, and the client subscriber's job is to (a) sync the command center
        // store's threadId, (b) sync the chats store's currentChatId, and (c) navigate to the
        // matching URL. All three steps must fire in order — without (a) the URL→store sync effect would bounce
        // the user back; without (c) the user would stay on the previous chat visually.
        const navigate = vi.fn();

        const {aiHubChatsStore} = await import('@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore');
        const {aiHubStore} = await import('@/ee/pages/automation/ai-hub/stores/useAiHubStore');

        aiHubChatsStore.getState().setCurrentChatId(0);
        aiHubStore.setState({chatId: 'thread-old', messages: []});

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            navigate,
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-pa', toolCallName: 'openAiHubTaskTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    chatId: 101,
                    opened: true,
                    threadId: '00000000-0000-0000-0000-000000000042',
                    title: 'Research Assistant',
                }),
                toolCallId: 'call-pa',
            })
        );

        // Pin all three side effects. A regression in any one of them would break the create-from-chat flow:
        // the user would either stay on the old chat (URL not updated), see no messages (threadId not
        // synced), or have a wrong active row in the sidebar (currentChatId not synced).
        expect(aiHubStore.getState().chatId).toBe('00000000-0000-0000-0000-000000000042');
        expect(aiHubChatsStore.getState().currentChatId).toBe(101);
        expect(navigate).toHaveBeenCalledWith('/automation/ai-hub/chats/101');
    });

    it('switches chat when openWorkflowChatTab tool result arrives', async () => {
        // Same shape as openAiHubTaskTab — both tools share the OpenChatTabResultType validator
        // and the same store-sync handler. Pin the workflow-chat sibling separately so a future divergence
        // (e.g. workflow-chat picks up an extra side effect like recording an artifact) doesn't accidentally
        // regress the task path.
        const navigate = vi.fn();

        const {aiHubChatsStore} = await import('@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore');
        const {aiHubStore} = await import('@/ee/pages/automation/ai-hub/stores/useAiHubStore');

        aiHubChatsStore.getState().setCurrentChatId(0);
        aiHubStore.setState({chatId: 'thread-old', messages: []});

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            navigate,
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-wfc', toolCallName: 'openWorkflowChatTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    chatId: 202,
                    opened: true,
                    threadId: '00000000-0000-0000-0000-00000000003e',
                    title: 'Customer Support',
                }),
                toolCallId: 'call-wfc',
            })
        );

        expect(aiHubStore.getState().chatId).toBe('00000000-0000-0000-0000-00000000003e');
        expect(aiHubChatsStore.getState().currentChatId).toBe(202);
        expect(navigate).toHaveBeenCalledWith('/automation/ai-hub/chats/202');
    });

    it('still syncs stores when navigate dep is undefined', async () => {
        // Tests and headless callers that build the subscriber without a navigate function should still
        // get the store-sync side effects (command center threadId + chatId currentId). The runtime patch
        // guards the navigate call with `if (navigate)` — without that guard the openX-Tab branch would
        // crash on subscribers built without a router context.
        const {aiHubChatsStore} = await import('@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore');
        const {aiHubStore} = await import('@/ee/pages/automation/ai-hub/stores/useAiHubStore');

        aiHubChatsStore.getState().setCurrentChatId(0);
        aiHubStore.setState({chatId: 'thread-old', messages: []});

        // No navigate dep supplied — tests that aren't wired through a router shouldn't have to mock one.
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-no-nav', toolCallName: 'openAiHubTaskTab'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    chatId: 303,
                    opened: true,
                    threadId: '00000000-0000-0000-0000-000000000099',
                    title: 'No Nav Agent',
                }),
                toolCallId: 'call-no-nav',
            })
        );

        // Both store-syncs land regardless of navigate. Pin: the user lands on the right chat
        // state-wise even when the URL isn't updated (e.g. headless replay scenarios), so the regular
        // URL→store sync effect can pick up the new state on the next render.
        expect(aiHubStore.getState().chatId).toBe('00000000-0000-0000-0000-000000000099');
        expect(aiHubChatsStore.getState().currentChatId).toBe(303);
    });

    it('ignores tool results that are not openFileTab', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-2', toolCallName: 'someOtherTool'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({opened: true, fileId: '42', name: 'spec.md'}),
                toolCallId: 'call-2',
            })
        );

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('ignores malformed tool results', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-3', toolCallName: 'openFileTab'}));

        subscriber.onToolCallResultEvent!(makeToolCallResultParams({content: 'not-json', toolCallId: 'call-3'}));

        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('error tool result populates the retryable-error store with toolName and lastUserMessage', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue('Create a report for Q1'),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-err', toolCallName: 'createReport'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({error: 'Report service unavailable'}),
                toolCallId: 'call-err',
            })
        );

        const {currentError} = aiChatRetryableErrorStore.getState();

        expect(currentError).not.toBeUndefined();
        expect(currentError!.toolName).toBe('createReport');
        expect(currentError!.errorMessage).toBe('Report service unavailable');
        expect(currentError!.lastUserMessage).toBe('Create a report for Q1');
        expect(aiHubTabsStore.getState().openTabs).toHaveLength(0);
    });

    it('runChatWorkflow streaming result calls openWorkflowSseStream with streamUrl and an onError handler', () => {
        const appendToLastAssistantMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage,
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    workflowExecutionId: 'exec-1',
                    streaming: true,
                    streamUrl: '/webhooks/exec-1/sse',
                }),
                toolCallId: 'call-rcw',
            })
        );

        expect(mockOpenWorkflowSseStream).toHaveBeenCalledOnce();
        expect(mockOpenWorkflowSseStream).toHaveBeenCalledWith(
            expect.objectContaining({streamUrl: '/webhooks/exec-1/sse'})
        );
        expect(typeof mockOpenWorkflowSseStream.mock.calls[0][0].onError).toBe('function');
        expect(mockFetchWorkflowResponse).not.toHaveBeenCalled();
    });

    it('runChatWorkflow streaming onError populates the retryable-error store', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue('Run my workflow'),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw-err', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    workflowExecutionId: 'exec-x',
                    streaming: true,
                    streamUrl: '/webhooks/exec-x/sse',
                }),
                toolCallId: 'call-rcw-err',
            })
        );

        const onErrorPassed = mockOpenWorkflowSseStream.mock.calls[0][0].onError!;

        onErrorPassed({message: 'Workflow blew up'});

        const {currentError} = aiChatRetryableErrorStore.getState();

        expect(currentError).not.toBeUndefined();
        expect(currentError!.toolName).toBe('runChatWorkflow');
        expect(currentError!.errorMessage).toBe('Workflow blew up');
        expect(currentError!.lastUserMessage).toBe('Run my workflow');
    });

    it('runChatWorkflow result without streamUrl or responseUrl flips card to error and sets retryable error', () => {
        // Pins the invariant: a server response that has neither streamUrl nor responseUrl must flip the
        // tool-call card to an error and surface a retryable error. Equivalent to the chunkCount===0 case —
        // both produce a stuck-running card with zero output if not handled, and the user gets no feedback.
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue('Run an empty result'),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw-empty', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    workflowExecutionId: 'exec-empty',
                    streaming: false,
                    // Neither streamUrl nor responseUrl present.
                }),
                toolCallId: 'call-rcw-empty',
            })
        );

        expect(mockOpenWorkflowSseStream).not.toHaveBeenCalled();
        expect(mockFetchWorkflowResponse).not.toHaveBeenCalled();

        const entry = aiChatToolCallStore.getState().toolCalls['call-rcw-empty'];

        expect(entry).toBeDefined();
        expect(entry!.status).toBe('error');

        const {currentError} = aiChatRetryableErrorStore.getState();

        expect(currentError).not.toBeUndefined();
        expect(currentError!.toolName).toBe('runChatWorkflow');
        expect(currentError!.lastUserMessage).toBe('Run an empty result');
    });

    it('runChatWorkflow streaming threads the per-turn controller signal end-to-end and DOES NOT abort it on chat switch', () => {
        // Pins both halves of the contract: (1) the AbortController owned by the provider is the SAME
        // one whose signal the subscriber threads into openWorkflowSseStream — a regression that split
        // the controller across two refs would silently leave streams running on real abort paths; and
        // (2) `cleanupForChatChange` does NOT abort that controller. The earlier shape of (2)
        // killed the agent run mid-stream when the user switched chats and they could never see
        // the assistant reply on return. Now (2) leaves the run alive — the unmount-only effect in the
        // provider handles real teardown — and the user gets the committed message back when
        // switchChat refetches on return.
        const turnController = new AbortController();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            workflowStreamSignal: turnController.signal,
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw-wiring', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    streamUrl: '/webhooks/wiring/sse',
                    streaming: true,
                    workflowExecutionId: 'exec-wiring',
                }),
                toolCallId: 'call-rcw-wiring',
            })
        );

        expect(mockOpenWorkflowSseStream).toHaveBeenCalledOnce();

        const passedSignal = mockOpenWorkflowSseStream.mock.calls[0][0].signal;

        expect(passedSignal).toBeDefined();
        expect(passedSignal).toBe(turnController.signal);
        expect(passedSignal!.aborted).toBe(false);

        cleanupForChatChange('conv-prev');

        // CRITICAL inversion of the previous assertion: the signal must remain UN-aborted after a
        // chat-switch cleanup. Provider-unmount in production aborts via a separate effect;
        // see AiHubRuntimeProviderCleanup.test.tsx for the contract guards.
        expect(passedSignal!.aborted).toBe(false);
    });

    it('runChatWorkflow non-streaming result calls fetchWorkflowResponse with responseUrl', () => {
        const appendToLastAssistantMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage,
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw-sync', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    workflowExecutionId: 'exec-2',
                    streaming: false,
                    responseUrl: '/webhooks/exec-2',
                }),
                toolCallId: 'call-rcw-sync',
            })
        );

        expect(mockFetchWorkflowResponse).toHaveBeenCalledOnce();
        expect(mockFetchWorkflowResponse).toHaveBeenCalledWith(
            expect.objectContaining({responseUrl: '/webhooks/exec-2'})
        );
        expect(mockOpenWorkflowSseStream).not.toHaveBeenCalled();
    });

    it('runChatWorkflow result without workflowExecutionId is ignored', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw-bad', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({awaitingInput: true, reason: 'Need input', inputSchema: {}}),
                toolCallId: 'call-rcw-bad',
            })
        );

        expect(mockOpenWorkflowSseStream).not.toHaveBeenCalled();
        expect(mockFetchWorkflowResponse).not.toHaveBeenCalled();
    });

    it('subagent-progress custom event populates the progress store', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onCustomEvent!(
            makeCustomEventParams('subagent-progress', {subagentName: 'research', text: 'Searching…'})
        );

        expect(aiHubProgressStore.getState().progressText).toBe('research: Searching…');
    });

    it('custom event with unknown name is ignored by progress store', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onCustomEvent!(makeCustomEventParams('other-event', {subagentName: 'research', text: 'Searching…'}));

        expect(aiHubProgressStore.getState().progressText).toBeNull();
    });

    it('onRunFinishedEvent clears the progress store', () => {
        aiHubProgressStore.setState({progressText: 'research: Searching…'});

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onRunFinishedEvent!(makeRunFinishedParams());

        expect(aiHubProgressStore.getState().progressText).toBeNull();
    });

    /*
     * Pinned regression: onRunFinishedEvent must invoke runLifecycle.onSettle so the parent provider can
     * release `isAgentRunning` the moment the server says the turn is done. Without this, a hang in
     * agent.runAgent's promise (e.g. SDK never sees its own RUN_FINISHED, or its RUN_ERROR was lost to
     * the old wire format) leaves the user stuck on the Stop button with the trailing typing dot on a
     * fully-streamed message and no way to send the next turn.
     */
    it('onRunFinishedEvent fires runLifecycle.onSettle so the running flag is released even if runAgent hangs', () => {
        const onSettle = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            runLifecycle: {onSettle},
        });

        subscriber.onRunFinishedEvent!(makeRunFinishedParams());

        expect(onSettle).toHaveBeenCalledOnce();
    });

    it('onRunErrorEvent fires runLifecycle.onSettle so the running flag is released on server-reported failure', () => {
        const onSettle = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            runLifecycle: {onSettle},
        });

        subscriber.onRunErrorEvent!({event: {message: 'boom'}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(onSettle).toHaveBeenCalledOnce();
    });

    it('onRunErrorEvent appends a data-run-error message so the failure renders as a red callout', () => {
        // Previously the assistant bubble stayed empty on RUN_ERROR — the user saw a blank chat row with
        // only the copy/refresh icons and no indication of what went wrong. The fix routes event.message
        // into a data-run-error message part which the message renderer styles as a red error callout.
        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onRunErrorEvent!({event: {message: 'Anthropic 400: bad request'}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(addMessage).toHaveBeenCalledWith({
            content: [
                {
                    data: {message: 'Anthropic 400: bad request'},
                    type: 'data-run-error',
                },
            ],
            role: 'assistant',
        });
    });

    it('onRunErrorEvent extracts the friendly message from an Anthropic JSON error envelope', () => {
        // The raw Spring AI error toString is verbose: "com.anthropic.errors.BadRequestException: 400:
        // {...JSON...}". Rendering that verbatim leaks the FQCN, status code, and internal request_id into
        // the chat transcript. The humanizer plucks out just the nested error.message so the user sees the
        // actionable text ("Your credit balance is too low...") instead of a stack-trace fragment.
        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        const raw =
            'com.anthropic.errors.BadRequestException: 400: {"type":"error","error":' +
            '{"type":"invalid_request_error","message":"Your credit balance is too low to access the Anthropic API."}' +
            ',"request_id":"req_011Cb4VoHaCFrbrbC7YTxb3r"}';

        subscriber.onRunErrorEvent!({event: {message: raw}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(addMessage).toHaveBeenCalledWith({
            content: [
                {
                    data: {message: 'Your credit balance is too low to access the Anthropic API.'},
                    type: 'data-run-error',
                },
            ],
            role: 'assistant',
        });
    });

    it('onRunErrorEvent strips the Java exception FQCN prefix when the payload is not JSON', () => {
        // Plain Java exception like "java.lang.IllegalStateException: Current user is not set!" — the user
        // shouldn't see the FQCN, just the message after the first colon. Belt-and-braces for runtime
        // errors that don't carry a structured envelope.
        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onRunErrorEvent!({
            event: {message: 'java.lang.IllegalStateException: Current user is not set!'},
        } as Parameters<NonNullable<typeof subscriber.onRunErrorEvent>>[0]);

        expect(addMessage).toHaveBeenCalledWith({
            content: [
                {
                    data: {message: 'Current user is not set!'},
                    type: 'data-run-error',
                },
            ],
            role: 'assistant',
        });
    });

    it('onRunErrorEvent suppresses the red callout when the user aborted the turn (Stop button)', () => {
        // Clicking Stop aborts the per-turn controller, which makes the AG-UI SSE transport throw a
        // DOMException ("BodyStreamBuffer was aborted") that the SDK re-emits as a RUN_ERROR. That's an
        // expected cancellation, not a failure — the handler must NOT stamp a red error bubble for it. The
        // discriminator is workflowStreamSignal.aborted (already true here because onCancel aborts before the
        // event arrives). onSettle still runs so the composer flips back to idle.
        const addMessage = vi.fn();
        const onSettle = vi.fn();
        const controller = new AbortController();

        controller.abort();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            runLifecycle: {onSettle},
            workflowStreamSignal: controller.signal,
        });

        subscriber.onRunErrorEvent!({event: {message: 'BodyStreamBuffer was aborted'}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(addMessage).not.toHaveBeenCalled();
        expect(onSettle).toHaveBeenCalledOnce();
    });

    it('onRunErrorEvent still renders the red callout for a real error when the turn was not aborted', () => {
        // Guard against over-suppression: a genuine server error arrives while the signal is NOT yet aborted
        // (the controller is only aborted later, via runLifecycle.onError). The bubble must still render.
        const addMessage = vi.fn();
        const controller = new AbortController();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            workflowStreamSignal: controller.signal,
        });

        subscriber.onRunErrorEvent!({event: {message: 'boom'}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(addMessage).toHaveBeenCalledWith({
            content: [{data: {message: 'boom'}, type: 'data-run-error'}],
            role: 'assistant',
        });
    });

    it('onToolCallResultEvent collapses a duplicate selectPropertyOption picker for the same property', async () => {
        // The agent looping (or re-invoking selectPropertyOption for the same property within a turn) would
        // otherwise stack identical comboboxes. The handler skips the redundant interactive bubble when a live
        // picker for the same component + property already exists.
        const {aiHubStore} = await import('@/ee/pages/automation/ai-hub/stores/useAiHubStore');

        aiHubStore.setState({
            messages: [
                {
                    content: [
                        {
                            data: {
                                componentName: 'slack',
                                kind: 'select-property-option',
                                options: [{label: '#general', value: 'C1'}],
                                propertyName: 'channel',
                            },
                            type: 'data-select-property-option',
                        },
                    ],
                    role: 'assistant',
                },
            ],
        });

        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-spo', toolCallName: 'selectPropertyOption'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    componentName: 'slack',
                    kind: 'select-property-option',
                    options: [{label: '#general', value: 'C1'}],
                    propertyName: 'channel',
                }),
                toolCallId: 'call-spo',
            })
        );

        expect(addMessage).not.toHaveBeenCalled();
    });

    it('onToolCallResultEvent appends a selectPropertyOption picker for a different property', async () => {
        // A live picker for a DIFFERENT property must not be collapsed — distinct properties each get their
        // own picker.
        const {aiHubStore} = await import('@/ee/pages/automation/ai-hub/stores/useAiHubStore');

        aiHubStore.setState({
            messages: [
                {
                    content: [
                        {
                            data: {
                                componentName: 'slack',
                                kind: 'select-property-option',
                                options: [{label: '#general', value: 'C1'}],
                                propertyName: 'channel',
                            },
                            type: 'data-select-property-option',
                        },
                    ],
                    role: 'assistant',
                },
            ],
        });

        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-spo2', toolCallName: 'selectPropertyOption'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    componentName: 'slack',
                    kind: 'select-property-option',
                    options: [{label: 'me', value: 'U1'}],
                    propertyName: 'username',
                }),
                toolCallId: 'call-spo2',
            })
        );

        expect(addMessage).toHaveBeenCalledWith({
            content: [
                {
                    data: {
                        componentName: 'slack',
                        kind: 'select-property-option',
                        options: [{label: 'me', value: 'U1'}],
                        propertyName: 'username',
                        truncated: undefined,
                    },
                    type: 'data-select-property-option',
                },
            ],
            role: 'assistant',
        });
    });

    it('onRunErrorEvent fires runLifecycle.onError so the caller can abort still-running workflow streams', () => {
        // Without onError firing on RUN_ERROR, a failed agent turn that started a runChatWorkflow
        // sub-stream would keep inflightStreamCount > 0 — the composer stays stuck on "running" with no
        // send button until the orphaned SSE naturally drains. onError gives the caller a hook to abort
        // the per-turn controller and short-circuit that wait.
        const onError = vi.fn();
        const onSettle = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
            runLifecycle: {onError, onSettle},
        });

        subscriber.onRunErrorEvent!({event: {message: 'boom'}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(onError).toHaveBeenCalledOnce();
        expect(onSettle).toHaveBeenCalledOnce();
    });

    it('onRunErrorEvent falls back to a generic message when the event payload has no message', () => {
        // Reactor / SDK edge cases can fire RUN_ERROR with an empty or absent message field. The bubble
        // must still render SOMETHING so the user knows the turn failed — silent failure with no inline
        // indication is exactly the regression this fix is preventing.
        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onRunErrorEvent!({event: {message: ''}} as Parameters<
            NonNullable<typeof subscriber.onRunErrorEvent>
        >[0]);

        expect(addMessage).toHaveBeenCalledWith({
            content: [
                {
                    data: {message: 'The agent run failed before completing this turn.'},
                    type: 'data-run-error',
                },
            ],
            role: 'assistant',
        });
    });

    it('createConnection tool result calls addMessage with a data-create-connection part', () => {
        const addMessage = vi.fn();

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rc', toolCallName: 'createConnection'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    kind: 'create-connection',
                    componentName: 'slack',
                    componentLabel: 'Slack',
                }),
                toolCallId: 'call-rc',
            })
        );

        expect(addMessage).toHaveBeenCalledOnce();

        const calledWith = addMessage.mock.calls[0][0];

        expect(calledWith.role).toBe('assistant');
        expect(Array.isArray(calledWith.content)).toBe(true);
        expect(calledWith.content[0].type).toBe('data-create-connection');
        expect(calledWith.content[0].data.componentName).toBe('slack');
        expect(calledWith.content[0].data.componentLabel).toBe('Slack');
    });
});

describe('buildStateToSend', () => {
    beforeEach(() => {
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
        aiHubComposerStore.setState({referencedResources: []});
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('includes currentTabs and activeFileId from the tabs store', () => {
        aiHubTabsStore.getState().openFileTab('42', 'spec.md');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 7,
        });

        expect(state.currentTabs).toHaveLength(1);
        expect(state.currentTabs[0]!.id).toBe('42');
        expect(state.currentTabs[0]!.kind).toBe('file');
        expect(state.activeFileId).toBe('42');
        expect(state.workspaceId).toBe('7');
        expect(state.source).toBe('AI_HUB');
        expect(state.mode).toBe('ASK');
    });

    it('uses null activeFileId when no tab is active', () => {
        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 7,
        });

        expect(state.activeFileId).toBeNull();
        expect(state.currentTabs).toHaveLength(0);
    });

    it('includes activeTab with correct kind/id/name for a file tab', () => {
        aiHubTabsStore.getState().openFileTab('file-1', 'notes.md');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.activeTab).not.toBeNull();
        expect(state.activeTab!.kind).toBe('file');
        expect(state.activeTab!.id).toBe('file-1');
        expect(state.activeTab!.name).toBe('notes.md');
    });

    it('includes activeTab with correct kind/id/name for a workflow tab', () => {
        aiHubTabsStore.getState().openWorkflowTab('wf-2', 'proj-2', 20, 'Order Pipeline');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.activeTab).not.toBeNull();
        expect(state.activeTab!.kind).toBe('workflow');
        expect(state.activeTab!.id).toBe('wf-2');
        expect(state.activeTab!.name).toBe('Order Pipeline');
    });

    it('includes activeTab with correct kind/id/name for a data-table tab', () => {
        aiHubTabsStore.getState().openDataTableTab('dt-2', 'Leads');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.activeTab).not.toBeNull();
        expect(state.activeTab!.kind).toBe('dataTable');
        expect(state.activeTab!.id).toBe('dt-2');
        expect(state.activeTab!.name).toBe('Leads');
    });

    it('includes activeTab with correct kind/id/name for a knowledge-base tab', () => {
        aiHubTabsStore.getState().openKnowledgeBaseTab('kb-2', 'Help Center');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.activeTab).not.toBeNull();
        expect(state.activeTab!.kind).toBe('knowledgeBase');
        expect(state.activeTab!.id).toBe('kb-2');
        expect(state.activeTab!.name).toBe('Help Center');
    });

    it('returns null activeTab when no tab is active', () => {
        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.activeTab).toBeNull();
    });

    it('currentTabs includes all tab kinds', () => {
        aiHubTabsStore.getState().openFileTab('file-3', 'readme.md');
        aiHubTabsStore.getState().openWorkflowTab('wf-3', 'proj-3', 30, 'ETL Flow');
        aiHubTabsStore.getState().openDataTableTab('dt-3', 'Products');
        aiHubTabsStore.getState().openKnowledgeBaseTab('kb-3', 'API Docs');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.currentTabs).toHaveLength(4);

        const kinds = state.currentTabs.map((tab) => tab.kind);

        expect(kinds).toContain('file');
        expect(kinds).toContain('workflow');
        expect(kinds).toContain('dataTable');
        expect(kinds).toContain('knowledgeBase');
    });

    it('referencedResources is empty when the composer store has no refs', () => {
        aiHubTabsStore.getState().openFileTab('file-4', 'test.md');

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.referencedResources).toEqual([]);
    });

    it('referencedResources includes refs from the composer store', () => {
        aiHubComposerStore.getState().addReference({id: 'file-5', kind: 'file', name: 'notes.md'});
        aiHubComposerStore.getState().addReference({id: 'wf-1', kind: 'workflow', name: 'My Flow'});

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.referencedResources).toHaveLength(2);
        expect(state.referencedResources).toContainEqual({id: 'file-5', kind: 'file', name: 'notes.md'});
        expect(state.referencedResources).toContainEqual({id: 'wf-1', kind: 'workflow', name: 'My Flow'});
    });

    it('composer store is cleared after calling clear() following buildStateToSend', () => {
        aiHubComposerStore.getState().addReference({id: 'kb-1', kind: 'knowledgeBase', name: 'Help Center'});

        buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        aiHubComposerStore.getState().clear();

        expect(aiHubComposerStore.getState().referencedResources).toHaveLength(0);
    });

    it('includes environmentId from the environment store as a string', () => {
        environmentStore.setState({currentEnvironmentId: 3});

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.environmentId).toBe('3');
    });

    it('defaults environmentId to "0" when the environment store has currentEnvironmentId 0', () => {
        environmentStore.setState({currentEnvironmentId: 0});

        const state = buildStateToSend({
            mode: MODE.ASK,
            workspaceId: 1,
        });

        expect(state.environmentId).toBe('0');
    });
});

describe('runPostTurnTelemetry', () => {
    beforeEach(() => {
        mockPatchChat.mockReset();
        mockGenerateChatTitle.mockReset();
        // We don't read the response body in any of these tests; satisfy the typed mock with a minimal
        // object that matches AiHubChatI's required fields rather than an `as never` cast.
        const stubChat = {
            createdAt: '2026-01-01T00:00:00Z',
            autoTitled: true,
            id: 1,
            kind: 'STANDARD' as const,
            lastPreview: null,
            messageCount: 0,
            aiHubTaskId: null,
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

    it('calls patchChat with lastPreview and messageCount after a turn — no title generation when messageCount < 2', async () => {
        const mockInvalidate = vi.fn();

        // messageCount=1 represents a partial turn: a user message has been added to the local store
        // but the assistant message hasn't arrived yet. Title generation should NOT fire here — there's
        // no assistant content to summarise. The patch telemetry still fires so the sidebar's
        // last-preview / sort updates remain accurate during streaming.
        runPostTurnTelemetry({
            chatId: 42,
            input: 'Hello world',
            invalidateChats: mockInvalidate,
            messageCount: 1,
            workspaceId: 7,
        });

        await vi.waitFor(() => {
            expect(mockPatchChat).toHaveBeenCalledWith({
                chatId: 42,
                patch: {lastPreview: 'Hello world', messageCount: 1},
                workspaceId: 7,
            });
        });

        expect(mockGenerateChatTitle).not.toHaveBeenCalled();
        expect(mockInvalidate).not.toHaveBeenCalled();
    });

    it('fires generateAiHubChatTitle and invalidates as soon as messageCount reaches 2 (after the first complete exchange)', async () => {
        // The threshold was lowered from a bounded `>= 6 && <= 7` window (three exchanges) to `>= 2`
        // (one exchange): the bounded window silently no-op'd if a single turn ever pushed the count
        // past 7, leaving titles stuck at "New Chat" forever after a transient failure or a
        // turn that emitted multiple assistant messages. The unbounded `>= 2` re-attempts on every
        // subsequent turn while the chat is untitled; the server endpoint is idempotent
        // (early-return when the row's title is already non-blank) so the retry loop is cheap on the
        // happy path.
        const mockInvalidate = vi.fn();

        runPostTurnTelemetry({
            chatId: 99,
            input: 'First turn',
            invalidateChats: mockInvalidate,
            messageCount: 2,
            workspaceId: 3,
        });

        await vi.waitFor(() => {
            expect(mockGenerateChatTitle).toHaveBeenCalledWith({chatId: 99, workspaceId: 3});
        });

        await vi.waitFor(() => {
            expect(mockInvalidate).toHaveBeenCalledOnce();
        });
    });

    it('continues attempting title generation on every subsequent turn while the chat stays untitled', async () => {
        // Pinned regression: the previous bounded window (`<= 7`) skipped retries on any turn past 7
        // messages. The new shape fires on every turn with `messageCount >= 2`; the server's
        // idempotency check (returns early when title is already set) keeps the cost negligible if a
        // title was already written — so the only observable behavior that needs pinning here is
        // "every turn re-attempts". An assertion failure here would mean a regression to a bounded
        // upper limit.
        runPostTurnTelemetry({
            chatId: 100,
            input: 'Tenth turn',
            invalidateChats: vi.fn(),
            messageCount: 20,
            workspaceId: 3,
        });

        await vi.waitFor(() => {
            expect(mockGenerateChatTitle).toHaveBeenCalledWith({chatId: 100, workspaceId: 3});
        });
    });
});

describe('AiHubChatsKeys.artifacts', () => {
    it('returns a key that includes the base key, artifacts, chatId, and workspaceId', () => {
        const key = AiHubChatsKeys.artifacts(42, 7);

        expect(key).toEqual(['aiHubChats', 'artifacts', 42, 7]);
    });

    it('returns different keys for different chatId values', () => {
        const keyA = AiHubChatsKeys.artifacts(1, 1);
        const keyB = AiHubChatsKeys.artifacts(2, 1);

        expect(keyA).not.toEqual(keyB);
    });

    it('returns different keys for different workspaceId values', () => {
        const keyA = AiHubChatsKeys.artifacts(1, 1);
        const keyB = AiHubChatsKeys.artifacts(1, 2);

        expect(keyA).not.toEqual(keyB);
    });

    it('key starts with the base all key so invalidating all also covers artifacts', () => {
        const allKey = AiHubChatsKeys.all;
        const artifactsKey = AiHubChatsKeys.artifacts(10, 5);

        expect(artifactsKey.slice(0, allKey.length)).toEqual([...allKey]);
    });
});

describe('tool-call store routing in buildAiHubSubscriber', () => {
    beforeEach(() => {
        aiChatToolCallStore.setState({order: [], toolCalls: {}});
        aiChatRetryableErrorStore.setState({currentError: undefined});
        aiHubProgressStore.setState({progressText: null});
        aiHubTabsStore.setState({
            activeTabId: undefined,
            openTabs: [],
            rightPanelOpen: false,
        });
        mockOpenWorkflowSseStream.mockReset();
        mockFetchWorkflowResponse.mockReset();
    });

    it('onToolCallStartEvent registers a running entry tagged with the assistant message index', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            assistantMessageIndex: 4,
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-x', toolCallName: 'createMemory'}));

        const entry = aiChatToolCallStore.getState().toolCalls['call-x'];

        expect(entry).toBeDefined();
        expect(entry.toolName).toBe('createMemory');
        expect(entry.status).toBe('running');
        expect(entry.messageIndex).toBe(4);
    });

    it('onToolCallEndEvent merges the args into the entry', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-args', toolCallName: 'createMemory'})
        );

        subscriber.onToolCallEndEvent!(
            makeToolCallEndParams({
                toolCallArgs: {name: 'standup'},
                toolCallName: 'createMemory',
                toolCallId: 'call-args',
            })
        );

        expect(aiChatToolCallStore.getState().toolCalls['call-args'].args).toEqual({name: 'standup'});
    });

    it('onToolCallResultEvent flips the entry to success and stores the parsed result', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-r', toolCallName: 'getFile'}));

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({content: JSON.stringify({contents: 'hello'}), toolCallId: 'call-r'})
        );

        const entry = aiChatToolCallStore.getState().toolCalls['call-r'];

        expect(entry.status).toBe('success');
        expect(entry.result).toEqual({contents: 'hello'});
    });

    it('onToolCallResultEvent flips to error when the result has an error field', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue('hello'),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-err', toolCallName: 'createReport'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({content: JSON.stringify({error: 'Service down'}), toolCallId: 'call-err'})
        );

        expect(aiChatToolCallStore.getState().toolCalls['call-err'].status).toBe('error');
    });

    it('runChatWorkflow streaming routes the SSE chunks into the matching tool-call progressive output', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(
            makeToolCallStartParams({toolCallId: 'call-rcw', toolCallName: 'runChatWorkflow'})
        );

        subscriber.onToolCallResultEvent!(
            makeToolCallResultParams({
                content: JSON.stringify({
                    streamUrl: '/webhooks/x/sse',
                    streaming: true,
                    workflowExecutionId: 'exec-1',
                }),
                toolCallId: 'call-rcw',
            })
        );

        expect(mockOpenWorkflowSseStream).toHaveBeenCalledOnce();

        // Simulate the SSE handler invoking the supplied onChunk callback (formerly named
        // appendToLastAssistantMessage — the rename also fixed a misleading-name bug, see the review).
        const args = mockOpenWorkflowSseStream.mock.calls[0][0]!;

        args.onChunk('Step 1 done');

        expect(aiChatToolCallStore.getState().toolCalls['call-rcw'].progressiveOutput).toBe('Step 1 done');
    });

    it('subagent-progress CustomEvent routes into the matching subagent tool-call progress list', () => {
        const subscriber = buildAiHubSubscriber({
            addMessage: vi.fn(),
            appendToLastAssistantMessage: vi.fn(),
            getLastUserMessage: vi.fn().mockReturnValue(''),
        });

        subscriber.onToolCallStartEvent!(makeToolCallStartParams({toolCallId: 'call-sub', toolCallName: 'research'}));

        subscriber.onCustomEvent!(
            makeCustomEventParams('subagent-progress', {subagentName: 'research', text: 'Searching'})
        );

        const entry = aiChatToolCallStore.getState().toolCalls['call-sub'];

        expect(entry.progress).toHaveLength(1);
        expect(entry.progress[0].text).toBe('Searching');
    });
});

describe('hasLivePropertyOptionPicker', () => {
    // Annotate the return as ThreadMessageLike so the literal `type: 'data-select-property-option'` is
    // checked against the `data-${string}` template type instead of widening to `string` (which fails to
    // satisfy DataPrefixedPart).
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

    it('returns false when the property differs', () => {
        expect(hasLivePropertyOptionPicker([picker('slack', 'channel')], 'slack', 'username')).toBe(false);
    });

    it('returns false when the component differs', () => {
        expect(hasLivePropertyOptionPicker([picker('slack', 'channel')], 'gmail', 'channel')).toBe(false);
    });

    it('ignores non-picker messages (plain text / other data parts)', () => {
        expect(
            hasLivePropertyOptionPicker(
                [{content: 'just text', role: 'assistant'}, picker('slack', 'channel')],
                'slack',
                'username'
            )
        ).toBe(false);
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
        const messages: ThreadMessageLike[] = [
            {content: 'Find me a workflow', role: 'user'},
            {content: 'On it', role: 'assistant'},
        ];

        const result = projectMessagesWithToolCalls(messages, [
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
        ]);

        const assistantMessage = result[1];

        expect(Array.isArray(assistantMessage.content)).toBe(true);

        const parts = assistantMessage.content as ReadonlyArray<{type: string; toolName?: string}>;

        expect(parts).toHaveLength(2);
        expect(parts[0].type).toBe('text');
        expect(parts[1].type).toBe('tool-call');
        expect(parts[1].toolName).toBe('searchWorkflows');
    });

    it('does not attach tool-call parts to user messages even if their index matches', () => {
        const messages: ThreadMessageLike[] = [{content: 'User', role: 'user'}];

        const result = projectMessagesWithToolCalls(messages, [
            {
                messageIndex: 0,
                progress: [],
                progressiveOutput: '',
                status: 'success',
                toolCallId: 'call-1',
                toolName: 'noop',
            },
        ]);

        expect(result[0]).toEqual(messages[0]);
    });

    it('groups multiple tool calls with the same messageIndex onto one assistant message', () => {
        const messages: ThreadMessageLike[] = [
            {content: 'q', role: 'user'},
            {content: '', role: 'assistant'},
        ];

        const result = projectMessagesWithToolCalls(messages, [
            {
                messageIndex: 1,
                progress: [],
                progressiveOutput: '',
                status: 'running',
                toolCallId: 'call-a',
                toolName: 'first',
            },
            {
                messageIndex: 1,
                progress: [],
                progressiveOutput: '',
                status: 'running',
                toolCallId: 'call-b',
                toolName: 'second',
            },
        ]);

        const assistantMessage = result[1];
        const parts = assistantMessage.content as ReadonlyArray<{type: string; toolName?: string}>;

        // Empty assistant text means no text part is created — only the two tool-call parts.
        expect(parts).toHaveLength(2);
        expect(parts.map((part) => part.toolName)).toEqual(['first', 'second']);
    });
});

describe('bootstrapAiHubTurnRunState', () => {
    it('calls markRunning BEFORE the (slow) chat-create resolves', async () => {
        const calls: string[] = [];
        let resolveCreate: () => void = () => {};

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

        // markRunning must have run synchronously, before the create promise resolves.
        expect(calls).toEqual(['mark-running', 'create-started']);

        resolveCreate();

        await expect(resultPromise).resolves.toBe(true);
        expect(calls).toEqual(['mark-running', 'create-started', 'create-resolved']);
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
