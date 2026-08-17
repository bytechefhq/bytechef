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
    | {id: string; kind: 'codeWorkflow'; language: string; name: string; projectId: string}
    | {aiAgentId: string; id: string; kind: 'aiAgent'; name: string};

/**
 * Per-chat snapshot of the tabs view. Keyed by `chatId` in
 * {@link AiHubTabsStateI.snapshotsByChatId} so that returning to a previous chat
 * restores the exact tabs the user had open — AND whether the resource (right) panel was open —
 * instead of inheriting that state from whatever chat was most recently active. Persisted via the
 * `persist` middleware below so a page refresh restores the same per-chat view.
 *
 * The right-panel open/closed state IS snapshotted per chat ({@link rightPanelOpen}): switching back
 * to a chat restores the panel exactly as the user left it for that chat, and a refresh restores it
 * too. (This was previously a single global field forced closed on every switch; it is now per chat by
 * design — the user explicitly wants each chat to remember its own panel state.)
 */
interface ChatTabsSnapshotI {
    activeTabId: string | undefined;
    openTabs: AiHubTabType[];
    rightPanelOpen: boolean;
}

interface AiHubTabsStateI {
    /** The chat whose tabs are currently mirrored to {@link openTabs} / {@link activeTabId}.
     * `undefined` means home view. Updated via {@link setActiveChatId}, which is responsible for
     * snapshotting the previous chat's tabs and restoring the new chat's tabs. */
    activeChatId: number | undefined;
    activeTabId: string | undefined;
    openTabs: AiHubTabType[];
    /** Whether the resource/right panel is shown for the ACTIVE chat. Snapshotted per chat by
     * {@link setActiveChatId} (into {@link snapshotsByChatId}) and restored on every switch, so each chat
     * remembers its own panel state. Persisted across reloads, so a refresh restores it too. */
    rightPanelOpen: boolean;
    snapshotsByChatId: Record<number, ChatTabsSnapshotI>;
    /** Whether the AI Hub Chats sidebar is hidden (`true`) or docked open (`false`). Driven only by the
     * user's toggle in the panel header (AiHubChatsSidebarToggle) — like the sidebar toggle on every
     * other page — and independent of the resource (right) panel and of chat switches. Transient: never
     * persisted, so a reload starts with the sidebar open, same as other pages. */
    chatsSidebarCollapsed: boolean;
    /** Transient hover-preview ("peek") of the Chats sidebar while it's hidden. Set true when the pointer
     * rests on the header toggle and false when it leaves the previewed sidebar, so the full sidebar
     * floats over the content as an overlay (no layout reflow) and slides back out on mouse-out. Clicking
     * the sidebar header's pin control promotes the peek to a docked-open sidebar
     * ({@link chatsSidebarCollapsed} = false). Never persisted. */
    chatsSidebarPeeking: boolean;

    closeTab: (tabId: string) => void;
    openAiAgentTab: (aiAgentId: string, name: string) => string;
    openCodeWorkflowTab: (projectId: string, language: string, name: string) => string;
    openCustomComponentTab: (customComponentId: string, name: string) => string;
    openDataTableTab: (dataTableId: string, name: string) => string;
    openFileTab: (fileId: string, name: string) => string;
    openKnowledgeBaseTab: (knowledgeBaseId: string, name: string) => string;
    openSkillTab: (skillId: string, name: string) => string;
    openWorkflowExecutionTab: (workflowExecutionId: number, name: string) => string;
    openWorkflowTab: (workflowId: string, projectId: string, projectWorkflowId: number, name: string) => string;
    reset: () => void;
    setActiveChatId: (chatId: number | undefined) => void;
    setActiveTab: (tabId: string) => void;
    setRightPanelOpen: (open: boolean) => void;
    setChatsSidebarCollapsed: (collapsed: boolean) => void;
    setChatsSidebarPeeking: (peeking: boolean) => void;
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
                activeChatId: undefined,
                activeTabId: undefined,
                openTabs: [],
                rightPanelOpen: false,
                snapshotsByChatId: {},
                chatsSidebarCollapsed: false,
                chatsSidebarPeeking: false,

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

                openAiAgentTab: (aiAgentId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab): tab is Extract<AiHubTabType, {kind: 'aiAgent'}> =>
                                tab.kind === 'aiAgent' && tab.aiAgentId === aiAgentId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {...state, activeTabId: existing.id, rightPanelOpen: true};
                        }

                        const newTab: AiHubTabType = {aiAgentId, id: getRandomId(), kind: 'aiAgent', name};

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
                        activeChatId: undefined,
                        activeTabId: undefined,
                        openTabs: [],
                        rightPanelOpen: false,
                        snapshotsByChatId: {},
                        chatsSidebarCollapsed: false,
                        chatsSidebarPeeking: false,
                    }),

                /*
                 * Snapshot/restore the tabs view as the active chat changes.
                 *
                 * When the user switches chats (sidebar click, deep-link nav, auto-create on first
                 * message), this method is the single integration point AiHub.tsx calls. It:
                 *
                 *   1. Captures the current view (`openTabs`, `activeTabId`, `rightPanelOpen`) into
                 *      `snapshotsByChatId[previousActiveChatId]` so the tabs the user had open
                 *      for that chat survive the swap. Skipped when the previous active chat is
                 *      `undefined` (home view) — home doesn't carry per-chat tabs.
                 *
                 *   2. Loads the snapshot for the new chat if one exists, otherwise resets the active
                 *      view to empty (no tabs, panel closed). The empty case is the first time the user opens
                 *      a chat in this session — they expect the resource panel to start clean, not to
                 *      inherit the previous chat's tabs.
                 *
                 *   3. Treats no-op transitions (same chatId in and out) as a true no-op so a stray
                 *      effect re-fire doesn't clobber tabs the user just opened.
                 *
                 * Snapshots persist across browser reloads via the {@code persist} middleware below — the
                 * `snapshotsByChatId` field is written to `localStorage` (key
                 * `bytechef.ai-hub.tabs`) on every change and rehydrated on store creation. On cold
                 * load with a deep-linked `/chats/:id`, `AiHub.tsx` calls
                 * `setActiveChatId(:id)`, which hits the existing snapshot lookup below and restores
                 * the previously open tabs for that chat — including whether the resource panel was open
                 * (the snapshot now carries {@link ChatTabsSnapshotI.rightPanelOpen}).
                 */
                setActiveChatId: (chatId) =>
                    set((state) => {
                        if (state.activeChatId === chatId) {
                            return state;
                        }

                        const nextSnapshots = {...state.snapshotsByChatId};

                        if (state.activeChatId != null) {
                            nextSnapshots[state.activeChatId] = {
                                activeTabId: state.activeTabId,
                                openTabs: state.openTabs,
                                rightPanelOpen: state.rightPanelOpen,
                            };
                        }

                        const restored = chatId != null ? nextSnapshots[chatId] : undefined;

                        // Home → chat hand-off: when the user is on the home view (no active
                        // chat), they can still open tabs by attaching artifacts in the composer
                        // (`@`-mention a file, plus-button menu pick, etc.). Sending a message auto-creates
                        // a chat and triggers this transition. The user's mental model is "the
                        // artifact I attached is part of the chat I just started", so we INHERIT
                        // the home-view tabs into the new chat instead of resetting to empty.
                        // Subsequent chat→chat switches still snapshot/restore as normal —
                        // this carry-over only fires on the special undefined→<convoId> initial transition.
                        const isHomeToChat = state.activeChatId == null && chatId != null;

                        if (restored == null && isHomeToChat) {
                            return {
                                ...state,
                                activeChatId: chatId,
                                snapshotsByChatId: nextSnapshots,
                                // openTabs / activeTabId retained from current state (carry-over of the
                                // home-view tabs). rightPanelOpen is likewise carried over via `...state`,
                                // so a panel the user opened on the home view stays open in the new chat.
                            };
                        }

                        return {
                            ...state,
                            activeChatId: chatId,
                            activeTabId: restored?.activeTabId,
                            openTabs: restored?.openTabs ?? [],
                            // Restore the resource panel exactly as the user left it for this chat. A chat
                            // with no snapshot yet (first open this session, or going home) lands closed.
                            // Per-chat by design: switching back to a chat brings its panel state with it,
                            // and a refresh restores it via the persisted snapshot.
                            rightPanelOpen: restored?.rightPanelOpen ?? false,
                            snapshotsByChatId: nextSnapshots,
                            // `chatsSidebarCollapsed` / `chatsSidebarPeeking` are deliberately left alone:
                            // the sidebar is a page-level control (open/closed by the header toggle), not
                            // per-chat state, so switching chats must not snap it open or shut.
                        };
                    }),

                setActiveTab: (tabId) =>
                    set((state) => {
                        if (!state.openTabs.some((tab) => tab.id === tabId)) {
                            return state;
                        }

                        return {...state, activeTabId: tabId};
                    }),

                // Independent of the Chats sidebar: opening the resource panel no longer collapses the
                // sidebar (the header toggle is always available to close it by hand), and closing it
                // no longer resets the sidebar.
                setRightPanelOpen: (open) => set((state) => ({...state, rightPanelOpen: open})),

                setChatsSidebarCollapsed: (collapsed) =>
                    // Hiding or pinning the sidebar settles its resting state, so any in-progress hover
                    // preview is no longer meaningful — clear it so a stale peek can't linger on top.
                    set((state) => ({...state, chatsSidebarCollapsed: collapsed, chatsSidebarPeeking: false})),

                setChatsSidebarPeeking: (peeking) => set((state) => ({...state, chatsSidebarPeeking: peeking})),

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
                 * Only `snapshotsByChatId` is persisted across browser reloads. The active
                 * mirror fields (`activeChatId` / `activeTabId` / `openTabs` / `rightPanelOpen`)
                 * are deliberately re-derived on every mount via the `setActiveChatId` call in
                 * AiHub.tsx — driven by the URL's `:chatId` param. Persisting the active
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
                 * `activeChatId`) and the per-chat snapshots map. Snapshots are only
                 * written when the user transitions AWAY from a chat, so without persisting
                 * the active state, refreshing the page mid-chat drops the right-panel tabs
                 * the user just opened — they were never snapshotted because they belonged to the
                 * still-active chat.
                 *
                 * On rehydrate, `setActiveChatId(currentChatId)` runs from
                 * AiHub.tsx's effect. If the saved `activeChatId` matches the URL's
                 * chat id, it's a no-op (state preserved). If they differ (e.g., the user
                 * deep-linked to a different chat), the standard snapshot/restore path runs.
                 */
                partialize: (state) => ({
                    activeChatId: state.activeChatId,
                    activeTabId: state.activeTabId,
                    openTabs: state.openTabs,
                    rightPanelOpen: state.rightPanelOpen,
                    snapshotsByChatId: state.snapshotsByChatId,
                }),
                version: 3,
                migrate: () => ({
                    activeChatId: undefined,
                    activeTabId: undefined,
                    openTabs: [],
                    rightPanelOpen: false,
                    snapshotsByChatId: {},
                }),
            }
        )
    )
);

export const useAiHubTabsStore = aiHubTabsStore;
