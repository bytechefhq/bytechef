import {createAiHubChat, generateAiHubChatTitle, patchChat} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {AiHubChatsKeys} from '@/ee/pages/automation/ai-hub/chats/hooks/useChats';
import {useTruncateAiHubChatMessagesMutation} from '@/ee/pages/automation/ai-hub/chats/hooks/useTruncateChatMessages';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {attachToInFlightRun} from '@/ee/pages/automation/ai-hub/runtime-providers/inFlightRunClient';
import {
    aiHubRunStateStore,
    isChatRunning,
    useAiHubRunStateStore,
} from '@/ee/pages/automation/ai-hub/runtime-providers/stores/useAiHubRunStateStore';
import {sanitizeAssistantMessages} from '@/ee/pages/automation/ai-hub/runtime-providers/stripLeakedToolMarkup';
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
import {readLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {useResolveAiHubToolApprovalMutation} from '@/shared/middleware/graphql';
import {ProjectWorkflowKeys} from '@/shared/queries/automation/projectWorkflows.queries';
import {WorkflowTestConfigurationKeys} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
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

interface BuildSubscriberDepsI {
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    /** Index of the assistant message that owns the tool calls of this turn. */
    assistantMessageIndex?: number;
    /** AG-UI thread id this turn belongs to; events arriving after a switch to another chat are dropped. */
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
 * Parses a tool result as JSON. Objects and arrays that fail to parse are logged; any other text that is not a JSON
 * value (prose) is returned as not parsed without logging.
 */
const parseToolResult = (content: string): {ok: true; value: unknown} | {ok: false} => {
    if (/^[[{]/.test(content.trimStart())) {
        const parsed = parseJson<unknown>(content, 'tool-result payload');

        return parsed === null ? {ok: false} : {ok: true, value: parsed};
    }

    try {
        return {ok: true, value: JSON.parse(content)};
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
 * Builds an AG-UI subscriber that streams text into the assistant message, records every tool call in the per-tool-
 * call store, opens workflow tabs, and turns interactive tool results into data parts. Server text is not split at
 * tool calls; the whole turn's text lands in one assistant message.
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

    const isStaleChat = () => subscriberChatId != null && useAiHubStore.getState().chatId !== subscriberChatId;

    const surfaceToolFailure = (toolName: string, reason: string, rawContent?: unknown) => {
        if (rawContent !== undefined) {
            console.warn(`[AiHub] ${toolName}: ${reason}`, rawContent);
        }

        aiChatRetryableErrorStore.getState().setError({
            errorMessage: reason,
            lastUserMessage: getLastUserMessage(),
            toolName,
        });
    };

    const openWorkflowTab = (toolCallName: string, toolResultContent: string) => {
        const parsed = validateOpenWorkflowTabResult(parseJson<unknown>(toolResultContent, 'openWorkflowTab result'));

        if (parsed === null) {
            surfaceToolFailure(toolCallName, 'unparseable result', toolResultContent);

            return;
        }

        if (!parsed.opened) {
            surfaceToolFailure(toolCallName, parsed.error);

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

            aiChatRetryableErrorStore.getState().setError({
                errorMessage: dataPart.errorMessage,
                lastUserMessage: getLastUserMessage(),
                toolName: dataPart.toolName,
            });

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

            runLifecycle?.onSettle();

            terminateOrphanRunningToolCalls(subscriberChatId);

            if (subscriberChatId != null) {
                aiHubChatsStore.getState().clearActivityState(subscriberChatId);
            }
        },
        onRunFinishedEvent: () => {
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

            appendToLastAssistantMessage(textMessageBuffer + event.delta);
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
                surfaceToolFailure(toolCallName ?? 'unknown', errorMessage);

                return;
            }

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
                .startToolCall(event.toolCallId, event.toolCallName, assistantMessageIndex, subscriberChatId);
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
 * remembered last-used model.
 */
export const buildStateToSend = ({mode, workspaceId}: {mode: MODE; workspaceId: number | undefined}) => {
    const tabsState = aiHubTabsStore.getState();
    const chatsState = aiHubChatsStore.getState();

    const activeTab = tabsState.openTabs.find((tab) => tab.id === tabsState.activeTabId);
    const chatLlmSelection =
        chatsState.currentChatId != null ? chatsState.chatLlmSelections[chatsState.currentChatId] : undefined;
    const llmSelection =
        chatLlmSelection?.provider != null && chatLlmSelection.model != null
            ? chatLlmSelection
            : isWorkspaceIdKnown(workspaceId)
              ? readLastUsedModel(workspaceId)
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

    const queryClient = useQueryClient();
    const resolveToolApprovalMutation = useResolveAiHubToolApprovalMutation();
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
     * hands the text back to the composer and tells the user a turn is already running in this chat.
     */
    const createTurnInFlightHandler = (optimisticTurnMarker: string, input: string) => {
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

            runtime.thread.composer.setText(input);
        };

        return {handleTurnInFlight, wasHandled: () => handled};
    };

    const runTurn = async ({
        input,
        onFailure,
        optimisticTurnMarker,
        runId,
    }: {
        input: string;
        onFailure: (errorMessage: string) => void;
        optimisticTurnMarker: string;
        runId: string;
    }): Promise<{turnInFlight: boolean}> => {
        const turnController = startTurnController();
        const {handleTurnInFlight, wasHandled} = createTurnInFlightHandler(optimisticTurnMarker, input);

        agent!.setState(buildStateToSend({mode, workspaceId: currentWorkspaceId}));

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

            if (persistedChatId != null) {
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

        if (!agent) {
            const errorMessage = 'AI Hub is not ready: missing chat id.';

            toast.error(errorMessage);

            aiChatRetryableErrorStore.getState().setError({errorMessage, lastUserMessage: input, toolName: 'aiHub'});

            return;
        }

        aiChatRetryableErrorStore.getState().clearError();

        const runId = getRandomId();
        const optimisticTurnMarker = getRandomId();

        addMessage(stampOptimisticTurnMarker({content: input, role: 'user'}, optimisticTurnMarker));

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

        agent.addMessage({content: input, id: getRandomId(), role: 'user'});

        const {turnInFlight} = await runTurn({
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
     * Re-runs a turn against the current {@code agent.messages}; onEdit / onReload stage the transcript first.
     */
    const reRunAgentTurn = async (lastUserInput: string) => {
        if (!agent) {
            return;
        }

        const runId = getRandomId();

        markChatRunning(runId);

        await runTurn({input: lastUserInput, onFailure: () => undefined, optimisticTurnMarker: getRandomId(), runId});
    };

    /**
     * Edit-and-resend: truncates the server-side history at the edited index, rewinds the local store and the AG-UI
     * transcript, then re-runs the turn. The external-store runtime assigns each message id = String(index).
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

        const editedIndex = Number(message.sourceId);

        if (!Number.isInteger(editedIndex) || editedIndex < 0) {
            toast.error('Could not resolve edited message');

            return;
        }

        const input = message.content[0].text;
        const persistedChatId = aiHubChatsStore.getState().currentChatId;

        try {
            if (persistedChatId != null && currentWorkspaceId != null) {
                await truncateChatMessagesMutation.mutateAsync({
                    fromMessageIndex: editedIndex,
                    id: String(persistedChatId),
                    workspaceId: String(currentWorkspaceId),
                });
            }

            editUserMessage(editedIndex, input);

            agent.setMessages(agent.messages.slice(0, editedIndex));
            agent.addMessage({content: input, id: getRandomId(), role: 'user'});

            await reRunAgentTurn(input);
        } catch (error) {
            toast.error(`Failed to update message: ${error instanceof Error ? error.message : String(error)}`);
        }
    };

    /**
     * Regenerates the reply to the user message at {@code parentId}: drops that reply and everything after it on the
     * server, locally and in the AG-UI transcript, then re-runs the turn.
     */
    const onReload = async (parentId: string | null) => {
        if (!agent) {
            toast.error('AI Hub is not ready yet');

            return;
        }

        const parentIndex = parentId == null ? -1 : Number(parentId);

        if (!Number.isInteger(parentIndex) || parentIndex < -1) {
            toast.error('Could not resolve message to refresh');

            return;
        }

        const persistedChatId = aiHubChatsStore.getState().currentChatId;
        const currentMessages = useAiHubStore.getState().messages;
        const lastUserContent = currentMessages[parentIndex]?.content;
        const lastUserInput =
            typeof lastUserContent === 'string'
                ? lastUserContent
                : Array.isArray(lastUserContent)
                  ? lastUserContent
                        .filter((part): part is {text: string; type: 'text'} => part?.type === 'text')
                        .map((part) => part.text)
                        .join('\n')
                  : '';

        try {
            if (persistedChatId != null && currentWorkspaceId != null) {
                await truncateChatMessagesMutation.mutateAsync({
                    fromMessageIndex: parentIndex + 1,
                    id: String(persistedChatId),
                    workspaceId: String(currentWorkspaceId),
                });
            }

            useAiHubStore.setState({messages: currentMessages.slice(0, parentIndex + 1)});

            agent.setMessages(agent.messages.slice(0, parentIndex + 1));

            await reRunAgentTurn(lastUserInput);
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
