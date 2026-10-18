import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type AiHubChatStatusType = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

/**
 * In-flight signal for a chat, set by AiHubRuntimeProvider on AG-UI lifecycle events. Drives the per-row pulse
 * (`running`) and the "needs your answer or approval" icon (`paused`) in the sidebar.
 */
export type ChatActivityStateType = 'running' | 'paused';

interface LlmSelectionI {
    model: string | null;
    provider: string | null;
}

interface AiHubChatsStateI {
    activeFilter: AiHubChatStatusType;
    /** Keyed by AG-UI thread id, the identifier the runtime provider's subscriber holds. */
    chatActivity: Record<string, ChatActivityStateType>;
    /** Per-chat LLM picker selection keyed by numeric chat id. Both null means no override. */
    chatLlmSelections: Record<number, LlmSelectionI>;
    clearActivityState: (threadId: string) => void;
    clearTitleGenerationFailure: (chatId: number) => void;
    /**
     * Moves the home-view picker selection into the freshly created chat's slot, so the override applies to the
     * first turn and shows in that chat's own picker. No-op without a draft.
     */
    consumeDraftLlmSelection: (chatId: number) => void;
    currentChatId: number | undefined;
    /** Picker selection chosen on the home view before a chat row exists. */
    draftLlmSelection: LlmSelectionI | null;
    markTitleGenerationFailed: (chatId: number, errorMessage: string) => void;
    reset: () => void;
    searchTerm: string;
    setActiveFilter: (filter: AiHubChatStatusType) => void;
    /** Writes paused over running but never running over paused, so the actionable indicator survives. */
    setActivityState: (threadId: string, state: ChatActivityStateType) => void;
    setChatLlmSelection: (chatId: number, provider: string | null, model: string | null) => void;
    setCurrentChatId: (id: number | undefined) => void;
    setDraftLlmSelection: (provider: string | null, model: string | null) => void;
    setSearchTerm: (term: string) => void;
    /** Chat id to error message; drives the sidebar's retry affordance for a failed title generation. */
    titleGenerationFailures: Record<number, string>;
}

export const aiHubChatsStore = create<AiHubChatsStateI>()(
    devtools((set) => ({
        activeFilter: 'ACTIVE',
        chatActivity: {},
        chatLlmSelections: {},
        clearActivityState: (threadId) =>
            set((state) => {
                if (!(threadId in state.chatActivity)) {
                    return state;
                }

                const nextChatActivity = {...state.chatActivity};

                delete nextChatActivity[threadId];

                return {chatActivity: nextChatActivity};
            }),
        clearTitleGenerationFailure: (chatId) =>
            set((state) => {
                if (!(chatId in state.titleGenerationFailures)) {
                    return state;
                }

                const nextTitleGenerationFailures = {...state.titleGenerationFailures};

                delete nextTitleGenerationFailures[chatId];

                return {titleGenerationFailures: nextTitleGenerationFailures};
            }),
        consumeDraftLlmSelection: (chatId) =>
            set((state) => {
                if (state.draftLlmSelection == null) {
                    return state;
                }

                return {
                    chatLlmSelections: {...state.chatLlmSelections, [chatId]: state.draftLlmSelection},
                    draftLlmSelection: null,
                };
            }),
        currentChatId: undefined,
        draftLlmSelection: null,
        markTitleGenerationFailed: (chatId, errorMessage) =>
            set((state) => ({
                titleGenerationFailures: {...state.titleGenerationFailures, [chatId]: errorMessage},
            })),
        reset: () =>
            set({
                activeFilter: 'ACTIVE',
                chatActivity: {},
                chatLlmSelections: {},
                currentChatId: undefined,
                draftLlmSelection: null,
                searchTerm: '',
                titleGenerationFailures: {},
            }),
        searchTerm: '',
        setActiveFilter: (filter) => set({activeFilter: filter}),
        setActivityState: (threadId, activityState) =>
            set((state) => {
                if (state.chatActivity[threadId] === 'paused' && activityState === 'running') {
                    return state;
                }

                return {chatActivity: {...state.chatActivity, [threadId]: activityState}};
            }),
        setChatLlmSelection: (chatId, provider, model) =>
            set((state) => ({
                chatLlmSelections: {...state.chatLlmSelections, [chatId]: {model, provider}},
            })),
        setCurrentChatId: (id) => set({currentChatId: id}),
        setDraftLlmSelection: (provider, model) =>
            set({draftLlmSelection: provider == null && model == null ? null : {model, provider}}),
        setSearchTerm: (term) => set({searchTerm: term}),
        titleGenerationFailures: {},
    }))
);

export const useAiHubChatsStore = aiHubChatsStore;
