import {createAiHubChat, generateAiHubChatTitle, patchChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useTruncateAiHubChatMessagesMutation} from '@/ee/pages/automation/ai-hub/chats/hooks/useTruncateChatMessages';
import {isWorkspaceIdKnown} from '@/ee/pages/automation/ai-hub/chats/isWorkspaceIdKnown';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {aiHubComposerStore} from '@/ee/pages/automation/ai-hub/composer/stores/useAiHubComposerStore';
import {attachToInFlightRun} from '@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient';
import {
    aiHubRunStateStore,
    isChatRunning,
    useAiHubRunStateStore,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {sanitizeAssistantMessages} from '@/ee/pages/automation/ai-hub/runtime-providers/stripLeakedToolMarkup';
import {isParsedToolResultValue} from '@/ee/pages/automation/ai-hub/runtime-providers/toolResultValue';
import {generateMessageId} from '@/ee/pages/automation/ai-hub/stores/messageId';
import {MODE, useAiHubStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubStore';
import {aiHubTabsStore, getTabGenericId} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {ApprovalResolutionContext} from '@/shared/components/ai-chat/approvalResolutionContext';
import {toRunErrorData} from '@/shared/components/ai-chat/messages/toRunErrorData';
import {parseJson, toToolResultDataPart} from '@/shared/components/ai-chat/messages/toToolResultDataPart';
import {aiChatRetryableErrorStore} from '@/shared/components/ai-chat/stores/useAiChatRetryableErrorStore';
import {
    ToolCallEntryI,
    aiChatToolCallStore,
    isRunningToolCall,
    useAiChatToolCallStore,
} from '@/shared/components/ai-chat/stores/useAiChatToolCallStore';
import {resolveRememberedModel} from '@/shared/components/ai/model-picker/resolveRememberedModel';
import {
    AiProviderCatalogQuery,
    useAiProviderCatalogQuery,
    useResolveAiHubToolApprovalMutation,
} from '@/shared/middleware/graphql';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {environmentStore, useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {getCookie} from '@/shared/util/cookie-utils';
import {getRandomId} from '@/shared/util/random-utils';
import {AgentSubscriber, HttpAgent} from '@ag-ui/client';
import {contentToText} from '@ag-ui/core';
import {AppendMessage, AssistantRuntimeProvider, ThreadMessageLike, useExternalStoreRuntime} from '@assistant-ui/react';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {ReactNode, useCallback, useEffect, useMemo, useRef} from 'react';
import {toast} from 'sonner';
import {useShallow} from 'zustand/react/shallow';

const AI_HUB_AGENT_ID = 'AI_HUB';

export const TURN_IN_FLIGHT_MESSAGE = 'A turn is already running in this chat.';

export const WORKSPACE_NOT_READY_MESSAGE = 'The workspace is still loading. Send again in a moment.';

export const EDIT_NOT_SENT_MESSAGE =
    'The chat changed while your edit was being saved, so the edited message was not sent. Open the chat to send it again.';

type OpenWorkflowTabResultType =
    | {name: string; opened: true; projectId: string; projectWorkflowId: number; workflowId: string}
    | {error: string; opened: false};

interface ToolCallErrorResultI {
    error: string;
}

/**
 * Narrows the {@code openWorkflowTab} result JSON. Lenient on the failure side: an {@code opened: false} without an
 * error string still yields a failure the user can see.
 */
const validateOpenWorkflowTabResult = (raw: unknown): OpenWorkflowTabResultType | null => {
    if (typeof raw !== 'object' || raw === null) {
        return null;
    }

    const record = raw as Record<string, unknown>;

    if (
        record.opened === true &&
        typeof record.workflowId === 'string' &&
        typeof record.projectId === 'string' &&
        typeof record.projectWorkflowId === 'number' &&
        typeof record.name === 'string'
    ) {
        return {
            name: record.name,
            opened: true,
            projectId: record.projectId,
            projectWorkflowId: record.projectWorkflowId,
            workflowId: record.workflowId,
        };
    }

    return {error: typeof record.error === 'string' ? record.error : 'tool reported opened:false', opened: false};
};

/**
 * Recognizes the server's 409 {@code {"error":"TURN_IN_FLIGHT"}} response, which the hub returns when a turn is
 * already running on the thread. The HttpAgent's rejection carries the parsed body as {@code payload}; the
 * subscriber's RUN_ERROR only carries the SDK's {@code `HTTP 409: <body>`} message, so the body is recovered from the
 * first {@code {}.
 */
export function isTurnInFlightError(source: {message?: string; payload?: unknown} | null | undefined): boolean {
    if (!source) {
        return false;
    }

    const isTurnInFlightBody = (body: unknown) =>
        body != null && typeof body === 'object' && (body as {error?: unknown}).error === 'TURN_IN_FLIGHT';

    if (isTurnInFlightBody(source.payload)) {
        return true;
    }

    if (typeof source.message !== 'string') {
        return false;
    }

    const jsonStart = source.message.indexOf('{');

    if (jsonStart < 0) {
        return false;
    }

    try {
        return isTurnInFlightBody(JSON.parse(source.message.slice(jsonStart)));
    } catch {
        return false;
    }
}

/**
 * Reads the marker {@link stampOptimisticTurnMarker} wrote onto a message, or `undefined` for a message that never
 * carried one.
 */
export function getOptimisticTurnMarker(message: ThreadMessageLike): string | undefined {
    const marker = message.metadata?.custom?.optimisticTurnMarker;

    return typeof marker === 'string' ? marker : undefined;
}

/**
 * Tags a message as belonging to one optimistic-append attempt, so a TURN_IN_FLIGHT rollback removes exactly the
 * messages it added, by identity rather than by position.
 */
export function stampOptimisticTurnMarker(message: ThreadMessageLike, marker: string): ThreadMessageLike {
    return {
        ...message,
        metadata: {...message.metadata, custom: {...message.metadata?.custom, optimisticTurnMarker: marker}},
    };
}

/** Removes every message tagged with `marker` from the store. */
export function removeOptimisticTurnMessages(marker: string): void {
    useAiHubStore.setState((state) => ({
        messages: state.messages.filter((message) => getOptimisticTurnMarker(message) !== marker),
    }));
}

/** The text of a message: its string content, or its text parts joined by newlines. */
export function getMessageText(message: ThreadMessageLike): string {
    if (typeof message.content === 'string') {
        return message.content;
    }

    return message.content
        .filter((part): part is {text: string; type: 'text'} => part?.type === 'text')
        .map((part) => part.text)
        .join('\n');
}

/**
 * The index of the user message a reload regenerates the reply to: the nearest user message at or before the reloaded
 * reply's parent, walking back over tool-only and data rows. -1 when there is none.
 */
export function findReloadedUserMessageIndex(messages: ThreadMessageLike[], parentId: string | null): number {
    const parentIndex = parentId == null ? -1 : messages.findIndex((message) => message.id === parentId);

    for (let index = parentIndex; index >= 0; index--) {
        if (messages[index].role === 'user') {
            return index;
        }
    }

    return -1;
}

interface BuildSubscriberDepsI {
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    /** Index of the assistant message that owns the tool calls of this turn. */
    assistantMessageIndex?: number;
    /**
     * AG-UI thread id this turn belongs to; events arriving after a switch to another chat are dropped, and so are
     * events arriving after the transcript the subscriber was built for was replaced, even by the same chat's.
     */
    chatId?: string;
    getLastUserMessage: () => string;
    /** Fires instead of the red error bubble when the server rejects the turn with 409 TURN_IN_FLIGHT. */
    onTurnInFlight?: (threadId: string) => void;
    /** Fires with the raw tool result after a successful {@code buildWorkflow} or {@code importWorkflow}. */
    onWorkflowMutated?: (toolResultContent: string) => void;
    /**
     * Fires on RUN_FINISHED and RUN_ERROR, releasing the running flag as soon as the server says the turn is done,
     * without waiting for {@code agent.runAgent}'s promise.
     */
    runLifecycle?: {onSettle: () => void};
    /** The per-turn abort signal; an already aborted signal marks a RUN_ERROR as a user cancel. */
    turnAbortSignal?: AbortSignal;
}

/**
 * Starts a turn so the Stop button shows immediately on send: {@code markRunning} runs synchronously before the
 * awaited {@code createChatIfNeeded}. On create failure {@code revertOnFailure} rolls back and this returns false.
 */
export async function bootstrapAiHubTurnRunState({
    createChatIfNeeded,
    markRunning,
    revertOnFailure,
}: {
    createChatIfNeeded: () => Promise<void>;
    markRunning: () => void;
    revertOnFailure: (error: unknown) => void;
}): Promise<boolean> {
    markRunning();

    try {
        await createChatIfNeeded();

        return true;
    } catch (error) {
        revertOnFailure(error);

        return false;
    }
}

/**
 * True when a live select-property-option picker for the same component and property already exists in the thread,
 * so a repeated tool call does not stack identical pickers.
 */
export const hasLivePropertyOptionPicker = (
    messages: ThreadMessageLike[],
    componentName: unknown,
    propertyName: unknown
): boolean =>
    messages.some(
        (message) =>
            Array.isArray(message.content) &&
            message.content.some((part) => {
                const dataPart = part as {data?: Record<string, unknown>; type?: unknown};

                return (
                    dataPart.type === 'data-select-property-option' &&
                    dataPart.data?.componentName === componentName &&
                    dataPart.data?.propertyName === propertyName
                );
            })
    );

const WORKFLOW_MUTATING_TOOL_NAMES = new Set([
    'buildWorkflow',
    'createProjectWorkflow',
    'deleteProject',
    'deleteWorkflow',
    'importWorkflow',
    'publishProject',
    'saveWorkflowTestConnection',
]);

/**
 * Invalidates what a successful workflow-mutating hub tool result may have changed: the workflow lists, the
 * workflow test configurations, and the project workflows of every project with an open workflow tab (plus the
 * project named in the result, when the result is JSON carrying one), which is what the embedded editor reads.
 */
export function invalidateWorkflowQueries(queryClient: QueryClient, toolResultContent?: string): void {
    const projectIds = new Set<number>();

    aiHubTabsStore.getState().openTabs.forEach((tab) => projectIds.add(Number(tab.projectId)));

    if (toolResultContent?.trimStart().startsWith('{')) {
        const result = parseJson<{projectId?: unknown}>(toolResultContent, 'workflow mutation result');

        if (result?.projectId != null) {
            projectIds.add(Number(result.projectId));
        }
    }

    queryClient.invalidateQueries({queryKey: WorkflowTestConfigurationKeys.workflowTestConfigurations});
    queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.workflows});

    projectIds.forEach((projectId) => {
        if (!Number.isNaN(projectId)) {
            queryClient.invalidateQueries({queryKey: ProjectWorkflowKeys.projectWorkflows(projectId)});
        }
    });
}

/**
 * Parses a tool result as JSON. Objects and arrays that fail to parse are logged. Any other text parses only when it is
 * a JSON string; prose and a bare JSON number, boolean or null are returned as not parsed, without logging, so the
 * card shows the text.
 */
const parseToolResult = (content: string): {ok: true; value: unknown} | {ok: false} => {
    if (/^[[{]/.test(content.trimStart())) {
        const parsed = parseJson<unknown>(content, 'tool-result payload');

        return parsed === null ? {ok: false} : {ok: true, value: parsed};
    }

    try {
        const parsed: unknown = JSON.parse(content);

        return isParsedToolResultValue(parsed) ? {ok: true, value: parsed} : {ok: false};
    } catch {
        return {ok: false};
    }
};

/**
 * Terminates the tool calls of {@code chatId} still running when the run ends; a call that never received its result
 * event would otherwise spin forever. Scoped by chat so a parallel chat's running calls are untouched.
 */
export const terminateOrphanRunningToolCalls = (chatId: string | undefined): void => {
    if (chatId == null) {
        return;
    }

    aiChatToolCallStore.getState().failRunningInChat(chatId, {reason: 'Run finished with no result event'}, 'aborted');
};

/**
 * Builds an AG-UI subscriber that streams text into assistant messages, records every tool call in the per-tool-
 * call store, opens workflow tabs, and turns interactive tool results into data parts. A text message that follows
 * earlier text or a tool call opens a new assistant message, and each tool call attaches to the assistant message
 * current at its start. A tool error, a failed workflow tab open or a malformed interactive payload raises the retry
 * banner only when the run finishes without a later successful call of a tool with the same name; any such success counts as the recovery, whatever its arguments, since a model
 * retrying a failed call usually changes them. A run that fails shows its run error bubble instead of the banner, and a
 * user Stop shows neither.
 */
export const buildAiHubSubscriber = ({
    addMessage,
    appendToLastAssistantMessage,
    assistantMessageIndex = 0,
    chatId: subscriberChatId,
    getLastUserMessage,
    onTurnInFlight,
    onWorkflowMutated,
    runLifecycle,
    turnAbortSignal,
}: BuildSubscriberDepsI): AgentSubscriber => {
    const toolCallNamesById = new Map<string, string>();
    const unrecoveredToolFailures = new Map<string, string>();

    let currentAssistantMessageIndex = assistantMessageIndex;
    let currentAssistantMessageId = useAiHubStore.getState().messages[assistantMessageIndex]?.id;
    let currentAssistantMessageHasOutput = false;
    let startsNewAssistantMessage = false;

    const subscriberTranscriptGeneration = useAiHubStore.getState().transcriptGeneration;

    const resolveCurrentAssistantMessageIndex = () => {
        const messageIndex =
            currentAssistantMessageId == null
                ? -1
                : useAiHubStore.getState().messages.findIndex((message) => message.id === currentAssistantMessageId);

        return messageIndex >= 0 ? messageIndex : currentAssistantMessageIndex;
    };

    const isStaleChat = () =>
        (subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId) ||
        useAiHubStore.getState().transcriptGeneration !== subscriberTranscriptGeneration;

    const recordToolFailure = (toolName: string, reason: string) => {
        unrecoveredToolFailures.delete(toolName);
        unrecoveredToolFailures.set(toolName, reason);
    };

    const surfaceUnrecoveredToolFailure = () => {
        const lastUnrecoveredToolFailure = Array.from(unrecoveredToolFailures).pop();

        unrecoveredToolFailures.clear();

        if (lastUnrecoveredToolFailure && !isStaleChat()) {
            aiChatRetryableErrorStore.getState().setError({
                errorMessage: lastUnrecoveredToolFailure[1],
                lastUserMessage: getLastUserMessage(),
                toolName: lastUnrecoveredToolFailure[0],
            });
        }
    };

    const openWorkflowTab = (toolCallName: string, toolResultContent: string) => {
        const parsed = validateOpenWorkflowTabResult(parseJson<unknown>(toolResultContent, 'openWorkflowTab result'));

        if (parsed === null) {
            console.warn(`[AiHub] ${toolCallName}: unparseable result`, toolResultContent);

            recordToolFailure(toolCallName, 'unparseable result');

            return;
        }

        if (!parsed.opened) {
            recordToolFailure(toolCallName, parsed.error);

            return;
        }

        aiHubTabsStore.getState().openWorkflowTab({
            name: parsed.name,
            projectId: parsed.projectId,
            projectWorkflowId: parsed.projectWorkflowId,
            workflowId: parsed.workflowId,
        });
    };

    const addInteractiveDataPart = (toolCallId: string, toolCallName: string | undefined, content: string) => {
        const dataPart = toToolResultDataPart(toolCallName ?? '', content);

        if (!dataPart) {
            return;
        }

        if (!dataPart.ok) {
            aiChatToolCallStore.getState().completeToolCall(toolCallId, {error: dataPart.errorMessage}, true);

            recordToolFailure(dataPart.toolName, dataPart.errorMessage);

            return;
        }

        if (
            dataPart.type === 'data-select-property-option' &&
            hasLivePropertyOptionPicker(
                useAiHubStore.getState().messages,
                dataPart.data.componentName,
                dataPart.data.propertyName
            )
        ) {
            return;
        }

        addMessage({
            content: [{data: dataPart.data, type: dataPart.type as `data-${string}`}],
            role: 'assistant',
        });

        if (dataPart.type === 'data-tool-approval-request' && subscriberChatId != null) {
            aiHubChatsStore.getState().setActivityState(subscriberChatId, 'paused');
        }
    };

    return {
        onRunErrorEvent: ({event}) => {
            const userAborted = turnAbortSignal?.aborted === true;
            const turnInFlight = subscriberChatId != null && isTurnInFlightError(event as {message?: string});

            if (turnInFlight) {
                onTurnInFlight?.(subscriberChatId!);
            } else if (!userAborted && !isStaleChat()) {
                addMessage({
                    content: [
                        {data: toRunErrorData({code: event?.code, message: event?.message}), type: 'data-run-error'},
                    ],
                    role: 'assistant',
                });
            }

            unrecoveredToolFailures.clear();

            runLifecycle?.onSettle();

            terminateOrphanRunningToolCalls(subscriberChatId);

            if (subscriberChatId != null) {
                aiHubChatsStore.getState().clearActivityState(subscriberChatId);
            }
        },
        onRunFinishedEvent: () => {
            surfaceUnrecoveredToolFailure();

            runLifecycle?.onSettle();

            terminateOrphanRunningToolCalls(subscriberChatId);

            if (subscriberChatId != null && aiHubChatsStore.getState().chatActivity[subscriberChatId] !== 'paused') {
                aiHubChatsStore.getState().clearActivityState(subscriberChatId);
            }
        },
        onTextMessageContentEvent: ({event, textMessageBuffer}) => {
            if (isStaleChat()) {
                return;
            }

            const text = textMessageBuffer + event.delta;

            if (startsNewAssistantMessage) {
                startsNewAssistantMessage = false;

                addMessage({content: text, role: 'assistant'});

                const {messages} = useAiHubStore.getState();

                currentAssistantMessageIndex = messages.length - 1;
                currentAssistantMessageId = messages[currentAssistantMessageIndex]?.id;
            } else {
                appendToLastAssistantMessage(text);
            }

            currentAssistantMessageHasOutput = true;
        },
        onTextMessageStartEvent: () => {
            if (isStaleChat()) {
                return;
            }

            startsNewAssistantMessage = currentAssistantMessageHasOutput;
        },
        onToolCallEndEvent: ({event, toolCallArgs}) => {
            if (isStaleChat()) {
                return;
            }

            aiChatToolCallStore.getState().updateToolCallArgs(event.toolCallId, toolCallArgs ?? {});
        },
        onToolCallResultEvent: ({event}) => {
            if (isStaleChat()) {
                return;
            }

            const toolResultContent = contentToText(event.content);
            const toolCallName = toolCallNamesById.get(event.toolCallId);

            toolCallNamesById.delete(event.toolCallId);

            const parsedResult = parseToolResult(toolResultContent);
            const errorResult = parsedResult.ok ? (parsedResult.value as ToolCallErrorResultI | null) : null;
            const errorMessage =
                errorResult != null && typeof errorResult === 'object' && typeof errorResult.error === 'string'
                    ? errorResult.error
                    : null;

            aiChatToolCallStore
                .getState()
                .completeToolCall(
                    event.toolCallId,
                    parsedResult.ok ? parsedResult.value : toolResultContent,
                    errorMessage !== null
                );

            if (errorMessage !== null) {
                recordToolFailure(toolCallName ?? 'unknown', errorMessage);

                return;
            }

            unrecoveredToolFailures.delete(toolCallName ?? 'unknown');

            if (toolCallName !== undefined && WORKFLOW_MUTATING_TOOL_NAMES.has(toolCallName)) {
                onWorkflowMutated?.(toolResultContent);
            }

            if (toolCallName === 'openWorkflowTab') {
                openWorkflowTab(toolCallName, toolResultContent);
            } else {
                addInteractiveDataPart(event.toolCallId, toolCallName, toolResultContent);
            }
        },
        onToolCallStartEvent: ({event}) => {
            if (isStaleChat()) {
                return;
            }

            toolCallNamesById.set(event.toolCallId, event.toolCallName);

            aiChatToolCallStore
                .getState()
                .startToolCall(
                    event.toolCallId,
                    event.toolCallName,
                    resolveCurrentAssistantMessageIndex(),
                    subscriberChatId
                );

            currentAssistantMessageHasOutput = true;
        },
    };
};

export interface AiHubStateToSendI {
    activeTab: {id: string; kind: string; name: string} | null;
    currentTabs: Array<{id: string; kind: string; name: string}>;
    environmentId: string;
    mode: string;
    parameters: Record<string, unknown>;
    source: string;
    userSelectedLlmModel?: string;
    userSelectedLlmProvider?: string;
    workspaceId: string;
}

/**
 * The AG-UI state sent with every turn: workspace, environment, mode, the open tabs, the active tab, and the model the
 * picker shows: the current chat's selection when both provider and model are set, otherwise the workspace's
 * remembered model while the provider catalog still offers it.
 */
export const buildStateToSend = ({
    mode,
    providerCatalog,
    workspaceId,
}: {
    mode: MODE;
    providerCatalog: AiProviderCatalogQuery['aiProviderCatalog'] | undefined;
    workspaceId: number | undefined;
}) => {
    const tabsState = aiHubTabsStore.getState();
    const chatsState = aiHubChatsStore.getState();

    const activeTab = tabsState.openTabs.find((tab) => tab.id === tabsState.activeTabId);
    const chatLlmSelection =
        chatsState.currentChatId != null ? chatsState.chatLlmSelections[chatsState.currentChatId] : undefined;
    const llmSelection =
        chatLlmSelection?.provider != null && chatLlmSelection.model != null
            ? chatLlmSelection
            : isWorkspaceIdKnown(workspaceId)
              ? resolveRememberedModel(workspaceId, providerCatalog)
              : null;

    const state: AiHubStateToSendI = {
        activeTab: activeTab ? {id: getTabGenericId(activeTab), kind: activeTab.kind, name: activeTab.name} : null,
        currentTabs: tabsState.openTabs.map((tab) => ({id: getTabGenericId(tab), kind: tab.kind, name: tab.name})),
        environmentId: String(environmentStore.getState().currentEnvironmentId ?? 0),
        mode,
        parameters: {},
        source: AI_HUB_AGENT_ID,
        workspaceId: String(workspaceId ?? ''),
    };

    if (llmSelection?.provider != null && llmSelection.model != null) {
        state.userSelectedLlmModel = llmSelection.model;
        state.userSelectedLlmProvider = llmSelection.provider;
    }

    return state;
};

interface PostTurnTelemetryArgsI {
    chatId: number;
    input: string;
    invalidateChats: () => void;
    messageCount: number;
    workspaceId: number;
}

/**
 * Patches the chat's preview and message count after a turn, and once a full exchange exists asks the server to
 * (re)generate the title; the server short-circuits for a chat that is no longer auto-titled.
 */
export const runPostTurnTelemetry = ({
    chatId,
    input,
    invalidateChats,
    messageCount,
    workspaceId,
}: PostTurnTelemetryArgsI): void => {
    patchChat({chatId, patch: {lastPreview: input.slice(0, 200), messageCount}, workspaceId}).catch((error) => {
        console.error(
            `[AiHub] patchChat telemetry failed (workspaceId=${workspaceId}, chatId=${chatId}, messageCount=${messageCount})`,
            error
        );
    });

    if (messageCount >= 2) {
        generateAiHubChatTitle({chatId, workspaceId})
            .then(() => {
                aiHubChatsStore.getState().clearTitleGenerationFailure(chatId);

                invalidateChats();
            })
            .catch((error) => {
                const errorMessage = error instanceof Error ? error.message : String(error);

                console.error('Failed to generate chat title:', error);

                aiHubChatsStore.getState().markTitleGenerationFailed(chatId, errorMessage);

                toast.error(`Failed to generate chat title: ${errorMessage}`);
            });
    }
};

/**
 * Runs when the focused chat changes: drops the tool-call cards of the chat being left. It does not abort the
 * in-flight run, so the turn completes server-side and is loaded on return; the subscriber's chat guards keep its
 * late events out of the new chat.
 */
export const cleanupForChatChange = (previousChatId: string | undefined): void => {
    aiChatToolCallStore.getState().resetForChat(previousChatId);
};

/**
 * Projects the per-tool-call store entries into the assistant messages as `tool-call` parts, after stripping leaked
 * tool markup from assistant text. Duplicates with the same tool name and args within one message collapse onto the
 * latest entry, and parts AG-UI already attached (or attached with an empty id) are not rendered twice.
 */
export const projectMessagesWithToolCalls = (
    messages: ThreadMessageLike[],
    toolCallEntries: ToolCallEntryI[]
): ThreadMessageLike[] => {
    const sanitizedMessages = sanitizeAssistantMessages(messages);

    if (toolCallEntries.length === 0) {
        return sanitizedMessages;
    }

    const callsByMessage = new Map<number, ToolCallEntryI[]>();

    for (const entry of toolCallEntries) {
        const entries = callsByMessage.get(entry.messageIndex) ?? [];

        entries.push(entry);

        callsByMessage.set(entry.messageIndex, entries);
    }

    for (const [messageIndex, entries] of callsByMessage) {
        const entriesByKey = new Map<string, ToolCallEntryI>();

        for (const entry of entries) {
            const key = `${entry.toolName}|${entry.args ? JSON.stringify(entry.args) : ''}`;
            const existing = entriesByKey.get(key);

            if (!existing || isRunningToolCall(existing)) {
                entriesByKey.set(key, entry);
            }
        }

        callsByMessage.set(messageIndex, Array.from(entriesByKey.values()));
    }

    return sanitizedMessages.map((message, index) => {
        const calls = callsByMessage.get(index);

        if (!calls || calls.length === 0 || message.role !== 'assistant') {
            return message;
        }

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const rawExistingParts: any[] =
            typeof message.content === 'string'
                ? message.content.length > 0
                    ? [{text: message.content, type: 'text'}]
                    : []
                : [...message.content];

        const existingParts = rawExistingParts.filter(
            (part) => part?.type !== 'tool-call' || (typeof part.toolCallId === 'string' && part.toolCallId.length > 0)
        );

        const existingToolCallIds = new Set(
            existingParts.filter((part) => part?.type === 'tool-call').map((part) => part.toolCallId as string)
        );

        const toolCallParts = calls
            .filter((call) => !existingToolCallIds.has(call.toolCallId))
            .map((call) => ({
                args: (call.args ?? {}) as Record<string, unknown>,
                argsText: call.args ? JSON.stringify(call.args) : '',
                isError: call.status === 'error',
                result: call.result,
                toolCallId: call.toolCallId,
                toolName: call.toolName,
                type: 'tool-call' as const,
            }));

        if (toolCallParts.length === 0) {
            return message;
        }

        return {...message, content: [...existingParts, ...toolCallParts]};
    });
};

const convertMessage = (message: ThreadMessageLike): ThreadMessageLike => message;

const isAbortError = (error: unknown, signal: AbortSignal) =>
    (error instanceof DOMException && error.name === 'AbortError') ||
    (error instanceof Error && error.name === 'AbortError') ||
    signal.aborted;

export function AiHubRuntimeProvider({children}: Readonly<{children: ReactNode}>) {
    const continuationDisposerRef = useRef<(() => void) | null>(null);
    const turnControllerRef = useRef<AbortController | null>(null);

    const {addMessage, appendToLastAssistantMessage, chatId, editUserMessage, messages, mode} = useAiHubStore(
        useShallow((state) => ({
            addMessage: state.addMessage,
            appendToLastAssistantMessage: state.appendToLastAssistantMessage,
            chatId: state.chatId,
            editUserMessage: state.editUserMessage,
            messages: state.messages,
            mode: state.mode,
        }))
    );
    const isRunning = useAiHubRunStateStore((state) => isChatRunning(state, chatId));
    const toolCalls = useAiChatToolCallStore((state) => state.toolCalls);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const catalogEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const queryClient = useQueryClient();
    const resolveToolApprovalMutation = useResolveAiHubToolApprovalMutation();

    const {data: providerCatalogData} = useAiProviderCatalogQuery(
        {environment: String(catalogEnvironmentId)},
        {enabled: catalogEnvironmentId >= 0}
    );
    const truncateChatMessagesMutation = useTruncateAiHubChatMessagesMutation();

    const handleWorkflowMutated = useCallback(
        (toolResultContent: string) => invalidateWorkflowQueries(queryClient, toolResultContent),
        [queryClient]
    );

    const attachToContinuation = useCallback(
        (threadId: string) => {
            aiHubRunStateStore.getState().setChatRunning(threadId, true);

            const subscriber = buildAiHubSubscriber({
                addMessage,
                appendToLastAssistantMessage,
                assistantMessageIndex: useAiHubStore.getState().messages.length - 1,
                chatId: threadId,
                getLastUserMessage: () => '(resumed turn)',
                onWorkflowMutated: handleWorkflowMutated,
                runLifecycle: {onSettle: () => aiHubRunStateStore.getState().setChatRunning(threadId, false)},
            });

            continuationDisposerRef.current?.();

            continuationDisposerRef.current = attachToInFlightRun({
                onClose: () => {
                    aiHubRunStateStore.getState().setChatRunning(threadId, false);
                    aiHubChatsStore.getState().clearActivityState(threadId);
                },
                subscriber,
                threadId,
            });
        },
        [addMessage, appendToLastAssistantMessage, handleWorkflowMutated]
    );

    const resolveToolApproval = useCallback(
        async (approvalId: number, approved: boolean, comment?: string) => {
            const currentChatId = useAiHubStore.getState().chatId;

            if (currentChatId == null || currentWorkspaceId == null) {
                throw new Error('No active chat');
            }

            const result = await resolveToolApprovalMutation.mutateAsync({
                approvalId: String(approvalId),
                approved,
                comment,
                workspaceId: String(currentWorkspaceId),
            });

            aiHubChatsStore.getState().clearActivityState(currentChatId);

            const {approval, continuationStarted} = result.resolveAiHubToolApproval;

            useAiHubStore.getState().markToolApprovalResolved(approvalId, {
                executionError: approval.executionError,
                status: approval.status,
            });

            if (continuationStarted) {
                addMessage({content: '', role: 'assistant'});

                aiHubChatsStore.getState().setActivityState(currentChatId, 'running');

                attachToContinuation(currentChatId);
            }

            return {executionError: approval.executionError, status: approval.status};
        },
        [addMessage, attachToContinuation, currentWorkspaceId, resolveToolApprovalMutation]
    );

    const approvalResolution = useMemo(() => ({resolveToolApproval}), [resolveToolApproval]);

    const projectedMessages = useMemo(
        () => projectMessagesWithToolCalls(messages, Object.values(toolCalls)),
        [messages, toolCalls]
    );

    const agent = useMemo(
        () =>
            chatId
                ? new HttpAgent({
                      agentId: AI_HUB_AGENT_ID,
                      headers: {'X-XSRF-TOKEN': getCookie('XSRF-TOKEN') || ''},
                      threadId: chatId,
                      url: `/api/platform/internal/ai/chat/${AI_HUB_AGENT_ID.toLowerCase()}`,
                  })
                : null,
        [chatId]
    );

    const startTurnController = () => {
        turnControllerRef.current?.abort();

        const turnController = new AbortController();

        turnControllerRef.current = turnController;

        return turnController;
    };

    const markChatRunning = (runId: string) => {
        aiHubRunStateStore.getState().setChatRunId(chatId, runId);
        aiHubRunStateStore.getState().setChatRunning(chatId, true);

        if (chatId != null) {
            aiHubChatsStore.getState().clearActivityState(chatId);
            aiHubChatsStore.getState().setActivityState(chatId, 'running');
        }
    };

    /**
     * Handles a 409 TURN_IN_FLIGHT once per turn: releases the run, rolls back this turn's optimistic messages,
     * hands {@code composerTextOnRollback} back to the composer when given and tells the user a turn is already
     * running in this chat.
     */
    const createTurnInFlightHandler = (optimisticTurnMarker: string, composerTextOnRollback: string | undefined) => {
        let handled = false;

        const handleTurnInFlight = () => {
            if (handled) {
                return;
            }

            handled = true;

            aiHubRunStateStore.getState().setChatRunning(chatId, false);

            if (chatId != null) {
                aiHubChatsStore.getState().clearActivityState(chatId);
            }

            toast.info(TURN_IN_FLIGHT_MESSAGE);

            if (useAiHubStore.getState().chatId !== chatId) {
                return;
            }

            removeOptimisticTurnMessages(optimisticTurnMarker);

            if (composerTextOnRollback !== undefined) {
                runtime.thread.composer.setText(composerTextOnRollback);
            }
        };

        return {handleTurnInFlight, wasHandled: () => handled};
    };

    const runTurn = async ({
        composerTextOnRollback,
        input,
        onFailure,
        optimisticTurnMarker,
        runId,
    }: {
        composerTextOnRollback?: string;
        input: string;
        onFailure: (errorMessage: string) => void;
        optimisticTurnMarker: string;
        runId: string;
    }): Promise<{turnInFlight: boolean}> => {
        const turnController = startTurnController();
        const {handleTurnInFlight, wasHandled} = createTurnInFlightHandler(
            optimisticTurnMarker,
            composerTextOnRollback
        );

        agent!.setState(
            buildStateToSend({
                mode,
                providerCatalog: providerCatalogData?.aiProviderCatalog,
                workspaceId: currentWorkspaceId,
            })
        );

        addMessage(stampOptimisticTurnMarker({content: '', role: 'assistant'}, optimisticTurnMarker));

        const subscriber = buildAiHubSubscriber({
            addMessage,
            appendToLastAssistantMessage,
            assistantMessageIndex: useAiHubStore.getState().messages.length - 1,
            chatId,
            getLastUserMessage: () => input,
            onTurnInFlight: handleTurnInFlight,
            onWorkflowMutated: handleWorkflowMutated,
            runLifecycle: {onSettle: () => aiHubRunStateStore.getState().setChatRunning(chatId, false)},
            turnAbortSignal: turnController.signal,
        });

        try {
            await agent!.runAgent({abortController: turnController, runId}, subscriber);
        } catch (error) {
            if (isTurnInFlightError(error as {message?: string; payload?: unknown})) {
                handleTurnInFlight();
            } else if (!isAbortError(error, turnController.signal)) {
                const errorMessage = error instanceof Error ? error.message : String(error);

                console.error('AI Hub agent run failed:', error);
                toast.error(`AI Hub failed: ${errorMessage}`);

                aiChatToolCallStore.getState().failAllRunning({error: errorMessage});

                onFailure(errorMessage);
            }
        } finally {
            aiHubRunStateStore.getState().setChatRunning(chatId, false);

            const persistedChatId = aiHubChatsStore.getState().currentChatId;

            if (persistedChatId != null && isWorkspaceIdKnown(currentWorkspaceId)) {
                queryClient.invalidateQueries({
                    queryKey: AiHubChatsKeys.artifacts(persistedChatId, currentWorkspaceId),
                });
            }
        }

        return {turnInFlight: wasHandled()};
    };

    const onNew = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            throw new Error('Only text messages are supported');
        }

        const input = message.content[0].text;

        if (!isWorkspaceIdKnown(currentWorkspaceId)) {
            toast.info(WORKSPACE_NOT_READY_MESSAGE);

            runtime.thread.composer.setText(input);

            return;
        }

        if (!agent) {
            const errorMessage = 'AI Hub is not ready: missing chat id.';

            toast.error(errorMessage);

            aiChatRetryableErrorStore.getState().setError({errorMessage, lastUserMessage: input, toolName: 'aiHub'});

            return;
        }

        aiChatRetryableErrorStore.getState().clearError();

        const runId = getRandomId();
        const optimisticTurnMarker = getRandomId();
        const userMessageId = generateMessageId();

        addMessage(stampOptimisticTurnMarker({content: input, id: userMessageId, role: 'user'}, optimisticTurnMarker));

        const turnStarted = await bootstrapAiHubTurnRunState({
            createChatIfNeeded: async () => {
                if (aiHubChatsStore.getState().currentChatId != null) {
                    return;
                }

                const currentEnvironmentId = environmentStore.getState().currentEnvironmentId ?? 0;

                const newChat = await createAiHubChat({
                    environment: currentEnvironmentId,
                    threadId: chatId!,
                    workspaceId: currentWorkspaceId,
                });

                aiHubChatsStore.getState().setCurrentChatId(newChat.id);
                aiHubChatsStore.getState().consumeDraftLlmSelection(newChat.id);

                queryClient.invalidateQueries({
                    queryKey: AiHubChatsKeys.list(currentWorkspaceId, currentEnvironmentId, 'ACTIVE'),
                });
            },
            markRunning: () => markChatRunning(runId),
            revertOnFailure: (error) => {
                const errorMessage = error instanceof Error ? error.message : String(error);

                aiHubRunStateStore.getState().setChatRunning(chatId, false);

                if (chatId != null) {
                    aiHubChatsStore.getState().clearActivityState(chatId);
                }

                useAiHubStore.getState().resetMessages();

                console.error('Failed to auto-create chat on first message:', error);
                toast.error(`Failed to start chat: ${errorMessage}`);

                aiChatRetryableErrorStore
                    .getState()
                    .setError({errorMessage, lastUserMessage: input, toolName: 'aiHub'});
            },
        });

        if (!turnStarted) {
            return;
        }

        agent.addMessage({content: input, id: userMessageId, role: 'user'});

        aiHubComposerStore.getState().clear();

        const {turnInFlight} = await runTurn({
            composerTextOnRollback: input,
            input,
            onFailure: (errorMessage) =>
                aiChatRetryableErrorStore
                    .getState()
                    .setError({errorMessage, lastUserMessage: input, toolName: 'aiHub'}),
            optimisticTurnMarker,
            runId,
        });

        const persistedChatId = aiHubChatsStore.getState().currentChatId;

        if (!turnInFlight && persistedChatId != null) {
            runPostTurnTelemetry({
                chatId: persistedChatId,
                input,
                invalidateChats: () => queryClient.invalidateQueries({queryKey: AiHubChatsKeys.all}),
                messageCount: useAiHubStore.getState().messages.length,
                workspaceId: currentWorkspaceId,
            });
        }
    };

    /**
     * Re-runs a turn for the user message {@code userMessage}, which is the only message the run sends: the server
     * takes the history from its session memory. A user message the server already holds is not saved again, since
     * the server keys a sent user row by its AG-UI message id.
     */
    const reRunAgentTurn = async (userMessage: {content: string; id: string}) => {
        if (!agent) {
            return;
        }

        const runId = getRandomId();

        agent.setMessages([{content: userMessage.content, id: userMessage.id, role: 'user'}]);

        markChatRunning(runId);

        await runTurn({
            input: userMessage.content,
            onFailure: () => undefined,
            optimisticTurnMarker: getRandomId(),
            runId,
        });
    };

    /**
     * Truncates the saved chat at the user message at {@code messageIndex}: after it when {@code keepMessage}, from it
     * otherwise. A chat not created yet has nothing saved. The server refuses while a turn runs, before deleting
     * anything. When it holds no message with that id, because the run that sent it failed before saving it, nothing
     * was saved after it either, so the last user message may go ahead; an earlier one no longer lines up with the
     * server and is refused. Returns whether the edit or reload may go ahead.
     */
    const truncateSavedChat = async ({
        keepMessage,
        messageIndex,
        messages,
        refusal,
    }: {
        keepMessage: boolean;
        messageIndex: number;
        messages: ThreadMessageLike[];
        refusal: string;
    }): Promise<boolean> => {
        const persistedChatId = aiHubChatsStore.getState().currentChatId;

        if (persistedChatId == null) {
            return true;
        }

        if (!isWorkspaceIdKnown(currentWorkspaceId)) {
            toast.info(WORKSPACE_NOT_READY_MESSAGE);

            return false;
        }

        const result = await truncateChatMessagesMutation.mutateAsync({
            id: String(persistedChatId),
            keepMessage,
            messageId: messages[messageIndex].id!,
            workspaceId: String(currentWorkspaceId),
        });

        if (result.truncateAiHubChatMessages != null) {
            return true;
        }

        const isLastUserMessage = messages.slice(messageIndex + 1).every((message) => message.role !== 'user');

        if (!isLastUserMessage) {
            toast.error(refusal);
        }

        return isLastUserMessage;
    };

    /** Drops the tool calls live turns attached to the thread messages from {@code fromMessageIndex} on. */
    const dropToolCallsFrom = (fromMessageIndex: number) => {
        if (chatId != null) {
            aiChatToolCallStore.getState().removeFromMessageIndex(chatId, fromMessageIndex);
        }
    };

    /**
     * Edit-and-resend: replaces the edited user message, found by the id the runtime passes as {@code sourceId}, and
     * drops everything after it, on the server too, then sends the new text as a new message under a new id.
     */
    const onEdit = async (message: AppendMessage) => {
        if (message.content[0]?.type !== 'text') {
            toast.error('Only text messages are supported');

            return;
        }

        if (!agent) {
            toast.error('AI Hub is not ready yet');

            return;
        }

        const currentMessages = useAiHubStore.getState().messages;
        const editedIndex = currentMessages.findIndex((storeMessage) => storeMessage.id === message.sourceId);

        if (editedIndex < 0 || currentMessages[editedIndex].role !== 'user') {
            toast.error('Could not resolve edited message');

            return;
        }

        const input = message.content[0].text;
        const transcriptGeneration = useAiHubStore.getState().transcriptGeneration;

        try {
            const truncated = await truncateSavedChat({
                keepMessage: false,
                messageIndex: editedIndex,
                messages: currentMessages,
                refusal: 'This message was not saved, so it cannot be edited.',
            });

            if (!truncated) {
                return;
            }

            if (useAiHubStore.getState().transcriptGeneration !== transcriptGeneration) {
                toast.error(EDIT_NOT_SENT_MESSAGE);

                return;
            }

            const editedMessageId = generateMessageId();

            dropToolCallsFrom(editedIndex);

            editUserMessage(editedIndex, input, editedMessageId);

            await reRunAgentTurn({content: input, id: editedMessageId});
        } catch (error) {
            toast.error(`Failed to update message: ${error instanceof Error ? error.message : String(error)}`);
        }
    };

    /**
     * Regenerates the reply to the nearest user message at or before {@code parentId}, walking back over tool-only and
     * data rows: drops everything after that message, on the server too, and re-sends it under its own id, which the
     * server keeps rather than saving again. With no such message there is nothing to regenerate.
     */
    const onReload = async (parentId: string | null) => {
        if (!agent) {
            toast.error('AI Hub is not ready yet');

            return;
        }

        const currentMessages = useAiHubStore.getState().messages;
        const userMessageIndex = findReloadedUserMessageIndex(currentMessages, parentId);

        if (userMessageIndex < 0) {
            toast.error('Could not resolve message to refresh');

            return;
        }

        const userMessage = currentMessages[userMessageIndex];
        const transcriptGeneration = useAiHubStore.getState().transcriptGeneration;

        try {
            const truncated = await truncateSavedChat({
                keepMessage: true,
                messageIndex: userMessageIndex,
                messages: currentMessages,
                refusal: 'This message was not saved, so its reply cannot be refreshed.',
            });

            if (!truncated || useAiHubStore.getState().transcriptGeneration !== transcriptGeneration) {
                return;
            }

            dropToolCallsFrom(userMessageIndex + 1);

            useAiHubStore.setState({messages: currentMessages.slice(0, userMessageIndex + 1)});

            await reRunAgentTurn({content: getMessageText(userMessage), id: userMessage.id!});
        } catch (error) {
            toast.error(`Failed to refresh response: ${error instanceof Error ? error.message : String(error)}`);
        }
    };

    const onCancel = async () => {
        turnControllerRef.current?.abort();
        turnControllerRef.current = null;

        aiChatToolCallStore.getState().failAllRunning({reason: 'Cancelled by user'}, 'aborted');

        if (chatId) {
            aiHubRunStateStore.getState().setChatRunning(chatId, false);
            aiHubChatsStore.getState().clearActivityState(chatId);
        }
    };

    const runtime = useExternalStoreRuntime({
        convertMessage,
        isRunning,
        messages: projectedMessages,
        onCancel,
        onEdit,
        onNew,
        onReload,
    });

    useEffect(() => {
        const previousChatId = chatId;

        return () => cleanupForChatChange(previousChatId);
    }, [chatId]);

    useEffect(() => {
        return () => {
            turnControllerRef.current?.abort();
            turnControllerRef.current = null;

            continuationDisposerRef.current?.();
            continuationDisposerRef.current = null;

            aiChatToolCallStore.getState().failAllRunning({reason: 'Page closed'}, 'aborted');
        };
    }, []);

    return (
        <ApprovalResolutionContext.Provider value={approvalResolution}>
            <AssistantRuntimeProvider runtime={runtime}>{children}</AssistantRuntimeProvider>
        </ApprovalResolutionContext.Provider>
    );
}
