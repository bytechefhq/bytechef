/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * Tool call lifecycle.
 * - `running` — request in flight
 * - `success` — completed normally
 * - `error` — completed with a real failure (server error, network failure, parse error)
 * - `aborted` — cleanup ritual on chat switch terminated the call; NOT a real error. Renderers should show
 *   a neutral "Switched away" affordance, not an error indicator. Distinguishing this from `error` prevents the
 *   flapping bug where `failAllRunning` would set `error` and a late SSE `onComplete({kind:'aborted'})` callback
 *   would then flip back to `success` — leaving a misleading or oscillating UI.
 */
export type ToolCallStatusType = 'running' | 'success' | 'error' | 'aborted';

export type TerminalToolCallStatusType = 'success' | 'error' | 'aborted';

export interface SubagentProgressEntryI {
    text: string;
    timestamp: number;
}

/**
 * Tool call entry. The {@link ToolCallStatusType} discriminator implies a contract: {@code result} is set only
 * after a terminal status ({@code success}/{@code error}/{@code aborted}). The flat shape leaves {@code result}
 * representable while {@code status} is still {@code 'running'}, which is illegal-but-representable; the
 * {@link isRunningToolCall} / {@link isTerminalToolCall} type predicates below let callers narrow at runtime so
 * they don't accidentally read a stale result on an in-flight entry. Project ESLint rules (interfaces must end
 * in {@code I}/{@code Props}; type aliases must end in {@code Type}) prevent expressing this as a discriminated
 * type-alias union without renaming every consumer; the predicates plus the contract docstring are the
 * pragmatic compromise.
 */
export interface ToolCallEntryI {
    args?: Record<string, unknown>;
    /** AG-UI thread/chat id this entry belongs to. Used to scope reset on chat switch. */
    chatId?: string;
    /** Index into the messages array of the assistant message that owns this tool call. */
    messageIndex: number;
    progress: SubagentProgressEntryI[];
    progressiveOutput: string;
    /**
     * Set by {@code completeToolCall} / {@code failAllRunning} when {@code status} transitions to a terminal
     * value. Readers MUST gate on {@link isTerminalToolCall} (or check {@code status !== 'running'}) before
     * dereferencing — a {@code running} entry's {@code result} is meaningless.
     */
    result?: unknown;
    status: ToolCallStatusType;
    toolCallId: string;
    toolName: string;
}

/** Discriminated alias of {@link ToolCallEntryI} where {@code status} is narrowed to {@code 'running'}. */
export type RunningToolCallEntryType = ToolCallEntryI & {status: 'running'};

/** Discriminated alias where {@code status} is narrowed to a terminal value; {@code result} may be present. */
export type TerminalToolCallEntryType = ToolCallEntryI & {status: TerminalToolCallStatusType};

/** Type predicate narrowing to {@link RunningToolCallEntryType} so callers can branch without re-asserting status. */
export const isRunningToolCall = (entry: ToolCallEntryI): entry is RunningToolCallEntryType =>
    entry.status === 'running';

/** Type predicate narrowing to {@link TerminalToolCallEntryType}. */
export const isTerminalToolCall = (entry: ToolCallEntryI): entry is TerminalToolCallEntryType =>
    entry.status !== 'running';

interface ToolCallStateI {
    addProgress: (toolCallId: string, text: string) => void;
    appendProgressiveOutput: (toolCallId: string, chunk: string) => void;
    completeToolCall: (toolCallId: string, result: unknown, isError: boolean) => void;
    /**
     * Force any tool calls still in `running` state to a terminal status. The default `aborted` is correct for the
     * chat-switch cleanup ritual — it prevents the flapping race where `error` would be later overwritten by
     * a successful `completeToolCall` from a late SSE callback. Pass `error` for genuine failures (e.g. agent throw).
     */
    failAllRunning: (errorResult: unknown, status?: 'aborted' | 'error') => void;
    /**
     * Chat-scoped sibling of {@link failAllRunning}: terminates only entries whose {@code chatId}
     * matches. Used by {@code onRunFinishedEvent} / {@code onRunErrorEvent} in the runtime provider so a tool call
     * that started but never received its matching {@code ToolCallResultEvent} stops spinning at end-of-run, without
     * clobbering parallel chats' legitimate in-flight cards.
     */
    failRunningInChat: (chatId: string, errorResult: unknown, status?: 'aborted' | 'error') => void;
    /**
     * Returns the most-recently-started running tool call matching {@code toolName}. Pass {@code chatId} so a
     * late breadcrumb arriving for a switched-away chat cannot land on the wrong card. The cleanup ritual
     * already flips entries from prior chats to 'error' on switch — this filter is defense in depth against
     * any race window where two chats briefly share a running call with the same toolName.
     */
    findRunningToolCallByName: (toolName: string, chatId?: string) => RunningToolCallEntryType | undefined;
    /** Insertion order of tool-call ids — used to project them into the assistant message in arrival order. */
    order: string[];
    reset: () => void;
    /**
     * Drop only the entries belonging to {@code chatId}. Used by the runtime provider on chat
     * switch so we don't nuke entries that already arrived for the NEW chat in a re-mount race.
     */
    resetForChat: (chatId: string | undefined) => void;
    startToolCall: (toolCallId: string, toolName: string, messageIndex: number, chatId?: string) => void;
    toolCalls: Record<string, ToolCallEntryI>;
    updateToolCallArgs: (toolCallId: string, args: Record<string, unknown>) => void;
}

export const aiChatToolCallStore = create<ToolCallStateI>()(
    devtools((set, get) => ({
        order: [] as string[],
        toolCalls: {} as Record<string, ToolCallEntryI>,

        startToolCall: (toolCallId, toolName, messageIndex, chatId) =>
            set((state) => {
                if (state.toolCalls[toolCallId]) {
                    return state;
                }

                const newEntry: RunningToolCallEntryType = {
                    chatId,
                    messageIndex,
                    progress: [],
                    progressiveOutput: '',
                    status: 'running',
                    toolCallId,
                    toolName,
                };

                return {
                    ...state,
                    order: [...state.order, toolCallId],
                    toolCalls: {
                        ...state.toolCalls,
                        [toolCallId]: newEntry,
                    },
                };
            }),

        updateToolCallArgs: (toolCallId, args) =>
            set((state) => {
                const existing = state.toolCalls[toolCallId];

                if (!existing) {
                    return state;
                }

                return {
                    ...state,
                    toolCalls: {
                        ...state.toolCalls,
                        [toolCallId]: {...existing, args},
                    },
                };
            }),

        completeToolCall: (toolCallId, result, isError) =>
            set((state) => {
                const existing = state.toolCalls[toolCallId];

                if (!existing) {
                    return state;
                }

                // If the cleanup ritual already terminated this call as `aborted`, do not let a late SSE callback
                // flap the status back to success/error. `aborted` is terminal once set — the user switched away
                // and must not see misleading post-hoc state on a card they can no longer act on.
                if (existing.status === 'aborted') {
                    return state;
                }

                // Rebuild into TerminalToolCallEntryType explicitly. Spreading existing then overriding `result`
                // and `status` is the natural pattern, but it lets stale fields from the prior shape leak; the
                // explicit shape lock confirms exactly which fields land on the terminal entry.
                const completed: TerminalToolCallEntryType = {
                    args: existing.args,
                    chatId: existing.chatId,
                    messageIndex: existing.messageIndex,
                    progress: existing.progress,
                    progressiveOutput: existing.progressiveOutput,
                    result,
                    status: isError ? 'error' : 'success',
                    toolCallId: existing.toolCallId,
                    toolName: existing.toolName,
                };

                return {
                    ...state,
                    toolCalls: {
                        ...state.toolCalls,
                        [toolCallId]: completed,
                    },
                };
            }),

        appendProgressiveOutput: (toolCallId, chunk) =>
            set((state) => {
                const existing = state.toolCalls[toolCallId];

                // Guard: never mutate an entry that has already been marked success/error.
                // Late chunks arriving after onComplete would otherwise flip the status indicator.
                if (!existing || existing.status !== 'running') {
                    return state;
                }

                return {
                    ...state,
                    toolCalls: {
                        ...state.toolCalls,
                        [toolCallId]: {
                            ...existing,
                            progressiveOutput: existing.progressiveOutput + chunk,
                        },
                    },
                };
            }),

        addProgress: (toolCallId, text) =>
            set((state) => {
                const existing = state.toolCalls[toolCallId];

                if (!existing || existing.status !== 'running') {
                    return state;
                }

                return {
                    ...state,
                    toolCalls: {
                        ...state.toolCalls,
                        [toolCallId]: {
                            ...existing,
                            progress: [...existing.progress, {text, timestamp: Date.now()}],
                        },
                    },
                };
            }),

        failAllRunning: (errorResult, status = 'error') =>
            set((state) => {
                const updated: Record<string, ToolCallEntryI> = {...state.toolCalls};
                let mutated = false;

                for (const id of state.order) {
                    const entry = updated[id];

                    if (entry && entry.status === 'running') {
                        const terminated: TerminalToolCallEntryType = {
                            args: entry.args,
                            chatId: entry.chatId,
                            messageIndex: entry.messageIndex,
                            progress: entry.progress,
                            progressiveOutput: entry.progressiveOutput,
                            result: errorResult,
                            status,
                            toolCallId: entry.toolCallId,
                            toolName: entry.toolName,
                        };

                        updated[id] = terminated;
                        mutated = true;
                    }
                }

                if (!mutated) {
                    return state;
                }

                return {...state, toolCalls: updated};
            }),

        failRunningInChat: (chatId, errorResult, status = 'aborted') =>
            set((state) => {
                const updated: Record<string, ToolCallEntryI> = {...state.toolCalls};
                let mutated = false;

                for (const id of state.order) {
                    const entry = updated[id];

                    if (entry && entry.status === 'running' && entry.chatId === chatId) {
                        const terminated: TerminalToolCallEntryType = {
                            args: entry.args,
                            chatId: entry.chatId,
                            messageIndex: entry.messageIndex,
                            progress: entry.progress,
                            progressiveOutput: entry.progressiveOutput,
                            result: errorResult,
                            status,
                            toolCallId: entry.toolCallId,
                            toolName: entry.toolName,
                        };

                        updated[id] = terminated;
                        mutated = true;
                    }
                }

                if (!mutated) {
                    return state;
                }

                return {...state, toolCalls: updated};
            }),

        findRunningToolCallByName: (toolName, chatId) => {
            const {order, toolCalls} = get();

            // Walk in reverse insertion order so the most recently started call wins. When chatId is
            // supplied, skip entries belonging to other chats so a late event after a chat switch
            // does not land on a card from the previous chat.
            for (let i = order.length - 1; i >= 0; i--) {
                const entry = toolCalls[order[i]];

                if (!entry || !isRunningToolCall(entry) || entry.toolName !== toolName) {
                    continue;
                }

                if (chatId !== undefined && entry.chatId !== chatId) {
                    continue;
                }

                return entry;
            }

            return undefined;
        },

        reset: () => set({order: [], toolCalls: {}}),

        resetForChat: (chatId) =>
            set((state) => {
                if (!chatId) {
                    // Untracked entries (callers without a chatId) — fall through to a hard reset so we
                    // don't leave them lingering forever.
                    return {order: [], toolCalls: {}};
                }

                const survivingOrder: string[] = [];
                const survivingToolCalls: Record<string, ToolCallEntryI> = {};

                for (const id of state.order) {
                    const entry = state.toolCalls[id];

                    if (entry && entry.chatId !== chatId) {
                        survivingOrder.push(id);
                        survivingToolCalls[id] = entry;
                    }
                }

                if (survivingOrder.length === state.order.length) {
                    return state;
                }

                return {...state, order: survivingOrder, toolCalls: survivingToolCalls};
            }),
    }))
);

export const useAiChatToolCallStore = aiChatToolCallStore;
