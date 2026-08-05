import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type AiHubTaskStatusType = 'ACTIVE' | 'ARCHIVED' | 'DELETED';

/**
 * In-flight signal for a task, set by AiHubRuntimeProvider on AG-UI lifecycle events. Drives
 * the per-row pulse / pause icon in the sidebar — without it, a workflow chat in mid-execution looked
 * indistinguishable from a finished chat, and a chat paused at `ask_user_question` looked the same as a
 * task the user had moved on from. Two signals rather than an enum so a turn that's both running and
 * paused-from-the-previous-question (rare, but possible during the streaming-resume hand-off) shows the more
 * actionable "needs your answer" indicator.
 */
export type TaskActivityStateType = 'running' | 'paused';

interface AiHubTasksStateI {
    activeFilter: AiHubTaskStatusType;
    // The activity-state store is keyed by AG-UI thread id (string), not the numeric task row id,
    // because the runtime provider's per-turn subscriber holds the threadId — not the row id (which it would
    // have to look up). titleGenerationFailures stays keyed by numeric id because it's set/read inside the
    // sidebar component which has the task row in scope. Two different keys for two different
    // call-site contexts; bridging them here would require threading the row id into the runtime provider
    // which adds coupling for no functional gain.
    clearActivityState: (threadId: string) => void;
    clearTitleGenerationFailure: (taskId: number) => void;
    // Picker selection chosen on the home view BEFORE a task row exists. AiHubRuntimeProvider.onNew
    // migrates this into taskLlmSelections[newTaskId] right after auto-creating the task, so the
    // override survives the home -> task transition with no extra branch in withCurrentTaskLlmSelection.
    // Cleared by consumeDraftLlmSelection or by reset.
    consumeDraftLlmSelection: (taskId: number) => void;
    currentTaskId: number | undefined;
    draftLlmSelection: {model: string | null; provider: string | null} | null;
    taskActivity: Record<string, TaskActivityStateType>;
    // Per-task LLM picker selection (user-chosen provider + model from the chat-toolbar picker). Keyed by
    // numeric taskId so the selection survives panel close/open and task switches without leaking across
    // tasks. Both null = no override (server uses workspace default; personal-agent kinds still get their
    // configured override via the routing-agent's existing personal-agent-LLM state keys). Cleared
    // implicitly when the store is reset; not cleared on task delete since deleted tasks can't be reopened
    // anyway — the entry leaks a few bytes per defunct task id, which is negligible.
    taskLlmSelections: Record<number, {model: string | null; provider: string | null}>;
    markTitleGenerationFailed: (taskId: number, errorMessage: string) => void;
    reset: () => void;
    searchTerm: string;

    setActiveFilter: (filter: AiHubTaskStatusType) => void;
    setActivityState: (threadId: string, state: TaskActivityStateType) => void;
    setCurrentTaskId: (id: number | undefined) => void;
    setDraftLlmSelection: (provider: string | null, model: string | null) => void;
    setSearchTerm: (term: string) => void;
    setTaskLlmSelection: (taskId: number, provider: string | null, model: string | null) => void;
    // Map of taskId -> error message. When non-empty for a given id, the sidebar shows a retry
    // affordance instead of leaving the task labelled "Untitled" with no recovery path. The toast
    // shown at failure time is dismissable; this state is the durable signal that lets a user re-invoke
    // title generation later from the sidebar row.
    titleGenerationFailures: Record<number, string>;
}

export const aiHubTasksStore = create<AiHubTasksStateI>()(
    devtools((set) => ({
        activeFilter: 'ACTIVE',
        clearActivityState: (threadId) =>
            set((state) => {
                if (!(threadId in state.taskActivity)) {
                    return state;
                }

                const next = {...state.taskActivity};

                delete next[threadId];

                return {taskActivity: next};
            }),
        clearTitleGenerationFailure: (taskId) =>
            set((state) => {
                if (!(taskId in state.titleGenerationFailures)) {
                    return state;
                }

                const next = {...state.titleGenerationFailures};

                delete next[taskId];

                return {titleGenerationFailures: next};
            }),
        // Move the draft selection into the per-task slot for the freshly-created task, then clear the
        // draft. No-op when no draft is set (user sent a message without picking anything), so it's safe
        // to call unconditionally from onNew's auto-create path. The migration vs. a fallback-read in
        // withCurrentTaskLlmSelection matters because the task panel's own ModelPicker reads from
        // taskLlmSelections[currentTaskId] — without the migration, the picker would render empty after
        // the home -> task transition even though the override is still active on the wire.
        consumeDraftLlmSelection: (taskId) =>
            set((state) => {
                if (state.draftLlmSelection == null) {
                    return state;
                }

                return {
                    draftLlmSelection: null,
                    taskLlmSelections: {
                        ...state.taskLlmSelections,
                        [taskId]: state.draftLlmSelection,
                    },
                };
            }),
        currentTaskId: undefined,
        draftLlmSelection: null,
        markTitleGenerationFailed: (taskId, errorMessage) =>
            set((state) => ({
                titleGenerationFailures: {...state.titleGenerationFailures, [taskId]: errorMessage},
            })),
        reset: () =>
            set({
                activeFilter: 'ACTIVE',
                currentTaskId: undefined,
                draftLlmSelection: null,
                searchTerm: '',
                taskActivity: {},
                taskLlmSelections: {},
                titleGenerationFailures: {},
            }),
        searchTerm: '',

        setActiveFilter: (filter) => set({activeFilter: filter}),
        // setActivityState writes paused over running but NOT running over paused — a paused chat that
        // restarts via the user's next message goes through clearActivityState first (called from the
        // RUN_STARTED handler), so the running state is set against an empty slot. Without this guard,
        // a duplicate RUN_STARTED firing during a paused turn would clobber the user-actionable "needs
        // answer" indicator with a less-actionable "running" pulse.
        setActivityState: (threadId, activityState) =>
            set((state) => {
                const current = state.taskActivity[threadId];

                if (current === 'paused' && activityState === 'running') {
                    return state;
                }

                return {
                    taskActivity: {...state.taskActivity, [threadId]: activityState},
                };
            }),
        setCurrentTaskId: (id) => set({currentTaskId: id}),
        setDraftLlmSelection: (provider, model) =>
            set({draftLlmSelection: provider == null && model == null ? null : {model, provider}}),
        setSearchTerm: (term) => set({searchTerm: term}),
        setTaskLlmSelection: (taskId, provider, model) =>
            set((state) => ({
                taskLlmSelections: {
                    ...state.taskLlmSelections,
                    [taskId]: {model, provider},
                },
            })),
        taskActivity: {},
        taskLlmSelections: {},
        titleGenerationFailures: {},
    }))
);

export const useAiHubTasksStore = aiHubTasksStore;
