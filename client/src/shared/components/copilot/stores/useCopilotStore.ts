import {generateRandomId} from '@/shared/util/random-utils';
import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    ASK = 'ASK',
    BUILD = 'BUILD',
}

const MAX_CONVERSATION_STACK_DEPTH = 10;

export enum Source {
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
    WORKFLOW_CODE_EDITOR = 'WORKFLOW_CODE_EDITOR',
    MCP_SERVER = 'MCP_SERVER',
    JSON_SCHEMA_BUILDER = 'JSON_SCHEMA_BUILDER',
    SAMPLE_OUTPUT = 'SAMPLE_OUTPUT',
    CODE_WORKFLOW = 'CODE_WORKFLOW',
    CODE_WORKFLOW_EMBEDDED = 'CODE_WORKFLOW_EMBEDDED',
    CUSTOM_COMPONENT = 'CUSTOM_COMPONENT',
    WORKFLOW_EDITOR_EMBEDDED = 'WORKFLOW_EDITOR_EMBEDDED',
    WORKFLOW_EXECUTION_EMBEDDED = 'WORKFLOW_EXECUTION_EMBEDDED',
    KNOWLEDGE_BASE = 'KNOWLEDGE_BASE',
    DATA_TABLE = 'DATA_TABLE',
    AI_AGENT = 'AI_AGENT',
    DEPLOYMENT = 'DEPLOYMENT',
    PROJECT = 'PROJECT',
    PROJECT_DEPLOYMENT = 'PROJECT_DEPLOYMENT',
    API_COLLECTION = 'API_COLLECTION',
    ASSET_FILE = 'ASSET_FILE',
    COMPONENT_RULE = 'COMPONENT_RULE',
}

export const isWorkflowEditorSource = (source: Source) =>
    source === Source.WORKFLOW_EDITOR || source === Source.WORKFLOW_EDITOR_EMBEDDED;

export interface CodeEditorCopilotParametersI {
    language: string;
}

export interface ClusterElementCopilotParametersI {
    taskName?: string;
}

export interface IntentCopilotParametersI {
    intent: string;
}

export interface WorkflowCodeEditorCopilotParametersI {
    format: string;
}

export interface WorkflowEditorCopilotParametersI {
    integrationId?: string;
    workflowId?: string;
}

export interface WorkflowExecutionCopilotParametersI {
    environmentId?: number;
    workflowExecutionId?: number;
    workflowId?: string;
    workspaceId?: number;
}

export interface WorkflowNodeCopilotParametersI {
    propertyPath?: string;
    title?: string;
    workflowId?: string;
    workflowNodeName?: string;
}

export type CopilotContextParametersType =
    | Record<string, never>
    | CodeEditorCopilotParametersI
    | ClusterElementCopilotParametersI
    | IntentCopilotParametersI
    | WorkflowCodeEditorCopilotParametersI
    | WorkflowEditorCopilotParametersI
    | WorkflowExecutionCopilotParametersI
    | WorkflowNodeCopilotParametersI;

export type ContextType = {
    environmentId?: number;
    source: Source;
    parameters: CopilotContextParametersType;
    mode: MODE;
    workflowExecutionError?: {
        errorMessage?: string;
        stackTrace?: string[];
        title?: string;
        workflowId?: string;
    };
};

interface ConversationSnapshotI {
    composerPlaceholder: string | undefined;
    context: ContextType;
    conversationId: string | undefined;
    messages: ThreadMessageLike[];
    selectedLlmModel: string | null;
    selectedLlmProvider: string | null;
    token: string;
}

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    context: ContextType;
    setContext: (context: ContextType | undefined) => void;

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

    conversationStack: ConversationSnapshotI[];
    saveConversationState: () => string;
    restoreConversationState: (token: string | null) => void;

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
            set((state) => ({
                context: {...state.context, mode: MODE.ASK},
                conversationId: generateRandomId(),
                selectedLlmModel: null,
                selectedLlmProvider: null,
            }));
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

                const top = state.conversationStack.at(-1);

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
