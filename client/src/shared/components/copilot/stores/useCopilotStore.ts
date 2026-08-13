import {generateRandomId} from '@/shared/util/random-utils';
import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    ASK = 'ASK',
    BUILD = 'BUILD',
}

// An unpaired save (a surface unmounted without its cleanup running) leaks one entry. A cap prevents
// unbounded growth in a long session; dropping the deepest entry discards the least likely to be
// returned to.
const MAX_CONVERSATION_STACK_DEPTH = 10;

export enum Source {
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
    WORKFLOW_CODE_EDITOR = 'WORKFLOW_CODE_EDITOR',
    JSON_SCHEMA_BUILDER = 'JSON_SCHEMA_BUILDER',
    SAMPLE_OUTPUT = 'SAMPLE_OUTPUT',
    // Coarse surfaces dispatched by CopilotApiController: workflow editor / code workflow, each with an
    // embedded (integration) counterpart. These values happen to already be lowercase, matching the
    // server-side dispatch key and the `/api/platform/internal/ai/chat/{source}` URL segment directly —
    // but that's incidental, not required: CopilotRuntimeProvider lowercases every Source value
    // (including the UPPER_CASE ones below, e.g. DATA_TABLE, AI_AGENT) before building the request URL.
    // New domain sources should follow the UPPER_CASE = 'UPPER_CASE' convention used elsewhere in this
    // enum, not this block's lowercase style.
    CODE_WORKFLOW = 'code_workflow',
    CODE_WORKFLOW_EMBEDDED = 'code_workflow_embedded',
    CUSTOM_COMPONENT = 'custom_component',
    WORKFLOW_EDITOR_EMBEDDED = 'workflow_editor_embedded',
    WORKFLOW_EXECUTION_EMBEDDED = 'workflow_execution_embedded',
    CONTEXT_STORE = 'CONTEXT_STORE',
    KNOWLEDGE_BASE = 'KNOWLEDGE_BASE',
    DATA_TABLE = 'DATA_TABLE',
    AI_AGENT = 'AI_AGENT',
    DEPLOYMENT = 'DEPLOYMENT',
    PROJECT = 'PROJECT',
    PROJECT_DEPLOYMENT = 'PROJECT_DEPLOYMENT',
    MCP_SERVER = 'MCP_SERVER',
    API_COLLECTION = 'API_COLLECTION',
    ASSET_FILE = 'ASSET_FILE',
}

export type ContextType = {
    environmentId?: number;
    source: Source;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: Record<string, any>;
    mode: MODE;
    workflowExecutionError?: {
        errorMessage?: string;
        stackTrace?: string[];
        title?: string;
        workflowId?: string;
    };
};

export interface ConversationSnapshotI {
    composerPlaceholder: string | undefined;
    context: ContextType;
    conversationId: string | undefined;
    messages: ThreadMessageLike[];
    selectedLlmModel: string | null;
    selectedLlmProvider: string | null;
    // Identifies the surface that pushed this entry, so its restore can pop only its own entry. Minted
    // by saveConversationState and handed back to the caller; see the conversationStack comment below.
    token: string;
}

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    context: ContextType;
    setContext: (context: ContextType | undefined) => void;

    // Cosmetic composer guidance, kept out of ContextType so it is never spread into the AG-UI state sent
    // to the backend (CopilotRuntimeProvider sends `context` verbatim as agent state).
    composerPlaceholder: string | undefined;
    setComposerPlaceholder: (composerPlaceholder: string | undefined) => void;

    setWorkflowExecutionError: (
        workflowExecutionError:
            | {
                  errorMessage?: string;
                  stackTrace?: string[];
                  title?: string;
                  workflowId?: string;
              }
            | undefined
    ) => void;

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    editUserMessage: (index: number, content: string) => void;
    resetMessages: () => void;
    selectedLlmProvider: string | null;
    selectedLlmModel: string | null;
    setSelectedLlm: (provider: string | null, model: string | null) => void;

    // A LIFO stack rather than one slot: a surface that opens Copilot over another surface's conversation
    // pushes, and its close pops, so the underlying conversation survives. What makes the many restore call
    // sites safe is that saveConversationState hands back a token identifying the entry it pushed, and
    // restoreConversationState only pops when that token strictly equals the top of the stack's token. A
    // surface that never pushed holds `null`, and `null` can never equal a real token, so its restore is
    // always a no-op regardless of stack depth — there is no separate "no token supplied" escape hatch. See
    // docs/superpowers/specs/2026-08-12-copilot-conversation-stack-research.md for the incident this
    // replaced: the first cut of this phase kept the empty-stack-only guard and shipped a regression where
    // an unrelated surface's restore could pop and steal another surface's conversation, and the token cut
    // right after it still let a `null`/absent token take an unconditional pop, which reopened the same hole.
    conversationStack: ConversationSnapshotI[];
    saveConversationState: () => string;
    restoreConversationState: (token: string | null) => void;

    // Token for the entry (if any) the global panel's own last open pushed. Needed because the global
    // panel's push (useOpenCopilot.ts) and pop (closeGlobalPanel in CopilotPanelImpl.tsx) live in different
    // files that don't share a component instance, so a local ref can't carry the token between them the
    // way it does for the local-panel surfaces.
    globalPanelConversationToken: string | null;
    setGlobalPanelConversationToken: (token: string | null) => void;
}

export const useCopilotStore = create<CopilotStateI>()(
    devtools((set) => ({
        conversationId: generateRandomId(),
        generateConversationId: () => {
            // New conversation = reset the picker too. The picker is "per conversation" by design, so a
            // user who picked GPT-4o in one conversation lands on the workspace default in the next one
            // rather than carrying the selection across unrelated threads.
            set({conversationId: generateRandomId(), selectedLlmModel: null, selectedLlmProvider: null});
        },

        context: {
            source: Source.WORKFLOW_EDITOR,
            parameters: {},
            mode: MODE.ASK,
            workflowExecutionError: undefined,
        },
        setContext: (context) =>
            set((state) => {
                return {
                    ...state,
                    context,
                };
            }),

        composerPlaceholder: undefined,
        setComposerPlaceholder: (composerPlaceholder) => set({composerPlaceholder}),

        setWorkflowExecutionError: (error) =>
            set((state) => {
                return {
                    ...state,
                    context: {
                        ...state.context,
                        workflowExecutionError: error,
                    },
                };
            }),

        messages: [],
        addMessage: (message) =>
            set((state) => {
                return {
                    ...state,
                    messages: [...state.messages, message],
                };
            }),
        appendToLastAssistantMessage: (text: string) =>
            set((state) => {
                const messages = [...state.messages];

                // Scan back from the end for this turn's streaming assistant message, stopping at the latest user
                // message so we never reach into a previous turn and overwrite its reply (issue #5348).
                for (let i = messages.length - 1; i >= 0; i--) {
                    const message = messages[i] as ThreadMessageLike;

                    if (message.role === 'user') {
                        break;
                    }

                    if (message.role === 'assistant' && typeof message.content === 'string') {
                        messages[i] = {...message, content: text} as ThreadMessageLike;

                        return {...state, messages};
                    }
                }

                // No assistant message for the current turn yet; create one.
                messages.push({role: 'assistant', content: text} as ThreadMessageLike);

                return {...state, messages};
            }),
        editUserMessage: (index, content) =>
            set((state) => {
                if (index < 0 || index >= state.messages.length) {
                    return state;
                }

                const target = state.messages[index];

                if (target?.role !== 'user') {
                    return state;
                }

                const truncated = state.messages.slice(0, index);

                truncated.push({...target, content} as ThreadMessageLike);

                return {...state, messages: truncated};
            }),
        resetMessages: () => set({messages: []}),

        // Per-conversation LLM picker selection. Stored as nullable strings; the runtime provider injects
        // both into the AG-UI state on every chat request when set. Null means "no override — server uses
        // the workspace @Primary ChatModel."
        selectedLlmProvider: null,
        selectedLlmModel: null,
        setSelectedLlm: (provider, model) => set({selectedLlmModel: model, selectedLlmProvider: provider}),

        conversationStack: [],
        saveConversationState: () => {
            const token = generateRandomId();

            set((state) => {
                const snapshot: ConversationSnapshotI = {
                    composerPlaceholder: state.composerPlaceholder,
                    context: state.context,
                    conversationId: state.conversationId,
                    messages: state.messages,
                    selectedLlmModel: state.selectedLlmModel,
                    selectedLlmProvider: state.selectedLlmProvider,
                    token,
                };

                const nextStack = [...state.conversationStack, snapshot];

                // A surface that unmounts without its restore running leaks one entry. Dropping the deepest
                // keeps a long session bounded, and warning makes the leak visible rather than silent.
                if (nextStack.length > MAX_CONVERSATION_STACK_DEPTH) {
                    console.warn(
                        `Copilot conversation stack exceeded ${MAX_CONVERSATION_STACK_DEPTH} entries; dropping the oldest.`
                    );

                    nextStack.shift();
                }

                return {...state, conversationStack: nextStack};
            });

            return token;
        },
        restoreConversationState: (token) =>
            set((state) => {
                if (state.conversationStack.length === 0) {
                    return state;
                }

                const top = state.conversationStack[state.conversationStack.length - 1];

                // A token identifies the entry its own save pushed. The pop is strictly conditional on that
                // token matching the top of the stack — a `null` token (the surface never pushed) can never
                // match a real token string, so it always no-ops here rather than taking an unconditional pop.
                if (top?.token !== token) {
                    return state;
                }

                const nextStack = [...state.conversationStack];
                const snapshot = nextStack.pop() as ConversationSnapshotI;

                return {
                    ...state,
                    composerPlaceholder: snapshot.composerPlaceholder,
                    context: snapshot.context,
                    conversationId: snapshot.conversationId,
                    conversationStack: nextStack,
                    messages: snapshot.messages,
                    selectedLlmModel: snapshot.selectedLlmModel,
                    selectedLlmProvider: snapshot.selectedLlmProvider,
                };
            }),

        globalPanelConversationToken: null,
        setGlobalPanelConversationToken: (token) => set({globalPanelConversationToken: token}),
    }))
);
