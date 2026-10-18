import {getRandomId} from '@/shared/util/random-utils';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export interface AiHubWorkflowTabI {
    id: string;
    kind: 'workflow';
    name: string;
    projectId: string;
    projectWorkflowId: number;
    workflowId: string;
}

export type AiHubTabType = AiHubWorkflowTabI;

export interface OpenWorkflowTabArgsI {
    name: string;
    projectId: string;
    projectWorkflowId: number;
    workflowId: string;
}

/**
 * Per-chat snapshot of the tabs view, keyed by chat id in {@link AiHubTabsStateI.snapshotsByChatId}, so returning
 * to a chat restores the tabs it had open and whether its resource panel was open.
 */
interface ChatTabsSnapshotI {
    activeTabId: string | undefined;
    openTabs: AiHubTabType[];
    rightPanelOpen: boolean;
}

interface AiHubTabsStateI {
    /** The chat whose tabs are mirrored to {@link openTabs}; `undefined` is the home view. */
    activeChatId: number | undefined;
    activeTabId: string | undefined;
    /** Whether the Chats sidebar is hidden. Page-level and transient: never persisted, untouched by chat switches. */
    chatsSidebarCollapsed: boolean;
    closeTab: (tabId: string) => void;
    openTabs: AiHubTabType[];
    /**
     * Opens a workflow tab, or focuses the existing one. A workflow tab is project-scoped: opening another workflow of
     * a project that already has a tab re-points that tab instead of adding a second one.
     */
    openWorkflowTab: (workflow: OpenWorkflowTabArgsI) => string;
    reset: () => void;
    /** Whether the resource panel is shown for the active chat; snapshotted per chat. */
    rightPanelOpen: boolean;
    setActiveChatId: (chatId: number | undefined) => void;
    setActiveTab: (tabId: string) => void;
    setChatsSidebarCollapsed: (collapsed: boolean) => void;
    setRightPanelOpen: (open: boolean) => void;
    snapshotsByChatId: Record<number, ChatTabsSnapshotI>;
}

/**
 * The id a tab carries for its underlying resource, independent of the tab's own (random) id.
 */
export const getTabGenericId = (tab: AiHubTabType): string => tab.workflowId;

export const aiHubTabsStore = create<AiHubTabsStateI>()(
    devtools(
        persist(
            (set) => ({
                activeChatId: undefined,
                activeTabId: undefined,
                chatsSidebarCollapsed: false,
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

                        return {
                            ...state,
                            activeTabId,
                            openTabs,
                        };
                    }),
                openTabs: [],
                openWorkflowTab: ({name, projectId, projectWorkflowId, workflowId}) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find((tab) => tab.projectId === projectId);

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) =>
                                    tab.id === existing.id ? {...tab, name, projectWorkflowId, workflowId} : tab
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
                reset: () =>
                    set({
                        activeChatId: undefined,
                        activeTabId: undefined,
                        chatsSidebarCollapsed: false,
                        openTabs: [],
                        rightPanelOpen: false,
                        snapshotsByChatId: {},
                    }),
                rightPanelOpen: false,
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

                        const isHomeToChat = state.activeChatId == null && chatId != null;

                        if (restored == null && isHomeToChat) {
                            return {
                                ...state,
                                activeChatId: chatId,
                                rightPanelOpen: state.openTabs.length > 0 && state.rightPanelOpen,
                                snapshotsByChatId: nextSnapshots,
                            };
                        }

                        return {
                            ...state,
                            activeChatId: chatId,
                            activeTabId: restored?.activeTabId,
                            openTabs: restored?.openTabs ?? [],
                            rightPanelOpen: restored?.rightPanelOpen ?? false,
                            snapshotsByChatId: nextSnapshots,
                        };
                    }),
                setActiveTab: (tabId) =>
                    set((state) => {
                        if (!state.openTabs.some((tab) => tab.id === tabId)) {
                            return state;
                        }

                        return {...state, activeTabId: tabId};
                    }),
                setChatsSidebarCollapsed: (collapsed) => set((state) => ({...state, chatsSidebarCollapsed: collapsed})),
                setRightPanelOpen: (open) => set((state) => ({...state, rightPanelOpen: open})),
                snapshotsByChatId: {},
            }),
            {
                migrate: () => ({
                    activeChatId: undefined,
                    activeTabId: undefined,
                    openTabs: [],
                    rightPanelOpen: false,
                    snapshotsByChatId: {},
                }),
                name: 'bytechef.ai-hub.tabs',
                partialize: (state) => ({
                    activeChatId: state.activeChatId,
                    activeTabId: state.activeTabId,
                    openTabs: state.openTabs,
                    rightPanelOpen: state.rightPanelOpen,
                    snapshotsByChatId: state.snapshotsByChatId,
                }),
                version: 1,
            }
        )
    )
);

export const useAiHubTabsStore = aiHubTabsStore;
