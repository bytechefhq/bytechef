/* eslint-disable sort-keys */
import {getRandomId} from '@/shared/util/random-utils';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export type AiHubViewModeType = 'editor' | 'preview' | 'split';

export type AiHubTabType =
    | {id: string; kind: 'file'; fileId: string; name: string; viewMode: AiHubViewModeType}
    | {
          id: string;
          kind: 'workflow';
          workflowId: string;
          projectId: string;
          projectWorkflowId: number;
          name: string;
      }
    | {id: string; kind: 'dataTable'; dataTableId: string; name: string}
    | {id: string; kind: 'workflowExecution'; workflowExecutionId: number; name: string}
    | {id: string; kind: 'knowledgeBase'; knowledgeBaseId: string; name: string}
    | {id: string; kind: 'skill'; name: string; skillId: string}
    | {customComponentId: string; id: string; kind: 'customComponent'; name: string}
    | {id: string; kind: 'codeWorkflow'; language: string; name: string; projectId: string};

/**
 * Per-task snapshot of the tabs view. Keyed by `taskId` in
 * {@link AiHubTabsStateI.snapshotsByTaskId} so that returning to a previous task
 * restores the exact tabs the user had open — AND whether the resource (right) panel was open —
 * instead of inheriting that state from whatever task was most recently active. Persisted via the
 * `persist` middleware below so a page refresh restores the same per-task view.
 *
 * The right-panel open/closed state IS snapshotted per task ({@link rightPanelOpen}): switching back
 * to a task restores the panel exactly as the user left it for that task, and a refresh restores it
 * too. (This was previously a single global field forced closed on every switch; it is now per task by
 * design — the user explicitly wants each task to remember its own panel state.)
 */
interface TaskTabsSnapshotI {
    activeTabId: string | undefined;
    openTabs: AiHubTabType[];
    rightPanelOpen: boolean;
}

interface AiHubTabsStateI {
    /** The task whose tabs are currently mirrored to {@link openTabs} / {@link activeTabId}.
     * `undefined` means home view. Updated via {@link setActiveTaskId}, which is responsible for
     * snapshotting the previous task's tabs and restoring the new task's tabs. */
    activeTaskId: number | undefined;
    activeTabId: string | undefined;
    openTabs: AiHubTabType[];
    /** Whether the resource/right panel is shown for the ACTIVE task. Snapshotted per task by
     * {@link setActiveTaskId} (into {@link snapshotsByTaskId}) and restored on every switch, so each task
     * remembers its own panel state. Persisted across reloads, so a refresh restores it too. */
    rightPanelOpen: boolean;
    snapshotsByTaskId: Record<number, TaskTabsSnapshotI>;
    /** Whether the AI Hub Tasks sidebar is shown as a thin rail (`true`) rather than its full
     * form. Only meaningful while the resource panel is open — that's the only layout state
     * where the sidebar would otherwise be hidden entirely. Transient: never persisted. */
    tasksSidebarCollapsed: boolean;
    /** Transient hover-preview ("peek") of the Tasks sidebar while it's collapsed to the rail.
     * Set true when the pointer enters the rail and false when it leaves the previewed sidebar, so the
     * full sidebar floats over the content as an overlay (no layout reflow) and collapses back to the
     * rail on mouse-out. Clicking the header's pin control promotes the peek to a pinned-open sidebar
     * ({@link tasksSidebarCollapsed} = false). Never persisted. */
    tasksSidebarPeeking: boolean;

    closeTab: (tabId: string) => void;
    openCodeWorkflowTab: (projectId: string, language: string, name: string) => string;
    openCustomComponentTab: (customComponentId: string, name: string) => string;
    openDataTableTab: (dataTableId: string, name: string) => string;
    openFileTab: (fileId: string, name: string) => string;
    openKnowledgeBaseTab: (knowledgeBaseId: string, name: string) => string;
    openSkillTab: (skillId: string, name: string) => string;
    openWorkflowExecutionTab: (workflowExecutionId: number, name: string) => string;
    openWorkflowTab: (workflowId: string, projectId: string, projectWorkflowId: number, name: string) => string;
    reset: () => void;
    setActiveTaskId: (taskId: number | undefined) => void;
    setActiveTab: (tabId: string) => void;
    setRightPanelOpen: (open: boolean) => void;
    setTasksSidebarCollapsed: (collapsed: boolean) => void;
    setTasksSidebarPeeking: (peeking: boolean) => void;
    setViewMode: (tabId: string, mode: AiHubViewModeType) => void;
}

const EDITOR_EXTENSIONS = new Set([
    'txt',
    'csv',
    'json',
    'yaml',
    'yml',
    'java',
    'js',
    'jsx',
    'ts',
    'tsx',
    'py',
    'rb',
    'go',
    'rs',
    'sh',
    'sql',
    'css',
    'scss',
    'xml',
    'toml',
    'ini',
    'env',
]);

const PREVIEW_EXTENSIONS = new Set(['md', 'markdown', 'html', 'htm']);

export function inferDefaultViewMode(name: string): AiHubViewModeType {
    const dotIndex = name.lastIndexOf('.');

    if (dotIndex < 0) {
        return 'editor';
    }

    const extension = name.slice(dotIndex + 1).toLowerCase();

    if (PREVIEW_EXTENSIONS.has(extension)) {
        return 'preview';
    }

    if (EDITOR_EXTENSIONS.has(extension)) {
        return 'editor';
    }

    return 'preview';
}

export const aiHubTabsStore = create<AiHubTabsStateI>()(
    devtools(
        persist(
            (set) => ({
                activeTaskId: undefined,
                activeTabId: undefined,
                openTabs: [],
                rightPanelOpen: false,
                snapshotsByTaskId: {},
                tasksSidebarCollapsed: true,
                tasksSidebarPeeking: false,

                closeTab: (tabId) =>
                    set((state) => {
                        const closingIndex = state.openTabs.findIndex((tab) => tab.id === tabId);

                        if (closingIndex < 0) {
                            return state;
                        }

                        const openTabs = state.openTabs.filter((tab) => tab.id !== tabId);

                        let activeTabId = state.activeTabId;

                        if (state.activeTabId === tabId) {
                            if (openTabs.length === 0) {
                                activeTabId = undefined;
                            } else if (closingIndex >= openTabs.length) {
                                activeTabId = openTabs[openTabs.length - 1]!.id;
                            } else {
                                activeTabId = openTabs[closingIndex]!.id;
                            }
                        }

                        return {...state, activeTabId, openTabs};
                    }),

                openCustomComponentTab: (customComponentId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'customComponent'}> =>
                                tab.kind === 'customComponent' && tab.customComponentId === customComponentId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {...state, activeTabId: existing.id, rightPanelOpen: true};
                        }

                        const newTab: AiHubTabType = {
                            customComponentId,
                            id: getRandomId(),
                            kind: 'customComponent',
                            name,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openCodeWorkflowTab: (projectId, language, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'codeWorkflow'}> =>
                                tab.kind === 'codeWorkflow' && tab.projectId === projectId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {...state, activeTabId: existing.id, rightPanelOpen: true};
                        }

                        const newTab: AiHubTabType = {
                            id: `codeWorkflow-${projectId}`,
                            kind: 'codeWorkflow',
                            language,
                            name,
                            projectId,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openFileTab: (fileId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'file'}> =>
                                tab.kind === 'file' && tab.fileId === fileId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            fileId,
                            id: getRandomId(),
                            kind: 'file',
                            name,
                            viewMode: inferDefaultViewMode(name),
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openWorkflowTab: (workflowId, projectId, projectWorkflowId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'workflow'}> =>
                                tab.kind === 'workflow' && tab.projectId === projectId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            // A workflow tab is project-scoped: opening another workflow from the same
                            // project (sidebar/composer pick or the in-tab selector) re-points THIS tab's
                            // selected workflow instead of spawning a duplicate. projectId/tab id stay put.
                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) =>
                                    tab.id === existing.id && tab.kind === 'workflow'
                                        ? {...tab, name, projectWorkflowId, workflowId}
                                        : tab
                                ),
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            id: getRandomId(),
                            kind: 'workflow',
                            name,
                            projectId,
                            projectWorkflowId,
                            workflowId,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openWorkflowExecutionTab: (workflowExecutionId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'workflowExecution'}> =>
                                tab.kind === 'workflowExecution' && tab.workflowExecutionId === workflowExecutionId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            id: getRandomId(),
                            kind: 'workflowExecution',
                            name,
                            workflowExecutionId,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openDataTableTab: (dataTableId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'dataTable'}> =>
                                tab.kind === 'dataTable' && tab.dataTableId === dataTableId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            dataTableId,
                            id: getRandomId(),
                            kind: 'dataTable',
                            name,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openKnowledgeBaseTab: (knowledgeBaseId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'knowledgeBase'}> =>
                                tab.kind === 'knowledgeBase' && tab.knowledgeBaseId === knowledgeBaseId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubTabType = {
                            id: getRandomId(),
                            kind: 'knowledgeBase',
                            knowledgeBaseId,
                            name,
                        };

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                openSkillTab: (skillId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'skill'}> =>
                                tab.kind === 'skill' && tab.skillId === skillId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {...state, activeTabId: existing.id, rightPanelOpen: true};
                        }

                        const newTab: AiHubTabType = {id: getRandomId(), kind: 'skill', name, skillId};

                        tabIdToReturn = newTab.id;

                        return {
                            ...state,
                            activeTabId: newTab.id,
                            openTabs: [...state.openTabs, newTab],
                            rightPanelOpen: true,
                        };
                    });

                    return tabIdToReturn;
                },

                reset: () =>
                    set({
                        activeTaskId: undefined,
                        activeTabId: undefined,
                        openTabs: [],
                        rightPanelOpen: false,
                        snapshotsByTaskId: {},
                        tasksSidebarCollapsed: true,
                        tasksSidebarPeeking: false,
                    }),

                /*
                 * Snapshot/restore the tabs view as the active task changes.
                 *
                 * When the user switches tasks (sidebar click, deep-link nav, auto-create on first
                 * message), this method is the single integration point AiHub.tsx calls. It:
                 *
                 *   1. Captures the current view (`openTabs`, `activeTabId`, `rightPanelOpen`) into
                 *      `snapshotsByTaskId[previousActiveTaskId]` so the tabs the user had open
                 *      for that task survive the swap. Skipped when the previous active task is
                 *      `undefined` (home view) — home doesn't carry per-task tabs.
                 *
                 *   2. Loads the snapshot for the new task if one exists, otherwise resets the active
                 *      view to empty (no tabs, panel closed). The empty case is the first time the user opens
                 *      a task in this session — they expect the resource panel to start clean, not to
                 *      inherit the previous task's tabs.
                 *
                 *   3. Treats no-op transitions (same taskId in and out) as a true no-op so a stray
                 *      effect re-fire doesn't clobber tabs the user just opened.
                 *
                 * Snapshots persist across browser reloads via the {@code persist} middleware below — the
                 * `snapshotsByTaskId` field is written to `localStorage` (key
                 * `bytechef.ai-hub.tabs`) on every change and rehydrated on store creation. On cold
                 * load with a deep-linked `/tasks/:id`, `AiHub.tsx` calls
                 * `setActiveTaskId(:id)`, which hits the existing snapshot lookup below and restores
                 * the previously open tabs for that task — including whether the resource panel was open
                 * (the snapshot now carries {@link TaskTabsSnapshotI.rightPanelOpen}).
                 */
                setActiveTaskId: (taskId) =>
                    set((state) => {
                        if (state.activeTaskId === taskId) {
                            return state;
                        }

                        const nextSnapshots = {...state.snapshotsByTaskId};

                        if (state.activeTaskId != null) {
                            nextSnapshots[state.activeTaskId] = {
                                activeTabId: state.activeTabId,
                                openTabs: state.openTabs,
                                rightPanelOpen: state.rightPanelOpen,
                            };
                        }

                        const restored = taskId != null ? nextSnapshots[taskId] : undefined;

                        // Home → task hand-off: when the user is on the home view (no active
                        // task), they can still open tabs by attaching artifacts in the composer
                        // (`@`-mention a file, plus-button menu pick, etc.). Sending a message auto-creates
                        // a task and triggers this transition. The user's mental model is "the
                        // artifact I attached is part of the task I just started", so we INHERIT
                        // the home-view tabs into the new task instead of resetting to empty.
                        // Subsequent task→task switches still snapshot/restore as normal —
                        // this carry-over only fires on the special undefined→<convoId> initial transition.
                        const isHomeToTask = state.activeTaskId == null && taskId != null;

                        if (restored == null && isHomeToTask) {
                            return {
                                ...state,
                                activeTaskId: taskId,
                                snapshotsByTaskId: nextSnapshots,
                                // openTabs / activeTabId retained from current state (carry-over of the
                                // home-view tabs). rightPanelOpen is likewise carried over via `...state`,
                                // so a panel the user opened on the home view stays open in the new task.
                                // Rail collapsed: see the comment on the default-branch return below.
                                tasksSidebarCollapsed: true,
                                tasksSidebarPeeking: false,
                            };
                        }

                        return {
                            ...state,
                            activeTaskId: taskId,
                            activeTabId: restored?.activeTabId,
                            openTabs: restored?.openTabs ?? [],
                            // Restore the resource panel exactly as the user left it for this task. A task
                            // with no snapshot yet (first open this session, or going home) lands closed.
                            // Per-task by design: switching back to a task brings its panel state with it,
                            // and a refresh restores it via the persisted snapshot.
                            rightPanelOpen: restored?.rightPanelOpen ?? false,
                            snapshotsByTaskId: nextSnapshots,
                            // Every task switch resets the Tasks sidebar to its collapsed (rail) default.
                            // The flag is global, but the sidebar is only clickable while VISIBLE — and
                            // with a resource panel open, "visible" means the user expanded it
                            // (`tasksSidebarCollapsed: false`). Without this reset, switching to another
                            // task whose snapshot also has the resource panel open would inherit that
                            // stale `false` and leave the full sidebar covering the new task's panels.
                            tasksSidebarCollapsed: true,
                            tasksSidebarPeeking: false,
                        };
                    }),

                setActiveTab: (tabId) =>
                    set((state) => {
                        if (!state.openTabs.some((tab) => tab.id === tabId)) {
                            return state;
                        }

                        return {...state, activeTabId: tabId};
                    }),

                setRightPanelOpen: (open) =>
                    set((state) => ({
                        ...state,
                        rightPanelOpen: open,
                        // Closing the resource panel returns the layout to the full-width sidebar.
                        // Reset the rail flag to collapsed so the NEXT resource-panel open starts as a
                        // thin rail again (the minimal, out-of-the-way default) rather than inheriting
                        // a stale expanded state from an earlier session of the panel.
                        tasksSidebarCollapsed: open ? state.tasksSidebarCollapsed : true,
                        // A peek only exists while the rail is showing; closing the panel removes the rail,
                        // so clear any in-progress preview.
                        tasksSidebarPeeking: open ? state.tasksSidebarPeeking : false,
                    })),

                setTasksSidebarCollapsed: (collapsed) =>
                    // Collapsing or pinning the sidebar settles its resting state, so any in-progress hover
                    // preview is no longer meaningful — clear it so a stale peek can't linger on top.
                    set((state) => ({...state, tasksSidebarCollapsed: collapsed, tasksSidebarPeeking: false})),

                setTasksSidebarPeeking: (peeking) => set((state) => ({...state, tasksSidebarPeeking: peeking})),

                setViewMode: (tabId, mode) =>
                    set((state) => ({
                        ...state,
                        openTabs: state.openTabs.map((tab) =>
                            tab.id === tabId && tab.kind === 'file' ? {...tab, viewMode: mode} : tab
                        ),
                    })),
            }),
            {
                /*
                 * Only `snapshotsByTaskId` is persisted across browser reloads. The active
                 * mirror fields (`activeTaskId` / `activeTabId` / `openTabs` / `rightPanelOpen`)
                 * are deliberately re-derived on every mount via the `setActiveTaskId` call in
                 * AiHub.tsx — driven by the URL's `:taskId` param. Persisting the active
                 * mirror too would create a brief visual flash on cold-load: the persisted active values
                 * would render before the URL→store sync effect catches up and replaces them with the
                 * URL-derived snapshot.
                 *
                 * Bumping `version` on a breaking change to the snapshot shape (new tab kind, renamed
                 * field) is enough to invalidate stale local entries — `migrate` is intentionally
                 * conservative: returns an empty snapshot map on any version mismatch so users with
                 * stale data don't see broken tabs after a deploy.
                 */
                name: 'bytechef.ai-hub.tabs',
                /*
                 * Persist BOTH the active state (`openTabs`, `activeTabId`, `rightPanelOpen`,
                 * `activeTaskId`) and the per-task snapshots map. Snapshots are only
                 * written when the user transitions AWAY from a task, so without persisting
                 * the active state, refreshing the page mid-task drops the right-panel tabs
                 * the user just opened — they were never snapshotted because they belonged to the
                 * still-active task.
                 *
                 * On rehydrate, `setActiveTaskId(currentTaskId)` runs from
                 * AiHub.tsx's effect. If the saved `activeTaskId` matches the URL's
                 * task id, it's a no-op (state preserved). If they differ (e.g., the user
                 * deep-linked to a different task), the standard snapshot/restore path runs.
                 */
                partialize: (state) => ({
                    activeTaskId: state.activeTaskId,
                    activeTabId: state.activeTabId,
                    openTabs: state.openTabs,
                    rightPanelOpen: state.rightPanelOpen,
                    snapshotsByTaskId: state.snapshotsByTaskId,
                }),
                version: 2,
                migrate: () => ({
                    activeTaskId: undefined,
                    activeTabId: undefined,
                    openTabs: [],
                    rightPanelOpen: false,
                    snapshotsByTaskId: {},
                }),
            }
        )
    )
);

export const useAiHubTabsStore = aiHubTabsStore;
