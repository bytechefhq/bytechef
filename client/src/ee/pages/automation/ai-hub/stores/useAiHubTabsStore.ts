import {AssetFileViewerModeType} from '@/shared/components/asset-file-viewer/AssetFileViewer';
import {getRandomId} from '@/shared/util/random-utils';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

export interface AiHubAiAgentTabI {
    aiAgentId: string;
    id: string;
    kind: 'aiAgent';
    name: string;
}

export interface AiHubCodeWorkflowTabI {
    id: string;
    kind: 'codeWorkflow';
    language: string;
    name: string;
    projectId: string;
}

export interface AiHubCustomComponentTabI {
    customComponentId: string;
    id: string;
    kind: 'customComponent';
    name: string;
}

export type AiHubViewModeType = AssetFileViewerModeType;

export interface AiHubFileTabI {
    fileId: string;
    id: string;
    kind: 'file';
    name: string;
    viewMode: AiHubViewModeType;
}

export interface AiHubDataTableTabI {
    dataTableId: string;
    id: string;
    kind: 'dataTable';
    name: string;
}

export interface AiHubKnowledgeBaseTabI {
    id: string;
    kind: 'knowledgeBase';
    knowledgeBaseId: string;
    name: string;
}

export interface AiHubWorkflowExecutionTabI {
    id: string;
    kind: 'workflowExecution';
    name: string;
    workflowExecutionId: number;
}

export interface AiHubWorkflowTabI {
    id: string;
    kind: 'workflow';
    name: string;
    projectId: string;
    projectWorkflowId: number;
    workflowId: string;
}

export type AiHubTabType =
    | AiHubAiAgentTabI
    | AiHubCodeWorkflowTabI
    | AiHubCustomComponentTabI
    | AiHubDataTableTabI
    | AiHubFileTabI
    | AiHubKnowledgeBaseTabI
    | AiHubWorkflowExecutionTabI
    | AiHubWorkflowTabI;

type AiHubNamedResourceTabType = AiHubDataTableTabI | AiHubKnowledgeBaseTabI | AiHubWorkflowExecutionTabI;

export interface OpenAiAgentTabArgsI {
    aiAgentId: string;
    name: string;
}

export interface OpenCodeWorkflowTabArgsI {
    language: string;
    name: string;
    projectId: string;
}

export interface OpenCustomComponentTabArgsI {
    customComponentId: string;
    name: string;
}

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
    chatsSidebarPeeking: boolean;
    closeTab: (tabId: string) => void;
    /** Opens an AI Agent tab, or focuses and renames the existing tab of the same agent. */
    openAiAgentTab: (aiAgent: OpenAiAgentTabArgsI) => string;
    openCodeWorkflowTab: (codeWorkflow: OpenCodeWorkflowTabArgsI) => string;
    openCustomComponentTab: (customComponent: OpenCustomComponentTabArgsI) => string;
    openDataTableTab: (dataTableId: string, name: string) => string;
    openFileTab: (fileId: string, name: string) => string;
    openKnowledgeBaseTab: (knowledgeBaseId: string, name: string) => string;
    openTabs: AiHubTabType[];
    openWorkflowExecutionTab: (workflowExecutionId: number, name: string) => string;
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
    setChatsSidebarPeeking: (peeking: boolean) => void;
    setRightPanelOpen: (open: boolean) => void;
    setViewMode: (tabId: string, viewMode: AiHubViewModeType) => void;
    snapshotsByChatId: Record<number, ChatTabsSnapshotI>;
}

const EDITOR_EXTENSIONS = new Set([
    'css',
    'csv',
    'env',
    'go',
    'ini',
    'java',
    'js',
    'json',
    'jsx',
    'py',
    'rb',
    'rs',
    'scss',
    'sh',
    'sql',
    'toml',
    'ts',
    'tsx',
    'txt',
    'xml',
    'yaml',
    'yml',
]);

export const inferDefaultViewMode = (name: string): AiHubViewModeType => {
    const dotIndex = name.lastIndexOf('.');

    if (dotIndex < 0) {
        return 'editor';
    }

    const extension = name.slice(dotIndex + 1).toLowerCase();

    return EDITOR_EXTENSIONS.has(extension) ? 'editor' : 'preview';
};

export const getTabGenericId = (tab: AiHubTabType): string => {
    switch (tab.kind) {
        case 'aiAgent':
            return tab.aiAgentId;
        case 'codeWorkflow':
            return tab.projectId;
        case 'customComponent':
            return tab.customComponentId;
        case 'dataTable':
            return tab.dataTableId;
        case 'file':
            return tab.fileId;
        case 'knowledgeBase':
            return tab.knowledgeBaseId;
        case 'workflowExecution':
            return String(tab.workflowExecutionId);
        case 'workflow':
            return tab.workflowId;
    }
};

const openNamedResourceTab = (
    state: AiHubTabsStateI,
    newTab: AiHubNamedResourceTabType
): {nextState: AiHubTabsStateI; tabId: string} => {
    const existing = state.openTabs.find(
        (tab) => tab.kind === newTab.kind && getTabGenericId(tab) === getTabGenericId(newTab)
    );

    if (existing) {
        return {
            nextState: {
                ...state,
                activeTabId: existing.id,
                openTabs: state.openTabs.map((tab) => (tab.id === existing.id ? {...tab, name: newTab.name} : tab)),
                rightPanelOpen: true,
            },
            tabId: existing.id,
        };
    }

    return {
        nextState: {
            ...state,
            activeTabId: newTab.id,
            openTabs: [...state.openTabs, newTab],
            rightPanelOpen: true,
        },
        tabId: newTab.id,
    };
};

export const attachTab = (open: () => string): {ownsTab: boolean; tabId: string} => {
    const openTabCountBefore = aiHubTabsStore.getState().openTabs.length;

    const tabId = open();

    const ownsTab = aiHubTabsStore.getState().openTabs.length > openTabCountBefore;

    return {ownsTab, tabId};
};

export const aiHubTabsStore = create<AiHubTabsStateI>()(
    devtools(
        persist(
            (set) => ({
                activeChatId: undefined,
                activeTabId: undefined,
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

                        return {
                            ...state,
                            activeTabId,
                            openTabs,
                        };
                    }),
                openAiAgentTab: ({aiAgentId, name}) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab) => tab.kind === 'aiAgent' && tab.aiAgentId === aiAgentId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) => (tab.id === existing.id ? {...tab, name} : tab)),
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubAiAgentTabI = {
                            aiAgentId,
                            id: getRandomId(),
                            kind: 'aiAgent',
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
                openCodeWorkflowTab: ({language, name, projectId}) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab) => tab.kind === 'codeWorkflow' && tab.projectId === projectId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) =>
                                    tab.id === existing.id && tab.kind === 'codeWorkflow'
                                        ? {...tab, language, name}
                                        : tab
                                ),
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubCodeWorkflowTabI = {
                            id: getRandomId(),
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
                openCustomComponentTab: ({customComponentId, name}) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab) => tab.kind === 'customComponent' && tab.customComponentId === customComponentId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                openTabs: state.openTabs.map((tab) => (tab.id === existing.id ? {...tab, name} : tab)),
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubCustomComponentTabI = {
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
                openDataTableTab: (dataTableId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const {nextState, tabId} = openNamedResourceTab(state, {
                            dataTableId,
                            id: getRandomId(),
                            kind: 'dataTable',
                            name,
                        });

                        tabIdToReturn = tabId;

                        return nextState;
                    });

                    return tabIdToReturn;
                },
                openFileTab: (fileId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find((tab) => tab.kind === 'file' && tab.fileId === fileId);

                        if (existing) {
                            tabIdToReturn = existing.id;

                            return {
                                ...state,
                                activeTabId: existing.id,
                                rightPanelOpen: true,
                            };
                        }

                        const newTab: AiHubFileTabI = {
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
                openKnowledgeBaseTab: (knowledgeBaseId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const {nextState, tabId} = openNamedResourceTab(state, {
                            id: getRandomId(),
                            kind: 'knowledgeBase',
                            knowledgeBaseId,
                            name,
                        });

                        tabIdToReturn = tabId;

                        return nextState;
                    });

                    return tabIdToReturn;
                },
                openTabs: [],
                openWorkflowExecutionTab: (workflowExecutionId, name) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const {nextState, tabId} = openNamedResourceTab(state, {
                            id: getRandomId(),
                            kind: 'workflowExecution',
                            name,
                            workflowExecutionId,
                        });

                        tabIdToReturn = tabId;

                        return nextState;
                    });

                    return tabIdToReturn;
                },
                openWorkflowTab: ({name, projectId, projectWorkflowId, workflowId}) => {
                    let tabIdToReturn = '';

                    set((state) => {
                        const existing = state.openTabs.find(
                            (tab) => tab.kind === 'workflow' && tab.projectId === projectId
                        );

                        if (existing) {
                            tabIdToReturn = existing.id;

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

                        const newTab: AiHubWorkflowTabI = {
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
                        chatsSidebarPeeking: false,
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
                setChatsSidebarCollapsed: (collapsed) =>
                    set((state) => ({...state, chatsSidebarCollapsed: collapsed, chatsSidebarPeeking: false})),
                setChatsSidebarPeeking: (peeking) => set((state) => ({...state, chatsSidebarPeeking: peeking})),
                setRightPanelOpen: (open) => set((state) => ({...state, rightPanelOpen: open})),
                setViewMode: (tabId, viewMode) =>
                    set((state) => ({
                        ...state,
                        openTabs: state.openTabs.map((tab) =>
                            tab.id === tabId && tab.kind === 'file' ? {...tab, viewMode} : tab
                        ),
                    })),
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
