/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * Tool call lifecycle.
 * - `running` — request in flight
 * - `success` — completed normally
 * - `error` — completed with a real failure (server error, network failure, parse error)
 * - `aborted` — call terminated by the task-switch cleanup ritual; NOT a real error. Kept distinct from
 *   `error` so renderers show a neutral "Switched away" affordance and a late aborted callback can't flip a
 *   failed call back to success.
 */
export type ToolCallStatusType = 'running' | 'success' | 'error' | 'aborted';

export type TerminalToolCallStatusType = 'success' | 'error' | 'aborted';

export interface SubagentProgressEntryI {
    text: string;
    timestamp: number;
}

export interface ToolCallEntryI {
    args?: Record<string, unknown>;
    taskId?: string;
    messageIndex: number;
    progress: SubagentProgressEntryI[];
    progressiveOutput: string;
    result?: unknown;
    status: ToolCallStatusType;
    toolCallId: string;
    toolName: string;
}

export type RunningToolCallEntryType = ToolCallEntryI & {status: 'running'};

export type TerminalToolCallEntryType = ToolCallEntryI & {status: TerminalToolCallStatusType};

export const isRunningToolCall = (entry: ToolCallEntryI): entry is RunningToolCallEntryType =>
    entry.status === 'running';

export const isTerminalToolCall = (entry: ToolCallEntryI): entry is TerminalToolCallEntryType =>
    entry.status !== 'running';

interface ToolCallStateI {
    addProgress: (toolCallId: string, text: string) => void;
    appendProgressiveOutput: (toolCallId: string, chunk: string) => void;
    completeToolCall: (toolCallId: string, result: unknown, isError: boolean) => void;
    failAllRunning: (errorResult: unknown, status?: 'aborted' | 'error') => void;
    failRunningInTask: (taskId: string, errorResult: unknown, status?: 'aborted' | 'error') => void;
    findRunningToolCallByName: (toolName: string, taskId?: string) => RunningToolCallEntryType | undefined;
    order: string[];
    reset: () => void;
    resetForTask: (taskId: string | undefined) => void;
    startToolCall: (toolCallId: string, toolName: string, messageIndex: number, taskId?: string) => void;
    toolCalls: Record<string, ToolCallEntryI>;
    updateToolCallArgs: (toolCallId: string, args: Record<string, unknown>) => void;
}

export const aiChatToolCallStore = create<ToolCallStateI>()(
    devtools((set, get) => ({
        order: [] as string[],
        toolCalls: {} as Record<string, ToolCallEntryI>,

        startToolCall: (toolCallId, toolName, messageIndex, taskId) =>
            set((state) => {
                if (state.toolCalls[toolCallId]) {
                    return state;
                }

                const newEntry: RunningToolCallEntryType = {
                    taskId,
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

                if (existing.status === 'aborted') {
                    return state;
                }

                const completed: TerminalToolCallEntryType = {
                    args: existing.args,
                    taskId: existing.taskId,
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
                            taskId: entry.taskId,
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

        failRunningInTask: (taskId, errorResult, status = 'aborted') =>
            set((state) => {
                const updated: Record<string, ToolCallEntryI> = {...state.toolCalls};
                let mutated = false;

                for (const id of state.order) {
                    const entry = updated[id];

                    if (entry && entry.status === 'running' && entry.taskId === taskId) {
                        const terminated: TerminalToolCallEntryType = {
                            args: entry.args,
                            taskId: entry.taskId,
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

        findRunningToolCallByName: (toolName, taskId) => {
            const {order, toolCalls} = get();

            // Walk in reverse insertion order so the most recently started call wins. When taskId is
            // supplied, skip entries belonging to other tasks so a late event after a task switch
            // does not land on a card from the previous task.
            for (let i = order.length - 1; i >= 0; i--) {
                const entry = toolCalls[order[i]];

                if (!entry || !isRunningToolCall(entry) || entry.toolName !== toolName) {
                    continue;
                }

                if (taskId !== undefined && entry.taskId !== taskId) {
                    continue;
                }

                return entry;
            }

            return undefined;
        },

        reset: () => set({order: [], toolCalls: {}}),

        resetForTask: (taskId) =>
            set((state) => {
                if (!taskId) {
                    return {order: [], toolCalls: {}};
                }

                const survivingOrder: string[] = [];
                const survivingToolCalls: Record<string, ToolCallEntryI> = {};

                for (const id of state.order) {
                    const entry = state.toolCalls[id];

                    if (entry && entry.taskId !== taskId) {
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
